import Foundation

/// A Java ME game owned by CoreJ2's internal library.
struct InstalledGame: Identifiable, Equatable, Hashable {
    let id: UUID
    let title: String
    let jarURL: URL
    let importedAt: Date
    /// SHA-256 digest of the JAR contents, as a lowercase hexadecimal string.
    let contentHash: String

    /// Publisher from the JAR manifest (`MIDlet-Vendor`), when detected.
    let vendor: String?

    /// Version from the JAR manifest (`MIDlet-Version`), when detected.
    let version: String?

    /// Java ME profile from the JAR manifest (for example `MIDP-2.0`), when detected.
    let midpProfile: String?

    /// Java ME configuration from the JAR manifest (for example `CLDC-1.1`), when detected.
    let cldcConfiguration: String?

    /// Identifier of the ``DeviceProfile`` used to run this game.
    let deviceProfileID: String

    // MARK: - Init

    init(
        id: UUID,
        title: String,
        jarURL: URL,
        importedAt: Date,
        contentHash: String,
        vendor: String? = nil,
        version: String? = nil,
        midpProfile: String? = nil,
        cldcConfiguration: String? = nil,
        deviceProfileID: String = DeviceProfile.defaultProfileID
    ) {
        self.id = id
        self.title = title
        self.jarURL = jarURL
        self.importedAt = importedAt
        self.contentHash = contentHash
        self.vendor = vendor
        self.version = version
        self.midpProfile = midpProfile
        self.cldcConfiguration = cldcConfiguration
        self.deviceProfileID = deviceProfileID
    }

    // MARK: - Derived

    /// The device profile used to run this game, falling back to the default profile.
    var deviceProfile: DeviceProfile {
        DeviceProfile.profile(withID: deviceProfileID) ?? DeviceProfile.defaultProfile
    }

    /// File URL of the extracted MIDlet icon (`artwork/icon.png` beside the JAR).
    var artworkURL: URL {
        jarURL
            .deletingLastPathComponent()
            .appendingPathComponent("artwork", isDirectory: true)
            .appendingPathComponent("icon.png", isDirectory: false)
    }

    /// Short info line for library rows, for example `Gameloft SA · 240×320`.
    var infoSummary: String {
        var parts: [String] = []
        if let vendor, !vendor.isEmpty {
            parts.append(vendor)
        }
        parts.append(deviceProfile.resolutionLabel)
        return parts.joined(separator: " · ")
    }

    // MARK: - Copying

    /// Returns a copy with a different device profile.
    func withDeviceProfile(_ profile: DeviceProfile) -> InstalledGame {
        InstalledGame(
            id: id,
            title: title,
            jarURL: jarURL,
            importedAt: importedAt,
            contentHash: contentHash,
            vendor: vendor,
            version: version,
            midpProfile: midpProfile,
            cldcConfiguration: cldcConfiguration,
            deviceProfileID: profile.id
        )
    }
}
