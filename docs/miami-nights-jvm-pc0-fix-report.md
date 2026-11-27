# Miami Nights — fix PC=0 / CODESIGNING en OpenJDK Mobile

**Fecha:** 27 Nov 2026 (America/Santo_Domingo)  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Decisión del usuario:** opción 1 — instrumentar/parchear OpenJDK Mobile.

---

## Veredicto

| Pregunta | Respuesta |
|----------|-----------|
| ¿Se corrigió el salto a PC=0 en `Arguments::parse`? | **Sí.** |
| ¿`JNI_CreateJavaVM` completa? | **Sí** (`corej2_embedded_jvm_create: OK`). |
| ¿Miami Nights abre / pinta? | **No aún.** Nuevo bloqueo: `UnsatisfiedLinkError: no awt` al construir `MobilePlatform`. |
| ¿Vendor/FreeJ2ME tocado? | **No.** |
| ¿Push a GitHub? | **No.** |

---

## 1) Causa raíz (PC=0)

HotSpot, en enlace estático iOS, resuelve `JIMAGE_*` / `JDK_Canonicalize` / `ZIP_InflateFully` solo vía `dlsym`. Sin `-force_load` (y/o referencias duras), el linker hace dead-strip → puntero NULL → salto a **PC=0** → iOS `CODESIGNING` / Invalid Page.

Evolución de stacks:

1. **Antes:** `Arguments::parse` (null `JIMAGE_Open`).
2. **Tras force_load jimage:** `ImageDecompressor::decompress_resource` (null `ZIP_InflateFully` vía `RTLD_FIRST` en el stub del ejecutable, no en `CoreJ2.debug.dylib`).
3. **Tras parches + restage:** JVM OK; bootstrap FreeJ2ME falla en AWT.

---

## 2) Cambios aplicados

### App / link

- `scripts/openjdk-mobile/generate-xcode-link-xcconfig.sh`  
  `-Wl,-force_load` de `libjimage.a`, `libjava.a`, `libzip.a`.
- `MaterializedJavaHome.swift` — `Documents/lib/modules` (+ nested fallback).
- `EmbeddedJVMNative.c` — preflight de ambas rutas `modules`.
- `build-jlink-java-home.sh` — `--compress=zip-0`.
- Diagnóstico JNI: `ExceptionDescribe` + logs de pasos de bootstrap.

### Parches OpenJDK Mobile (`patches/openjdk-mobile/`)

| Patch | Qué hace |
|-------|----------|
| `0001-static-jimage-direct-symbols.patch` | `&JIMAGE_*` / `&JDK_Canonicalize` + fail ruidoso |
| `0002-ios-boot-path-lib-modules.patch` | `set_boot_path` → `%/lib/modules` (+ fallback nested) |
| `0003-ios-rtld-default-process-handle.patch` | iOS: `get_default_process_handle` → `RTLD_DEFAULT` |
| `0004-static-zip-inflate-fully.patch` | `&ZIP_InflateFully` en `ImageDecompressor` |

Aplicación idempotente: `scripts/openjdk-mobile/apply-patches.sh` (llamado desde `01-fetch-sources.sh` y `04-build-static-libs.sh`).

Fuentes JVM: symlink  
`ThirdParty/OpenJDKMobile/src/mobile` → `~/corej2-jvm/ThirdParty/OpenJDKMobile/src/mobile`.

---

## 3) Evidencia en dispositivo (consola)

```
corej2-jvm: set_boot_path modules=…/Documents/lib/modules
corej2-jvm: load_jimage_library OK open=0x…
corej2-jvm: lookup_vm_options path=…/lib/modules JImageOpen=0x…
corej2_embedded_jvm_create: OK
corej2-launch: bootstrap create MobilePlatform
java.lang.UnsatisfiedLinkError: no awt in system library path: …/Documents/lib
  at java.awt.image.ColorModel.loadLibraries
  at java.awt.image.BufferedImage.<clinit>
  at org.recompile.mobile.PlatformImage.<init>
  at org.recompile.mobile.MobilePlatform.<init>
```

---

## 4) Nuevo bloqueo (decisión requerida)

`MobilePlatform` → `PlatformImage` → `BufferedImage` exige **native AWT** (`libawt`).  
El image estático Zero de OpenJDK Mobile **no** incluye `libawt*.a` para iOS.

Opciones sugeridas (sin tocar `Vendor/FreeJ2ME`):

1. **Overlay de classpath** CoreJ2-owned: `org.recompile.mobile.PlatformImage` (y dependencias mínimas) con framebuffer `int[]` puro, delante del classpath FreeJ2ME.
2. **Stub / headless AWT** estático linkeado (trabajo grande; Gluon puede no exportarlo).
3. **Fork mínimo** de FreeJ2ME solo en bridge (no recomendado si Vendor debe quedar intacto).

---

## 5) Cómo reproducir el estado actual

```bash
export BOOT_JDK_HOME=/Users/sakamoto/jdk24
./scripts/openjdk-mobile/apply-patches.sh
./scripts/openjdk-mobile/04-build-static-libs.sh
./scripts/openjdk-mobile/05-stage-artifacts.sh
./scripts/openjdk-mobile/generate-xcode-link-xcconfig.sh
./scripts/openjdk-mobile/build-jlink-java-home.sh

xcodebuild -project CoreJ2.xcodeproj -scheme CoreJ2 -configuration Debug \
  -destination 'generic/platform=iOS' \
  -derivedDataPath build/DerivedData -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic DEVELOPMENT_TEAM=3JT5LP9X6X build

xcrun devicectl device install app --device 00008020-001645C20221002E \
  build/DerivedData/Build/Products/Debug-iphoneos/CoreJ2.app
./scripts/device/copy-jar-to-app-documents.sh
: > /tmp/autolaunch
xcrun devicectl device copy to --device 00008020-001645C20221002E \
  --domain-type appDataContainer --domain-identifier dev.abadx.corej2 \
  --destination Documents/autolaunch --source /tmp/autolaunch
xcrun devicectl device process launch --device 00008020-001645C20221002E \
  --terminate-existing --activate --console dev.abadx.corej2
```

Expectativa: JVM OK; fallo AWT en `MobilePlatform.<init>`.
