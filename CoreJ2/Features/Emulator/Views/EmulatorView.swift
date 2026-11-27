import SwiftUI

/// Owns the visual LCD surface that displays FreeJ2ME frames plus on-screen controls.
///
/// On appear, starts observing `runtimeEvents` and launches `game` through the
/// bridge. On disappear, stops the session. Renders via `CGImage` (no Metal).
/// Controls sit below the LCD so thumbs do not cover the framebuffer.
struct EmulatorView: View {

    // MARK: - Dependencies

    @ObservedObject private var viewModel: EmulatorViewModel
    private let game: InstalledGame
    @State private var pointerIsDown = false

    // MARK: - Init

    init(viewModel: EmulatorViewModel, game: InstalledGame) {
        self.viewModel = viewModel
        self.game = game
    }

    // MARK: - Body

    var body: some View {
        VStack(spacing: 0) {
            lcdSurface
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            if viewModel.activeSession != nil {
                EmulatorKeypadView(
                    onKeyDown: { viewModel.keyDown($0) },
                    onKeyUp: { viewModel.keyUp($0) },
                    onKeyRepeat: { viewModel.keyRepeat($0) }
                )
            }
        }
        .navigationTitle(game.title)
        .navigationBarTitleDisplayMode(.inline)
        .alert(
            "Launch Failed",
            isPresented: launchErrorBinding
        ) {
            Button("OK", role: .cancel) {
                viewModel.dismissLaunchError()
            }
        } message: {
            Text(viewModel.launchErrorMessage ?? "")
        }
        .task(id: game.id) {
            print("corej2-launch: EmulatorView.task start \(game.title) id=\(game.id)")
            await viewModel.startSession(for: game)
            print("corej2-launch: EmulatorView.task finished \(game.title) error=\(viewModel.launchErrorMessage ?? "nil")")
        }
        .onDisappear {
            // Explicit back already calls dismissEmulator → endSession via the
            // navigation binding. Only stop here if a session is actually running
            // so SwiftUI destination churn during a second push cannot cancel an
            // in-flight launch (isLaunching, activeSession == nil) silently.
            if viewModel.activeSession != nil {
                print("corej2-launch: EmulatorView.onDisappear → endSession")
                viewModel.endSession()
            } else {
                print("corej2-launch: EmulatorView.onDisappear ignored (no active session)")
            }
        }
    }

    // MARK: - Surface

    /// LCD plane showing the latest frame, or a waiting placeholder.
    private var lcdSurface: some View {
        GeometryReader { geometry in
            let size = fittedLCDSize(in: geometry.size)
            ZStack {
                Color.black
                lcdContent
                    .frame(width: size.width, height: size.height)
                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                    .gesture(lcdPointerGesture(displaySize: size))
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(accessibilityLabel)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
        .padding(.horizontal)
        .padding(.top, 8)
        .background(Color(uiColor: .systemBackground))
    }

    @ViewBuilder
    private var lcdContent: some View {
        if let cgImage = viewModel.lcdImage {
            Image(decorative: cgImage, scale: 1.0, orientation: .up)
                .resizable()
                .interpolation(.none)
                .aspectRatio(contentMode: .fit)
        } else {
            RoundedRectangle(cornerRadius: 4, style: .continuous)
                .fill(Color(white: 0.12))
                .overlay {
                    Text(statusMessage)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.center)
                        .padding()
                }
        }
    }

    private var statusMessage: String {
        if viewModel.launchErrorMessage != nil {
            return "Launch failed"
        }
        if viewModel.activeSession != nil {
            return "Waiting for frame…"
        }
        if viewModel.isLaunching {
            return "Starting…"
        }
        return "Starting…"
    }

    // MARK: - Pointer

    private func lcdPointerGesture(displaySize: CGSize) -> some Gesture {
        DragGesture(minimumDistance: 0)
            .onChanged { value in
                let point = scaledLCDPoint(value.location, in: displaySize)
                if !pointerIsDown {
                    pointerIsDown = true
                    viewModel.pointerPressed(x: point.x, y: point.y)
                } else {
                    viewModel.pointerDragged(x: point.x, y: point.y)
                }
            }
            .onEnded { value in
                let point = scaledLCDPoint(value.location, in: displaySize)
                pointerIsDown = false
                viewModel.pointerReleased(x: point.x, y: point.y)
            }
    }

    private func scaledLCDPoint(_ location: CGPoint, in displaySize: CGSize) -> (x: Int32, y: Int32) {
        let lcdW = CGFloat(viewModel.lcdWidthPixels(for: game))
        let lcdH = CGFloat(viewModel.lcdHeightPixels(for: game))
        let clampedX = min(max(location.x, 0), displaySize.width)
        let clampedY = min(max(location.y, 0), displaySize.height)
        let x = Int32((clampedX / max(displaySize.width, 1)) * lcdW)
        let y = Int32((clampedY / max(displaySize.height, 1)) * lcdH)
        return (min(max(x, 0), Int32(lcdW) - 1), min(max(y, 0), Int32(lcdH) - 1))
    }

    // MARK: - Layout

    private func fittedLCDSize(in container: CGSize) -> CGSize {
        let aspect: CGFloat
        if let metadata = viewModel.surfaceMetadata, metadata.height > 0 {
            aspect = CGFloat(metadata.width) / CGFloat(metadata.height)
        } else {
            aspect = CGFloat(game.deviceProfile.aspectRatio)
        }

        let maxWidth = container.width
        let maxHeight = container.height
        let widthLimitedHeight = maxWidth / aspect
        if widthLimitedHeight <= maxHeight {
            return CGSize(width: maxWidth, height: widthLimitedHeight)
        }
        return CGSize(width: maxHeight * aspect, height: maxHeight)
    }

    private var accessibilityLabel: String {
        if let metadata = viewModel.surfaceMetadata {
            return "Emulator LCD, \(metadata.width) by \(metadata.height), \(viewModel.receivedFrameCount) frames"
        }
        return "Emulator LCD surface, waiting for frame"
    }

    // MARK: - Bindings

    private var launchErrorBinding: Binding<Bool> {
        Binding(
            get: { viewModel.launchErrorMessage != nil },
            set: { isPresented in
                if !isPresented {
                    viewModel.dismissLaunchError()
                }
            }
        )
    }
}

#if DEBUG
#Preview {
    NavigationStack {
        EmulatorView(
            viewModel: EmulatorViewModel(bridge: PreviewEmulatorBridge()),
            game: InstalledGame(
                id: UUID(),
                title: "Demo",
                jarURL: URL(fileURLWithPath: "/tmp/demo.jar"),
                importedAt: Date(),
                contentHash: "abc"
            )
        )
    }
}

@MainActor
private final class PreviewEmulatorBridge: EmulatorBridgeProtocol {
    private let pipe = RuntimeEventPipe()

    var runtimeEvents: AsyncStream<RuntimeEvent> {
        pipe.events
    }

    func launch(_ configuration: LaunchConfiguration) async throws -> EmulatorSession {
        EmulatorSession(configuration: configuration, state: .running)
    }

    func pause(_ session: EmulatorSession) throws {
        session.transition(to: .paused)
    }

    func resume(_ session: EmulatorSession) throws {
        session.transition(to: .running)
    }

    func stop(_ session: EmulatorSession) async throws {
        session.transition(to: .stopped)
    }
}
#endif
