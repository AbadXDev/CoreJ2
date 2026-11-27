import Foundation

/// A Java ME device profile describing the LCD size a game was built for.
///
/// Profiles cover the screen sizes used by the major Java ME handset makers
/// of the 2000s (Nokia, Sony Ericsson, Motorola, Samsung, Alcatel, LG,
/// Siemens). The emulator creates its `MobilePlatform` at the profile's LCD
/// size so each game runs at its native resolution.
struct DeviceProfile: Identifiable, Equatable, Hashable, Sendable {

    // MARK: - Properties

    /// Stable identifier persisted with each installed game.
    let id: String

    /// Handset maker, for example "Nokia".
    let brand: String

    /// Human-readable profile name, for example "Nokia S40 · 240×320".
    let name: String

    /// LCD width in pixels.
    let lcdWidth: Int

    /// LCD height in pixels.
    let lcdHeight: Int

    // MARK: - Derived

    /// Resolution label, for example "240×320".
    var resolutionLabel: String {
        "\(lcdWidth)×\(lcdHeight)"
    }

    /// LCD aspect ratio (width / height).
    var aspectRatio: Double {
        guard lcdHeight > 0 else { return 1 }
        return Double(lcdWidth) / Double(lcdHeight)
    }

    /// `true` when the native LCD is wider than tall (e.g. 640×360 touch).
    /// Emulator UI locks landscape and puts controls on the sides.
    var prefersLandscapeLayout: Bool {
        lcdWidth > lcdHeight
    }
}

// MARK: - Catalog

extension DeviceProfile {

    /// Identifier of the fallback profile used when detection is inconclusive.
    static let defaultProfileID = "nokia-s40-240x320"

    /// Fallback profile: the most common Java ME screen size of the era.
    static var defaultProfile: DeviceProfile {
        profile(withID: defaultProfileID) ?? catalog[0]
    }

    /// Returns the catalog profile with `id`, if any.
    static func profile(withID id: String) -> DeviceProfile? {
        catalog.first { $0.id == id }
    }

    /// Returns the first catalog profile matching an LCD size, preferring `brand` when given.
    static func profile(lcdWidth: Int, lcdHeight: Int, brand: String? = nil) -> DeviceProfile? {
        let matches = catalog.filter { $0.lcdWidth == lcdWidth && $0.lcdHeight == lcdHeight }
        if let brand {
            let normalized = brand.lowercased()
            if let branded = matches.first(where: { $0.brand.lowercased() == normalized }) {
                return branded
            }
        }
        return matches.first
    }

    /// All profiles, grouped by brand in catalog order.
    static var profilesByBrand: [(brand: String, profiles: [DeviceProfile])] {
        var order: [String] = []
        var grouped: [String: [DeviceProfile]] = [:]
        for profile in catalog {
            if grouped[profile.brand] == nil {
                order.append(profile.brand)
            }
            grouped[profile.brand, default: []].append(profile)
        }
        return order.map { (brand: $0, profiles: grouped[$0] ?? []) }
    }

