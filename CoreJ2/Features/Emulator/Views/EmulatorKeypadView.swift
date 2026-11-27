import SwiftUI

/// On-screen J2ME controls: soft keys, D-pad + FIRE, optional numeric keypad.
///
/// LCD stays above this view. Hold a button to emit `keyRepeated` after a short delay.
struct EmulatorKeypadView: View {

    // MARK: - Callbacks

    let onKeyDown: (Int32) -> Void
    let onKeyUp: (Int32) -> Void
    let onKeyRepeat: (Int32) -> Void

    // MARK: - State

    @State private var showNumericPad = AppSettings.shared.numericKeypadVisibleByDefault

    // MARK: - Body

    var body: some View {
        VStack(spacing: 12) {
            softKeyRow
            dpadAndFire
            numericToggle
            if showNumericPad {
                numericPad
                    .transition(.move(edge: .bottom).combined(with: .opacity))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .background(Color(uiColor: .secondarySystemBackground))
        .animation(.easeInOut(duration: 0.2), value: showNumericPad)
    }

    // MARK: - Soft keys

    private var softKeyRow: some View {
        HStack {
            HoldableKeyButton(
                title: "LS",
                accessibilityLabel: "Soft key left",
                keyCode: J2MEKeyCode.softLeft,
                onDown: onKeyDown,
                onUp: onKeyUp,
                onRepeat: onKeyRepeat
            )
            .frame(width: 88, height: 44)

            Spacer()

            HoldableKeyButton(
                title: "RS",
                accessibilityLabel: "Soft key right",
                keyCode: J2MEKeyCode.softRight,
                onDown: onKeyDown,
                onUp: onKeyUp,
                onRepeat: onKeyRepeat
            )
            .frame(width: 88, height: 44)
        }
    }

    // MARK: - D-pad + FIRE

    private var dpadAndFire: some View {
        HStack(spacing: 28) {
            dpad
            HoldableKeyButton(
                title: "OK",
                accessibilityLabel: "Fire",
                keyCode: J2MEKeyCode.fireNum5,
                isPrimary: true,
                onDown: onKeyDown,
                onUp: onKeyUp,
                onRepeat: onKeyRepeat
            )
            .frame(width: 72, height: 72)
        }
    }

    private var dpad: some View {
        VStack(spacing: 6) {
            HoldableKeyButton(
                title: "▲",
                accessibilityLabel: "Up",
                keyCode: J2MEKeyCode.up,
                onDown: onKeyDown,
                onUp: onKeyUp,
                onRepeat: onKeyRepeat
            )
            .frame(width: 64, height: 52)

            HStack(spacing: 6) {
                HoldableKeyButton(
                    title: "◀",
                    accessibilityLabel: "Left",
                    keyCode: J2MEKeyCode.left,
                    onDown: onKeyDown,
                    onUp: onKeyUp,
                    onRepeat: onKeyRepeat
                )
                .frame(width: 64, height: 52)

                HoldableKeyButton(
                    title: "▼",
                    accessibilityLabel: "Down",
                    keyCode: J2MEKeyCode.down,
                    onDown: onKeyDown,
                    onUp: onKeyUp,
                    onRepeat: onKeyRepeat
                )
                .frame(width: 64, height: 52)

                HoldableKeyButton(
                    title: "▶",
                    accessibilityLabel: "Right",
                    keyCode: J2MEKeyCode.right,
                    onDown: onKeyDown,
                    onUp: onKeyUp,
                    onRepeat: onKeyRepeat
                )
                .frame(width: 64, height: 52)
            }
        }
    }

    // MARK: - Numeric

    private var numericToggle: some View {
        Button {
            showNumericPad.toggle()
        } label: {
            Text(showNumericPad ? "Hide keypad" : "Show 0–9 * #")
                .font(.subheadline.weight(.semibold))
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
        }
        .buttonStyle(.bordered)
    }

    private var numericPad: some View {
        let rows: [[(String, Int32)]] = [
            [("1", J2MEKeyCode.num1), ("2", J2MEKeyCode.num2), ("3", J2MEKeyCode.num3)],
            [("4", J2MEKeyCode.num4), ("5", J2MEKeyCode.num5), ("6", J2MEKeyCode.num6)],
            [("7", J2MEKeyCode.num7), ("8", J2MEKeyCode.num8), ("9", J2MEKeyCode.num9)],
            [("*", J2MEKeyCode.star), ("0", J2MEKeyCode.num0), ("#", J2MEKeyCode.pound)],
        ]
        return VStack(spacing: 8) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                        HoldableKeyButton(
                            title: cell.0,
                            accessibilityLabel: "Key \(cell.0)",
                            keyCode: cell.1,
                            onDown: onKeyDown,
                            onUp: onKeyUp,
                            onRepeat: onKeyRepeat
                        )
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                    }
                }
            }
        }
    }
}

// MARK: - Holdable key

/// Press → `onDown`; hold → `onRepeat` every 100 ms after 350 ms; release → `onUp`.
private struct HoldableKeyButton: View {
    let title: String
    let accessibilityLabel: String
    let keyCode: Int32
    var isPrimary: Bool = false
    let onDown: (Int32) -> Void
    let onUp: (Int32) -> Void
    let onRepeat: (Int32) -> Void

    @State private var isPressed = false
    @State private var repeatTask: Task<Void, Never>?

    var body: some View {
        Text(title)
            .font(isPrimary ? .title2.weight(.bold) : .title3.weight(.semibold))
            .foregroundStyle(isPressed ? Color.white : Color.primary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .fill(isPressed
                          ? Color.accentColor
                          : (isPrimary ? Color.accentColor.opacity(0.25) : Color(uiColor: .tertiarySystemFill)))
            )
            .contentShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { _ in
                        guard !isPressed else { return }
                        isPressed = true
                        onDown(keyCode)
                        startRepeat()
                    }
                    .onEnded { _ in
                        guard isPressed else { return }
                        isPressed = false
                        stopRepeat()
                        onUp(keyCode)
                    }
            )
            .accessibilityLabel(accessibilityLabel)
            .accessibilityAddTraits(.isButton)
    }

    private func startRepeat() {
        stopRepeat()
        let code = keyCode
        repeatTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 350_000_000)
            while !Task.isCancelled {
                onRepeat(code)
                try? await Task.sleep(nanoseconds: 100_000_000)
            }
        }
    }

    private func stopRepeat() {
        repeatTask?.cancel()
        repeatTask = nil
    }
}
