import SwiftUI
import UIKit

/// UIKit-backed LCD bitmap that always uses `.scaleAspectFit` (never fill/crop).
///
/// SwiftUI `Image` + `GeometryReader` sizing raced with the on-screen keypad and
/// could lay out a taller-than-visible frame that the parent then clipped.
struct EmulatorLCDImageView: UIViewRepresentable {

    /// Latest LCD frame, or `nil` while waiting for the first paint.
    var image: CGImage?

    func makeUIView(context: Context) -> UIImageView {
        let view = UIImageView()
        view.contentMode = .scaleAspectFit
        view.backgroundColor = .black
        view.clipsToBounds = true
        view.isOpaque = true
        view.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        view.setContentCompressionResistancePriority(.defaultLow, for: .vertical)
        view.setContentHuggingPriority(.defaultLow, for: .horizontal)
        view.setContentHuggingPriority(.defaultLow, for: .vertical)
        return view
    }

    func updateUIView(_ uiView: UIImageView, context: Context) {
        if let image {
            // scale 1: treat pixel grid as the intrinsic aspect source; contentMode fits it.
            uiView.image = UIImage(cgImage: image, scale: 1.0, orientation: .up)
        } else {
            uiView.image = nil
        }
    }
}
