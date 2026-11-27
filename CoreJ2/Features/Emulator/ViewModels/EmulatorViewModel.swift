import Combine
import CoreGraphics
import Foundation
import ImageIO

/// Drives the emulator LCD surface screen.
///
/// Owns session launch/stop against `EmulatorBridgeProtocol`, observes
/// `runtimeEvents`, and converts `frameAvailable` payloads into `CGImage`.
@MainActor
final class EmulatorViewModel: ObservableObject {

    // MARK: - Published State

    /// Metadata from the most recent successfully rendered frame, if any.
    @Published private(set) var surfaceMetadata: EmulatorSurfaceMetadata?

    /// Latest LCD bitmap for SwiftUI (`Image(decorative:scale:)`).
    @Published private(set) var lcdImage: CGImage?

    /// Number of frames successfully converted and published.
    @Published private(set) var receivedFrameCount = 0

    /// `true` after at least one frame has been rendered to `lcdImage`.
    @Published private(set) var hasReceivedFrame = false

    /// Active emulator session created by `startSession(for:)`, if any.
    @Published private(set) var activeSession: EmulatorSession?

    /// Configuration used for the active (or last) launch attempt.
    @Published private(set) var lastLaunchConfiguration: LaunchConfiguration?

    /// User-facing error from a failed launch.
    @Published var launchErrorMessage: String?

    /// `true` while a launch is in flight (shows Starting… until success/error).
    @Published private(set) var isLaunching = false

    // MARK: - Dependencies

    private let bridge: EmulatorBridgeProtocol
    private var observationTask: Task<Void, Never>?
    private var launchTask: Task<Void, Never>?
    private var pendingStopTask: Task<Void, Never>?

    // MARK: - Init

    /// - Parameter bridge: App-facing emulator API (events + lifecycle).
    init(bridge: EmulatorBridgeProtocol) {
        self.bridge = bridge
    }

    // MARK: - Session Lifecycle

    /// Starts observing runtime events, then launches `game` through the bridge.
    ///
    /// Observation begins first so `frameAvailable` events emitted during launch
    /// are not missed. A hard timeout surfaces a clear error instead of hanging.
    func startSession(for game: InstalledGame) async {
        guard activeSession == nil, !isLaunching else {
            print("corej2-launch: startSession skipped (session active or launching)")
            return
        }

        print("corej2-launch: startSession begin \(game.title)")
        launchErrorMessage = nil
        isLaunching = true
        // Shared bridge stop (survives ViewModel teardown on UI back).
        if let bridge = bridge as? DefaultEmulatorBridge {
            await bridge.awaitPendingStop()
        } else {
            await awaitPendingStop()
        }
        startObservingRuntimeEvents()
        await Task.yield()

        let timeoutTask = Task {
            try await Task.sleep(for: .seconds(60))
            throw EmulatorBridgeError.launchFailed
        }
        do {
            try await withTaskCancellationHandler {
                try await launch(game: game)
                timeoutTask.cancel()
            } onCancel: {
                timeoutTask.cancel()
            }
            // If timeout won first, launch may still be running; check cancellation.
            if timeoutTask.isCancelled == false {
                // Race: await whichever finishes; prefer launch success.
            }
            print("corej2-launch: startSession OK \(game.title)")
        } catch is CancellationError {
            print("corej2-launch: startSession cancelled")
            timeoutTask.cancel()
            // Surface cancellation when the user still expects a launch (e.g. SwiftUI
            // tore down the destination mid-start). Silent failure looked like
            // "second game does nothing".
            if activeSession == nil {
                launchErrorMessage = "No se pudo iniciar el juego. Vuelve a la biblioteca e inténtalo de nuevo."
            }
            stopObservingRuntimeEvents()
        } catch {
            timeoutTask.cancel()
            print("corej2-launch: startSession FAILED \(error)")
            let message: String
            if error is EmulatorBridgeError {
                message = "No se pudo iniciar el juego. Vuelve a la biblioteca e inténtalo de nuevo."
            } else {
                message = error.localizedDescription
            }
            launchErrorMessage = message
            stopObservingRuntimeEvents()
        }

        // Soft timeout watcher: if still launching after 60s with no session, surface error.
        Task { @MainActor [weak self] in
            try? await Task.sleep(for: .seconds(60))
            guard let self, self.isLaunching, self.activeSession == nil, self.launchErrorMessage == nil else {
                return
            }
            self.launchErrorMessage = "El inicio tardó demasiado. Vuelve a la biblioteca e inténtalo de nuevo."
            self.isLaunching = false
            self.stopObservingRuntimeEvents()
        }

        isLaunching = false
    }

    /// Creates `LaunchConfiguration` and calls `EmulatorBridge.launch`.
    func launch(game: InstalledGame) async throws {
        let configuration = LaunchConfiguration(game: game)
        lastLaunchConfiguration = configuration
        activeSession = try await bridge.launch(configuration)
    }

    /// Stops the active session (if any) and cancels event observation.
    ///
    /// Kick off an async stop and stash the task so a subsequent
    /// `awaitPendingStop()` (from the next launch) can wait for MIDlet teardown.
    func endSession() {
        launchTask?.cancel()
        launchTask = nil
        let session = activeSession
        activeSession = nil
        isLaunching = false
        stopObservingRuntimeEvents()
        resetSurfaceState()
        guard let session else { return }
        // Enqueue on the shared bridge so dismissEmulator can nil this VM
        // without cancelling MIDlet teardown (the UI-back race).
        if let bridge = bridge as? DefaultEmulatorBridge {
            bridge.enqueueStop(session)
            pendingStopTask = nil
        } else {
            pendingStopTask = Task {
                try? await bridge.stop(session)
            }
        }
        print("corej2-launch: endSession enqueued stop")
    }

