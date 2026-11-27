import Foundation

/// Inspects and deletes FreeJ2ME RecordStore files under `Documents/rms/`.
struct RMSStoreService {

    struct Suite: Identifiable, Hashable {
        let name: String
        var id: String { name }
        let stores: [Store]
        var totalBytes: Int { stores.reduce(0) { $0 + $1.byteCount } }
    }

    struct Store: Identifiable, Hashable {
        let name: String
        var id: String { name }
        let byteCount: Int
        let fileURL: URL
    }

    private let fileManager: FileManager

    init(fileManager: FileManager = .default) {
        self.fileManager = fileManager
    }

    /// Root of RMS data (`Documents/rms`).
    func rmsRootURL() -> URL? {
        guard let documents = fileManager.urls(for: .documentDirectory, in: .userDomainMask).first else {
            return nil
        }
        return documents.appendingPathComponent("rms", isDirectory: true)
    }

    /// Lists every suite folder and its record-store files.
    func listSuites() -> [Suite] {
        guard let root = rmsRootURL(),
              fileManager.fileExists(atPath: root.path),
              let suiteDirs = try? fileManager.contentsOfDirectory(
                at: root,
                includingPropertiesForKeys: [.isDirectoryKey],
                options: [.skipsHiddenFiles]
              ) else {
            return []
        }

        return suiteDirs.compactMap { suiteURL -> Suite? in
            var isDir: ObjCBool = false
            guard fileManager.fileExists(atPath: suiteURL.path, isDirectory: &isDir), isDir.boolValue else {
                return nil
            }
            let stores = listStores(in: suiteURL)
            return Suite(name: suiteURL.lastPathComponent, stores: stores)
        }
        .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }

    /// Deletes every record store for `suiteName`.
    func deleteSuite(named suiteName: String) throws {
        guard let root = rmsRootURL() else { return }
        let url = root.appendingPathComponent(suiteName, isDirectory: true)
        if fileManager.fileExists(atPath: url.path) {
            try fileManager.removeItem(at: url)
        }
    }

    /// Deletes one record store file.
    func deleteStore(_ store: Store) throws {
        if fileManager.fileExists(atPath: store.fileURL.path) {
            try fileManager.removeItem(at: store.fileURL)
        }
        // Remove empty suite folder.
        let suiteDir = store.fileURL.deletingLastPathComponent()
        if let remaining = try? fileManager.contentsOfDirectory(atPath: suiteDir.path), remaining.isEmpty {
            try? fileManager.removeItem(at: suiteDir)
        }
    }

    private func listStores(in suiteURL: URL) -> [Store] {
        guard let files = try? fileManager.contentsOfDirectory(
            at: suiteURL,
            includingPropertiesForKeys: [.fileSizeKey, .isRegularFileKey],
            options: [.skipsHiddenFiles]
        ) else {
            return []
        }
        return files.compactMap { fileURL -> Store? in
            let values = try? fileURL.resourceValues(forKeys: [.fileSizeKey, .isRegularFileKey])
            guard values?.isRegularFile == true else { return nil }
            return Store(
                name: fileURL.lastPathComponent,
                byteCount: values?.fileSize ?? 0,
                fileURL: fileURL
            )
        }
        .sorted { $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending }
    }
}
