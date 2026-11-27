import XCTest
@testable import CoreJ2

@MainActor
final class EmulatorLaunchValidationTests: XCTestCase {

    // MARK: - Missing JAR

    func testLaunchThrowsWhenJARIsMissing() async {
        let bridge = DefaultEmulatorBridge()
        let missingURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("jar")
        let configuration = makeConfiguration(jarURL: missingURL)

        do {
            _ = try await bridge.launch(configuration)
            XCTFail("Expected launch to throw")
        } catch {
            XCTAssertEqual(error as? EmulatorBridgeError, .jarNotFound)
        }
    }

    // MARK: - Unreadable JAR

    func testLaunchThrowsWhenJARIsUnreadable() async throws {
        let jarURL = try makeTemporaryJAR()
        defer { try? FileManager.default.removeItem(at: jarURL) }

        try FileManager.default.setAttributes(
            [.posixPermissions: 0o000],
            ofItemAtPath: jarURL.path(percentEncoded: false)
        )
        defer {
            try? FileManager.default.setAttributes(
                [.posixPermissions: 0o644],
                ofItemAtPath: jarURL.path(percentEncoded: false)
            )
        }

        let bridge = DefaultEmulatorBridge()
        let configuration = makeConfiguration(jarURL: jarURL)

        do {
            _ = try await bridge.launch(configuration)
            XCTFail("Expected launch to throw")
        } catch {
            XCTAssertEqual(error as? EmulatorBridgeError, .jarNotReadable)
        }
    }

    // MARK: - Second Launch Rejected

    func testSecondLaunchIsRejectedWhileSessionIsActive() async throws {
        let jarURL = try makeTemporaryJAR()
        defer { try? FileManager.default.removeItem(at: jarURL) }

        let bridge = DefaultEmulatorBridge()
        let configuration = makeConfiguration(jarURL: jarURL)
        let first = try await bridge.launch(configuration)

        do {
            _ = try await bridge.launch(configuration)
            XCTFail("Expected second launch to throw")
        } catch {
            XCTAssertEqual(error as? EmulatorBridgeError, .runtimeAlreadyRunning)
        }
        XCTAssertEqual(first.state, .running)
    }

    func testLaunchSucceedsAfterPreviousSessionIsStopped() async throws {
        let jarURL = try makeTemporaryJAR()
        defer { try? FileManager.default.removeItem(at: jarURL) }

        let bridge = DefaultEmulatorBridge()
        let configuration = makeConfiguration(jarURL: jarURL)
        let first = try await bridge.launch(configuration)
        try bridge.stop(first)

        let second = try await bridge.launch(configuration)

        XCTAssertEqual(second.state, .running)
        XCTAssertNotEqual(first.id, second.id)
    }

    // MARK: - Valid Launch

    func testValidLaunchSucceeds() async throws {
        let jarURL = try makeTemporaryJAR()
        defer { try? FileManager.default.removeItem(at: jarURL) }

        let bridge = DefaultEmulatorBridge()
        let configuration = makeConfiguration(jarURL: jarURL)

        let session = try await bridge.launch(configuration)

        XCTAssertEqual(session.state, .running)
        XCTAssertEqual(session.configuration, configuration)
        XCTAssertNil(session.endedAt)
    }

    // MARK: - Helpers

    private func makeConfiguration(jarURL: URL) -> LaunchConfiguration {
        LaunchConfiguration(
            game: InstalledGame(
                id: UUID(),
                title: "Test Game",
                jarURL: jarURL,
                importedAt: Date(),
                contentHash: "hash"
            )
        )
    }

    private func makeTemporaryJAR() throws -> URL {
        let jarURL = FileManager.default.temporaryDirectory
            .appendingPathComponent(UUID().uuidString)
            .appendingPathExtension("jar")
        try Data([0x50, 0x4B, 0x03, 0x04]).write(to: jarURL)
        return jarURL
    }
}
