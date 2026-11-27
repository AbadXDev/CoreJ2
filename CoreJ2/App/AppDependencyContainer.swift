import Foundation
import SwiftData

/// Composition root that builds and wires application dependencies.
@MainActor
final class AppDependencyContainer {

    // MARK: - Dependencies

    private let modelContainer: ModelContainer

    // MARK: - Repositories

    private lazy var gameLibraryRepository: GameLibraryRepository = SwiftDataGameLibraryRepository(
        modelContext: modelContainer.mainContext
    )

    // MARK: - Services

    private lazy var importService: ImportService = FileImportService()

    private lazy var manifestService: ManifestService = JARManifestService()

    private lazy var importSteps: [any ImportStep] = [
        ManifestStep(manifestService: manifestService),
        HashStep(),
        DuplicateDetectionStep(repository: gameLibraryRepository),
        ArtworkStep()
    ]

    private lazy var importPipeline: ImportPipelineProtocol = DefaultImportPipeline(
        importService: importService,
        steps: importSteps
    )

    private lazy var importEngine: ImportEngineProtocol = DefaultImportEngine(
        pipeline: importPipeline
    )

    // MARK: - Emulator

    private lazy var runtimeHost: RuntimeHostProtocol = makeRuntimeHost()

    private lazy var emulatorBridge: EmulatorBridgeProtocol = DefaultEmulatorBridge(
        runtimeHost: runtimeHost
    )

    // MARK: - Init

    init(modelContainer: ModelContainer) {
        self.modelContainer = modelContainer
    }

    // MARK: - Factories

    /// Creates a view model for the library screen.
    func makeLibraryViewModel() -> LibraryViewModel {
        LibraryViewModel(
            repository: gameLibraryRepository,
            importEngine: importEngine,
            makeEmulatorViewModel: { [self] in
                makeEmulatorViewModel()
            }
        )
    }

    /// Returns the shared emulator bridge used to launch installed games.
    func makeEmulatorBridge() -> EmulatorBridgeProtocol {
        emulatorBridge
    }

    /// Creates a view model for the emulator LCD surface.
    func makeEmulatorViewModel() -> EmulatorViewModel {
        EmulatorViewModel(bridge: emulatorBridge)
    }

    /// The concrete runtime host wired for production.
    ///
    /// Exposed for DI verification tests. Previews and isolated unit tests should
    /// continue to inject `PlaceholderRuntimeHost` directly.
    var productionRuntimeHost: RuntimeHostProtocol {
        runtimeHost
    }

    /// Builds the production runtime host behind the emulator bridge.
    ///
    /// Pipeline: Library → EmulatorBridge → FreeJ2MERuntimeHost →
    /// FreeJ2MERuntimeAdapter → FreeJ2ME.
    ///
    /// On iOS the adapter runs on the embedded OpenJDK Mobile JVM
    /// (in-process, device builds). On macOS it uses the host-JVM bootstrap.
    /// On the iOS Simulator (no libjvm linked) launch fails with a typed
    /// `EmulatorBridgeError` from the JVM create step.
    private func makeRuntimeHost() -> RuntimeHostProtocol {
        #if os(iOS)
        return FreeJ2MERuntimeHost(
            adapter: FreeJ2MERuntimeAdapter(platformBootstrap: makeEmbeddedBootstrap())
        )
        #else
        return FreeJ2MERuntimeHost()
        #endif
    }

    #if os(iOS)
    /// Builds the in-process OpenJDK Mobile bootstrap from bundle resources
    /// (`OpenJDKMobile/java.home` + `OpenJDKMobile/classpath`, copied by the
    /// "Copy Runtime" build phase on device builds).
    private func makeEmbeddedBootstrap() -> EmbeddedFreeJ2MEMobilePlatformBootstrap {
        let bundleRoot = Bundle.main.resourceURL ?? Bundle.main.bundleURL
        let runtimeRoot = bundleRoot.appendingPathComponent("OpenJDKMobile", isDirectory: true)
        let bundleJavaHome = runtimeRoot.appendingPathComponent("java.home", isDirectory: true)
        let classpath = runtimeRoot.appendingPathComponent("classpath", isDirectory: true)
        // iOS OpenJDK Mobile ignores -Djava.home and forces $HOME/Documents;
        // materialize lib/modules into the path set_boot_path expects.
        let javaHome: URL
        do {
            javaHome = try MaterializedJavaHome.resolve(bundleJavaHome: bundleJavaHome)
        } catch {
            print("corej2-launch: MaterializedJavaHome FAILED: \(error) — using bundle path")
            javaHome = bundleJavaHome
        }
        let configuration = EmbeddedJVMConfiguration(
            librarySource: .openJDKMobile(stagedRoot: runtimeRoot, javaHome: javaHome),
            classpath: classpath
        )
        let manager = EmbeddedJVMManager(
            configuration: configuration,
            native: JNICreateJavaVMNativeRuntime()
        )
        let gateway = DefaultJNIGateway(
            readiness: manager,
            native: ProductionJNIGatewayNativeBackend()
        )
        return EmbeddedFreeJ2MEMobilePlatformBootstrap(jvm: manager, gateway: gateway)
    }
    #endif
}