    /// The full catalog of supported device profiles.
    static let catalog: [DeviceProfile] = [
        // MARK: Nokia
        DeviceProfile(id: "nokia-s40-96x65", brand: "Nokia", name: "Nokia S40 · 96×65", lcdWidth: 96, lcdHeight: 65),
        DeviceProfile(id: "nokia-s40-128x128", brand: "Nokia", name: "Nokia S40 · 128×128", lcdWidth: 128, lcdHeight: 128),
        DeviceProfile(id: "nokia-s40-128x160", brand: "Nokia", name: "Nokia S40 · 128×160", lcdWidth: 128, lcdHeight: 160),
        DeviceProfile(id: "nokia-s40-176x208", brand: "Nokia", name: "Nokia S40 · 176×208", lcdWidth: 176, lcdHeight: 208),
        DeviceProfile(id: "nokia-s40-208x208", brand: "Nokia", name: "Nokia S40 · 208×208", lcdWidth: 208, lcdHeight: 208),
        DeviceProfile(id: "nokia-s40-240x320", brand: "Nokia", name: "Nokia S40 · 240×320", lcdWidth: 240, lcdHeight: 320),
        DeviceProfile(id: "nokia-s60-176x208", brand: "Nokia", name: "Nokia S60 · 176×208", lcdWidth: 176, lcdHeight: 208),
        DeviceProfile(id: "nokia-s60-240x320", brand: "Nokia", name: "Nokia S60 · 240×320", lcdWidth: 240, lcdHeight: 320),
        DeviceProfile(id: "nokia-s60-320x240", brand: "Nokia", name: "Nokia S60 · 320×240", lcdWidth: 320, lcdHeight: 240),
        DeviceProfile(id: "nokia-s60-352x416", brand: "Nokia", name: "Nokia S60 · 352×416", lcdWidth: 352, lcdHeight: 416),
        DeviceProfile(id: "nokia-s60-360x640", brand: "Nokia", name: "Nokia S60 Touch · 360×640", lcdWidth: 360, lcdHeight: 640),
        DeviceProfile(id: "nokia-s60-640x360", brand: "Nokia", name: "Nokia S60 Touch · 640×360", lcdWidth: 640, lcdHeight: 360),

        // MARK: Sony Ericsson
        DeviceProfile(id: "sonyericsson-128x160", brand: "Sony Ericsson", name: "Sony Ericsson · 128×160", lcdWidth: 128, lcdHeight: 160),
        DeviceProfile(id: "sonyericsson-176x220", brand: "Sony Ericsson", name: "Sony Ericsson · 176×220", lcdWidth: 176, lcdHeight: 220),
        DeviceProfile(id: "sonyericsson-240x320", brand: "Sony Ericsson", name: "Sony Ericsson · 240×320", lcdWidth: 240, lcdHeight: 320),
        DeviceProfile(id: "sonyericsson-320x240", brand: "Sony Ericsson", name: "Sony Ericsson · 320×240", lcdWidth: 320, lcdHeight: 240),

        // MARK: Motorola
        DeviceProfile(id: "motorola-128x128", brand: "Motorola", name: "Motorola · 128×128", lcdWidth: 128, lcdHeight: 128),
        DeviceProfile(id: "motorola-176x220", brand: "Motorola", name: "Motorola · 176×220", lcdWidth: 176, lcdHeight: 220),
        DeviceProfile(id: "motorola-240x320", brand: "Motorola", name: "Motorola · 240×320", lcdWidth: 240, lcdHeight: 320),

        // MARK: Samsung
        DeviceProfile(id: "samsung-128x160", brand: "Samsung", name: "Samsung · 128×160", lcdWidth: 128, lcdHeight: 160),
        DeviceProfile(id: "samsung-176x220", brand: "Samsung", name: "Samsung · 176×220", lcdWidth: 176, lcdHeight: 220),
        DeviceProfile(id: "samsung-240x320", brand: "Samsung", name: "Samsung · 240×320", lcdWidth: 240, lcdHeight: 320),
        DeviceProfile(id: "samsung-240x400", brand: "Samsung", name: "Samsung Touch · 240×400", lcdWidth: 240, lcdHeight: 400),
        DeviceProfile(id: "samsung-320x240", brand: "Samsung", name: "Samsung · 320×240", lcdWidth: 320, lcdHeight: 240),
        DeviceProfile(id: "samsung-480x800", brand: "Samsung", name: "Samsung Touch · 480×800", lcdWidth: 480, lcdHeight: 800),

        // MARK: Alcatel
        DeviceProfile(id: "alcatel-128x160", brand: "Alcatel", name: "Alcatel · 128×160", lcdWidth: 128, lcdHeight: 160),
        DeviceProfile(id: "alcatel-176x220", brand: "Alcatel", name: "Alcatel · 176×220", lcdWidth: 176, lcdHeight: 220),
        DeviceProfile(id: "alcatel-240x320", brand: "Alcatel", name: "Alcatel · 240×320", lcdWidth: 240, lcdHeight: 320),

        // MARK: LG
        DeviceProfile(id: "lg-128x160", brand: "LG", name: "LG · 128×160", lcdWidth: 128, lcdHeight: 160),
        DeviceProfile(id: "lg-176x220", brand: "LG", name: "LG · 176×220", lcdWidth: 176, lcdHeight: 220),
        DeviceProfile(id: "lg-240x320", brand: "LG", name: "LG · 240×320", lcdWidth: 240, lcdHeight: 320),
        DeviceProfile(id: "lg-240x400", brand: "LG", name: "LG Touch · 240×400", lcdWidth: 240, lcdHeight: 400),

        // MARK: Siemens
        DeviceProfile(id: "siemens-132x176", brand: "Siemens", name: "Siemens · 132×176", lcdWidth: 132, lcdHeight: 176),
        DeviceProfile(id: "siemens-176x220", brand: "Siemens", name: "Siemens · 176×220", lcdWidth: 176, lcdHeight: 220),

        // MARK: Generic
        DeviceProfile(id: "generic-128x128", brand: "Generic", name: "Generic · 128×128", lcdWidth: 128, lcdHeight: 128),
        DeviceProfile(id: "generic-128x160", brand: "Generic", name: "Generic · 128×160", lcdWidth: 128, lcdHeight: 160),
        DeviceProfile(id: "generic-176x208", brand: "Generic", name: "Generic · 176×208", lcdWidth: 176, lcdHeight: 208),
        DeviceProfile(id: "generic-176x220", brand: "Generic", name: "Generic · 176×220", lcdWidth: 176, lcdHeight: 220),
        DeviceProfile(id: "generic-240x320", brand: "Generic", name: "Generic · 240×320", lcdWidth: 240, lcdHeight: 320),
        DeviceProfile(id: "generic-320x240", brand: "Generic", name: "Generic · 320×240", lcdWidth: 320, lcdHeight: 240),
        DeviceProfile(id: "generic-240x400", brand: "Generic", name: "Generic · 240×400", lcdWidth: 240, lcdHeight: 400),
        DeviceProfile(id: "generic-640x360", brand: "Generic", name: "Generic Touch · 640×360", lcdWidth: 640, lcdHeight: 360),
        DeviceProfile(id: "generic-480x800", brand: "Generic", name: "Generic · 480×800", lcdWidth: 480, lcdHeight: 800),
    ]
}
