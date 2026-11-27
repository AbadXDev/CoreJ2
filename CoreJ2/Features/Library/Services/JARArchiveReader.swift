import Foundation
import zlib

/// Errors produced while reading entries from a JAR/ZIP archive.
enum JARArchiveError: Error, Equatable {
    case unableToReadArchive
}

/// Reads named entries from a JAR (ZIP) archive.
///
/// Uses the central directory so entries that store sizes only in a data
/// descriptor (general-purpose bit 3 — common for MIDlet `icon.png`) still resolve.
enum JARArchiveReader {

    // MARK: - Public

    /// Returns the uncompressed bytes for `path`, or `nil` if the entry is absent.
    static func data(forEntryNamed path: String, inArchiveAt url: URL) throws -> Data? {
        let archiveData: Data
        do {
            archiveData = try Data(contentsOf: url)
        } catch {
            throw JARArchiveError.unableToReadArchive
        }
        return try data(forEntryNamed: path, in: archiveData)
    }

    /// Returns the uncompressed bytes for `path`, or `nil` if the entry is absent.
    static func data(forEntryNamed path: String, in zipData: Data) throws -> Data? {
        let normalizedPath = normalize(path).lowercased()
        guard let eocd = findEOCD(in: zipData) else {
            throw JARArchiveError.unableToReadArchive
        }

        var offset = eocd.centralDirectoryOffset
        for _ in 0..<eocd.totalEntries {
            guard offset + 46 <= zipData.count else {
                throw JARArchiveError.unableToReadArchive
            }
            let signature = try zipData.uint32LE(at: offset)
            guard signature == centralDirectorySignature else {
                throw JARArchiveError.unableToReadArchive
            }

            let compressionMethod = try zipData.uint16LE(at: offset + 10)
            let generalPurpose = try zipData.uint16LE(at: offset + 8)
            let compressedSize = Int(try zipData.uint32LE(at: offset + 20))
            let uncompressedSize = Int(try zipData.uint32LE(at: offset + 24))
            let fileNameLength = Int(try zipData.uint16LE(at: offset + 28))
            let extraFieldLength = Int(try zipData.uint16LE(at: offset + 30))
            let commentLength = Int(try zipData.uint16LE(at: offset + 32))
            let localHeaderOffset = Int(try zipData.uint32LE(at: offset + 42))

            let fileNameStart = offset + 46
            let fileNameEnd = fileNameStart + fileNameLength
            guard fileNameEnd <= zipData.count else {
                throw JARArchiveError.unableToReadArchive
            }
            let fileNameData = zipData.subdata(in: fileNameStart..<fileNameEnd)
            let fileName = String(data: fileNameData, encoding: .utf8)?
                .replacingOccurrences(of: "\\", with: "/")
                .lowercased()

            let nextOffset = fileNameEnd + extraFieldLength + commentLength
            defer { offset = nextOffset }

            guard fileName == normalizedPath || fileName == "/" + normalizedPath else {
                continue
            }

            return try readLocalEntry(
                in: zipData,
                localHeaderOffset: localHeaderOffset,
                compressionMethod: compressionMethod,
                compressedSize: compressedSize,
                uncompressedSize: uncompressedSize,
                usesDataDescriptor: (generalPurpose & 0x8) != 0
            )
        }

        return nil
    }

    // MARK: - Path Helpers

    static func normalize(_ path: String) -> String {
        var normalized = path
            .replacingOccurrences(of: "\\", with: "/")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        while normalized.hasPrefix("/") {
            normalized = String(normalized.dropFirst())
        }
        return normalized
    }

    // MARK: - Private

    private static let localFileHeaderSignature: UInt32 = 0x04034b50
    private static let centralDirectorySignature: UInt32 = 0x02014b50
    private static let endOfCentralDirectorySignature: UInt32 = 0x06054b50
    private static let storedMethod: UInt16 = 0
    private static let deflateMethod: UInt16 = 8

    private struct EOCD {
        let totalEntries: Int
        let centralDirectoryOffset: Int
    }

    private static func findEOCD(in zipData: Data) -> EOCD? {
        // EOCD is at least 22 bytes; comment may extend it. Scan backwards.
        let minSize = 22
        guard zipData.count >= minSize else { return nil }
        let start = max(0, zipData.count - minSize - 0xFFFF)
        var index = zipData.count - minSize
        while index >= start {
            if let sig = try? zipData.uint32LE(at: index), sig == endOfCentralDirectorySignature {
                guard let total = try? zipData.uint16LE(at: index + 10),
                      let offset = try? zipData.uint32LE(at: index + 16) else {
                    return nil
                }
                return EOCD(totalEntries: Int(total), centralDirectoryOffset: Int(offset))
            }
            if index == 0 { break }
            index -= 1
        }
        return nil
    }

