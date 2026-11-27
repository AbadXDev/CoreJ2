import Foundation

/// Extracts the primary MIDlet icon from the imported JAR into the game library folder.
@MainActor
final class ArtworkStep: ImportStep {

    // MARK: - Dependencies

    private let fileManager: FileManager

    // MARK: - Init

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
    }

    // MARK: - ImportStep

    /// Writes `artwork/icon.png` beside the imported JAR when a manifest icon is available.
    func run(on context: ImportContext) throws {
        guard let jarURL = context.importedJarURL ?? context.installedGame?.jarURL else {
            context.warnings.append("Unable to extract artwork: imported JAR is unavailable.")
            return
        }

        do {
            guard let artworkURL = try Self.extractIcon(
                fromJARAt: jarURL,
                preferredPath: context.manifest?.iconPath,
                fileManager: fileManager
            ) else {
                context.warnings.append("MIDlet icon path was not found inside the JAR.")
                return
            }
            context.artworkURL = artworkURL
        } catch {
            context.warnings.append("Unable to save the extracted MIDlet icon.")
        }
    }

    // MARK: - Shared extraction

    /// Extracts a MIDlet icon next to `jarURL`, trying the manifest path then common fallbacks.
    ///
    /// Returns `nil` when no icon entry exists. Used by import and library backfill.
    static func extractIcon(
        fromJARAt jarURL: URL,
        preferredPath: String?,
        fileManager: FileManager = .default
    ) throws -> URL? {
        var candidates: [String] = []
        if let preferredPath {
            let trimmed = preferredPath.trimmingCharacters(in: .whitespacesAndNewlines)
            if !trimmed.isEmpty {
                candidates.append(trimmed)
            }
        }
        candidates.append(contentsOf: ["icon.png", "Icon.png", "/icon.png", "i.png"])

        var seen = Set<String>()
        for raw in candidates {
            let key = JARArchiveReader.normalize(raw).lowercased()
            guard seen.insert(key).inserted else { continue }
            guard let iconData = try JARArchiveReader.data(
                forEntryNamed: raw,
                inArchiveAt: jarURL
            ), !iconData.isEmpty else {
                continue
            }
            return try writeIcon(iconData, nextToJARAt: jarURL, fileManager: fileManager)
        }
        return nil
    }

    // MARK: - Private

    private static func writeIcon(
        _ iconData: Data,
        nextToJARAt jarURL: URL,
        fileManager: FileManager
    ) throws -> URL {
        let gameDirectory = jarURL.deletingLastPathComponent()
        let artworkDirectory = gameDirectory.appendingPathComponent("artwork", isDirectory: true)
        try fileManager.createDirectory(
            at: artworkDirectory,
            withIntermediateDirectories: true
        )

        let iconURL = artworkDirectory.appendingPathComponent("icon.png", isDirectory: false)
        if fileManager.fileExists(atPath: iconURL.path) {
            try fileManager.removeItem(at: iconURL)
        }
        try iconData.write(to: iconURL, options: .atomic)
        return iconURL
    }
}
