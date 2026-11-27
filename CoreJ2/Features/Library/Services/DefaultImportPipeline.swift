import Foundation

/// Default import pipeline that copies JARs and runs ordered import steps.
@MainActor
final class DefaultImportPipeline: ImportPipelineProtocol {

    // MARK: - Dependencies

    private let importService: ImportService
    private let steps: [any ImportStep]
    private let fileManager: FileManager

    // MARK: - Init

    /// Creates a pipeline that runs `steps` in order after copying the JAR.
    init(
        importService: ImportService,
        steps: [any ImportStep],
        fileManager: FileManager = .default
    ) {
        self.importService = importService
        self.steps = steps
        self.fileManager = fileManager
    }

    // MARK: - ImportPipelineProtocol

    func run(from sourceURL: URL) throws -> InstalledGame {
        let context = ImportContext(sourceURL: sourceURL)
        do {
            try execute(using: context)
        } catch {
            // Copy happens before steps; purge orphan Library/<UUID>/ on any failure
            // (including duplicateGame) so retries do not accumulate empty folders.
            if let provisional = context.installedGame {
                DuplicateDetectionStep.removeGameDirectory(
                    containing: provisional.jarURL,
                    fileManager: fileManager
                )
            }
            throw error
        }

        guard let installedGame = context.installedGame else {
            preconditionFailure("Import pipeline completed without an installed game.")
        }

        return installedGame
    }

    // MARK: - Private

    private func execute(using context: ImportContext) throws {
        let provisionalGame = try importService.importJAR(from: context.sourceURL)
        context.importedJarURL = provisionalGame.jarURL
        context.installedGame = provisionalGame

        for step in steps {
            try step.run(on: context)
        }
    }
}
