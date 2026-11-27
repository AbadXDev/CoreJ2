import XCTest
@testable import CoreJ2

final class DeviceProfileDetectorTests: XCTestCase {

    // MARK: - Resolution in File Name

    func testDetectsResolutionFromFileName() {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: "Miami-Nights_J2ME_ES-ML_240x320_v103.jar",
            manifest: nil
        )

        XCTAssertEqual(profile.lcdWidth, 240)
        XCTAssertEqual(profile.lcdHeight, 320)
    }

    func testDetectsSmallResolutionFromFileName() {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: "Game_128x160_EN.jar",
            manifest: nil
        )

        XCTAssertEqual(profile.lcdWidth, 128)
        XCTAssertEqual(profile.lcdHeight, 160)
    }

    func testDetectsLandscapeResolutionFromFileName() {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: "Game_320x240.jar",
            manifest: nil
        )

        XCTAssertEqual(profile.lcdWidth, 320)
        XCTAssertEqual(profile.lcdHeight, 240)
    }

    // MARK: - Brand Hints

    func testPrefersBrandProfileWhenBrandAndResolutionArePresent() {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: "Game_Samsung_240x400.jar",
            manifest: nil
        )

        XCTAssertEqual(profile.brand, "Samsung")
        XCTAssertEqual(profile.lcdWidth, 240)
        XCTAssertEqual(profile.lcdHeight, 400)
    }

    func testDetectsBrandFromFileNameWithoutResolution() {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: "Game_SonyEricsson_K800.jar",
            manifest: nil
        )

        XCTAssertEqual(profile.brand, "Sony Ericsson")
    }

    func testDetectsNokiaFromManifestAttributes() {
        let manifest = MidletManifest(
            midletName: "Game",
            iconPath: nil,
            hasNokiaAttributes: true
        )

        let profile = DeviceProfileDetector.detectProfile(
            fileName: "game.jar",
            manifest: manifest
        )

        XCTAssertEqual(profile.brand, "Nokia")
    }

    // MARK: - Manifest Display Size

    func testManifestOriginalDisplaySizeWinsOverFileName() {
        let manifest = MidletManifest(
            midletName: "Game",
            iconPath: nil,
            originalDisplayWidth: 176,
            originalDisplayHeight: 208
        )

        let profile = DeviceProfileDetector.detectProfile(
            fileName: "Game_240x320.jar",
            manifest: manifest
        )

        XCTAssertEqual(profile.lcdWidth, 176)
        XCTAssertEqual(profile.lcdHeight, 208)
    }

    // MARK: - Fallback

    func testFallsBackToDefaultProfileWhenNothingIsDetected() {
        let profile = DeviceProfileDetector.detectProfile(
            fileName: "game.jar",
            manifest: nil
        )

        XCTAssertEqual(profile.id, DeviceProfile.defaultProfileID)
        XCTAssertEqual(profile.lcdWidth, 240)
        XCTAssertEqual(profile.lcdHeight, 320)
    }

    // MARK: - Resolution Parsing

    func testDetectResolutionReturnsNilWithoutSize() {
        XCTAssertNil(DeviceProfileDetector.detectResolution(in: "game.jar"))
    }

    func testDetectResolutionParsesSize() {
        let size = DeviceProfileDetector.detectResolution(in: "Game_176x220_EN.jar")

        XCTAssertEqual(size?.width, 176)
        XCTAssertEqual(size?.height, 220)
    }
}
