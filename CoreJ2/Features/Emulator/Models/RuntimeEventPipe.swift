import Foundation

/// Publishes `RuntimeEvent` values to one or more `AsyncStream` subscribers.
///
/// Each access to ``events`` creates a fresh stream, so a new EmulatorViewModel
/// can observe after the previous one cancelled — required for N games per process.
@MainActor
final class RuntimeEventPipe {

    // MARK: - Nested

    private struct Subscriber {
        let id: UUID
        let continuation: AsyncStream<RuntimeEvent>.Continuation
    }

    // MARK: - Properties

    private var subscribers: [UUID: AsyncStream<RuntimeEvent>.Continuation] = [:]

    // MARK: - Init

    init(bufferSize: Int = 64) {
        // bufferSize kept for API compatibility with call sites; fan-out uses
        // bufferingNewest per subscriber stream.
        _ = bufferSize
    }

    // MARK: - Stream

    /// A new stream that receives every subsequent ``yield(_:)``.
    var events: AsyncStream<RuntimeEvent> {
        let id = UUID()
        let (stream, continuation) = AsyncStream.makeStream(
            of: RuntimeEvent.self,
            bufferingPolicy: .bufferingNewest(64)
        )
        subscribers[id] = continuation
        continuation.onTermination = { [weak self] _ in
            Task { @MainActor in
                self?.subscribers.removeValue(forKey: id)
            }
        }
        return stream
    }

    // MARK: - Publishing

    /// Yields `event` to every active subscriber.
    func yield(_ event: RuntimeEvent) {
        for continuation in subscribers.values {
            continuation.yield(event)
        }
    }

    /// Ends every subscriber stream.
    func finish() {
        for continuation in subscribers.values {
            continuation.finish()
        }
        subscribers.removeAll()
    }
}
