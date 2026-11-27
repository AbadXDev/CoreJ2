import Combine
import Foundation
import SwiftUI

/// User preferences persisted in `UserDefaults`.
@MainActor
final class AppSettings: ObservableObject {

    static let shared = AppSettings()

    enum Appearance: String, CaseIterable, Identifiable {
        case system
        case light
        case dark

        var id: String { rawValue }

        var title: String {
            switch self {
            case .system: return "Sistema"
            case .light: return "Claro"
            case .dark: return "Oscuro"
            }
        }

        var colorScheme: ColorScheme? {
            switch self {
            case .system: return nil
            case .light: return .light
            case .dark: return .dark
            }
        }
    }

    /// On-screen D-pad presentation preferred by the user.
    enum DPadStyle: String, CaseIterable, Identifiable {
        /// Virtual stick: drag freely, 8 directions from angle.
        case joystick
        /// Continuous cross with diagonals while sliding.
        case sliding
        /// Four discrete buttons (classic keypad).
        case classic

        var id: String { rawValue }

        var title: String {
            switch self {
            case .joystick: return "Palanca"
            case .sliding: return "Cruceta deslizable"
            case .classic: return "Cruceta clásica"
            }
        }

        var detail: String {
            switch self {
            case .joystick: return "Joystick virtual en cualquier dirección"
            case .sliding: return "Deslizar entre direcciones y diagonales"
            case .classic: return "Botones individuales (como antes)"
            }
        }
    }

    private enum Keys {
        static let numericKeypadVisible = "corej2.settings.numericKeypadVisible"
        static let vibrationEnabled = "corej2.settings.vibrationEnabled"
        static let appearance = "corej2.settings.appearance"
        static let dpadStyle = "corej2.settings.dpadStyle"
        static let deletedHashes = "corej2.settings.deletedContentHashes"
    }

    @Published var numericKeypadVisibleByDefault: Bool {
        didSet { UserDefaults.standard.set(numericKeypadVisibleByDefault, forKey: Keys.numericKeypadVisible) }
    }

    @Published var vibrationEnabled: Bool {
        didSet { UserDefaults.standard.set(vibrationEnabled, forKey: Keys.vibrationEnabled) }
    }

    @Published var appearance: Appearance {
        didSet { UserDefaults.standard.set(appearance.rawValue, forKey: Keys.appearance) }
    }

    @Published var dpadStyle: DPadStyle {
        didSet { UserDefaults.standard.set(dpadStyle.rawValue, forKey: Keys.dpadStyle) }
    }

    private init() {
        let defaults = UserDefaults.standard
        numericKeypadVisibleByDefault = defaults.object(forKey: Keys.numericKeypadVisible) as? Bool ?? false
        vibrationEnabled = defaults.object(forKey: Keys.vibrationEnabled) as? Bool ?? true
        if let raw = defaults.string(forKey: Keys.appearance), let value = Appearance(rawValue: raw) {
            appearance = value
        } else {
            appearance = .system
        }
        if let raw = defaults.string(forKey: Keys.dpadStyle), let value = DPadStyle(rawValue: raw) {
            dpadStyle = value
        } else {
            dpadStyle = .sliding
        }
    }

    // MARK: - Deleted-game tombstones (auto-import denylist)

    func tombstoneContentHash(_ hash: String) {
        let trimmed = hash.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        var set = Set(UserDefaults.standard.stringArray(forKey: Keys.deletedHashes) ?? [])
        set.insert(trimmed)
        UserDefaults.standard.set(Array(set), forKey: Keys.deletedHashes)
    }

    func clearTombstone(forContentHash hash: String) {
        let trimmed = hash.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        var set = Set(UserDefaults.standard.stringArray(forKey: Keys.deletedHashes) ?? [])
        set.remove(trimmed)
        UserDefaults.standard.set(Array(set), forKey: Keys.deletedHashes)
    }

    func isTombstoned(contentHash hash: String) -> Bool {
        let trimmed = hash.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return false }
        let set = Set(UserDefaults.standard.stringArray(forKey: Keys.deletedHashes) ?? [])
        return set.contains(trimmed)
    }
}
