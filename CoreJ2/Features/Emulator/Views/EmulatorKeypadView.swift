import SwiftUI

/// On-screen J2ME controls: soft keys, configurable D-pad + FIRE, optional numeric keypad.
///
/// D-pad style comes from `AppSettings.dpadStyle`: palanca (8-way stick),
/// cruceta deslizable (continuous cross), or cruceta clásica (discrete buttons).
struct EmulatorKeypadView: View {

    // MARK: - Callbacks

    let onKeyDown: (Int32) -> Void
    let onKeyUp: (Int32) -> Void
    let onKeyRepeat: (Int32) -> Void
    /// Compact side layout for landscape LCD sessions.
    var landscapeSide: LandscapeSide? = nil

    enum LandscapeSide {
        case left
        case right
    }

    // MARK: - State

    @ObservedObject private var settings = AppSettings.shared
    @State private var showNumericPad = AppSettings.shared.numericKeypadVisibleByDefault

    // MARK: - Body

    var body: some View {
        Group {
            if let side = landscapeSide {
                landscapeColumn(side: side)
            } else {
                portraitStack
            }
        }
        .animation(.easeInOut(duration: 0.2), value: showNumericPad)
        .animation(.easeInOut(duration: 0.15), value: settings.dpadStyle)
    }

    // MARK: - Portrait

    private var portraitStack: some View {
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
    }

    // MARK: - Landscape sides

    private func landscapeColumn(side: LandscapeSide) -> some View {
        VStack(spacing: 10) {
            switch side {
            case .left:
                HoldableKeyButton(
                    title: "LS",
                    accessibilityLabel: "Soft key left",
                    keyCode: J2MEKeyCode.softLeft,
                    onDown: onKeyDown,
                    onUp: onKeyUp,
                    onRepeat: onKeyRepeat
                )
                .frame(width: 64, height: 40)
                dpadControl
                    .frame(width: 140, height: 140)
            case .right:
                HoldableKeyButton(
                    title: "RS",
                    accessibilityLabel: "Soft key right",
                    keyCode: J2MEKeyCode.softRight,
                    onDown: onKeyDown,
                    onUp: onKeyUp,
                    onRepeat: onKeyRepeat
                )
                .frame(width: 64, height: 40)
                HoldableKeyButton(
                    title: "OK",
                    accessibilityLabel: "Fire",
                    keyCode: J2MEKeyCode.fireNum5,
                    isPrimary: true,
                    onDown: onKeyDown,
                    onUp: onKeyUp,
                    onRepeat: onKeyRepeat
                )
                .frame(width: 64, height: 64)
                numericToggle
                if showNumericPad {
                    numericPadCompact
                }
            }
        }
        .padding(8)
        .frame(maxHeight: .infinity)
        .background(Color(uiColor: .secondarySystemBackground))
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
            dpadControl
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

    @ViewBuilder
    private var dpadControl: some View {
        switch settings.dpadStyle {
        case .joystick:
            AngleDPadView(
                style: .joystick,
                onKeyDown: onKeyDown,
                onKeyUp: onKeyUp,
                onKeyRepeat: onKeyRepeat
            )
        case .sliding:
            AngleDPadView(
                style: .sliding,
                onKeyDown: onKeyDown,
                onKeyUp: onKeyUp,
                onKeyRepeat: onKeyRepeat
            )
        case .classic:
            ClassicDPadView(
                onKeyDown: onKeyDown,
                onKeyUp: onKeyUp,
                onKeyRepeat: onKeyRepeat
            )
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

    private var numericPadCompact: some View {
        let rows: [[(String, Int32)]] = [
            [("1", J2MEKeyCode.num1), ("2", J2MEKeyCode.num2), ("3", J2MEKeyCode.num3)],
            [("4", J2MEKeyCode.num4), ("5", J2MEKeyCode.num5), ("6", J2MEKeyCode.num6)],
            [("7", J2MEKeyCode.num7), ("8", J2MEKeyCode.num8), ("9", J2MEKeyCode.num9)],
            [("*", J2MEKeyCode.star), ("0", J2MEKeyCode.num0), ("#", J2MEKeyCode.pound)],
        ]
        return VStack(spacing: 4) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 4) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, cell in
                        HoldableKeyButton(
                            title: cell.0,
                            accessibilityLabel: "Key \(cell.0)",
                            keyCode: cell.1,
                            onDown: onKeyDown,
                            onUp: onKeyUp,
                            onRepeat: onKeyRepeat
                        )
                        .frame(width: 36, height: 32)
                    }
                }
            }
        }
    }
}

