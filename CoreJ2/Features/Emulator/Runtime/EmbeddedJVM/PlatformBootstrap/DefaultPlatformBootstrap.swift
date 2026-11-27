import Foundation

/// Default iOS PlatformBootstrap (E3-US001…E3-US007).
///
/// Sequence on `initialize()`:
/// 1. `EmbeddedJVMControlling.ensureStarted()`
/// 2. `gateway.bind()`
/// 3. Register `NativeCallbackHost.onBootstrapHeartbeat` (infrastructure)
/// 4. `resolveClass(Mobile)` / `resolveClass(MobilePlatform)`
/// 5. `NewObject MobilePlatform(width, height)` → retain `JNIObjectId`
/// 6. `Mobile.setPlatform(platform)`
/// 7. Register `PainterRunnable.run`, construct Runnable, `MobilePlatform.setPainter`
///
/// After ready:
/// - `loadJar(url:)` → `MobilePlatform.loadJar` + `JarLoadSupport` name check
/// - `runJar()` → `RunJarSupport.runAndVerifyStartApp` (Vendor `runJar` + markers)
/// - `verifyDisplay()` → `DisplaySupport.inspectStatus` (Display + current Displayable)
///
/// Ownership: Bootstrap owns MobilePlatform + painter `JNIObjectId`s; Gateway owns GlobalRefs;
/// Manager owns JVM lifetime. When `onFrameExtracted` is set, each painter
/// invocation also extracts the LCD framebuffer via `LcdSupport` and emits a
/// `FreeJ2MEFrameCapture` (little-endian ARGB8888).
final class DefaultPlatformBootstrap: PlatformBootstrap, @unchecked Sendable {

    // MARK: - Constants

    /// Contract with `dev.abadx.corej2.bootstrap.NativeCallbackHost` (CoreJ2-owned).
    enum Infrastructure {
        static let callbackHostBinaryName = "dev/abadx/corej2/bootstrap/NativeCallbackHost"
        static let heartbeatMethodName = "onBootstrapHeartbeat"
        static let heartbeatSignature = "()V"
        static let fireHeartbeatMethodName = "fireHeartbeat"
        static let fireHeartbeatSignature = "()V"
    }

    /// Contract with `dev.abadx.corej2.bootstrap.AudioBridge` (CoreJ2-owned MMAPI → Swift).
    enum Audio {
        static let binaryName = "dev/abadx/corej2/bootstrap/AudioBridge"
        static let prepareName = "nativePrepare"
        static let startName = "nativeStart"
        static let stopName = "nativeStop"
        static let closeName = "nativeClose"
        static let setVolumeName = "nativeSetVolume"
        static let intVoidSignature = "(I)V"
        static let getPathName = "getPath"
        static let getMimeName = "getMime"
        static let getVolumeName = "getVolume"
        static let getLoopName = "getLoopCount"
        static let getStringSignature = "(I)Ljava/lang/String;"
        static let getIntSignature = "(I)I"
    }

    /// Contract with `dev.abadx.corej2.bootstrap.VibrationBridge`.
    enum Vibration {
        static let binaryName = "dev/abadx/corej2/bootstrap/VibrationBridge"
        static let nativeName = "nativeVibrate"
        static let signature = "(I)V"
    }

    /// CoreJ2-owned painter Runnable (not FreeJ2ME).
    enum Painter {
        static let binaryName = "dev/abadx/corej2/bootstrap/PainterRunnable"
        static let constructorName = "<init>"
        static let constructorSignature = "()V"
        static let runMethodName = "run"
        static let runSignature = "()V"
        static let firePaintMethodName = "firePaint"
        static let firePaintSignature = "()V"
    }

    /// FreeJ2ME MobilePlatform bind (Vendor read-only; resolved via classpath).
    enum FreeJ2ME {
        static let mobileBinaryName = "org/recompile/mobile/Mobile"
        static let mobilePlatformBinaryName = "org/recompile/mobile/MobilePlatform"
        static let constructorName = "<init>"
        static let constructorSignature = "(II)V"
        static let setPlatformName = "setPlatform"
        static let setPlatformSignature = "(Lorg/recompile/mobile/MobilePlatform;)V"
        static let setPainterName = "setPainter"
        static let setPainterSignature = "(Ljava/lang/Runnable;)V"
        static let loadJarName = "loadJar"
        static let loadJarSignature = "(Ljava/lang/String;)Z"
        static let runJarName = "runJar"
        static let runJarSignature = "()V"
        static let stopMidletName = "stopMidlet"
        static let stopMidletSignature = "()V"
        static let resizeLCDName = "resizeLCD"
        static let resizeLCDSignature = "(II)V"
        static let keyPressedName = "keyPressed"
        static let keyReleasedName = "keyReleased"
        static let keyRepeatedName = "keyRepeated"
        static let keySignature = "(I)V"
        static let pointerPressedName = "pointerPressed"
        static let pointerDraggedName = "pointerDragged"
        static let pointerReleasedName = "pointerReleased"
        static let pointerSignature = "(II)V"
        static let defaultLCDWidth: Int32 = 240
        static let defaultLCDHeight: Int32 = 320
    }

    /// CoreJ2-owned post-loadJar verification (not FreeJ2ME).
    enum JarLoad {
        static let supportBinaryName = "dev/abadx/corej2/bootstrap/JarLoadSupport"
        static let midletNameMethodName = "midletNameAfterLoad"
        static let midletNameSignature =
            "(Lorg/recompile/mobile/MobilePlatform;)Ljava/lang/String;"
    }

