import Foundation

/// Abstraction over the native / FreeJ2ME runtime host.
///
/// `EmulatorBridge` owns session validation and lifecycle state. Implementations
/// of this protocol perform the actual runtime work and publish `RuntimeEvent`s.
@MainActor
protocol RuntimeHostProtocol {

    /// Lifecycle and frame signals produced by this host.
    var events: AsyncStream<RuntimeEvent> { get }

    /// Starts (or prepares) a runtime for `configuration`.
    ///
    /// Placeholder hosts must succeed without starting an emulator.
    /// Async so embedded JVM bring-up does not block the main actor.
    /// - Parameter configuration: Validated launch inputs from the bridge.
    /// - Throws: If the runtime cannot start.
    func launch(_ configuration: LaunchConfiguration) async throws

    /// Suspends runtime activity for `session`.
    /// - Parameter session: The active session to pause.
    /// - Throws: If the runtime cannot pause.
    func pause(_ session: EmulatorSession) throws

    /// Resumes runtime activity for `session`.
    /// - Parameter session: The paused session to resume.
    /// - Throws: If the runtime cannot resume.
    func resume(_ session: EmulatorSession) throws

    /// Tears down runtime activity for `session` (stops MIDlet; keeps JVM alive).
    /// - Parameter session: The session to stop.
    /// - Throws: If the runtime cannot stop.
    func stop(_ session: EmulatorSession) async throws

    /// Injects a MIDP key press into the running MIDlet.
    func keyPressed(_ keyCode: Int32) async throws

    /// Injects a MIDP key release into the running MIDlet.
    func keyReleased(_ keyCode: Int32) async throws

    /// Injects a MIDP key-repeat into the running MIDlet.
    func keyRepeated(_ keyCode: Int32) async throws

    /// Injects a pointer press in LCD pixel coordinates.
    func pointerPressed(x: Int32, y: Int32) async throws

    /// Injects a pointer drag in LCD pixel coordinates.
    func pointerDragged(x: Int32, y: Int32) async throws

    /// Injects a pointer release in LCD pixel coordinates.
    func pointerReleased(x: Int32, y: Int32) async throws
}

extension RuntimeHostProtocol {
    func keyPressed(_ keyCode: Int32) async throws { _ = keyCode }
    func keyReleased(_ keyCode: Int32) async throws { _ = keyCode }
    func keyRepeated(_ keyCode: Int32) async throws { _ = keyCode }
    func pointerPressed(x: Int32, y: Int32) async throws { _ = (x, y) }
    func pointerDragged(x: Int32, y: Int32) async throws { _ = (x, y) }
    func pointerReleased(x: Int32, y: Int32) async throws { _ = (x, y) }
}