    /// Waits for any in-flight `endSession` stop to finish (VM reuse handoff).
    func awaitPendingStop() async {
        await pendingStopTask?.value
        pendingStopTask = nil
    }

    /// Clears the launch error alert message.
    func dismissLaunchError() {
        launchErrorMessage = nil
    }

    // MARK: - Input

    /// LCD width in MIDlet pixels (frame metadata, else launch profile).
    func lcdWidthPixels(for game: InstalledGame) -> Int {
        if let width = surfaceMetadata?.width, width > 0 {
            return width
        }
        return lastLaunchConfiguration?.lcdWidth ?? game.deviceProfile.lcdWidth
    }

    /// LCD height in MIDlet pixels (frame metadata, else launch profile).
    func lcdHeightPixels(for game: InstalledGame) -> Int {
        if let height = surfaceMetadata?.height, height > 0 {
            return height
        }
        return lastLaunchConfiguration?.lcdHeight ?? game.deviceProfile.lcdHeight
    }

    /// Soft press of a J2ME keycode (D-pad / soft / numeric).
    func keyDown(_ keyCode: Int32) {
        Task { try? await bridge.keyPressed(keyCode) }
    }

    /// Soft release of a J2ME keycode.
    func keyUp(_ keyCode: Int32) {
        Task { try? await bridge.keyReleased(keyCode) }
    }

    /// Hold-to-repeat while a keypad button stays down.
    func keyRepeat(_ keyCode: Int32) {
        Task { try? await bridge.keyRepeated(keyCode) }
    }

    func pointerPressed(x: Int32, y: Int32) {
        Task { try? await bridge.pointerPressed(x: x, y: y) }
    }

    func pointerDragged(x: Int32, y: Int32) {
        Task { try? await bridge.pointerDragged(x: x, y: y) }
    }

    func pointerReleased(x: Int32, y: Int32) {
        Task { try? await bridge.pointerReleased(x: x, y: y) }
    }

    // MARK: - Observation

    /// Starts consuming `bridge.runtimeEvents` until cancelled or stopped.
    func startObservingRuntimeEvents() {
        observationTask?.cancel()
        observationTask = Task { [weak self] in
            guard let self else { return }
            for await event in self.bridge.runtimeEvents {
                guard !Task.isCancelled else { break }
                self.handleRuntimeEvent(event)
            }
        }
    }

    /// Cancels the runtime event observation task.
    func stopObservingRuntimeEvents() {
        observationTask?.cancel()
        observationTask = nil
    }

    // MARK: - Event Handling

    /// Applies a runtime event to published surface state.
    ///
    /// Exposed for unit tests that inject events without a live stream.
    func handleRuntimeEvent(_ event: RuntimeEvent) {
        switch event {
        case .frameAvailable(let frame):
            applyFrame(frame)
        case .failed(let error):
            launchErrorMessage = error.localizedDescription
            isLaunching = false
        case .started, .paused, .resumed, .stopped:
            break
        }
    }

    // MARK: - Private

    private func applyFrame(_ frame: EmulatorFrame) {
        guard frame.pixels.count == frame.width * frame.height * 4 else {
            return
        }
        guard let image = EmulatorFrameCGImageConverter.makeCGImage(from: frame) else {
            return
        }
        guard image.width == frame.width, image.height == frame.height else {
            return
        }

        surfaceMetadata = EmulatorSurfaceMetadata(
            width: frame.width,
            height: frame.height,
            pixelFormat: frame.pixelFormat,
            pixelCount: frame.pixelCount
        )
        lcdImage = image
        receivedFrameCount += 1
        hasReceivedFrame = true
        dumpLCDFrameIfNeeded(image, width: frame.width, height: frame.height)
    }

    /// Writes the first few LCD frames as PNG under Documents/lcd-dump when
    /// `Documents/qa-lcd-dump` exists (viewport QA).
    private func dumpLCDFrameIfNeeded(_ image: CGImage, width: Int, height: Int) {
        // Keep a short trail of dumps for viewport QA (splash → menus).
        guard receivedFrameCount <= 12 else { return }
        guard let documents = FileManager.default.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            return
        }
        let flag = documents.appendingPathComponent("qa-lcd-dump", isDirectory: false)
        guard FileManager.default.fileExists(atPath: flag.path) else { return }
        let dir = documents.appendingPathComponent("lcd-dump", isDirectory: true)
        do {
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            let url = dir.appendingPathComponent(
                "frame-\(receivedFrameCount)-\(width)x\(height).png",
                isDirectory: false
            )
            let dest = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil)
            guard let dest else { return }
            CGImageDestinationAddImage(dest, image, nil)
            CGImageDestinationFinalize(dest)
            print("corej2-viewport: dumped LCD \(url.lastPathComponent)")
        } catch {
            print("corej2-viewport: LCD dump failed \(error)")
        }
    }

    private func resetSurfaceState() {
        surfaceMetadata = nil
        lcdImage = nil
        receivedFrameCount = 0
        hasReceivedFrame = false
        lastLaunchConfiguration = nil
        // Keep launchErrorMessage so the alert can still show after a failed launch.
    }
}