    /// CoreJ2-owned runJar + startApp marker verification (not FreeJ2ME).
    enum JarRun {
        static let supportBinaryName = "dev/abadx/corej2/bootstrap/RunJarSupport"
        static let runAndVerifyMethodName = "runAndVerifyStartApp"
        static let runAndVerifySignature = "(Lorg/recompile/mobile/MobilePlatform;)Z"
    }

    /// CoreJ2-owned Display lifecycle inspection (not FreeJ2ME).
    enum DisplayInspection {
        static let supportBinaryName = "dev/abadx/corej2/bootstrap/DisplaySupport"
        static let inspectStatusMethodName = "inspectStatus"
        static let inspectStatusSignature = "()Ljava/lang/String;"
        static let statusNoDisplay = "NO_DISPLAY"
        static let statusNoCurrent = "NO_CURRENT"
        static let statusOKPrefix = "OK:"
    }

    /// CoreJ2-owned LCD framebuffer extraction (not FreeJ2ME).
    enum Lcd {
        static let supportBinaryName = "dev/abadx/corej2/bootstrap/LcdSupport"
        static let extractPixelsMethodName = "extractPixelsIfChanged"
        static let extractPixelsSignature = "(Lorg/recompile/mobile/MobilePlatform;)[B"
        static let widthMethodName = "lcdWidth"
        static let widthSignature = "(Lorg/recompile/mobile/MobilePlatform;)I"
        static let heightMethodName = "lcdHeight"
        static let heightSignature = "(Lorg/recompile/mobile/MobilePlatform;)I"
        static let pixelFormatName = "TYPE_INT_ARGB"
    }

    // MARK: - Properties

    private let lock = NSLock()
    private let workQueue: DispatchQueue
    private let jvm: any EmbeddedJVMControlling
    private let gateway: any JNIGateway
    private let lcdWidth: Int32
    private let lcdHeight: Int32
    private let audioEngine = MMAAudioEngine()
    private let vibrationEngine = MMAVibrationEngine()

    private var stateStorage: PlatformBootstrapState = .uninitialized
    private var nativeRegistrations: [JNINativeRegistrationId] = []
    private var heartbeatCountStorage = 0
    private var paintCountStorage = 0
    private var mobilePlatformObjectIdStorage: JNIObjectId?
    private var painterObjectIdStorage: JNIObjectId?
    private var lastJarLoadResultStorage: FreeJ2MEJarLoadResult?
    private var lastJarRunResultStorage: FreeJ2MEJarRunResult?
    private var lastDisplayResultStorage: FreeJ2MEDisplayResult?
    private var onFrameExtractedStorage: ((FreeJ2MEFrameCapture) -> Void)?
    private var lcdSupportClassIdStorage: JNIClassId?
    private var lcdExtractMethodIdStorage: JNIMethodId?
    private var lcdWidthMethodIdStorage: JNIMethodId?
    private var lcdHeightMethodIdStorage: JNIMethodId?
    /// Frame pacing: drop extracts that arrive faster than ~30 fps.
    private var lastFrameEmitUptimeMs: UInt64 = 0
    private var framesEmittedStorage = 0
    private var framesDroppedUnchangedStorage = 0
    private var framesDroppedPacedStorage = 0
    /// Larger LCDs (640×360) emit more pixels — pace harder to cut heat.
    private let minFrameIntervalMs: UInt64

    /// Receives one `FreeJ2MEFrameCapture` per painter invocation once set.
    /// Set before `initialize()`; invoked on the Java painter thread.
    var onFrameExtracted: ((FreeJ2MEFrameCapture) -> Void)? {
        get {
            lock.lock()
            defer { lock.unlock() }
            return onFrameExtractedStorage
        }
        set {
            lock.lock()
            onFrameExtractedStorage = newValue
            lock.unlock()
        }
    }

    // MARK: - Init

    /// - Parameters:
    ///   - jvm: Embedded JVM controller (typically `EmbeddedJVMManager`).
    ///   - gateway: Bound only after the JVM is ready; must share the same readiness source as `jvm`.
    ///   - lcdWidth: Passed to `MobilePlatform(int,int)`.
    ///   - lcdHeight: Passed to `MobilePlatform(int,int)`.
    init(
        jvm: any EmbeddedJVMControlling,
        gateway: any JNIGateway,
        lcdWidth: Int32 = FreeJ2ME.defaultLCDWidth,
        lcdHeight: Int32 = FreeJ2ME.defaultLCDHeight,
        workQueue: DispatchQueue = DispatchQueue(label: "CoreJ2.PlatformBootstrap", qos: .userInitiated)
    ) {
        self.jvm = jvm
        self.gateway = gateway
        self.lcdWidth = lcdWidth
        self.lcdHeight = lcdHeight
        self.workQueue = workQueue
        let pixels = Int(lcdWidth) * Int(lcdHeight)
        // Pace harder on tall/wide LCDs to cut heat (240×400, 640×360, …).
        if pixels >= 200_000 {
            self.minFrameIntervalMs = 50 // ~20 fps (Prince / AC)
        } else if pixels >= 90_000 {
            self.minFrameIntervalMs = 40 // ~25 fps (Real Football / Diamond)
        } else {
            self.minFrameIntervalMs = 33 // ~30 fps (240×320)
        }
    }

