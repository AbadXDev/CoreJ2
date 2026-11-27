import Foundation

/// J2ME / FreeJ2ME keycodes used by the on-screen keypad.
///
/// Values match `org.recompile.mobile.Mobile` and MIDP `Canvas` constants.
/// Miami Nights (Gameloft) maps Nokia arrows/softkeys and ITU-T digits.
enum J2MEKeyCode {
    // MARK: ITU-T

    static let num0: Int32 = 48
    static let num1: Int32 = 49
    static let num2: Int32 = 50
    static let num3: Int32 = 51
    static let num4: Int32 = 52
    static let num5: Int32 = 53
    static let num6: Int32 = 54
    static let num7: Int32 = 55
    static let num8: Int32 = 56
    static let num9: Int32 = 57
    static let star: Int32 = 42
    static let pound: Int32 = 35

    // MARK: Nokia (preferred by Miami Nights)

    static let up: Int32 = -1
    static let down: Int32 = -2
    static let left: Int32 = -3
    static let right: Int32 = -4
    /// Center / FIRE (Nokia softkey 3).
    static let fire: Int32 = -5
    static let softLeft: Int32 = -6
    static let softRight: Int32 = -7

    /// Alternate FIRE that Miami also accepts (`KEY_NUM5`).
    static let fireNum5: Int32 = 53
}
