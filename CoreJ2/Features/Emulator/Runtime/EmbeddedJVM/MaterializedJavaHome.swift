import Foundation

/// Prepares the on-device OpenJDK Mobile `java.home` layout for iOS.
///
/// OpenJDK Mobile (`os_bsd.cpp`) forces `java.home` to `$HOME/Documents` and
/// ignores `-Djava.home`. `ClassLoader::lookup_vm_options()` then opens
/// `$JAVA_HOME/lib/modules` during `Arguments::parse`.
///
/// We therefore install `lib/modules` at **`$HOME/Documents/lib/modules`**.
/// (An earlier nested `Documents/Documents/lib/modules` path matched a buggy
/// `#ifdef __IOS__` boot-path format; prefer the canonical `lib/modules`.)
enum MaterializedJavaHome {

    /// Installs `lib/modules` (+ conf / jvm.cfg) under Documents; returns Documents.
    static func resolve(bundleJavaHome: URL) throws -> URL {
        let fileManager = FileManager.default
        guard let documents = fileManager.urls(
            for: .documentDirectory,
            in: .userDomainMask
        ).first else {
            throw EmbeddedJVMError.failed("documents directory unavailable")
        }

        let sourceModules = bundleJavaHome
            .appendingPathComponent("lib", isDirectory: true)
            .appendingPathComponent("modules", isDirectory: false)
        guard fileManager.fileExists(atPath: sourceModules.path) else {
            throw EmbeddedJVMError.failed("bundle missing lib/modules at \(sourceModules.path)")
        }

        let destLib = documents.appendingPathComponent("lib", isDirectory: true)
        let destModules = destLib.appendingPathComponent("modules", isDirectory: false)

        let sourceSize = (try? sourceModules.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? -1
        let destSize = (try? destModules.resourceValues(forKeys: [.fileSizeKey]).fileSize) ?? -1
        if fileManager.fileExists(atPath: destModules.path), sourceSize > 0, destSize == sourceSize {
            print("corej2-launch: iOS java.home already materialized (\(destSize) bytes)")
            return documents
        }

        try fileManager.createDirectory(at: destLib, withIntermediateDirectories: true)
        if fileManager.fileExists(atPath: destModules.path) {
            try fileManager.removeItem(at: destModules)
        }
        try fileManager.copyItem(at: sourceModules, to: destModules)

        let cfgText = """
            # CoreJ2: Zero interpreter only (OpenJDK Mobile static link).
            -zero KNOWN

            """
        try cfgText.write(
            to: destLib.appendingPathComponent("jvm.cfg"),
            atomically: true,
            encoding: .utf8
        )

        let sourceConf = bundleJavaHome.appendingPathComponent("conf", isDirectory: true)
        let destConf = documents.appendingPathComponent("conf", isDirectory: true)
        if fileManager.fileExists(atPath: sourceConf.path) {
            if fileManager.fileExists(atPath: destConf.path) {
                try fileManager.removeItem(at: destConf)
            }
            try fileManager.copyItem(at: sourceConf, to: destConf)
        }

        // Keep nested path too: stock iOS set_boot_path looks for
        // $JAVA_HOME/Documents/lib/modules (= Documents/Documents/lib/modules).
        let nestedLib = documents
            .appendingPathComponent("Documents", isDirectory: true)
            .appendingPathComponent("lib", isDirectory: true)
        try fileManager.createDirectory(at: nestedLib, withIntermediateDirectories: true)
        let nestedModules = nestedLib.appendingPathComponent("modules", isDirectory: false)
        if fileManager.fileExists(atPath: nestedModules.path) {
            try fileManager.removeItem(at: nestedModules)
        }
        try fileManager.copyItem(at: sourceModules, to: nestedModules)
        try cfgText.write(
            to: nestedLib.appendingPathComponent("jvm.cfg"),
            atomically: true,
            encoding: .utf8
        )

        guard fileManager.fileExists(atPath: destModules.path) else {
            throw EmbeddedJVMError.failed("materialize failed: \(destModules.path)")
        }
        print("corej2-launch: materialized iOS modules → \(destModules.path) (\(sourceSize) bytes)")
        return documents
    }
}
