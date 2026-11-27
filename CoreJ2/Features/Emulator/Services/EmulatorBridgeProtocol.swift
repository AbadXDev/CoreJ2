import Foundation

/// Single entry point for launching and controlling installed Java ME games.
///
/// CoreJ2 talks to the underlying emulator only through this bridge so FreeJ2ME
/// can be integrated later without coupling UI or Library code to the runtime.
@MainActor
protocol EmulatorBridgeProtocol {

    /// Lifecycle and frame signals forwarded from the underlying runtime host.
    var runtimeEvents: AsyncStream<RuntimeEvent> { get }

    /// Prepares an emulator session for `configuration`.
    ///
    /// Placeholder implementations must not start an emulator process.
    /// Async so embedded JVM bring-up does not block the main actor.
    /// - Returns: A session representing the prepared launch.
    /// - Throws: If the session cannot be prepared.
    func launch(_ configuration: LaunchConfiguration) async throws -> EmulatorSession

    /// Suspends a running session.
    ///
    /// - Parameter session: Session currently in `.running`.
    /// - Throws: `EmulatorBridgeError.invalidSession` when the transition is not allowed.
    func pause(_ session: EmulatorSession) throws

    /// Resumes a paused session.
    ///
    /// - Parameter session: Session currently in `.paused`.
    /// - Throws: `EmulatorBridgeError.invalidSession` when the transition is not allowed.
    func resume(_ session: EmulatorSession) throws

    /// Tears down a running or paused session (stops MIDlet; JVM stays alive).
    ///
    /// Transitions through `.stopping` into `.stopped`.
    /// - Parameter session: Session currently in `.running` or `.paused`.
    /// - Throws: `EmulatorBridgeError.invalidSession` when the transition is not allowed.
    func stop(_ session: EmulatorSession) async throws

    /// Injects a MIDP key press into the active session.
    func keyPressed(_ keyCode: Int32) async throws

    /// Injects a MIDP key release into the active session.
    func keyReleased(_ keyCode: Int32) async throws

    /// Injects a MIDP key-repeat into the active session.
    func keyRepeated(_ keyCode: Int32) async throws

    /// Injects a pointer press (LCD pixel coordinates).
    func pointerPressed(x: Int32, y: Int32) async throws

    /// Injects a pointer drag (LCD pixel coordinates).
    func pointerDragged(x: Int32, y: Int32) async throws

    /// Injects a pointer release (LCD pixel coordinates).
    func pointerReleased(x: Int32, y: Int32) async throws
}

extension EmulatorBridgeProtocol {
    func keyPressed(_ keyCode: Int32) async throws { _ = keyCode }
    func keyReleased(_ keyCode: Int32) async throws { _ = keyCode }
    func keyRepeated(_ keyCode: Int32) async throws { _ = keyCode }
    func pointerPressed(x: Int32, y: Int32) async throws { _ = (x, y) }
    func pointerDragged(x: Int32, y: Int32) async throws { _ = (x, y) }
    func pointerReleased(x: Int32, y: Int32) async throws { _ = (x, y) }
}
