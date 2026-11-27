import Foundation

/// Reads `META-INF/MANIFEST.MF` from a JAR (ZIP) archive.
final class JARManifestService: ManifestService {

    // MARK: - ManifestService

    func readManifest(from jarURL: URL) throws -> MidletManifest {
        let manifestData: Data
        do {
            guard let entry = try JARArchiveReader.data(
                forEntryNamed: "META-INF/MANIFEST.MF",
                inArchiveAt: jarURL
            ) else {
                throw ManifestServiceError.manifestMissing
            }
            manifestData = entry
        } catch let error as ManifestServiceError {
            throw error
        } catch {
            throw ManifestServiceError.unableToReadJAR
        }

        return try ManifestParser.parse(manifestData)
    }
}

// MARK: - ManifestParser

private enum ManifestParser {
    static func parse(_ data: Data) throws -> MidletManifest {
        guard let text = String(data: data, encoding: .utf8)
            ?? String(data: data, encoding: .isoLatin1) else {
            throw ManifestServiceError.invalidManifest
        }

        var attributes: [String: String] = [:]
        var currentName: String?
        var currentValue = ""

        func commit() {
            guard let name = currentName else { return }
            attributes[name] = currentValue
            currentName = nil
            currentValue = ""
        }

        for rawLine in text.split(whereSeparator: \.isNewline) {
            let line = String(rawLine)
            if line.hasPrefix(" ") {
                guard currentName != nil else {
                    throw ManifestServiceError.invalidManifest
                }
                currentValue += line.dropFirst()
                continue
            }

            commit()

            if line.isEmpty {
                continue
            }

            guard let separatorIndex = line.firstIndex(of: ":") else {
                throw ManifestServiceError.invalidManifest
            }

            let name = String(line[..<separatorIndex])
            var value = String(line[line.index(after: separatorIndex)...])
            if value.hasPrefix(" ") {
                value = String(value.dropFirst())
            }

            currentName = name
            currentValue = value
        }

        commit()

        let displaySize = originalDisplaySize(from: attributes)
        let attributeNames = attributes.keys.map { $0.lowercased() }

        return MidletManifest(
            midletName: attributes["MIDlet-Name"],
            iconPath: iconPath(from: attributes),
            vendor: trimmed(attributes["MIDlet-Vendor"]),
            version: trimmed(attributes["MIDlet-Version"]),
            midpProfile: trimmed(attributes["MicroEdition-Profile"]),
            cldcConfiguration: trimmed(attributes["MicroEdition-Configuration"]),
            midletClassName: midletClassName(from: attributes),
            originalDisplayWidth: displaySize?.width,
            originalDisplayHeight: displaySize?.height,
            hasNokiaAttributes: attributeNames.contains { $0.hasPrefix("nokia-") },
            hasSonyEricssonAttributes: attributeNames.contains { $0.contains("sonyericsson") },
            hasMotorolaAttributes: attributeNames.contains { $0.contains("motorola") }
        )
    }

    private static func trimmed(_ value: String?) -> String? {
        guard let value = value?.trimmingCharacters(in: .whitespacesAndNewlines),
              !value.isEmpty else {
            return nil
        }
        return value
    }

    private static func midletClassName(from attributes: [String: String]) -> String? {
        guard let midletEntry = attributes["MIDlet-1"] else {
            return nil
        }
        let parts = midletEntry
            .split(separator: ",", omittingEmptySubsequences: false)
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
        guard parts.count >= 3, !parts[2].isEmpty else {
            return nil
        }
        return parts[2]
    }

    /// Parses `Nokia-MIDlet-Original-Display-Size` (for example `240,320`).
    private static func originalDisplaySize(
        from attributes: [String: String]
    ) -> (width: Int, height: Int)? {
        guard let raw = attributes["Nokia-MIDlet-Original-Display-Size"] else {
            return nil
        }
        let parts = raw
            .split(whereSeparator: { $0 == "," || $0 == "x" || $0 == "X" || $0 == " " })
            .map { $0.trimmingCharacters(in: .whitespaces) }
        guard parts.count >= 2,
              let width = Int(parts[0]),
              let height = Int(parts[1]),
              width > 0, height > 0 else {
            return nil
        }
        return (width: width, height: height)
    }

    private static func iconPath(from attributes: [String: String]) -> String? {
        if let midletIcon = attributes["MIDlet-Icon"]?
            .trimmingCharacters(in: .whitespacesAndNewlines),
           !midletIcon.isEmpty {
            return JARArchiveReader.normalize(midletIcon)
        }

        if let midletEntry = attributes["MIDlet-1"] {
            let parts = midletEntry
                .split(separator: ",", omittingEmptySubsequences: false)
                .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            if parts.count >= 2, !parts[1].isEmpty {
                return JARArchiveReader.normalize(parts[1])
            }
        }

        return nil
    }
}
