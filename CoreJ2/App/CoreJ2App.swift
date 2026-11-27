import SwiftData
import SwiftUI
import UIKit

/// Locks supported interface orientations while an emulator session is visible.
enum AppOrientationLock {
    // UIKit orientation APIs are main-thread only; this flag is read from the
    // app delegate on the same thread.
    nonisolated(unsafe) private static var mask: UIInterfaceOrientationMask = .all

    static var supportedMask: UIInterfaceOrientationMask { mask }

    @MainActor
    static func lock(_ mask: UIInterfaceOrientationMask) {
        self.mask = mask
        guard let scene = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .first else {
            return
        }
        let geometry = UIWindowScene.GeometryPreferences.iOS(interfaceOrientations: mask)
        scene.requestGeometryUpdate(geometry) { _ in }
        UIViewController.attemptRotationToDeviceOrientation()
    }
}

final class CoreJ2AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        supportedInterfaceOrientationsFor window: UIWindow?
    ) -> UIInterfaceOrientationMask {
        AppOrientationLock.supportedMask
    }
}

@main
struct CoreJ2App: App {

    // MARK: - Dependencies

    @UIApplicationDelegateAdaptor(CoreJ2AppDelegate.self) private var appDelegate
    private let modelContainer: ModelContainer
    @StateObject private var libraryViewModel: LibraryViewModel
    @StateObject private var settings = AppSettings.shared

    // MARK: - Init

    init() {
        let container: ModelContainer

        do {
            container = try ModelContainer(for: InstalledGameEntity.self)
        } catch {
            fatalError("Failed to create the SwiftData model container: \(error)")
        }

        modelContainer = container
        let dependencies = AppDependencyContainer(modelContainer: container)
        _libraryViewModel = StateObject(wrappedValue: dependencies.makeLibraryViewModel())
    }

    // MARK: - Body

    var body: some Scene {
        WindowGroup {
            LibraryView(viewModel: libraryViewModel)
                .preferredColorScheme(settings.appearance.colorScheme)
                .environmentObject(settings)
        }
        .modelContainer(modelContainer)
    }
}
