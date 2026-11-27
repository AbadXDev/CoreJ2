import SwiftUI

/// App settings: appearance, controls, vibration, and RMS savedata.
struct SettingsView: View {

    @ObservedObject private var settings = AppSettings.shared
    @State private var suites: [RMSStoreService.Suite] = []
    @State private var suitePendingDelete: RMSStoreService.Suite?
    @State private var storePendingDelete: RMSStoreService.Store?
    private let rms = RMSStoreService()

    var body: some View {
        List {
            appearanceSection
            controlsSection
            vibrationSection
            savedataSection
        }
        .navigationTitle("Ajustes")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { reloadRMS() }
        .alert("¿Eliminar datos?", isPresented: suiteDeleteBinding) {
            Button("Cancelar", role: .cancel) { suitePendingDelete = nil }
            Button("Eliminar", role: .destructive) {
                if let suite = suitePendingDelete {
                    try? rms.deleteSuite(named: suite.name)
                    reloadRMS()
                }
                suitePendingDelete = nil
            }
        } message: {
            if let suite = suitePendingDelete {
                Text("Se borrarán todos los record stores de \"\(suite.name)\".")
            }
        }
        .alert("¿Eliminar record store?", isPresented: storeDeleteBinding) {
            Button("Cancelar", role: .cancel) { storePendingDelete = nil }
            Button("Eliminar", role: .destructive) {
                if let store = storePendingDelete {
                    try? rms.deleteStore(store)
                    reloadRMS()
                }
                storePendingDelete = nil
            }
        } message: {
            if let store = storePendingDelete {
                Text("Se eliminará \"\(store.name)\".")
            }
        }
    }

    // MARK: - Sections

    private var appearanceSection: some View {
        Section("Apariencia") {
            Picker("Tema", selection: $settings.appearance) {
                ForEach(AppSettings.Appearance.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
        }
    }

    private var controlsSection: some View {
        Section("Controles") {
            Toggle("Teclado numérico visible", isOn: $settings.numericKeypadVisibleByDefault)
        }
    }

    private var vibrationSection: some View {
        Section("Vibración") {
            Toggle("Vibración del juego", isOn: $settings.vibrationEnabled)
            Button("Probar vibración") {
                MMAVibrationEngine().vibrate(durationMs: 200)
            }
        }
    }

    private var savedataSection: some View {
        Section {
            if suites.isEmpty {
                Text("No hay datos guardados.")
                    .foregroundStyle(.secondary)
            } else {
                ForEach(suites) { suite in
                    DisclosureGroup {
                        ForEach(suite.stores) { store in
                            HStack {
                                Text(store.name)
                                Spacer()
                                Text(byteString(store.byteCount))
                                    .foregroundStyle(.secondary)
                                    .font(.caption)
                            }
                            .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                                Button(role: .destructive) {
                                    storePendingDelete = store
                                } label: {
                                    Label("Eliminar", systemImage: "trash")
                                }
                            }
                        }
                    } label: {
                        HStack {
                            Text(suite.name)
                            Spacer()
                            Text(byteString(suite.totalBytes))
                                .foregroundStyle(.secondary)
                                .font(.caption)
                        }
                    }
                    .contextMenu {
                        Button(role: .destructive) {
                            suitePendingDelete = suite
                        } label: {
                            Label("Eliminar todo", systemImage: "trash")
                        }
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            suitePendingDelete = suite
                        } label: {
                            Label("Eliminar", systemImage: "trash")
                        }
                    }
                }
            }
        } header: {
            Text("Datos guardados (RMS)")
        } footer: {
            Text("Cada juego guarda progress en carpetas bajo Documents/rms.")
        }
    }

    // MARK: - Helpers

    private func reloadRMS() {
        suites = rms.listSuites()
    }

    private func byteString(_ bytes: Int) -> String {
        ByteCountFormatter.string(fromByteCount: Int64(bytes), countStyle: .file)
    }

    private var suiteDeleteBinding: Binding<Bool> {
        Binding(
            get: { suitePendingDelete != nil },
            set: { if !$0 { suitePendingDelete = nil } }
        )
    }

    private var storeDeleteBinding: Binding<Bool> {
        Binding(
            get: { storePendingDelete != nil },
            set: { if !$0 { storePendingDelete = nil } }
        )
    }
}