    // MARK: - Diagnostics (tests)

    var registeredNativeCount: Int {
        lock.lock()
        defer { lock.unlock() }
        return nativeRegistrations.count
    }

    var heartbeatInvocationCount: Int {
        lock.lock()
        defer { lock.unlock() }
        return heartbeatCountStorage
    }

    var paintInvocationCount: Int {
        lock.lock()
        defer { lock.unlock() }
        return paintCountStorage
    }

    // MARK: - PlatformBootstrap

    var state: PlatformBootstrapState {
        lock.lock()
        defer { lock.unlock() }
        return stateStorage
    }

    var isReady: Bool {
        lock.lock()
        defer { lock.unlock() }
        if case .ready = stateStorage { return true }
        return false
    }

    var mobilePlatformObjectId: JNIObjectId? {
        lock.lock()
        defer { lock.unlock() }
        return mobilePlatformObjectIdStorage
    }

    var painterObjectId: JNIObjectId? {
        lock.lock()
        defer { lock.unlock() }
        return painterObjectIdStorage
    }

    var lastJarLoadResult: FreeJ2MEJarLoadResult? {
        lock.lock()
        defer { lock.unlock() }
        return lastJarLoadResultStorage
    }

    var lastJarRunResult: FreeJ2MEJarRunResult? {
        lock.lock()
        defer { lock.unlock() }
        return lastJarRunResultStorage
    }

    var lastDisplayResult: FreeJ2MEDisplayResult? {
        lock.lock()
        defer { lock.unlock() }
        return lastDisplayResultStorage
    }

