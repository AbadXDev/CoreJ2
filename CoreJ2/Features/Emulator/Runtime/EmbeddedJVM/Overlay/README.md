# CoreJ2 classpath overlay (clean-room + bridges)

## FASE 3 — pure product classpath (default)

`scripts/embedded-jvm/compile-fase3-pure.sh`
→ `artifacts/corej2-embedded-classpath-fase3/`

**Zero FreeJ2ME / Vendor MIDlet classes.** Sources:

1. ObjectWeb ASM (BSD) from `Vendor/FreeJ2ME/src/org/objectweb/asm` (third-party only)
2. This Overlay tree (MIDP / MMAPI / M3G / Nokia + platform bridges)
3. `EmbeddedJVM/Bootstrap` (`LcdSupport`, `RunJarSupport`, …)

Copy Runtime embeds the fase3 tree by default (`COREJ2_CLASSPATH_VARIANT=reference` keeps the Vendor+overlay build as a reference).

`Vendor/FreeJ2ME` is never modified.

## LCDUI clean-room (FASE 1+)

| Class | Notes |
|-------|-------|
| `Display` / `Displayable` / `Canvas` / `Graphics` / `Font` / `Image` | JSR-118 + platform bridges |
| `Command` / `CommandListener` / `Screen` / `Alert` / `AlertType` / `Form` | |
| `Item` / `ImageItem` / `StringItem` / `TextField` / `Gauge` / `Ticker` | Form elements |
| `lcdui.game.*` | `GameCanvas`, `Layer`, `Sprite`, `TiledLayer`, `LayerManager` |

## FASE 2 packages

`midlet`, `rms`, `media` (+ `VolumeControl`), `io` (GCF stubs), `com.nokia.mid.ui`.

## M3G (JSR-184 subset)

Software path via `Graphics3D` + `dev.abadx.corej2.overlay.m3g.SoftwareM3GRenderer`.
Covers the Asphalt-referenced types (`Appearance`, `Background`, `Camera`,
`CompositingMode`, `PolygonMode`, `Sprite3D`, `Texture2D`, `Vertex*`, …).

## Platform bridges (`org.recompile.mobile` FQCNs)

Kept as stable JNI/bootstrap FQCNs. Implementations are CoreJ2-owned:

| Class | Role |
|-------|------|
| `Mobile` | Platform registry (clean-room rewrite) |
| `MIDletLoader` | Suite load + ASM resource rewrite (clean-room rewrite) |
| `MobilePlatform` | LCD / input / runJar |
| `PlatformImage` / `PlatformGraphics` / `PlatformFont` | Software LCD |
| `PlatformPlayer` | MMAPI → `AudioBridge` |

Helpers: `dev.abadx.corej2.overlay.*`, `dev.abadx.corej2.bootstrap.*`.

## Reference build (optional)

`scripts/embedded-jvm/compile-freej2me-classpath.sh` still produces
`artifacts/freej2me-embedded-classpath/` (Vendor + overlay overwrite) for comparison.
