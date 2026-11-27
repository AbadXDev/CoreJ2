import Foundation

/// Reads the MIDlet manifest and resolves the final game title for an import session.
@MainActor
final class ManifestStep: ImportStep {

    // MARK: - Dependencies

    private let manifestService: ManifestService

    // MARK: - Init

    init(manifestService: ManifestService) {
        self.manifestService = manifestService
    }

    // MARK: - ImportStep

    /// Updates `context.manifest` and the installed game title when possible.
    func run(on context: ImportContext) throws {
        guard let installedGame = context.installedGame else { return }

        do {
            let jarURL = context.importedJarURL ?? installedGame.jarURL
            let manifest = try manifestService.readManifest(from: jarURL)
            context.manifest = manifest

            let title: String
            if let midletName = manifest.midletName?
                .trimmingCharacters(in: .whitespacesAndNewlines),
               !midletName.isEmpty {
                title = midletName
            } else {
                title = installedGame.title
                context.warnings.append("MIDlet-Name is missing or empty; using the filename title.")
            }

            let profile = DeviceProfileDetector.detectProfile(
                fileName: context.sourceURL.lastPathComponent,
                manifest: manifest
            )

            context.installedGame = InstalledGame(
                id: installedGame.id,
                title: title,
                jarURL: installedGame.jarURL,
                importedAt: installedGame.importedAt,
                contentHash: installedGame.contentHash,
                vendor: manifest.vendor,
                version: manifest.version,
                midpProfile: manifest.midpProfile,
                cldcConfiguration: manifest.cldcConfiguration,
                deviceProfileID: profile.id
            )
        } catch ManifestServiceError.manifestMissing {
            context.warnings.append("MANIFEST.MF is missing; using the filename title.")
            detectProfileFromFileName(on: context, installedGame: installedGame)
        } catch ManifestServiceError.invalidManifest {
            context.warnings.append("MANIFEST.MF is invalid; using the filename title.")
            detectProfileFromFileName(on: context, installedGame: installedGame)
        } catch {
            context.warnings.append("Unable to read MANIFEST.MF; using the filename title.")
            detectProfileFromFileName(on: context, installedGame: installedGame)
        }
    }

    // MARK: - Private

    /// Falls back to file-name-only profile detection when no manifest is available.
    private func detectProfileFromFileName(on context: ImportContext, installedGame: InstalledGame) {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: context.sourceURL.lastPathComponent,
            manifest: nil
        )
        context.installedGame = installedGame.withDeviceProfile(profile)
    }
}
