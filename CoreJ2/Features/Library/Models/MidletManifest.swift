import Foundation

/// Parsed attributes from a Java ME JAR manifest.
struct MidletManifest: Equatable, Sendable {
    /// Value of the `MIDlet-Name` attribute, if present.
    let midletName: String?

    /// Relative path of the primary MIDlet icon inside the JAR, if present.
    let iconPath: String?

    /// Value of the `MIDlet-Vendor` attribute, if present.
    let vendor: String?

    /// Value of the `MIDlet-Version` attribute, if present.
    let version: String?

    /// Value of the `MicroEdition-Profile` attribute (for example `MIDP-2.0`), if present.
    let midpProfile: String?

    /// Value of the `MicroEdition-Configuration` attribute (for example `CLDC-1.1`), if present.
    let cldcConfiguration: String?

    /// MIDlet class name from the `MIDlet-1` attribute, if present.
    let midletClassName: String?

    /// Original display width from `Nokia-MIDlet-Original-Display-Size`, if present.
    let originalDisplayWidth: Int?

    /// Original display height from `Nokia-MIDlet-Original-Display-Size`, if present.
    let originalDisplayHeight: Int?

    /// `true` when the manifest carries Nokia-specific attributes.
    let hasNokiaAttributes: Bool

    /// `true` when the manifest carries Sony Ericsson-specific attributes.
    let hasSonyEricssonAttributes: Bool

    /// `true` when the manifest carries Motorola-specific attributes.
    let hasMotorolaAttributes: Bool

    // MARK: - Init

    init(
        midletName: String?,
        iconPath: String?,
        vendor: String? = nil,
        version: String? = nil,
        midpProfile: String? = nil,
        cldcConfiguration: String? = nil,
        midletClassName: String? = nil,
        originalDisplayWidth: Int? = nil,
        originalDisplayHeight: Int? = nil,
        hasNokiaAttributes: Bool = false,
        hasSonyEricssonAttributes: Bool = false,
        hasMotorolaAttributes: Bool = false
    ) {
        self.midletName = midletName
        self.iconPath = iconPath
        self.vendor = vendor
        self.version = version
        self.midpProfile = midpProfile
        self.cldcConfiguration = cldcConfiguration
        self.midletClassName = midletClassName
        self.originalDisplayWidth = originalDisplayWidth
        self.originalDisplayHeight = originalDisplayHeight
        self.hasNokiaAttributes = hasNokiaAttributes
        self.hasSonyEricssonAttributes = hasSonyEricssonAttributes
        self.hasMotorolaAttributes = hasMotorolaAttributes
    }
}