    private static func readLocalEntry(
        in zipData: Data,
        localHeaderOffset: Int,
        compressionMethod: UInt16,
        compressedSize: Int,
        uncompressedSize: Int,
        usesDataDescriptor: Bool
    ) throws -> Data? {
        guard localHeaderOffset + 30 <= zipData.count else {
            throw JARArchiveError.unableToReadArchive
        }
        let signature = try zipData.uint32LE(at: localHeaderOffset)
        guard signature == localFileHeaderSignature else {
            throw JARArchiveError.unableToReadArchive
        }

        let fileNameLength = Int(try zipData.uint16LE(at: localHeaderOffset + 26))
        let extraFieldLength = Int(try zipData.uint16LE(at: localHeaderOffset + 28))
        let dataStart = localHeaderOffset + 30 + fileNameLength + extraFieldLength

        var size = compressedSize
        if size == 0, !usesDataDescriptor {
            size = Int(try zipData.uint32LE(at: localHeaderOffset + 18))
        }
        // Data-descriptor entries often leave local sizes at 0; trust the
        // central-directory compressed size when present.
        if size <= 0 {
            throw JARArchiveError.unableToReadArchive
        }
        let dataEnd = dataStart + size
        guard dataEnd <= zipData.count else {
            throw JARArchiveError.unableToReadArchive
        }

        let compressedData = zipData.subdata(in: dataStart..<dataEnd)
        switch compressionMethod {
        case storedMethod:
            return compressedData
        case deflateMethod:
            let expected = uncompressedSize > 0 ? uncompressedSize : size * 4
            return try inflateRaw(compressedData, uncompressedSize: expected)
        default:
            throw JARArchiveError.unableToReadArchive
        }
    }

    private static func inflateRaw(_ data: Data, uncompressedSize: Int) throws -> Data {
        guard uncompressedSize >= 0 else {
            throw JARArchiveError.unableToReadArchive
        }

        var stream = z_stream()
        let initStatus = inflateInit2_(
            &stream,
            -MAX_WBITS,
            ZLIB_VERSION,
            Int32(MemoryLayout<z_stream>.size)
        )
        guard initStatus == Z_OK else {
            throw JARArchiveError.unableToReadArchive
        }
        defer { inflateEnd(&stream) }

        var output = Data()
        let chunkCapacity = max(uncompressedSize, 64 * 1024)
        let destinationBuffer = UnsafeMutablePointer<UInt8>.allocate(capacity: chunkCapacity)
        defer { destinationBuffer.deallocate() }

        let status: Int32 = data.withUnsafeBytes { inputBuffer in
            guard let inputBase = inputBuffer.bindMemory(to: Bytef.self).baseAddress else {
                return Z_DATA_ERROR
            }
            stream.next_in = UnsafeMutablePointer(mutating: inputBase)
            stream.avail_in = uInt(data.count)

            var result: Int32 = Z_OK
            while true {
                stream.next_out = destinationBuffer
                stream.avail_out = uInt(chunkCapacity)
                result = inflate(&stream, Z_NO_FLUSH)
                let produced = chunkCapacity - Int(stream.avail_out)
                if produced > 0 {
                    output.append(destinationBuffer, count: produced)
                }
                if result == Z_STREAM_END {
                    return Z_STREAM_END
                }
                if result != Z_OK {
                    return result
                }
                if stream.avail_in == 0 && stream.avail_out > 0 {
                    return Z_OK
                }
            }
        }

        guard status == Z_STREAM_END || status == Z_OK, !output.isEmpty else {
            throw JARArchiveError.unableToReadArchive
        }
        return output
    }
}

// MARK: - Data Helpers

private extension Data {
    func uint16LE(at offset: Int) throws -> UInt16 {
        guard offset + 2 <= count else {
            throw JARArchiveError.unableToReadArchive
        }
        return self[offset..<offset + 2].withUnsafeBytes { buffer in
            buffer.loadUnaligned(as: UInt16.self).littleEndian
        }
    }

    func uint32LE(at offset: Int) throws -> UInt32 {
        guard offset + 4 <= count else {
            throw JARArchiveError.unableToReadArchive
        }
        return self[offset..<offset + 4].withUnsafeBytes { buffer in
            buffer.loadUnaligned(as: UInt32.self).littleEndian
        }
    }
}
