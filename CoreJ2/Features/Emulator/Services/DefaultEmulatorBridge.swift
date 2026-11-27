import Foundation

/// Emulator bridge that validates launches and delegates runtime work to a host.
@MainActor
final class DefaultEmulatorBridge: EmulatorBridgeProtocol {

    // MARK: - Properties

    private let runtimeHost: RuntimeHostProtocol

    /// The session currently owned by this bridge, if any.
    private var activeSession: EmulatorSession?

    /// In-flight stop from a previous dismiss — shared across ViewModels so the
    /// next UI launch can wait for MIDlet teardown before starting.
    private var pendingStopTask: Task<Void, Never>?

    // MARK: - Init

    /// Creates a bridge that drives session lifecycle through `runtimeHost`.
    ///
    /// - Parameter runtimeHost: Runtime abstraction. Defaults to
    ///   `PlaceholderRuntimeHost` for previews and isolated unit tests.
    ///   Production injects `FreeJ2MERuntimeHost` from `AppDependencyContainer`.
    init(runtimeHost: RuntimeHostProtocol = PlaceholderRuntimeHost()) {
        self.runtimeHost = runtimeHost
    }

    // MARK: - EmulatorBridgeProtocol

    var runtimeEvents: AsyncStream<RuntimeEvent> {
        runtimeHost.events
    }

    /// Validates `configuration`, asks the host to launch, then marks the session `.running`.
    ///
    /// Always awaits any pending stop first. If a stale session is still marked
    /// active (UI dismiss race), stops it instead of throwing `runtimeAlreadyRunning`.
    func launch(_ configuration: LaunchConfiguration) async throws -> EmulatorSession {
        await awaitPendingStop()
        try await ensureNoActiveSession()
        try validateConfiguration(configuration)
        try validateJAR(at: configuration.game.jarURL)

        let session = EmulatorSession(
            configuration: configuration,
            state: .starting,
            startedAt: Date()
        )
        // Reserve the slot before the host work so a concurrent tap cannot sneak in.
        activeSession = session

        do {
            try await runtimeHost.launch(configuration)
            session.transition(to: .running)
            return session
        } catch {
            session.transition(to: .failed)
            if activeSession?.id == session.id {
                activeSession = nil
            }
            throw error
        }
    }

    /// Asks the host to pause, then moves a `.running` session to `.paused`.
    func pause(_ session: EmulatorSession) throws {
        guard session.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try runtimeHost.pause(session)
        session.transition(to: .paused)
    }

    /// Asks the host to resume, then moves a `.paused` session to `.running`.
    func resume(_ session: EmulatorSession) throws {
        guard session.state == .paused else {
            throw EmulatorBridgeError.invalidSession
        }
        try runtimeHost.resume(session)
        session.transition(to: .running)
    }

    /// Asks the host to stop, then moves through `.stopping` to `.stopped`.
    func stop(_ session: EmulatorSession) async throws {
        switch session.state {
        case .running, .paused, .starting:
            session.transition(to: .stopping)
            do {
                try await runtimeHost.stop(session)
            } catch {
                // Always clear the slot so the next launch is not blocked forever.
                if activeSession?.id == session.id {
                    activeSession = nil
                }
                session.transition(to: .failed)
                throw error
            }
            session.transition(to: .stopped)
            if activeSession?.id == session.id {
                activeSession = nil
            }
        case .stopping:
            // A stop is already in flight — wait for the shared pending task.
            await awaitPendingStop()
        case .idle, .stopped, .failed:
            if activeSession?.id == session.id {
                activeSession = nil
            }
        }
    }

    /// Schedules a stop without requiring the caller to keep the ViewModel alive.
    ///
    /// Used by `EmulatorViewModel.endSession` so dismissing the navigation stack
    /// (which nils the VM) does not cancel MIDlet teardown.
    func enqueueStop(_ session: EmulatorSession) {
        pendingStopTask = Task { [weak self] in
            guard let self else { return }
            try? await self.stop(session)
        }
    }

    /// Waits for any enqueued stop to finish.
    func awaitPendingStop() async {
        await pendingStopTask?.value
        pendingStopTask = nil
    }

    // MARK: - Input

    func keyPressed(_ keyCode: Int32) async throws {
        guard activeSession?.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try await runtimeHost.keyPressed(keyCode)
    }

    func keyReleased(_ keyCode: Int32) async throws {
        guard activeSession?.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try await runtimeHost.keyReleased(keyCode)
    }

    func keyRepeated(_ keyCode: Int32) async throws {
        guard activeSession?.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try await runtimeHost.keyRepeated(keyCode)
    }

    func pointerPressed(x: Int32, y: Int32) async throws {
        guard activeSession?.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try await runtimeHost.pointerPressed(x: x, y: y)
    }

    func pointerDragged(x: Int32, y: Int32) async throws {
        guard activeSession?.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try await runtimeHost.pointerDragged(x: x, y: y)
    }

    func pointerReleased(x: Int32, y: Int32) async throws {
        guard activeSession?.state == .running else {
            throw EmulatorBridgeError.invalidSession
        }
        try await runtimeHost.pointerReleased(x: x, y: y)
    }

    // MARK: - Private

    private func ensureNoActiveSession() async throws {
        await awaitPendingStop()
        guard let existing = activeSession else { return }
        switch existing.state {
        case .idle, .stopped, .failed:
            activeSession = nil
        case .running, .paused, .starting, .stopping:
            print("corej2-launch: clearing stale session before relaunch state=\(existing.state)")
            try? await stop(existing)
            activeSession = nil
        }
    }

    private func validateConfiguration(_ configuration: LaunchConfiguration) throws {
        let game = configuration.game
        let trimmedTitle = game.title.trimmingCharacters(in: .whitespacesAndNewlines)

        guard !trimmedTitle.isEmpty else {
            throw EmulatorBridgeError.invalidConfiguration
        }

        guard !game.contentHash.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw EmulatorBridgeError.invalidConfiguration
        }

        guard game.jarURL.isFileURL else {
            throw EmulatorBridgeError.invalidConfiguration
        }
    }

    private func validateJAR(at jarURL: URL) throws {
        let path = jarURL.path(percentEncoded: false)
        let fileManager = FileManager.default

        guard fileManager.fileExists(atPath: path) else {
            throw EmulatorBridgeError.jarNotFound
        }

        guard fileManager.isReadableFile(atPath: path) else {
            throw EmulatorBridgeError.jarNotReadable
        }
    }
}
