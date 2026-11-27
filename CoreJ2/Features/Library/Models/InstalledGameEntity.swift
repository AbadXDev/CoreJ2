import Foundation
import SwiftData

/// SwiftData persistence model for an installed Java ME game.
@Model
final class InstalledGameEntity {

    // MARK: - Properties

    @Attribute(.unique) var id: UUID
    var title: String
    var jarPath: String
    var importedAt: Date
    var contentHash: String

    /// Publisher from the JAR manifest (`MIDlet-Vendor`), when detected.
    var vendor: String?

    /// Version from the JAR manifest (`MIDlet-Version`), when detected.
    var version: String?

    /// Java ME profile from the JAR manifest (for example `MIDP-2.0`), when detected.
    var midpProfile: String?

    /// Java ME configuration from the JAR manifest (for example `CLDC-1.1`), when detected.
    var cldcConfiguration: String?

    /// Identifier of the ``DeviceProfile`` used to run this game.
    var deviceProfileID: String = DeviceProfile.defaultProfileID

    // MARK: - Init

    init(
        id: UUID,
        title: String,
        jarPath: String,
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
        self.jarPath = jarPath
        self.importedAt = importedAt
        self.contentHash = contentHash
        self.vendor = vendor
        self.version = version
        self.midpProfile = midpProfile
        self.cldcConfiguration = cldcConfiguration
        self.deviceProfileID = deviceProfileID
    }

    // MARK: - Mapping

    /// Maps this entity to the domain model.
    func toDomain() -> InstalledGame {
        InstalledGame(
            id: id,
            title: title,
            jarURL: URL(fileURLWithPath: jarPath),
            importedAt: importedAt,
            contentHash: contentHash,
            vendor: vendor,
            version: version,
            midpProfile: midpProfile,
            cldcConfiguration: cldcConfiguration,
            deviceProfileID: deviceProfileID
        )
    }
}