// MARK: - Angle-based D-pad (joystick + sliding)

/// 8-way pad from drag angle. Wide diagonal sectors + hysteresis so sliding
/// from one cardinal to the next always passes through the diagonal without
/// dropping to an empty dead zone.
private struct AngleDPadView: View {
    enum Style {
        case joystick
        case sliding
    }

    let style: Style
    let onKeyDown: (Int32) -> Void
    let onKeyUp: (Int32) -> Void
    let onKeyRepeat: (Int32) -> Void

    @State private var activeKeys: Set<Int32> = []
    @State private var sector: Int? = nil
    @State private var isTouching = false
    @State private var knobOffset: CGSize = .zero
    @State private var repeatTask: Task<Void, Never>?

    /// Enter active zone.
    private let enterRadius: CGFloat = 0.18
    /// Exit back to idle (hysteresis) — stays sticky near center.
    private let exitRadius: CGFloat = 0.08
    /// Extra half-width kept on the current sector while sliding.
    private let sectorHysteresisDegrees: Double = 22
    /// Cardinal half-width (E/N/W/S). Diagonals use the remainder → ~55° each.
    private let cardinalHalfWidth: Double = 17.5

    var body: some View {
        GeometryReader { geo in
            let size = min(geo.size.width, geo.size.height)
            ZStack {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(Color(uiColor: .tertiarySystemFill))

                if style == .sliding {
                    directionLabel("▲", active: activeKeys.contains(J2MEKeyCode.up), size: size)
                        .offset(y: -size * 0.30)
                    directionLabel("▼", active: activeKeys.contains(J2MEKeyCode.down), size: size)
                        .offset(y: size * 0.30)
                    directionLabel("◀", active: activeKeys.contains(J2MEKeyCode.left), size: size)
                        .offset(x: -size * 0.30)
                    directionLabel("▶", active: activeKeys.contains(J2MEKeyCode.right), size: size)
                        .offset(x: size * 0.30)
                }

                Circle()
                    .strokeBorder(Color.primary.opacity(0.15), lineWidth: 2)
                    .frame(width: size * 0.72, height: size * 0.72)

                Circle()
                    .fill(isTouching ? Color.accentColor.opacity(0.85) : Color.primary.opacity(0.2))
                    .frame(width: size * 0.28, height: size * 0.28)
                    .offset(knobOffset)
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
                        let clamped = clampToUnit(nx: nx, ny: ny)
                        let travel = style == .joystick ? size * 0.28 : size * 0.22
                        knobOffset = CGSize(width: clamped.x * travel, height: clamped.y * travel)
                        applyStick(nx: clamped.x, ny: clamped.y)
                    }
                    .onEnded { _ in
                        isTouching = false
                        stopRepeat()
                        releaseAll()
                        withAnimation(.easeOut(duration: 0.12)) {
                            knobOffset = .zero
                        }
                    }
            )
            .accessibilityElement(children: .ignore)
            .accessibilityLabel(style == .joystick ? "Virtual joystick" : "Sliding directional pad")
            .accessibilityAddTraits(.isButton)
        }
    }

    private func directionLabel(_ title: String, active: Bool, size: CGFloat) -> some View {
        Text(title)
            .font(.title3.weight(.bold))
            .foregroundStyle(active ? Color.white : Color.primary)
            .frame(width: size * 0.26, height: size * 0.26)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(active ? Color.accentColor : Color.clear)
            )
    }

    private func clampToUnit(nx: CGFloat, ny: CGFloat) -> (x: CGFloat, y: CGFloat) {
        let r = hypot(nx, ny)
        if r <= 1 { return (nx, ny) }
        return (nx / r, ny / r)
    }

    private func applyStick(nx: CGFloat, ny: CGFloat) {
        let r = hypot(nx, ny)
        if sector == nil {
            guard r >= enterRadius else {
                syncKeys(to: [])
                return
            }
        } else if r < exitRadius {
            sector = nil
            syncKeys(to: [])
            return
        }

        // Screen Y grows downward; negate so 90° = up.
        let angle = Double(atan2(-ny, nx)) * 180.0 / Double.pi
        let normalized = angle < 0 ? angle + 360 : angle
        let nextSector = selectSector(degrees: normalized, previous: sector)
        sector = nextSector
        syncKeys(to: keysForSector(nextSector))
    }

    /// Eight sectors: diagonals are wider (~55°) than cardinals (~35°) so a
    /// slide from one cardinal to the next always lands on the diagonal first.
    /// Hysteresis keeps the active sector while crossing near the center.
    private func selectSector(degrees: Double, previous: Int?) -> Int {
        if let previous {
            let half = sectorHalfWidth(previous) + sectorHysteresisDegrees
            let center = Double(previous) * 45.0
            var delta = degrees - center
            while delta > 180 { delta -= 360 }
            while delta < -180 { delta += 360 }
            if abs(delta) <= half {
                return previous
            }
        }
        return sectorForAngle(degrees)
    }

    private func sectorHalfWidth(_ sector: Int) -> Double {
        sector % 2 == 0 ? cardinalHalfWidth : (45.0 - cardinalHalfWidth)
    }

    private func sectorForAngle(_ degrees: Double) -> Int {
        var angle = degrees
        while angle < 0 { angle += 360 }
        while angle >= 360 { angle -= 360 }
        // Distance-to-center weighted by inverse half-width → wider diagonals win.
        var best = 0
        var bestScore = Double.greatestFiniteMagnitude
        for i in 0..<8 {
            let center = Double(i) * 45.0
            var delta = angle - center
            while delta > 180 { delta -= 360 }
            while delta < -180 { delta += 360 }
            let score = abs(delta) / sectorHalfWidth(i)
            if score < bestScore {
                bestScore = score
                best = i
            }
        }
        return best
    }

    private func keysForSector(_ sector: Int) -> Set<Int32> {
        switch sector {
        case 0: return [J2MEKeyCode.right]
        case 1: return [J2MEKeyCode.right, J2MEKeyCode.up]
        case 2: return [J2MEKeyCode.up]
        case 3: return [J2MEKeyCode.left, J2MEKeyCode.up]
        case 4: return [J2MEKeyCode.left]
        case 5: return [J2MEKeyCode.left, J2MEKeyCode.down]
        case 6: return [J2MEKeyCode.down]
        case 7: return [J2MEKeyCode.right, J2MEKeyCode.down]
        default: return []
        }
    }

    private func syncKeys(to next: Set<Int32>) {
        let released = activeKeys.subtracting(next)
        let pressed = next.subtracting(activeKeys)
        for code in released.sorted() { onKeyUp(code) }
        for code in pressed.sorted() { onKeyDown(code) }
        activeKeys = next
    }

    private func releaseAll() {
        for code in activeKeys.sorted() { onKeyUp(code) }
        activeKeys = []
        sector = nil
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

// MARK: - Classic discrete D-pad

private struct ClassicDPadView: View {
    let onKeyDown: (Int32) -> Void
    let onKeyUp: (Int32) -> Void
    let onKeyRepeat: (Int32) -> Void

    var body: some View {
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
