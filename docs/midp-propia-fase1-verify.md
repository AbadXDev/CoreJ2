# MIDP propia — FASE 0 inventario + FASE 1 LCDUI — verificación XS Max

**Fecha:** 2026-11-27 ~06:22 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Regla:** clean-room (JSR-118 / comportamiento observable); Vendor/FreeJ2ME intacto; **sin push a GitHub**.

---

## FASE 0 — Inventario

Documento: [`docs/midp-propia-inventario.md`](midp-propia-inventario.md)

- 10 JAR en `games/` escaneados (constant pool / method refs).
- Superficie agregada: LCDUI, Game API, MIDlet, MMAPI, RMS, GCF, M3G, Nokia MID.
- Orden por capas 0→7 documentado.

---

## FASE 1 — LCDUI clean-room

Overlay (pisan FQCN Vendor en classpath embebido):

| Clase | Estado |
|-------|--------|
| `Display` | clean-room + `VibrationBridge` |
| `Displayable` | clean-room + framebuffer/`platformImage` |
| `Canvas` | clean-room paint→`PlatformImage`→`MobilePlatform.repaint` |
| `Graphics` | clean-room base; dibujo en `PlatformGraphics` |
| `Font` | clean-room + `PlatformFont`/`BitmapFont` |
| `Image` | overlay propio (factories → `PlatformImage`) |
| `Command` / `CommandListener` | clean-room |
| `Screen` / `Alert` / `Form` | mínimos clean-room |

Marcadores de runtime: logs `corej2-lcdui: Display ready` / `corej2-lcdui: Canvas WxH`.

---

## Verificación dispositivo

### (a) PruebaVibracion — nuestro LCDUI

- Autolaunch `PruebaVibracion.jar`
- `corej2-lcdui: Display ready`
- `corej2-lcdui: Canvas 240x320`
- `START_APP_OK`
- `lcdNonZeroPixels=76800`
- `startSession OK PruebaVibracion` / `error=nil`
- QA tap `(120,160)` → `corej2-vibrate: 500ms` / `start 500ms pulses=4` (nuestro `Display.vibrate`)

### (b) Miami EN v1.0.8 — sin regresión

- Autolaunch Miami EN
- Mismos marcadores `corej2-lcdui:*`
- `START_APP_OK`
- `lcdNonZeroPixels=76800`
- `startSession OK Miami Nights` / `error=nil`

### (c) Inventario en docs/

- `docs/midp-propia-inventario.md` presente

---

## Build

- `scripts/embedded-jvm/compile-freej2me-classpath.sh` → OK (423 classes, overlay applied)
- `xcodebuild` Debug → **BUILD SUCCEEDED**
- Instalado en XS Max

Vendor/FreeJ2ME **no modificado**. Sin push.
