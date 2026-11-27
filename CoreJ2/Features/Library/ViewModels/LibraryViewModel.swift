import Combine
import CryptoKit
import Foundation

/// Drives the game library screen and presentation of the emulator.
@MainActor
final class LibraryViewModel: ObservableObject {

    // MARK: - Published State

    @Published private(set) var games: [InstalledGame] = []

    /// File name of the most recently picked JAR, if any.
    @Published private(set) var selectedFileName: String?

    /// User-facing message shown after a successful import.
    @Published var successMessage: String?

    /// User-facing message shown after a failed import.
    @Published var errorMessage: String?

    /// Game selected for emulator presentation; `nil` dismisses the emulator.
    @Published var gameToLaunch: InstalledGame?

    /// View model bound to the presented `EmulatorView`, if any.
    @Published private(set) var activeEmulatorViewModel: EmulatorViewModel?

    /// Timestamp of the last emulator dismiss — used to delay the next push
    /// until `NavigationStack` has finished popping.
    private var lastDismissAt: Date?

    // MARK: - Dependencies

    private let repository: GameLibraryRepository
    private let importEngine: ImportEngineProtocol
    private let makeEmulatorViewModel: () -> EmulatorViewModel

    // MARK: - Init

    /// - Parameters:
    ///   - repository: Library persistence.
    ///   - importEngine: JAR import pipeline.
    ///   - makeEmulatorViewModel: Factory for a fresh emulator surface VM per launch.
    init(
        repository: GameLibraryRepository,
        importEngine: ImportEngineProtocol,
        makeEmulatorViewModel: @escaping () -> EmulatorViewModel
    ) {
        self.repository = repository
        self.importEngine = importEngine
        self.makeEmulatorViewModel = makeEmulatorViewModel
    }

    // MARK: - Intentions

    /// Loads games from the library repository.
    ///
    /// Also imports any `*.jar` sitting in the app Documents root (File Sharing /
    /// `devicectl copy`), so copies land in the library without using the picker.
    func loadGames() {
        importDocumentRootJARs()
        dedupeAndPruneLibrary()
        repairLegacyTinyProfiles()
        let fileManager = FileManager.default
        games = repository.fetchGames().filter { game in
            fileManager.fileExists(atPath: game.jarURL.path(percentEncoded: false))
        }
        consumeDebugAutolaunchFlagIfNeeded()
    }

    /// Upgrades library entries stuck on the accidental Nokia 96×65 pick
    /// (brand-first detector before the Multi-JAR fix) to the default 240×320.
    private func repairLegacyTinyProfiles() {
        let tinyID = "nokia-s40-96x65"
        let fixed = DeviceProfile.defaultProfile
        for game in repository.fetchGames() where game.deviceProfileID == tinyID {
            repository.save(game.withDeviceProfile(fixed))
            Self.appendLaunchDiagnostic(
                "repaired profile \(game.title) \(tinyID) → \(fixed.id)"
            )
        }
    }

    /// Deletes `game` from SwiftData and removes its `Library/<UUID>/` directory.
    ///
    /// Also tombstones `contentHash` so Documents auto-import does not resurrect it.
    func deleteGame(_ game: InstalledGame) {
        AppSettings.shared.tombstoneContentHash(game.contentHash)
        repository.delete(game)
        DuplicateDetectionStep.removeGameDirectory(containing: game.jarURL)
        games = repository.fetchGames().filter {
            FileManager.default.fileExists(atPath: $0.jarURL.path(percentEncoded: false))
        }
        if gameToLaunch?.id == game.id {
            dismissEmulator()
        }
    }

