import SwiftUI

/// Owns the visual LCD surface that displays FreeJ2ME frames plus on-screen controls.
///
/// Portrait games keep LCD above controls. Landscape profiles (LCD wider than
/// tall, e.g. 640×360) lock landscape and place controls on the left/right.
///
/// The LCD always uses aspect **fit** so the full framebuffer (e.g. 240×400) is
/// visible — never aspect-fill / crop. Keypad space is reserved from the first
/// frame so `GeometryReader` does not briefly claim the full screen height.
struct EmulatorView: View {

    // MARK: - Dependencies

    @ObservedObject private var viewModel: EmulatorViewModel
    private let game: InstalledGame
    @State private var pointerIsDown = false
    @State private var lastLoggedViewport: String = ""

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

            // Always reserve control height so the LCD GeometryReader never
            // measures the full screen before the session is ready (that race
            // produced a taller-than-visible fitted frame that got clipped).
            EmulatorKeypadView(
                onKeyDown: { viewModel.keyDown($0) },
                onKeyUp: { viewModel.keyUp($0) },
                onKeyRepeat: { viewModel.keyRepeat($0) }
            )
            .opacity(viewModel.activeSession == nil ? 0.35 : 1)
            .allowsHitTesting(viewModel.activeSession != nil)
        }
    }

    // MARK: - Landscape

    private var landscapeBody: some View {
        HStack(spacing: 0) {
            EmulatorKeypadView(
                onKeyDown: { viewModel.keyDown($0) },
                onKeyUp: { viewModel.keyUp($0) },
                onKeyRepeat: { viewModel.keyRepeat($0) },
                landscapeSide: .left
            )
            .frame(width: 156)
            .opacity(viewModel.activeSession == nil ? 0.35 : 1)
            .allowsHitTesting(viewModel.activeSession != nil)

            lcdSurface
                .frame(maxWidth: .infinity, maxHeight: .infinity)

            EmulatorKeypadView(
                onKeyDown: { viewModel.keyDown($0) },
                onKeyUp: { viewModel.keyUp($0) },
                onKeyRepeat: { viewModel.keyRepeat($0) },
                landscapeSide: .right
            )
            .frame(width: 156)
            .opacity(viewModel.activeSession == nil ? 0.35 : 1)
            .allowsHitTesting(viewModel.activeSession != nil)
        }
        .ignoresSafeArea(edges: .bottom)
    }

    // MARK: - Surface

    /// LCD plane: black letterbox + aspect-fitted framebuffer.
    private var lcdSurface: some View {
        GeometryReader { geometry in
            let size = Self.fittedLCDSize(in: geometry.size, aspect: lcdAspectRatio)
            let origin = CGPoint(
                x: (geometry.size.width - size.width) / 2,
                y: (geometry.size.height - size.height) / 2
            )
            ZStack(alignment: .topLeading) {
                Color.black
                lcdContent
                    .frame(width: size.width, height: size.height)
                    .offset(x: origin.x, y: origin.y)
                    .clipShape(RoundedRectangle(cornerRadius: 4, style: .continuous))
                    .gesture(lcdPointerGesture(displaySize: size))
                    .accessibilityElement(children: .ignore)
                    .accessibilityLabel(accessibilityLabel)
            }
            .frame(width: geometry.size.width, height: geometry.size.height)
            .clipped()
            .onAppear {
                logViewport(geometry: geometry.size, fitted: size)
            }
            .onChange(of: geometry.size) { _, newSize in
                let fitted = Self.fittedLCDSize(in: newSize, aspect: lcdAspectRatio)
                logViewport(geometry: newSize, fitted: fitted)
            }
        }
        .padding(.horizontal, prefersLandscape ? 4 : 16)
        .padding(.top, prefersLandscape ? 4 : 8)
        .background(Color(uiColor: .systemBackground))
    }

    /// Width/height of the live LCD (frame metadata), else the device profile.
    private var lcdAspectRatio: CGFloat {
        if let metadata = viewModel.surfaceMetadata, metadata.height > 0, metadata.width > 0 {
            return CGFloat(metadata.width) / CGFloat(metadata.height)
        }
        let profile = game.deviceProfile
        guard profile.lcdHeight > 0 else { return 1 }
        return CGFloat(profile.lcdWidth) / CGFloat(profile.lcdHeight)
    }

    @ViewBuilder
    private var lcdContent: some View {
        if viewModel.lcdImage != nil {
            EmulatorLCDImageView(image: viewModel.lcdImage)
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

    /// Largest size that fits `container` while preserving `aspect` (width/height).
    /// Never exceeds the container — prevents parent clipping of the LCD.
    static func fittedLCDSize(in container: CGSize, aspect: CGFloat) -> CGSize {
        let safeAspect = aspect > 0 ? aspect : 1
        let maxWidth = max(container.width, 0)
        let maxHeight = max(container.height, 0)
        guard maxWidth > 0, maxHeight > 0 else {
            return .zero
        }
        let widthLimitedHeight = maxWidth / safeAspect
        if widthLimitedHeight <= maxHeight {
            return CGSize(width: maxWidth, height: widthLimitedHeight)
        }
        return CGSize(width: maxHeight * safeAspect, height: maxHeight)
    }

    private func logViewport(geometry: CGSize, fitted: CGSize) {
        let line =
            "geo=\(Int(geometry.width))x\(Int(geometry.height))"
            + " fitted=\(Int(fitted.width))x\(Int(fitted.height))"
            + " aspect=\(String(format: "%.4f", lcdAspectRatio))"
            + " lcd=\(viewModel.surfaceMetadata.map { "\($0.width)x\($0.height)" } ?? "nil")"
        guard line != lastLoggedViewport else { return }
        lastLoggedViewport = line
        print("corej2-viewport: \(line)")
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
