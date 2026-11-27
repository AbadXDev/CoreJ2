import Foundation

/// Embedded (in-process) FreeJ2ME bootstrap for iOS device builds.
///
/// Keeps one HotSpot VM + one `DefaultPlatformBootstrap` for the process lifetime.
/// Switching games = `stopMidlet` → `loadJar` → `runJar` on the same platform
/// (HotSpot cannot create a second VM in-process).
@MainActor
final class EmbeddedFreeJ2MEMobilePlatformBootstrap: FreeJ2MEMobilePlatformBootstrapping {

    // MARK: - Properties

    /// Invoked on the main actor for every frame extracted after painter registration.
    var frameHandler: ((FreeJ2MEFrameCapture) -> Void)?

    private let jvm: EmbeddedJVMManager
    private let gateway: DefaultJNIGateway
    private var platformBootstrap: DefaultPlatformBootstrap?
    /// Serializes start/stop so a dismiss Task cannot race the next launch.
    private var sessionGate = false

    // MARK: - Init

    /// - Parameters:
    ///   - jvm: Embedded JVM manager (owns JVM lifetime).
    ///   - gateway: JNI gateway sharing the manager's readiness source.
    init(jvm: EmbeddedJVMManager, gateway: DefaultJNIGateway) {
        self.jvm = jvm
        self.gateway = gateway
    }

    // MARK: - Production async entry

    /// Full Contract C bring-up without blocking the main run loop.
    ///
    /// Prefer this over the synchronous protocol methods: those exist for
    /// `FreeJ2MEMobilePlatformBootstrapping` conformance but must not wait on
    /// `JNI_CreateJavaVM` on the main actor (iOS watchdog).
    func startEmbedded(configuration: LaunchConfiguration) async throws {
        while sessionGate {
            try await Task.sleep(for: .milliseconds(20))
        }
        sessionGate = true
        defer { sessionGate = false }

        Self.appendDiagnostic(
            "startEmbedded begin jar=\(configuration.game.jarURL.lastPathComponent)"
        )

        if let bootstrap = platformBootstrap, bootstrap.isPlatformReady {
            Self.appendDiagnostic("reusing live platform (stopMidlet → loadJar → runJar)")
            bootstrap.onFrameExtracted = { [weak self] capture in
                Task { @MainActor in
                    self?.frameHandler?(capture)
                }
            }
            do {
                try await bootstrap.stopMidlet()
                try await bootstrap.resizeLCD(
                    width: Int32(configuration.lcdWidth),
                    height: Int32(configuration.lcdHeight)
                )
                _ = try await bootstrap.loadJar(url: configuration.game.jarURL)
                Self.appendDiagnostic("loadJar OK (reuse)")
                _ = try await bootstrap.runJar()
                Self.appendDiagnostic("runJar OK (reuse)")
                return
            } catch {
                Self.appendDiagnostic("reuse FAILED: \(error)")
                throw error
            }
        }

        let bundleRoot = Bundle.main.resourceURL ?? Bundle.main.bundleURL
        let javaHome = bundleRoot
            .appendingPathComponent("OpenJDKMobile", isDirectory: true)
            .appendingPathComponent("java.home", isDirectory: true)
        let modulesImage = javaHome
            .appendingPathComponent("lib", isDirectory: true)
            .appendingPathComponent("modules", isDirectory: false)
        let modulesOK = FileManager.default.fileExists(atPath: modulesImage.path)
        Self.appendDiagnostic(
            "java.home=\(javaHome.path) modules=\(modulesOK) jarExists=\(FileManager.default.fileExists(atPath: configuration.game.jarURL.path))"
        )
        let bootstrap = DefaultPlatformBootstrap(
            jvm: jvm,
            gateway: gateway,
            lcdWidth: Int32(configuration.lcdWidth),
            lcdHeight: Int32(configuration.lcdHeight)
        )
        bootstrap.onFrameExtracted = { [weak self] capture in
            Task { @MainActor in
                self?.frameHandler?(capture)
            }
        }
        platformBootstrap = bootstrap

        do {
            try await bootstrap.initialize()
            Self.appendDiagnostic("initialize OK")
            _ = try await bootstrap.loadJar(url: configuration.game.jarURL)
            Self.appendDiagnostic("loadJar OK")
            _ = try await bootstrap.runJar()
            Self.appendDiagnostic("runJar OK")
        } catch {
            Self.appendDiagnostic("startEmbedded FAILED: \(error)")
            throw error
        }
    }

