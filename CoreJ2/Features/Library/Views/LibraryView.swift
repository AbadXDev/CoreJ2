import SwiftUI
import UniformTypeIdentifiers

/// Displays the user's Java ME game library and launches the emulator.
struct LibraryView: View {

    // MARK: - Dependencies

    @ObservedObject private var viewModel: LibraryViewModel

    // MARK: - UI State

    @State private var isImportPresented = false
    @State private var gameForProfilePicker: InstalledGame?
    @State private var gamePendingDeletion: InstalledGame?

    // MARK: - Init

    init(viewModel: LibraryViewModel) {
        self.viewModel = viewModel
    }

    // MARK: - Body

    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Library")
                .toolbar {
                    ToolbarItem(placement: .topBarLeading) {
                        NavigationLink {
                            SettingsView()
                        } label: {
                            Image(systemName: "gearshape")
                        }
                        .accessibilityLabel("Ajustes")
                    }
                    ToolbarItem(placement: .primaryAction) {
                        Button("Import Game") {
                            isImportPresented = true
                        }
                    }
                }
                .fileImporter(
                    isPresented: $isImportPresented,
                    allowedContentTypes: Self.jarContentTypes,
                    allowsMultipleSelection: false
                ) { result in
                    viewModel.handleImportResult(result)
                }
                .alert(
                    "Import Successful",
                    isPresented: successAlertBinding
                ) {
                    Button("OK", role: .cancel) {
                        viewModel.dismissSuccessMessage()
                    }
                } message: {
                    Text(viewModel.successMessage ?? "")
                }
                .alert(
                    "Import Failed",
                    isPresented: errorAlertBinding
                ) {
                    Button("OK", role: .cancel) {
                        viewModel.dismissErrorMessage()
                    }
                } message: {
                    Text(viewModel.errorMessage ?? "")
                }
                .alert(
                    "¿Eliminar?",
                    isPresented: deleteConfirmBinding
                ) {
                    Button("Cancelar", role: .cancel) {
                        gamePendingDeletion = nil
                    }
                    Button("Eliminar", role: .destructive) {
                        if let game = gamePendingDeletion {
                            viewModel.deleteGame(game)
                        }
                        gamePendingDeletion = nil
                    }
                } message: {
                    if let game = gamePendingDeletion {
                        Text("Se eliminará \"\(game.title)\" de la biblioteca y su archivo JAR.")
                    }
                }
                .navigationDestination(item: gameToLaunchBinding) { game in
                    if let emulatorViewModel = viewModel.activeEmulatorViewModel {
                        EmulatorView(viewModel: emulatorViewModel, game: game)
                    }
                }
                .sheet(item: $gameForProfilePicker) { game in
                    DeviceProfilePickerView(game: game) { profile in
                        viewModel.updateDeviceProfile(profile, for: game)
                    }
                }
        }
        .onAppear {
            viewModel.loadGames()
        }
    }

    // MARK: - Content

    @ViewBuilder
    private var content: some View {
        if viewModel.games.isEmpty {
            emptyState
        } else {
            gameList
        }
    }

    private var emptyState: some View {
        VStack(spacing: 12) {
            Image(systemName: "square.grid.2x2")
                .font(.system(size: 48))
                .foregroundStyle(.secondary)

            Text("No Games")
                .font(.title2.weight(.semibold))

            Text("Import a Java ME game to get started.")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)

            if let selectedFileName = viewModel.selectedFileName {
                Text(selectedFileName)
                    .font(.body.weight(.medium))
                    .multilineTextAlignment(.center)
                    .padding(.top, 8)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    private var gameList: some View {
        List(viewModel.games) { game in
            Button {
                viewModel.selectGame(game)
            } label: {
                HStack(spacing: 12) {
                    gameIcon(for: game)

                    VStack(alignment: .leading, spacing: 4) {
                        Text(game.title)
                            .font(.body)
                            .foregroundStyle(.primary)

                        Text(game.infoSummary)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }

                    Spacer()

                    Image(systemName: "play.circle.fill")
                        .font(.title3)
                        .foregroundStyle(.tint)
                }
                .padding(.vertical, 2)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                Button(role: .destructive) {
                    gamePendingDeletion = game
                } label: {
                    Label("Eliminar", systemImage: "trash")
                }
            }
            .contextMenu {
                Button {
                    gameForProfilePicker = game
                } label: {
                    Label("Device Profile", systemImage: "iphone.gen1")
                }
                Button(role: .destructive) {
                    gamePendingDeletion = game
                } label: {
                    Label("Eliminar", systemImage: "trash")
                }
            }
        }
    }

    /// MIDlet icon extracted at import time, with a placeholder when unavailable.
    private func gameIcon(for game: InstalledGame) -> some View {
        AsyncImage(url: game.artworkURL) { image in
            image
                .resizable()
                .interpolation(.none)
                .aspectRatio(contentMode: .fit)
        } placeholder: {
            RoundedRectangle(cornerRadius: 6, style: .continuous)
                .fill(Color.secondary.opacity(0.15))
                .overlay {
                    Image(systemName: "gamecontroller")
                        .foregroundStyle(.secondary)
                }
        }
        .frame(width: 40, height: 40)
        .clipShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
    }

    // MARK: - Bindings

    private var gameToLaunchBinding: Binding<InstalledGame?> {
        Binding(
            get: { viewModel.gameToLaunch },
            set: { newValue in
                if newValue == nil {
                    viewModel.dismissEmulator()
                } else {
                    viewModel.gameToLaunch = newValue
                }
            }
        )
    }

    private var successAlertBinding: Binding<Bool> {
        Binding(
            get: { viewModel.successMessage != nil },
            set: { isPresented in
                if !isPresented {
                    viewModel.dismissSuccessMessage()
                }
            }
        )
    }

    private var errorAlertBinding: Binding<Bool> {
        Binding(
            get: { viewModel.errorMessage != nil },
            set: { isPresented in
                if !isPresented {
                    viewModel.dismissErrorMessage()
                }
            }
        )
    }

    private var deleteConfirmBinding: Binding<Bool> {
        Binding(
            get: { gamePendingDeletion != nil },
            set: { isPresented in
                if !isPresented {
                    gamePendingDeletion = nil
                }
            }
        )
    }

    // MARK: - File Types

    /// JAR plus fallbacks: on-device Files often tags unknown `.jar` as
    /// `public.data` / zip, which a JAR-only filter hides from the picker.
    private static let jarContentTypes: [UTType] = {
        var types: [UTType] = [
            UTType(exportedAs: "com.sun.java-archive"),
            .zip,
            .archive,
            .data
        ]
        if let jar = UTType(filenameExtension: "jar") {
            types.insert(jar, at: 0)
        }
        return types
    }()
}

// MARK: - Previews

struct LibraryView_Previews: PreviewProvider {
    static var previews: some View {
        let repository = InMemoryGameLibraryRepository()
        LibraryView(
            viewModel: LibraryViewModel(
                repository: repository,
                importEngine: DefaultImportEngine(
                    pipeline: DefaultImportPipeline(
                        importService: FileImportService(),
                        steps: [
                            ManifestStep(manifestService: JARManifestService()),
                            HashStep(),
                            DuplicateDetectionStep(repository: repository),
                            ArtworkStep()
                        ]
                    )
                ),
                makeEmulatorViewModel: {
                    EmulatorViewModel(bridge: DefaultEmulatorBridge())
                }
            )
        )
    }
}
