import Foundation

/// Rejects or replaces imports whose JAR content hash already exists in the library.
@MainActor
final class DuplicateDetectionStep: ImportStep {

    // MARK: - Dependencies

    private let repository: GameLibraryRepository
    private let fileManager: FileManager

    // MARK: - Init

    init(repository: GameLibraryRepository, fileManager: FileManager = .default) {
        self.repository = repository
        self.fileManager = fileManager
    }

    // MARK: - ImportStep

    /// Throws ``ImportEngineError/duplicateGame`` when a live copy of the hash exists.
    ///
    /// If a prior install has a stale absolute path (reinstall / container move),
    /// replaces that record in-place with the provisional import instead of
    /// inserting a second library entry.
    func run(on context: ImportContext) throws {
        guard let contentHash = context.contentHash, !contentHash.isEmpty else {
            context.warnings.append("Unable to check for duplicates: content hash is unavailable.")
            return
        }

        let existing = repository.games(withContentHash: contentHash)
        guard !existing.isEmpty else {
            return
        }

        let live = existing.filter {
            fileManager.fileExists(atPath: $0.jarURL.path(percentEncoded: false))
        }

        if !live.isEmpty {
            // Discard the provisional Library/<newUUID>/ copy — keep the live install.
            if let provisional = context.installedGame {
                Self.removeGameDirectory(containing: provisional.jarURL, fileManager: fileManager)
            }
            throw ImportEngineError.duplicateGame
        }

        // All prior rows are stale (missing JAR). Reuse the oldest id and drop extras.
        let keeper = existing.sorted(by: { $0.importedAt < $1.importedAt }).first
        guard var provisional = context.installedGame, let keeper else {
            return
        }
        for stale in existing where stale.id != keeper.id {
            repository.delete(stale)
            Self.removeGameDirectory(containing: stale.jarURL, fileManager: fileManager)
        }
        Self.removeGameDirectory(containing: keeper.jarURL, fileManager: fileManager)

        provisional = InstalledGame(
            id: keeper.id,
            title: provisional.title,
            jarURL: provisional.jarURL,
            importedAt: provisional.importedAt,
            contentHash: provisional.contentHash,
            vendor: provisional.vendor,
            version: provisional.version,
            midpProfile: provisional.midpProfile,
            cldcConfiguration: provisional.cldcConfiguration,
            deviceProfileID: provisional.deviceProfileID.isEmpty
                ? keeper.deviceProfileID
                : provisional.deviceProfileID
        )
        context.installedGame = provisional
        repository.delete(keeper)
    }

    // MARK: - File cleanup

    static func removeGameDirectory(containing jarURL: URL, fileManager: FileManager = .default) {
        let directory = jarURL.deletingLastPathComponent()
        guard directory.lastPathComponent.count == 36 else {
            // Expect UUID folder under Library/; refuse to delete unexpected paths.
            return
        }
        try? fileManager.removeItem(at: directory)
    }
}