    func initialize() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            workQueue.async {
                do {
                    try self.beginStartingSync()
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }

        do {
            try await jvm.ensureStarted()
        } catch {
            await setStateAsync(.failed(String(describing: error)))
            throw PlatformBootstrapError.jvmUnavailable(String(describing: error))
        }

        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            workQueue.async {
                do {
                    try self.bindGatewayRegisterAndCreatePlatformSync()
                    self.lock.lock()
                    self.stateStorage = .ready
                    self.lock.unlock()
                    continuation.resume()
                } catch {
                    self.lock.lock()
                    self.stateStorage = .failed(String(describing: error))
                    self.lock.unlock()
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func shutdown() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            workQueue.async {
                do {
                    try self.beginShuttingDownSync()
                    self.releasePainterAndPlatformSync()
                    self.unregisterAllNativesSync()
                    self.gateway.invalidate()
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }

        do {
            try await jvm.shutdown()
        } catch {
            await setStateAsync(.failed(String(describing: error)))
            throw PlatformBootstrapError.jvmUnavailable(String(describing: error))
        }

        await setStateAsync(.shutdown)
    }

    func loadJar(url: URL) async throws -> FreeJ2MEJarLoadResult {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<FreeJ2MEJarLoadResult, Error>) in
            workQueue.async {
                do {
                    let result = try self.loadJarSync(url: url)
                    continuation.resume(returning: result)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func runJar() async throws -> FreeJ2MEJarRunResult {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<FreeJ2MEJarRunResult, Error>) in
            workQueue.async {
                do {
                    let result = try self.runJarSync()
                    continuation.resume(returning: result)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Stops the current MIDlet without destroying the HotSpot VM.
    func stopMidlet() async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            workQueue.async {
                do {
                    try self.stopMidletSync()
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    /// Resizes the live LCD when switching profiles between games.
    func resizeLCD(width: Int32, height: Int32) async throws {
        try await invokePlatformVoid(
            name: FreeJ2ME.resizeLCDName,
            signature: FreeJ2ME.resizeLCDSignature,
            arguments: [.int(width), .int(height)]
        )
    }

    /// `true` when Contract C platform + painter are live on the shared JVM.
    var isPlatformReady: Bool {
        lock.lock()
        defer { lock.unlock() }
        if case .ready = stateStorage, mobilePlatformObjectIdStorage != nil {
            return true
        }
        return false
    }

    func verifyDisplay() async throws -> FreeJ2MEDisplayResult {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<FreeJ2MEDisplayResult, Error>) in
            workQueue.async {
                do {
                    let result = try self.verifyDisplaySync()
                    continuation.resume(returning: result)
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    func keyPressed(_ keyCode: Int32) async throws {
        try await invokePlatformVoid(name: FreeJ2ME.keyPressedName, signature: FreeJ2ME.keySignature, arguments: [.int(keyCode)])
    }

    func keyReleased(_ keyCode: Int32) async throws {
        try await invokePlatformVoid(name: FreeJ2ME.keyReleasedName, signature: FreeJ2ME.keySignature, arguments: [.int(keyCode)])
    }

    func keyRepeated(_ keyCode: Int32) async throws {
        try await invokePlatformVoid(name: FreeJ2ME.keyRepeatedName, signature: FreeJ2ME.keySignature, arguments: [.int(keyCode)])
    }

    func pointerPressed(x: Int32, y: Int32) async throws {
        try await invokePlatformVoid(
            name: FreeJ2ME.pointerPressedName,
            signature: FreeJ2ME.pointerSignature,
            arguments: [.int(x), .int(y)]
        )
    }

    func pointerDragged(x: Int32, y: Int32) async throws {
        try await invokePlatformVoid(
            name: FreeJ2ME.pointerDraggedName,
            signature: FreeJ2ME.pointerSignature,
            arguments: [.int(x), .int(y)]
        )
    }

    func pointerReleased(x: Int32, y: Int32) async throws {
        try await invokePlatformVoid(
            name: FreeJ2ME.pointerReleasedName,
            signature: FreeJ2ME.pointerSignature,
            arguments: [.int(x), .int(y)]
        )
    }

    // MARK: - Private

    private func invokePlatformVoid(
        name: String,
        signature: String,
        arguments: [JNIValue]
    ) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            workQueue.async {
                do {
                    try self.invokePlatformVoidSync(name: name, signature: signature, arguments: arguments)
                    continuation.resume()
                } catch {
                    continuation.resume(throwing: error)
                }
            }
        }
    }

    private func invokePlatformVoidSync(
        name: String,
        signature: String,
        arguments: [JNIValue]
    ) throws {
        lock.lock()
        let currentState = stateStorage
        let platformObjectId = mobilePlatformObjectIdStorage
        lock.unlock()

        guard case .ready = currentState, let platformObjectId else {
            throw PlatformBootstrapError.invalidState(currentState)
        }

        do {
            let platformClassId = try gateway.resolveClass(binaryName: FreeJ2ME.mobilePlatformBinaryName)
            let methodId = try gateway.resolveMethod(
                classId: platformClassId,
                name: name,
                signature: signature,
                isStatic: false
            )
            _ = try gateway.callInstance(
                objectId: platformObjectId,
                methodId: methodId,
                arguments: arguments,
                returning: .void
            )
        } catch let error as PlatformBootstrapError {
            throw error
        } catch {
            throw PlatformBootstrapError.gatewayFailure(String(describing: error))
        }
    }


    private func loadJarSync(url: URL) throws -> FreeJ2MEJarLoadResult {
        lock.lock()
        let currentState = stateStorage
        let platformObjectId = mobilePlatformObjectIdStorage
        lock.unlock()

        guard case .ready = currentState, let platformObjectId else {
            throw PlatformBootstrapError.invalidState(currentState)
        }

        let fileURL = Self.standardizedFileURL(from: url)
        try Self.validateJarURL(fileURL)
        let jarURLString = fileURL.absoluteString

        do {
            let platformClassId = try gateway.resolveClass(binaryName: FreeJ2ME.mobilePlatformBinaryName)
            let loadJarMethodId = try gateway.resolveMethod(
                classId: platformClassId,
                name: FreeJ2ME.loadJarName,
                signature: FreeJ2ME.loadJarSignature,
                isStatic: false
            )
            let loadedValue = try gateway.callInstance(
                objectId: platformObjectId,
                methodId: loadJarMethodId,
                arguments: [.string(jarURLString)],
                returning: .boolean
            )
            guard case .boolean(true) = loadedValue else {
                throw PlatformBootstrapError.loadJarFailed(jarURLString)
            }

            let supportClassId = try gateway.resolveClass(binaryName: JarLoad.supportBinaryName)
            let midletNameMethodId = try gateway.resolveMethod(
                classId: supportClassId,
                name: JarLoad.midletNameMethodName,
                signature: JarLoad.midletNameSignature,
                isStatic: true
            )
            let nameValue = try gateway.callStatic(
                classId: supportClassId,
                methodId: midletNameMethodId,
                arguments: [.object(platformObjectId)],
                returning: .string
            )
            guard case .string(let midletName) = nameValue, !midletName.isEmpty else {
                throw PlatformBootstrapError.malformedJar(jarURLString)
            }

            let result = FreeJ2MEJarLoadResult(midletName: midletName, jarURLString: jarURLString)
            lock.lock()
            lastJarLoadResultStorage = result
            // A new load invalidates any prior run / display verification for this session.
            lastJarRunResultStorage = nil
            lastDisplayResultStorage = nil
            lock.unlock()
            return result
        } catch let error as PlatformBootstrapError {
            throw error
        } catch {
            throw PlatformBootstrapError.gatewayFailure(String(describing: error))
        }
    }

    private func runJarSync() throws -> FreeJ2MEJarRunResult {
        lock.lock()
        let currentState = stateStorage
        let platformObjectId = mobilePlatformObjectIdStorage
        let loadResult = lastJarLoadResultStorage
        let priorRun = lastJarRunResultStorage
        lock.unlock()

        guard case .ready = currentState, let platformObjectId else {
            throw PlatformBootstrapError.invalidState(currentState)
        }
        guard let loadResult else {
            throw PlatformBootstrapError.midletNotLoaded
        }
        if priorRun != nil {
            throw PlatformBootstrapError.alreadyRunning
        }

        do {
            let supportClassId = try gateway.resolveClass(binaryName: JarRun.supportBinaryName)
            let verifyMethodId = try gateway.resolveMethod(
                classId: supportClassId,
                name: JarRun.runAndVerifyMethodName,
                signature: JarRun.runAndVerifySignature,
                isStatic: true
            )
            let verifyValue = try gateway.callStatic(
                classId: supportClassId,
                methodId: verifyMethodId,
                arguments: [.object(platformObjectId)],
                returning: .boolean
            )
            guard case .boolean(true) = verifyValue else {
                throw PlatformBootstrapError.runJarFailed(loadResult.jarURLString)
            }

            let result = FreeJ2MEJarRunResult(
                midletName: loadResult.midletName,
                jarURLString: loadResult.jarURLString,
                reachedStartApp: true
            )
            lock.lock()
            lastJarRunResultStorage = result
            lastDisplayResultStorage = nil
            lock.unlock()
            return result
        } catch let error as PlatformBootstrapError {
            throw error
        } catch {
            throw PlatformBootstrapError.gatewayFailure(String(describing: error))
        }
    }

    private func verifyDisplaySync() throws -> FreeJ2MEDisplayResult {
        lock.lock()
        let currentState = stateStorage
        let runResult = lastJarRunResultStorage
        lock.unlock()

        guard case .ready = currentState else {
            throw PlatformBootstrapError.invalidState(currentState)
        }
        guard let runResult else {
            throw PlatformBootstrapError.midletNotRunning
        }

        do {
            let supportClassId = try gateway.resolveClass(
                binaryName: DisplayInspection.supportBinaryName
            )
            let inspectMethodId = try gateway.resolveMethod(
                classId: supportClassId,
                name: DisplayInspection.inspectStatusMethodName,
                signature: DisplayInspection.inspectStatusSignature,
                isStatic: true
            )
            let statusValue = try gateway.callStatic(
                classId: supportClassId,
                methodId: inspectMethodId,
                arguments: [],
                returning: .string
            )
            guard case .string(let status) = statusValue else {
                throw PlatformBootstrapError.displayNotPresent
            }

            if status == DisplayInspection.statusNoDisplay {
                throw PlatformBootstrapError.displayNotPresent
            }
            if status == DisplayInspection.statusNoCurrent {
                throw PlatformBootstrapError.noCurrentDisplayable
            }
            guard status.hasPrefix(DisplayInspection.statusOKPrefix) else {
                throw PlatformBootstrapError.noCurrentDisplayable
            }

            let className = String(status.dropFirst(DisplayInspection.statusOKPrefix.count))
            let result = FreeJ2MEDisplayResult(
                midletName: runResult.midletName,
                jarURLString: runResult.jarURLString,
                displayInitialized: true,
                hasCurrentDisplayable: true,
                currentDisplayableClassName: className.isEmpty ? nil : className
            )
            lock.lock()
            lastDisplayResultStorage = result
            lock.unlock()
            return result
        } catch let error as PlatformBootstrapError {
            throw error
        } catch {
            throw PlatformBootstrapError.gatewayFailure(String(describing: error))
        }
    }

    private static func standardizedFileURL(from url: URL) -> URL {
        if url.isFileURL {
            return url.standardizedFileURL
        }
        return URL(fileURLWithPath: url.path).standardizedFileURL
    }

    private static func validateJarURL(_ fileURL: URL) throws {
        let path = fileURL.path
        guard FileManager.default.fileExists(atPath: path) else {
            throw PlatformBootstrapError.jarNotFound(path)
        }
        guard FileManager.default.isReadableFile(atPath: path) else {
            throw PlatformBootstrapError.jarNotReadable(path)
        }
    }

    private func setStateAsync(_ state: PlatformBootstrapState) async {
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            workQueue.async {
                self.lock.lock()
                self.stateStorage = state
                self.lock.unlock()
                continuation.resume()
            }
        }
    }

    private func beginStartingSync() throws {
        lock.lock()
        defer { lock.unlock() }
        switch stateStorage {
        case .uninitialized, .shutdown:
            stateStorage = .starting
            paintCountStorage = 0
            heartbeatCountStorage = 0
        case .ready, .starting, .shuttingDown, .failed:
            throw PlatformBootstrapError.invalidState(stateStorage)
        }
    }

    private func beginShuttingDownSync() throws {
        lock.lock()
        defer { lock.unlock() }
        switch stateStorage {
        case .ready, .starting, .failed, .uninitialized:
            stateStorage = .shuttingDown
        case .shuttingDown, .shutdown:
            throw PlatformBootstrapError.invalidState(stateStorage)
        }
    }

    private func bindGatewayRegisterAndCreatePlatformSync() throws {
        do {
            print("corej2-launch: bootstrap bind gateway")
            try gateway.bind()
            print("corej2-launch: bootstrap register natives")
            try registerInfrastructureNativesSync()
            print("corej2-launch: bootstrap create MobilePlatform")
            try createAndBindMobilePlatformSync()
            print("corej2-launch: bootstrap install painter")
            try installPainterSync()
            print("corej2-launch: bootstrap ready")
        } catch let error as PlatformBootstrapError {
            print("corej2-launch: bootstrap FAILED \(error)")
            releasePainterAndPlatformSync()
            gateway.invalidate()
            throw error
        } catch {
            print("corej2-launch: bootstrap FAILED gateway \(error)")
            releasePainterAndPlatformSync()
            gateway.invalidate()
            throw PlatformBootstrapError.gatewayFailure(String(describing: error))
        }
    }

    private func registerInfrastructureNativesSync() throws {
        let classId = try gateway.resolveClass(binaryName: Infrastructure.callbackHostBinaryName)
        let registration = try gateway.registerNative(
            classId: classId,
            methodName: Infrastructure.heartbeatMethodName,
            signature: Infrastructure.heartbeatSignature,
            isStatic: true
        ) { [weak self] _ in
            self?.noteHeartbeat()
            return .void
        }
        lock.lock()
        nativeRegistrations.append(registration)
        lock.unlock()

        try registerAudioNativesSync()
        try registerVibrationNativesSync()
    }

    private func registerVibrationNativesSync() throws {
        let classId = try gateway.resolveClass(binaryName: Vibration.binaryName)
        let registration = try gateway.registerNative(
            classId: classId,
            methodName: Vibration.nativeName,
            signature: Vibration.signature,
            isStatic: true
        ) { [weak self] event in
            if let first = event.arguments.first, case .int(let durationMs) = first {
                self?.vibrationEngine.vibrate(durationMs: durationMs)
            }
            return .void
        }
        lock.lock()
        nativeRegistrations.append(registration)
        lock.unlock()
    }

    private func stopMidletSync() throws {
        lock.lock()
        let currentState = stateStorage
        let platformObjectId = mobilePlatformObjectIdStorage
        lock.unlock()
        guard case .ready = currentState, let platformObjectId else {
            throw PlatformBootstrapError.invalidState(currentState)
        }
        let platformClassId = try gateway.resolveClass(binaryName: FreeJ2ME.mobilePlatformBinaryName)
        let stopId = try gateway.resolveMethod(
            classId: platformClassId,
            name: FreeJ2ME.stopMidletName,
            signature: FreeJ2ME.stopMidletSignature,
            isStatic: false
        )
        _ = try gateway.callInstance(
            objectId: platformObjectId,
            methodId: stopId,
            arguments: [],
            returning: .void
        )
        // Belt-and-suspenders: even if Java stopAll missed a slot, silence AV*.
        audioEngine.stopAll()
        lock.lock()
        lastJarLoadResultStorage = nil
        lastJarRunResultStorage = nil
        lastDisplayResultStorage = nil
        lastFrameEmitUptimeMs = 0
        framesEmittedStorage = 0
        framesDroppedUnchangedStorage = 0
        framesDroppedPacedStorage = 0
        lock.unlock()
        // Reset Java LcdSupport change-detection between sessions.
        try? resetLcdFrameCacheSync()
        print("corej2-launch: stopMidlet OK")
    }

    private func resetLcdFrameCacheSync() throws {
        let classId = try gateway.resolveClass(binaryName: Lcd.supportBinaryName)
        let methodId = try gateway.resolveMethod(
            classId: classId,
            name: "resetFrameCache",
            signature: "()V",
            isStatic: true
        )
        _ = try gateway.callStatic(
            classId: classId,
            methodId: methodId,
            arguments: [],
            returning: .void
        )
    }

    private func registerAudioNativesSync() throws {
        let classId = try gateway.resolveClass(binaryName: Audio.binaryName)
        let names = [
            Audio.prepareName,
            Audio.startName,
            Audio.stopName,
            Audio.closeName,
            Audio.setVolumeName,
        ]
        for name in names {
            let methodName = name
            let registration = try gateway.registerNative(
                classId: classId,
                methodName: methodName,
                signature: Audio.intVoidSignature,
                isStatic: true
            ) { [weak self] event in
                guard let self else { return .void }
                guard let first = event.arguments.first, case .int(let playerId) = first else {
                    return .void
                }
                switch methodName {
                case Audio.prepareName:
                    self.handleAudioPrepare(playerId)
                case Audio.startName:
                    self.audioEngine.start(playerId: playerId)
                case Audio.stopName:
                    self.audioEngine.stop(playerId: playerId)
                case Audio.closeName:
                    self.audioEngine.close(playerId: playerId)
                case Audio.setVolumeName:
                    self.handleAudioSetVolume(playerId)
                default:
                    break
                }
                return .void
            }
            lock.lock()
            nativeRegistrations.append(registration)
            lock.unlock()
        }
    }

    private func handleAudioPrepare(_ playerId: Int32) {
        do {
            let classId = try gateway.resolveClass(binaryName: Audio.binaryName)
            let pathId = try gateway.resolveMethod(
                classId: classId,
                name: Audio.getPathName,
                signature: Audio.getStringSignature,
                isStatic: true
            )
            let mimeId = try gateway.resolveMethod(
                classId: classId,
                name: Audio.getMimeName,
                signature: Audio.getStringSignature,
                isStatic: true
            )
            let volumeId = try gateway.resolveMethod(
                classId: classId,
                name: Audio.getVolumeName,
                signature: Audio.getIntSignature,
                isStatic: true
            )
            let loopId = try gateway.resolveMethod(
                classId: classId,
                name: Audio.getLoopName,
                signature: Audio.getIntSignature,
                isStatic: true
            )
            let pathValue = try gateway.callStatic(
                classId: classId,
                methodId: pathId,
                arguments: [.int(playerId)],
                returning: .string
            )
            let mimeValue = try gateway.callStatic(
                classId: classId,
                methodId: mimeId,
                arguments: [.int(playerId)],
                returning: .string
            )
            let volumeValue = try gateway.callStatic(
                classId: classId,
                methodId: volumeId,
                arguments: [.int(playerId)],
                returning: .int
            )
            let loopValue = try gateway.callStatic(
                classId: classId,
                methodId: loopId,
                arguments: [.int(playerId)],
                returning: .int
            )
            guard case .string(let path) = pathValue else { return }
            let mime: String
            if case .string(let value) = mimeValue {
                mime = value
            } else {
                mime = ""
            }
            let volume: Int32
            if case .int(let value) = volumeValue {
                volume = value
            } else {
                volume = 100
            }
            let loops: Int32
            if case .int(let value) = loopValue {
                loops = value
            } else {
                loops = 1
            }
            audioEngine.prepare(
                playerId: playerId,
                path: path,
                mime: mime,
                volume: volume,
                loopCount: loops
            )
        } catch {
            print("corej2-audio: prepare handler failed \(error)")
        }
    }

    private func handleAudioSetVolume(_ playerId: Int32) {
        do {
            let classId = try gateway.resolveClass(binaryName: Audio.binaryName)
            let volumeId = try gateway.resolveMethod(
                classId: classId,
                name: Audio.getVolumeName,
                signature: Audio.getIntSignature,
                isStatic: true
            )
            let volumeValue = try gateway.callStatic(
                classId: classId,
                methodId: volumeId,
                arguments: [.int(playerId)],
                returning: .int
            )
            if case .int(let volume) = volumeValue {
                audioEngine.setVolume(playerId: playerId, volume: volume)
            }
        } catch {
            print("corej2-audio: volume handler failed \(error)")
        }
    }

    private func createAndBindMobilePlatformSync() throws {
        let mobileClassId = try gateway.resolveClass(binaryName: FreeJ2ME.mobileBinaryName)
        let platformClassId = try gateway.resolveClass(binaryName: FreeJ2ME.mobilePlatformBinaryName)
        let constructorId = try gateway.resolveMethod(
            classId: platformClassId,
            name: FreeJ2ME.constructorName,
            signature: FreeJ2ME.constructorSignature,
            isStatic: false
        )
        let platformObjectId = try gateway.createGlobalObjectReference(
            classId: platformClassId,
            constructorMethodId: constructorId,
            arguments: [.int(lcdWidth), .int(lcdHeight)]
        )
        lock.lock()
        mobilePlatformObjectIdStorage = platformObjectId
        lock.unlock()

        let setPlatformId = try gateway.resolveMethod(
            classId: mobileClassId,
            name: FreeJ2ME.setPlatformName,
            signature: FreeJ2ME.setPlatformSignature,
            isStatic: true
        )
        _ = try gateway.callStatic(
            classId: mobileClassId,
            methodId: setPlatformId,
            arguments: [.object(platformObjectId)],
            returning: .void
        )
    }

    private func installPainterSync() throws {
        guard let platformObjectId = mobilePlatformObjectIdStorage else {
            throw PlatformBootstrapError.gatewayFailure("MobilePlatform handle missing before setPainter")
        }

        let painterClassId = try gateway.resolveClass(binaryName: Painter.binaryName)
        let registration = try gateway.registerNative(
            classId: painterClassId,
            methodName: Painter.runMethodName,
            signature: Painter.runSignature,
            isStatic: false
        ) { [weak self] _ in
            self?.notePaint()
            return .void
        }
        lock.lock()
        nativeRegistrations.append(registration)
        lock.unlock()

        let constructorId = try gateway.resolveMethod(
            classId: painterClassId,
            name: Painter.constructorName,
            signature: Painter.constructorSignature,
            isStatic: false
        )
        let painterObjectId = try gateway.createGlobalObjectReference(
            classId: painterClassId,
            constructorMethodId: constructorId,
            arguments: []
        )
        lock.lock()
        painterObjectIdStorage = painterObjectId
        lock.unlock()

        let platformClassId = try gateway.resolveClass(binaryName: FreeJ2ME.mobilePlatformBinaryName)
        let setPainterId = try gateway.resolveMethod(
            classId: platformClassId,
            name: FreeJ2ME.setPainterName,
            signature: FreeJ2ME.setPainterSignature,
            isStatic: false
        )
        _ = try gateway.callInstance(
            objectId: platformObjectId,
            methodId: setPainterId,
            arguments: [.object(painterObjectId)],
            returning: .void
        )
    }

    private func noteHeartbeat() {
        lock.lock()
        heartbeatCountStorage += 1
        lock.unlock()
    }

    private func notePaint() {
        lock.lock()
        paintCountStorage += 1
        let paintIndex = paintCountStorage
        let handler = onFrameExtractedStorage
        let nowMs = DispatchTime.now().uptimeNanoseconds / 1_000_000
        let elapsed = nowMs &- lastFrameEmitUptimeMs
        let shouldPace = lastFrameEmitUptimeMs != 0 && elapsed < minFrameIntervalMs
        if shouldPace {
            framesDroppedPacedStorage += 1
            lock.unlock()
            return
        }
        lock.unlock()

        guard let handler else {
            return
        }
        do {
            guard let frame = try extractFrameSync(paintIndex: paintIndex) else {
                lock.lock()
                framesDroppedUnchangedStorage += 1
                lock.unlock()
                return
            }
            lock.lock()
            lastFrameEmitUptimeMs = DispatchTime.now().uptimeNanoseconds / 1_000_000
            framesEmittedStorage += 1
            let emitted = framesEmittedStorage
            let droppedU = framesDroppedUnchangedStorage
            let droppedP = framesDroppedPacedStorage
            lock.unlock()
            if emitted == 1 || emitted % 120 == 0 {
                print(
                    "corej2-perf: frames emitted=\(emitted) skippedUnchanged=\(droppedU) paced=\(droppedP) paintIndex=\(paintIndex)"
                )
            }
            handler(frame)
        } catch {
            // Keep painter alive even if a single extract fails.
        }
    }

    /// Reads the current LCD framebuffer through `LcdSupport` and packages it
    /// as a `FreeJ2MEFrameCapture`. Runs on the Java painter thread.
    ///
    /// Returns `nil` when the LCD content is unchanged (Java returned null).
    private func extractFrameSync(paintIndex: Int) throws -> FreeJ2MEFrameCapture? {
        lock.lock()
        let platformObjectId = mobilePlatformObjectIdStorage
        lock.unlock()
        guard let platformObjectId else {
            throw PlatformBootstrapError.gatewayFailure("MobilePlatform handle missing before frame extraction")
        }

        let (classId, extractId, widthId, heightId) = try resolveLcdSupportSync()

        let widthValue = try gateway.callStatic(
            classId: classId,
            methodId: widthId,
            arguments: [.object(platformObjectId)],
            returning: .int
        )
        let heightValue = try gateway.callStatic(
            classId: classId,
            methodId: heightId,
            arguments: [.object(platformObjectId)],
            returning: .int
        )
        guard case .int(let width) = widthValue, case .int(let height) = heightValue, width > 0, height > 0 else {
            throw PlatformBootstrapError.gatewayFailure("LcdSupport returned invalid LCD size")
        }

        let pixelsValue = try gateway.callStatic(
            classId: classId,
            methodId: extractId,
            arguments: [.object(platformObjectId)],
            returning: .object
        )
        // extractPixelsIfChanged returns null when the framebuffer is unchanged.
        switch pixelsValue {
        case .null:
            return nil
        case .object(let pixelsObjectId):
            defer {
                try? gateway.releaseObjectReference(pixelsObjectId)
            }
            let pixels = try gateway.readByteArray(objectId: pixelsObjectId)
            return FreeJ2MEFrameCapture(
                paintIndex: paintIndex,
                width: Int(width),
                height: Int(height),
                pixelCount: Int(width) * Int(height),
                checksum: Self.fnv1aChecksum(ofLittleEndianARGB: pixels),
                pixelFormat: Lcd.pixelFormatName,
                pixels: pixels
            )
        default:
            return nil
        }
    }

    /// Resolves (once) the `LcdSupport` class and method handles.
    private func resolveLcdSupportSync() throws -> (JNIClassId, JNIMethodId, JNIMethodId, JNIMethodId) {
        lock.lock()
        if let classId = lcdSupportClassIdStorage,
           let extractId = lcdExtractMethodIdStorage,
           let widthId = lcdWidthMethodIdStorage,
           let heightId = lcdHeightMethodIdStorage {
            lock.unlock()
            return (classId, extractId, widthId, heightId)
        }
        lock.unlock()

        let classId = try gateway.resolveClass(binaryName: Lcd.supportBinaryName)
        let extractId = try gateway.resolveMethod(
            classId: classId,
            name: Lcd.extractPixelsMethodName,
            signature: Lcd.extractPixelsSignature,
            isStatic: true
        )
        let widthId = try gateway.resolveMethod(
            classId: classId,
            name: Lcd.widthMethodName,
            signature: Lcd.widthSignature,
            isStatic: true
        )
        let heightId = try gateway.resolveMethod(
            classId: classId,
            name: Lcd.heightMethodName,
            signature: Lcd.heightSignature,
            isStatic: true
        )

        lock.lock()
        lcdSupportClassIdStorage = classId
        lcdExtractMethodIdStorage = extractId
        lcdWidthMethodIdStorage = widthId
        lcdHeightMethodIdStorage = heightId
        lock.unlock()
        return (classId, extractId, widthId, heightId)
    }

    /// FNV-1a over packed ARGB ints, matching the host bootstrap checksum.
    private static func fnv1aChecksum(ofLittleEndianARGB pixels: Data) -> UInt64 {
        var checksum: UInt64 = 0xcbf29ce484222325
        var index = pixels.startIndex
        while index + 4 <= pixels.endIndex {
            let value = UInt32(pixels[index])
                | UInt32(pixels[index + 1]) << 8
                | UInt32(pixels[index + 2]) << 16
                | UInt32(pixels[index + 3]) << 24
            checksum ^= UInt64(value)
            checksum &*= 0x100000001b3
            index += 4
        }
        return checksum
    }

    private func releasePainterAndPlatformSync() {
        lock.lock()
        let painterId = painterObjectIdStorage
        let platformId = mobilePlatformObjectIdStorage
        painterObjectIdStorage = nil
        mobilePlatformObjectIdStorage = nil
        lastJarLoadResultStorage = nil
        lastJarRunResultStorage = nil
        lastDisplayResultStorage = nil
        lcdSupportClassIdStorage = nil
        lcdExtractMethodIdStorage = nil
        lcdWidthMethodIdStorage = nil
        lcdHeightMethodIdStorage = nil
        lock.unlock()
        if let painterId {
            try? gateway.releaseObjectReference(painterId)
        }
        if let platformId {
            try? gateway.releaseObjectReference(platformId)
        }
    }

    private func unregisterAllNativesSync() {
        lock.lock()
        let registrations = nativeRegistrations
        nativeRegistrations.removeAll(keepingCapacity: false)
        lock.unlock()
        for registration in registrations {
            try? gateway.unregisterNative(registration)
        }
    }
}
