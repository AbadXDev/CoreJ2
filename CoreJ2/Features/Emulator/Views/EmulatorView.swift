import SwiftUI

/// Owns the visual LCD surface that displays FreeJ2ME frames plus on-screen controls.
///
/// Portrait games keep LCD above controls. Landscape profiles (LCD wider than
/// tall, e.g. 640×360) lock landscape and place controls on the left/right.
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

    private var prefersLandscape: Bool {
        game.deviceProfile.prefersLandscapeLayout
    }

    // MARK: - Body

    var body: some View {
        Group {
            if prefersLandscape {
                landscapeBody
            } else {
                portraitBody
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
            print("corej2-launch: EmulatorView.task start \(game.title) id=\(game.id) landscape=\(prefersLandscape)")
            if prefersLandscape {
                AppOrientationLock.lock(.landscape)
            } else {
                AppOrientationLock.lock(.portrait)
            }
            await viewModel.startSession(for: game)
            print("corej2-launch: EmulatorView.task finished \(game.title) error=\(viewModel.launchErrorMessage ?? "nil")")
        }
        .onDisappear {
            AppOrientationLock.lock(.all)
            if viewModel.activeSession != nil {
                print("corej2-launch: EmulatorView.onDisappear → endSession")
                viewModel.endSession()
            } else {
                print("corej2-launch: EmulatorView.onDisappear ignored (no active session)")
            }
        }
    }

    // MARK: - Portrait

    private var portraitBody: some View {
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
    }

    // MARK: - Landscape

    private var landscapeBody: some View {
        HStack(spacing: 0) {
            if viewModel.activeSession != nil {
                EmulatorKeypadView(
                    onKeyDown: { viewModel.keyDown($0) },
                    onKeyUp: { viewModel.keyUp($0) },
                    onKeyRepeat: { viewModel.keyRepeat($0) },
                    landscapeSide: .left
                )
                .frame(width: 156)
            }

            lcdSurface
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            if viewModel.activeSession != nil {
                EmulatorKeypadView(
                    onKeyDown: { viewModel.keyDown($0) },
                    onKeyUp: { viewModel.keyUp($0) },
                    onKeyRepeat: { viewModel.keyRepeat($0) },
                    landscapeSide: .right
                )
                .frame(width: 156)
            }
        }
        .ignoresSafeArea(edges: .bottom)
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
        .padding(.horizontal, prefersLandscape ? 4 : 16)
        .padding(.top, prefersLandscape ? 4 : 8)
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
