import XCTest
@testable import CoreJ2

final class EmulatorViewLayoutTests: XCTestCase {

    // MARK: - fittedLCDSize (aspect fit, never crop)

    func testFittedLCDSizeHeightLimitedForTallPhoneLCD() {
        // XS Max-ish LCD slot under keypad: wider aspect than 240×400.
        let container = CGSize(width: 382, height: 420)
        let size = EmulatorView.fittedLCDSize(in: container, aspect: 240.0 / 400.0)
        XCTAssertEqual(size.width, 252, accuracy: 0.5)
        XCTAssertEqual(size.height, 420, accuracy: 0.5)
        XCTAssertLessThanOrEqual(size.width, container.width + 0.01)
        XCTAssertLessThanOrEqual(size.height, container.height + 0.01)
    }

    func testFittedLCDSizeWidthLimitedWhenContainerIsTall() {
        let container = CGSize(width: 300, height: 700)
        let size = EmulatorView.fittedLCDSize(in: container, aspect: 240.0 / 400.0)
        XCTAssertEqual(size.width, 300, accuracy: 0.5)
        XCTAssertEqual(size.height, 500, accuracy: 0.5)
    }

    func testFittedLCDSizeNeverExceedsContainer() {
        let container = CGSize(width: 200, height: 200)
        let size = EmulatorView.fittedLCDSize(in: container, aspect: 0.6)
        XCTAssertLessThanOrEqual(size.width, 200.01)
        XCTAssertLessThanOrEqual(size.height, 200.01)
        XCTAssertEqual(size.width / size.height, 0.6, accuracy: 0.001)
    }
}
