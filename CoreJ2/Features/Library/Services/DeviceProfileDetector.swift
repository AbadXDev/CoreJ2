import Foundation

/// Detects the most likely ``DeviceProfile`` for an imported JAR.
///
/// Detection uses, in order:
/// 1. An explicit resolution in the original file name (for example `240x320`).
/// 2. A handset brand hint in the file name or manifest attributes.
/// 3. The fallback profile (Nokia S40 · 240×320), the most common Java ME
///    screen size of the era.
///
/// Detection is a best guess: the user can always override the profile
/// per game from the library.
enum DeviceProfileDetector {

    // MARK: - Detection

    /// Returns the detected profile for a JAR.
    ///
    /// - Parameters:
    ///   - fileName: Original file name chosen by the user (for example
    ///     `MiamiNights_Samsung_S5230_ML_IGP_TS_103.jar`).
    ///   - manifest: Parsed MIDlet manifest, when available.
    static func detectProfile(fileName: String, manifest: MidletManifest?) -> DeviceProfile {
        let brandHint = detectBrand(fileName: fileName, manifest: manifest)

        // 1. Explicit original display size declared in the manifest.
        if let manifest,
           let width = manifest.originalDisplayWidth,
           let height = manifest.originalDisplayHeight,
           let profile = DeviceProfile.profile(
               lcdWidth: width,
               lcdHeight: height,
               brand: brandHint
           ) {
            return profile
        }

        // 2. Explicit resolution in the original file name.
        if let size = detectResolution(in: fileName),
           let profile = DeviceProfile.profile(
               lcdWidth: size.width,
               lcdHeight: size.height,
               brand: brandHint
           ) {
            return profile
        }

        // Brand-only: prefer the common 240×320 size for that brand. Never pick
        // the first catalog entry (e.g. Nokia 96×65) — that breaks "Multi" JARs
        // whose vendor is Nokia but target a modern S40 screen.
        if let brandHint,
           let profile = DeviceProfile.profile(lcdWidth: 240, lcdHeight: 320, brand: brandHint) {
            return profile
        }

        return DeviceProfile.defaultProfile
    }

    // MARK: - Resolution

    /// Extracts a `WIDTHxHEIGHT` resolution from a file name, if present.
    static func detectResolution(in fileName: String) -> (width: Int, height: Int)? {
        let pattern = #"(\d{2,3})\s*[x×]\s*(\d{2,3})"#
        guard let regex = try? NSRegularExpression(pattern: pattern) else {
            return nil
        }

        let range = NSRange(fileName.startIndex..., in: fileName)
        guard let match = regex.firstMatch(in: fileName, range: range),
              match.numberOfRanges == 3,
              let widthRange = Range(match.range(at: 1), in: fileName),
              let heightRange = Range(match.range(at: 2), in: fileName),
              let width = Int(fileName[widthRange]),
              let height = Int(fileName[heightRange]) else {
            return nil
        }

        return (width: width, height: height)
    }

    // MARK: - Brand

    /// Detects a handset brand hint from the file name or manifest.
    static func detectBrand(fileName: String, manifest: MidletManifest?) -> String? {
        let name = fileName.lowercased()

        let brandHints: [(brand: String, tokens: [String])] = [
            ("Sony Ericsson", ["sonyericsson", "sony_ericsson", "sony-ericsson", "_se_", "se_", "k800", "k750", "w910", "w595", "c902", "t715"]),
            ("Nokia", ["nokia", "s40", "s60", "n73", "n95", "e71", "e72", "5800"]),
            ("Motorola", ["motorola", "moto", "v3", "razr", "krzr", "l7", "e398"]),
            ("Samsung", ["samsung", "sgh", "s5230", "s8000", "f480", "e2550", "b5310"]),
            ("Alcatel", ["alcatel", "ot-", "one touch"]),
            ("LG", ["_lg", "lg_", "lg-", "kp500", "ku990", "ke970"]),
            ("Siemens", ["siemens", "benq", "cx65", "s65", "sk65"]),
        ]

        for hint in brandHints where hint.tokens.contains(where: { name.contains($0) }) {
            return hint.brand
        }

        if let manifest {
            if manifest.hasNokiaAttributes {
                return "Nokia"
            }
            if manifest.hasSonyEricssonAttributes {
                return "Sony Ericsson"
            }
            if manifest.hasMotorolaAttributes {
                return "Motorola"
            }
        }

        return nil
    }
}