    /// Debug/device QA: if `Documents/autolaunch` exists, select the first game
    /// (then delete the flag) so JVM bring-up can be exercised without a tap.
    private func consumeDebugAutolaunchFlagIfNeeded() {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            return
        }
        let flagURL = documentsURL.appendingPathComponent("autolaunch", isDirectory: false)
        guard fileManager.fileExists(atPath: flagURL.path) else {
            Self.appendLaunchDiagnostic("autolaunch skipped (flag=false games=\(games.count))")
            return
        }
        // Optional selectors (checked in order):
        // - Documents/autolaunch-match  (text = Documents JAR basename → match by content hash)
        // - Documents/autolaunch-en      (prefer Miami v1.0.8)
        let matchFlag = documentsURL.appendingPathComponent("autolaunch-match", isDirectory: false)
        let preferEN = fileManager.fileExists(
            atPath: documentsURL.appendingPathComponent("autolaunch-en", isDirectory: false).path
        )
        let game: InstalledGame?
        if fileManager.fileExists(atPath: matchFlag.path),
           let name = try? String(contentsOf: matchFlag, encoding: .utf8)
            .trimmingCharacters(in: .whitespacesAndNewlines),
           !name.isEmpty,
           let hash = Self.sha256Hex(ofFileAt: documentsURL.appendingPathComponent(name)) {
            game = games.first(where: { $0.contentHash == hash }) ?? games.first
            try? fileManager.removeItem(at: matchFlag)
            Self.appendLaunchDiagnostic("autolaunch-match \(name) hash=\(hash.prefix(12))…")
        } else if preferEN {
            game = games.first(where: { $0.version == "1.0.8" })
                ?? games.first(where: { ($0.jarURL.path as NSString).lastPathComponent == "game.jar"
                    && ((try? Data(contentsOf: $0.jarURL))?.count == 385280) })
                ?? games.first
            try? fileManager.removeItem(
                at: documentsURL.appendingPathComponent("autolaunch-en", isDirectory: false)
            )
        } else {
            game = games.first
        }
        guard let game else {
            Self.appendLaunchDiagnostic("autolaunch skipped (flag=true games=\(games.count))")
            return
        }
        try? fileManager.removeItem(at: flagURL)
        Self.appendLaunchDiagnostic(
            "autolaunch armed for \(game.title) v=\(game.version ?? "?") path=\(game.jarURL.path)"
        )
        // Defer until NavigationStack has attached `navigationDestination`.
        // A single `Task.yield()` is too early on device cold start.
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(800))
            Self.appendLaunchDiagnostic("autolaunch selectGame \(game.title)")
            self.selectGame(game)
        }
    }

    private static func appendLaunchDiagnostic(_ line: String) {
        let stamp = ISO8601DateFormatter().string(from: Date())
        let text = "\(stamp) \(line)\n"
        print("corej2-launch: \(line)")
        guard let documents = FileManager.default.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            return
        }
        let url = documents.appendingPathComponent("jvm-diag.log", isDirectory: false)
        if let handle = try? FileHandle(forWritingTo: url) {
            defer { try? handle.close() }
            try? handle.seekToEnd()
            if let data = text.data(using: .utf8) {
                try? handle.write(contentsOf: data)
            }
        } else {
            try? text.data(using: .utf8)?.write(to: url, options: .atomic)
        }
    }

    /// Imports top-level Documents `*.jar` files that are not already installed.
    private func importDocumentRootJARs() {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            return
        }

        let jarURLs: [URL]
        do {
            jarURLs = try fileManager.contentsOfDirectory(
                at: documentsURL,
                includingPropertiesForKeys: nil,
                options: [.skipsHiddenFiles]
            ).filter { $0.pathExtension.lowercased() == "jar" }
        } catch {
            return
        }

        for jarURL in jarURLs {
            if let hash = Self.sha256Hex(ofFileAt: jarURL),
               AppSettings.shared.isTombstoned(contentHash: hash) {
                continue
            }
            do {
                let installed = try importEngine.importJAR(from: jarURL)
                repository.save(installed)
            } catch ImportEngineError.duplicateGame {
                continue
            } catch {
                // Leave picker-based import as the fallback; surface nothing here.
                continue
            }
        }
    }

    private static func sha256Hex(ofFileAt url: URL) -> String? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return SHA256.hash(data: data).map { String(format: "%02x", $0) }.joined()
    }

    /// Collapses duplicate contentHash rows, drops missing-JAR records, and
    /// removes orphan `Library/<UUID>/` directories left by failed imports.
    private func dedupeAndPruneLibrary() {
        let fileManager = FileManager.default
        let all = repository.fetchGames()
        var seenHash: [String: InstalledGame] = [:]
        var keepDirectoryNames = Set<String>()

        for game in all.sorted(by: { $0.importedAt < $1.importedAt }) {
            let path = game.jarURL.path(percentEncoded: false)
            let jarExists = fileManager.fileExists(atPath: path)
            if !jarExists {
                repository.delete(game)
                DuplicateDetectionStep.removeGameDirectory(containing: game.jarURL)
                continue
            }

            let hash = game.contentHash.trimmingCharacters(in: .whitespacesAndNewlines)
            if !hash.isEmpty, let keeper = seenHash[hash] {
                // Newer duplicate of an already-kept hash → remove this one.
                repository.delete(game)
                DuplicateDetectionStep.removeGameDirectory(containing: game.jarURL)
                _ = keeper
                continue
            }
            if !hash.isEmpty {
                seenHash[hash] = game
            }
            keepDirectoryNames.insert(game.jarURL.deletingLastPathComponent().lastPathComponent)
        }

        purgeOrphanLibraryDirectories(keeping: keepDirectoryNames, fileManager: fileManager)
    }

    private func purgeOrphanLibraryDirectories(keeping keepNames: Set<String>, fileManager: FileManager) {
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let libraryRoot = documentsURL
            .appendingPathComponent("CoreJ2", isDirectory: true)
            .appendingPathComponent("Library", isDirectory: true)
        guard let contents = try? fileManager.contentsOfDirectory(
            at: libraryRoot,
            includingPropertiesForKeys: [.isDirectoryKey],
            options: [.skipsHiddenFiles]
        ) else {
            return
        }
        for url in contents {
            let name = url.lastPathComponent
            guard name.count == 36, !keepNames.contains(name) else { continue }
            try? fileManager.removeItem(at: url)
        }
    }

    /// Handles the result of the system file importer.
    /// Cancellation is ignored. A selected JAR is copied into the internal library.
    func handleImportResult(_ result: Result<[URL], Error>) {
        switch result {
        case .success(let urls):
            guard let url = urls.first else { return }
            selectedFileName = url.lastPathComponent
            importSelectedJAR(from: url)
        case .failure:
            break
        }
    }

    /// Prepares an emulator view model and presents `EmulatorView` for `game`.
    func selectGame(_ game: InstalledGame) {
        // If a previous emulator is still presented, tear it down first so the
        // shared bridge can finish stopMidlet before the next launch.
        if gameToLaunch != nil || activeEmulatorViewModel != nil {
            dismissEmulator()
            presentEmulatorAfterNavigationSettles(game)
            return
        }
        // Real UI path: Library back already called dismissEmulator; a tap that
        // arrives while NavigationStack is still popping must wait or SwiftUI
        // cancels EmulatorView.task mid-launch (silent "second game does nothing").
        if let lastDismissAt, Date().timeIntervalSince(lastDismissAt) < 0.7 {
            presentEmulatorAfterNavigationSettles(game)
            return
        }
        presentEmulator(for: game)
    }

    private func presentEmulatorAfterNavigationSettles(_ game: InstalledGame) {
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(450))
            presentEmulator(for: game)
        }
    }

    private func presentEmulator(for game: InstalledGame) {
        let nextVM = makeEmulatorViewModel()
        activeEmulatorViewModel = nextVM
        gameToLaunch = game
        armAutoSwitchIfNeeded(after: game)
        armUISwitchTestIfNeeded(after: game)
        armQATapScriptIfNeeded(viewModel: nextVM)
        armQAKeyScriptIfNeeded(viewModel: nextVM)
        armQADpadSlideIfNeeded(viewModel: nextVM)
        armQAAudioStopIfNeeded()
    }

    /// QA: `Documents/qa-tap` lines `delayMs x y` inject pointer press/release
    /// after the emulator is presented (Diamond Rush language hit-testing, etc.).
    private func armQATapScriptIfNeeded(viewModel: EmulatorViewModel) {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let flag = documentsURL.appendingPathComponent("qa-tap", isDirectory: false)
        guard fileManager.fileExists(atPath: flag.path),
              let body = try? String(contentsOf: flag, encoding: .utf8) else {
            return
        }
        try? fileManager.removeItem(at: flag)
        let lines = body.split(whereSeparator: \.isNewline).map(String.init)
        Self.appendLaunchDiagnostic("qa-tap armed lines=\(lines.count)")
        Task { @MainActor in
            for line in lines {
                let parts = line.split(whereSeparator: { $0 == " " || $0 == "," }).map(String.init)
                guard parts.count >= 3,
                      let delayMs = UInt64(parts[0]),
                      let x = Int32(parts[1]),
                      let y = Int32(parts[2]) else {
                    continue
                }
                try? await Task.sleep(for: .milliseconds(delayMs))
                Self.appendLaunchDiagnostic("qa-tap \(x),\(y)")
                viewModel.pointerPressed(x: x, y: y)
                try? await Task.sleep(for: .milliseconds(80))
                viewModel.pointerReleased(x: x, y: y)
            }
        }
    }

    /// QA: `Documents/qa-key` lines `delayMs keyCode` inject MIDP key press/release.
    private func armQAKeyScriptIfNeeded(viewModel: EmulatorViewModel) {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let flag = documentsURL.appendingPathComponent("qa-key", isDirectory: false)
        guard fileManager.fileExists(atPath: flag.path),
              let body = try? String(contentsOf: flag, encoding: .utf8) else {
            return
        }
        try? fileManager.removeItem(at: flag)
        let lines = body.split(whereSeparator: \.isNewline).map(String.init)
        Self.appendLaunchDiagnostic("qa-key armed lines=\(lines.count)")
        Task { @MainActor in
            for line in lines {
                let parts = line.split(whereSeparator: { $0 == " " || $0 == "," }).map(String.init)
                guard parts.count >= 2,
                      let delayMs = UInt64(parts[0]),
                      let key = Int32(parts[1]) else {
                    continue
                }
                try? await Task.sleep(for: .milliseconds(delayMs))
                Self.appendLaunchDiagnostic("qa-key \(key)")
                viewModel.keyDown(key)
                try? await Task.sleep(for: .milliseconds(80))
                viewModel.keyUp(key)
            }
        }
    }

    /// QA: `Documents/qa-dpad-slide` — simulate continuous D-pad slide without lifting:
    /// left → diagonal(left+up) → up, with hold-repeat, then release.
    private func armQADpadSlideIfNeeded(viewModel: EmulatorViewModel) {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let flag = documentsURL.appendingPathComponent("qa-dpad-slide", isDirectory: false)
        guard fileManager.fileExists(atPath: flag.path) else { return }
        try? fileManager.removeItem(at: flag)
        Self.appendLaunchDiagnostic("qa-dpad-slide armed")
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(2500))
            // Press left
            Self.appendLaunchDiagnostic("qa-dpad-slide down left")
            viewModel.keyDown(J2MEKeyCode.left)
            try? await Task.sleep(for: .milliseconds(200))
            viewModel.keyRepeat(J2MEKeyCode.left)
            // Slide to diagonal: keep left, add up
            Self.appendLaunchDiagnostic("qa-dpad-slide diagonal left+up")
            viewModel.keyDown(J2MEKeyCode.up)
            try? await Task.sleep(for: .milliseconds(150))
            viewModel.keyRepeat(J2MEKeyCode.left)
            viewModel.keyRepeat(J2MEKeyCode.up)
            // Slide to up only: release left
            Self.appendLaunchDiagnostic("qa-dpad-slide up-only (release left)")
            viewModel.keyUp(J2MEKeyCode.left)
            try? await Task.sleep(for: .milliseconds(150))
            viewModel.keyRepeat(J2MEKeyCode.up)
            // Release
            Self.appendLaunchDiagnostic("qa-dpad-slide release up")
            viewModel.keyUp(J2MEKeyCode.up)
            Self.appendLaunchDiagnostic("qa-dpad-slide done")
        }
    }

    /// QA: `Documents/qa-audio-stop` — after 6s dismiss to library so logs show stopAll.
    private func armQAAudioStopIfNeeded() {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let flag = documentsURL.appendingPathComponent("qa-audio-stop", isDirectory: false)
        guard fileManager.fileExists(atPath: flag.path) else { return }
        try? fileManager.removeItem(at: flag)
        Self.appendLaunchDiagnostic("qa-audio-stop armed")
        Task { @MainActor in
            try? await Task.sleep(for: .seconds(6))
            Self.appendLaunchDiagnostic("qa-audio-stop dismiss")
            dismissEmulator()
        }
    }

    /// QA: `Documents/ui-switch-test` simulates the real back-button path
    /// (dismissEmulator nils the VM without awaiting) then launches another game.
    ///
    /// Optional `Documents/cross-switch-match` (JAR basename in Documents) picks
    /// the second title by content hash; otherwise switches Miami EN ↔ ES.
    private func armUISwitchTestIfNeeded(after current: InstalledGame) {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let flag = documentsURL.appendingPathComponent("ui-switch-test", isDirectory: false)
        guard fileManager.fileExists(atPath: flag.path) else { return }
        try? fileManager.removeItem(at: flag)

        let crossFlag = documentsURL.appendingPathComponent("cross-switch-match", isDirectory: false)
        var crossHash: String?
        if fileManager.fileExists(atPath: crossFlag.path),
           let name = try? String(contentsOf: crossFlag, encoding: .utf8)
            .trimmingCharacters(in: .whitespacesAndNewlines),
           !name.isEmpty {
            crossHash = Self.sha256Hex(ofFileAt: documentsURL.appendingPathComponent(name))
            try? fileManager.removeItem(at: crossFlag)
            Self.appendLaunchDiagnostic("ui-switch cross-target \(name)")
        }

        Task { @MainActor in
            try? await Task.sleep(for: .seconds(4))
            Self.appendLaunchDiagnostic("ui-switch-test dismiss (UI back simulation)")
            // Exact UI path: dismiss without awaiting on this VM.
            dismissEmulator()
            try? await Task.sleep(for: .milliseconds(200))
            let next: InstalledGame?
            if let crossHash {
                next = games.first(where: { $0.contentHash == crossHash && $0.id != current.id })
                    ?? games.first(where: { $0.contentHash == crossHash })
            } else {
                let targetVersion = current.version == "1.0.8" ? "1.0.3" : "1.0.8"
                next = games.first(where: { $0.version == targetVersion })
            }
            guard let next else {
                Self.appendLaunchDiagnostic("ui-switch-test missed next game")
                return
            }
            Self.appendLaunchDiagnostic("ui-switch-test selectGame \(next.title)")
            selectGame(next)
        }
    }

    /// QA: `Documents/auto-switch-es` or `auto-switch-en` — after the current
    /// game starts, dismiss and launch the other Miami build in-process (Home path).
    private func armAutoSwitchIfNeeded(after current: InstalledGame) {
        let fileManager = FileManager.default
        guard let documentsURL = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return
        }
        let wantES = fileManager.fileExists(
            atPath: documentsURL.appendingPathComponent("auto-switch-es", isDirectory: false).path
        )
        let wantEN = fileManager.fileExists(
            atPath: documentsURL.appendingPathComponent("auto-switch-en", isDirectory: false).path
        )
        guard wantES || wantEN else { return }
        try? fileManager.removeItem(at: documentsURL.appendingPathComponent("auto-switch-es", isDirectory: false))
        try? fileManager.removeItem(at: documentsURL.appendingPathComponent("auto-switch-en", isDirectory: false))
        let targetVersion = wantES ? "1.0.3" : "1.0.8"
        Task { @MainActor in
            // Let the first MIDlet finish starting.
            try? await Task.sleep(for: .seconds(4))
            let previous = activeEmulatorViewModel
            previous?.endSession()
            await previous?.awaitPendingStop()
            gameToLaunch = nil
            activeEmulatorViewModel = nil
            try? await Task.sleep(for: .milliseconds(300))
            guard let next = games.first(where: { $0.version == targetVersion && $0.id != current.id })
                    ?? games.first(where: { $0.version == targetVersion }) else {
                Self.appendLaunchDiagnostic("auto-switch missed target v=\(targetVersion)")
                return
            }
            Self.appendLaunchDiagnostic("auto-switch → \(next.title) v=\(next.version ?? "?")")
            selectGame(next)
        }
    }

    /// Changes the device profile used to run `game` and persists the choice.
    func updateDeviceProfile(_ profile: DeviceProfile, for game: InstalledGame) {
        let updatedGame = game.withDeviceProfile(profile)
        repository.save(updatedGame)
        games = repository.fetchGames()
    }

    /// Dismisses the emulator screen and releases its view model.
    func dismissEmulator() {
        activeEmulatorViewModel?.endSession()
        gameToLaunch = nil
        activeEmulatorViewModel = nil
        lastDismissAt = Date()
    }

    /// Clears the success alert message.
    func dismissSuccessMessage() {
        successMessage = nil
    }

    /// Clears the error alert message.
    func dismissErrorMessage() {
        errorMessage = nil
    }

    // MARK: - Private

    private func importSelectedJAR(from url: URL) {
        do {
            // Explicit picker import clears any tombstone for this content.
            if let hash = Self.sha256Hex(ofFileAt: url) {
                AppSettings.shared.clearTombstone(forContentHash: hash)
            }
            let installedGame = try importEngine.importJAR(from: url)
            repository.save(installedGame)
            games = repository.fetchGames()
            errorMessage = nil
            successMessage = "\"\(url.lastPathComponent)\" was imported successfully."
        } catch {
            successMessage = nil
            errorMessage = error.localizedDescription
        }
    }
}
