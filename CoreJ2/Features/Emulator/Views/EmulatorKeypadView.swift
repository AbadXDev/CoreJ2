import SwiftUI

/// On-screen J2ME controls: soft keys, sliding D-pad + FIRE, optional numeric keypad.
///
/// LCD stays above this view. The D-pad accepts a continuous drag: sliding from
/// one direction to another emits `keyReleased` of the previous key(s) and
/// `keyPressed` of the new one(s); holding repeats; between two axes = diagonal.
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
            SlidingDPadView(
                onKeyDown: onKeyDown,
                onKeyUp: onKeyUp,
                onKeyRepeat: onKeyRepeat
            )
            .frame(width: 168, height: 168)

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

// MARK: - Sliding D-pad

/// Continuous cross: drag without lifting; corners press two axes (diagonal).
private struct SlidingDPadView: View {
    let onKeyDown: (Int32) -> Void
    let onKeyUp: (Int32) -> Void
    let onKeyRepeat: (Int32) -> Void

    @State private var activeKeys: Set<Int32> = []
    @State private var isTouching = false
    @State private var repeatTask: Task<Void, Never>?

    private let threshold: CGFloat = 0.28

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            ZStack {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(uiColor: .tertiarySystemFill))

                directionLabel("▲", active: activeKeys.contains(J2MEKeyCode.up))
                    .offset(y: -size * 0.28)
                directionLabel("▼", active: activeKeys.contains(J2MEKeyCode.down))
                    .offset(y: size * 0.28)
                directionLabel("◀", active: activeKeys.contains(J2MEKeyCode.left))
                    .offset(x: -size * 0.28)
                directionLabel("▶", active: activeKeys.contains(J2MEKeyCode.right))
                    .offset(x: size * 0.28)

                Circle()
                    .fill(isTouching ? Color.accentColor.opacity(0.35) : Color.primary.opacity(0.12))
                    .frame(width: size * 0.22, height: size * 0.22)
            }
            .frame(width: size, height: size)
            .contentShape(Rectangle())
            .gesture(
                DragGesture(minimumDistance: 0)
                    .onChanged { value in
                        if !isTouching {
                            isTouching = true
                            startRepeat()
                        }
                        let nx = (value.location.x / size) * 2 - 1
                        let ny = (value.location.y / size) * 2 - 1
                        applyDirections(nx: nx, ny: ny)
                    }
                    .onEnded { _ in
                        isTouching = false
                        stopRepeat()
                        releaseAll()
                    }
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Directional pad")
            .accessibilityHint("Slide between directions without lifting")
            .accessibilityAddTraits(.isButton)
        }
    }

    private func directionLabel(_ title: String, active: Bool) -> some View {
        Text(title)
            .font(.title2.weight(.bold))
            .foregroundStyle(active ? Color.white : Color.primary)
            .frame(width: 44, height: 44)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(active ? Color.accentColor : Color.clear)
            )
    }

    private func applyDirections(nx: CGFloat, ny: CGFloat) {
        var next = Set<Int32>()
        if nx <= -threshold { next.insert(J2MEKeyCode.left) }
        if nx >= threshold { next.insert(J2MEKeyCode.right) }
        if ny <= -threshold { next.insert(J2MEKeyCode.up) }
        if ny >= threshold { next.insert(J2MEKeyCode.down) }
        // Opposing axes cancel (can't hold left+right).
        if next.contains(J2MEKeyCode.left) && next.contains(J2MEKeyCode.right) {
            next.remove(J2MEKeyCode.left)
            next.remove(J2MEKeyCode.right)
        }
        if next.contains(J2MEKeyCode.up) && next.contains(J2MEKeyCode.down) {
            next.remove(J2MEKeyCode.up)
            next.remove(J2MEKeyCode.down)
        }
        syncKeys(to: next)
    }

    private func syncKeys(to next: Set<Int32>) {
        let released = activeKeys.subtracting(next)
        let pressed = next.subtracting(activeKeys)
        for code in released.sorted() {
            onKeyUp(code)
        }
        for code in pressed.sorted() {
            onKeyDown(code)
        }
        activeKeys = next
    }

    private func releaseAll() {
        for code in activeKeys.sorted() {
            onKeyUp(code)
        }
        activeKeys = []
    }

    private func startRepeat() {
        stopRepeat()
        repeatTask = Task { @MainActor in
            try? await Task.sleep(nanoseconds: 350_000_000)
            while !Task.isCancelled {
                for code in activeKeys.sorted() {
                    onKeyRepeat(code)
                }
                try? await Task.sleep(nanoseconds: 100_000_000)
            }
        }
    }

    private func stopRepeat() {
        repeatTask?.cancel()
        repeatTask = nil
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
