# CoreJ2 — build/install en iPhone XS Max

**Fecha:** 2026-11-27 00:47 AST (America/Santo_Domingo)  
**Dispositivo:** iPhone XS Max A12+ (`00008020-001645C20221002E`)  
**Scheme:** CoreJ2 · **SDK:** iphoneos · **Team:** 3JT5LP9X6X  
**Resultado:** **BUILD SUCCEEDED** · app instalada · proceso vivo sin crash

---

## 1) Errores encontrados y correcciones

### A. Include C incorrecto (`JNIGatewayNative.c`)
- **Error:** `'../Native/EmbeddedJVMNative.h' file not found`
- **Causa:** el `.c` vive en `EmbeddedJVM/JNIGateway/Native/`; `../Native` apuntaba al mismo directorio, no a `EmbeddedJVM/Native/`.
- **Fix:** `#include "../../Native/EmbeddedJVMNative.h"`

### B. Swift 6 region isolation (`EmbeddedFreeJ2MEMobilePlatformBootstrap.swift`)
- **Error:** `passing closure as a 'sending' parameter risks causing data races` en `Task.detached` dentro de `runSync`.
- **Fix:** `runSync` pasó a `nonisolated` y exige `@Sendable` + `T: Sendable`, para no arrastrar aislamiento del `@MainActor`.

### C. Archivo Swift fuera del target Xcode
- **Error:** `cannot find 'JNICreateJavaVMNativeRuntime' in scope`
- **Causa:** `JNICreateJavaVMNativeRuntime.swift` existía en disco pero no estaba en `CoreJ2.xcodeproj`.
- **Fix:** añadido al `project.pbxproj` (fileRef + Sources + grupo EmbeddedJVM).

### D. Enlace OpenJDK Mobile
- El link con `-force_load` de `libjvm.a` **completó sin símbolos indefinidos**.
- `OpenJDKMobileWXStubs.cpp` no necesitó stubs nuevos en este build.
- Warnings de deployment target (JVM built for iOS 27.2 vs app 17.0): no bloqueantes.
- Símbolos verificados en el dylib: `_JNI_CreateJavaVM`, `_corej2_embedded_jvm_create`.

**No se modificó** `Vendor/FreeJ2ME`. **No hubo push** a GitHub. El checkout local no es un repo git.

---

## 2) ¿App instalada?

**Sí.**

```text
bundleID: dev.abadx.corej2
installationURL: file:///private/var/containers/Bundle/Application/BF7218F4-BA08-42D3-B3A2-B339AEB51E95/CoreJ2.app/
display name: CoreJ2
```

Firma: Apple Development (Leonel Tomas Figuereo Abad) · Team `3JT5LP9X6X`.

---

## 3) ¿Arranca en el iPhone?

**Sí.** Launch con `devicectl` OK; proceso estable:

| Momento | PID | Estado |
|---------|-----|--------|
| ~t+4s | 1171 | running |
| ~t+12s | 1171 | running |
| ~t+20s+ | 1171 | running |

Sin crash reports de CoreJ2/CoreJ2 en el dispositivo. La UI del proceso se mostró como app activa (no se verificó juego JAR; solo arranque de la app).

---

## 4) Rutas de artefactos

| Qué | Ruta absoluta |
|-----|----------------|
| Informe (este) | `/Users/sakamoto/Desktop/Muse archivos/corej2/docs/iphone-xs-max-device-build-report.md` |
| `.app` instalable | `/Users/sakamoto/Desktop/Muse archivos/corej2/build/DerivedData/Build/Products/Debug-iphoneos/CoreJ2.app` |
| Log build exitoso | `/Users/sakamoto/Desktop/Muse archivos/corej2/build/device/build4.log` |
| Logs builds previos | `/Users/sakamoto/Desktop/Muse archivos/corej2/build/device/build1.log` … `build3.log` |
| DerivedData | `/Users/sakamoto/Desktop/Muse archivos/corej2/build/DerivedData` |

### Archivos tocados
- `/Users/sakamoto/Desktop/Muse archivos/corej2/CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/JNIGateway/Native/JNIGatewayNative.c`
- `/Users/sakamoto/Desktop/Muse archivos/corej2/CoreJ2/Features/Emulator/Runtime/EmbeddedFreeJ2MEMobilePlatformBootstrap.swift`
- `/Users/sakamoto/Desktop/Muse archivos/corej2/CoreJ2.xcodeproj/project.pbxproj`

### Precondiciones verificadas (Mac local)
- `artifacts/openjdk-mobile/staged/current/static-libs/lib/zero/libjvm.a` (~24 MB)
- `artifacts/freej2me-embedded-classpath/classes` (410 `.class`)
- `ThirdParty/OpenJDKMobile/src/mobile/build/ios-aarch64-zero-release/jdk`
- iPhone XS Max paired + available