    private static func appendDiagnostic(_ line: String) {
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

    // MARK: - FreeJ2MEMobilePlatformBootstrapping

    func bootstrapMobilePlatform(lcdWidth: Int, lcdHeight: Int) throws {
        _ = lcdWidth
        _ = lcdHeight
        throw EmulatorBridgeError.launchFailed
    }

    func registerPainter(lcdWidth: Int, lcdHeight: Int) throws -> FreeJ2MEPainterRegistrationResult {
        _ = lcdWidth
        _ = lcdHeight
        guard let platformBootstrap else {
            throw EmulatorBridgeError.launchFailed
        }
        return FreeJ2MEPainterRegistrationResult(
            paintEventCount: platformBootstrap.paintInvocationCount,
            frames: []
        )
    }

    func loadJar(
        lcdWidth: Int,
        lcdHeight: Int,
        jarFileURLString: String
    ) throws -> FreeJ2MEJarLoadResult {
        _ = (lcdWidth, lcdHeight, jarFileURLString)
        throw EmulatorBridgeError.launchFailed
    }

    func runJar(
        lcdWidth: Int,
        lcdHeight: Int,
        jarFileURLString: String
    ) throws -> FreeJ2MEJarRunResult {
        _ = (lcdWidth, lcdHeight, jarFileURLString)
        throw EmulatorBridgeError.launchFailed
    }

    func shutdownRuntime() throws {
        guard let bootstrap = platformBootstrap, bootstrap.isPlatformReady else {
            return
        }
        // Keep the JVM + bootstrap alive; only stop the MIDlet.
        // Callers must await `stopMidletAsync` for a clean handoff — bridge.stop
        // uses the async path via FreeJ2MERuntimeAdapter.
        Self.appendDiagnostic("shutdownRuntime → stopMidlet (VM retained)")
    }

    /// Async MIDlet teardown used by the runtime adapter between games.
    func stopMidletAsync() async throws {
        while sessionGate {
            try await Task.sleep(for: .milliseconds(20))
        }
        sessionGate = true
        defer { sessionGate = false }
        guard let bootstrap = platformBootstrap, bootstrap.isPlatformReady else {
            return
        }
        try await bootstrap.stopMidlet()
        Self.appendDiagnostic("stopMidletAsync OK")
    }

    // MARK: - Input

    func keyPressed(_ keyCode: Int32) async throws {
        guard let platformBootstrap else { throw EmulatorBridgeError.launchFailed }
        try await platformBootstrap.keyPressed(keyCode)
    }

    func keyReleased(_ keyCode: Int32) async throws {
        guard let platformBootstrap else { throw EmulatorBridgeError.launchFailed }
        try await platformBootstrap.keyReleased(keyCode)
    }

    func keyRepeated(_ keyCode: Int32) async throws {
        guard let platformBootstrap else { throw EmulatorBridgeError.launchFailed }
        try await platformBootstrap.keyRepeated(keyCode)
    }

    func pointerPressed(x: Int32, y: Int32) async throws {
        guard let platformBootstrap else { throw EmulatorBridgeError.launchFailed }
        try await platformBootstrap.pointerPressed(x: x, y: y)
    }

    func pointerDragged(x: Int32, y: Int32) async throws {
        guard let platformBootstrap else { throw EmulatorBridgeError.launchFailed }
        try await platformBootstrap.pointerDragged(x: x, y: y)
    }

    func pointerReleased(x: Int32, y: Int32) async throws {
        guard let platformBootstrap else { throw EmulatorBridgeError.launchFailed }
        try await platformBootstrap.pointerReleased(x: x, y: y)
    }
}
