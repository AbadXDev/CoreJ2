# MIDP propia — FASE 2 — verificación XS Max

**Fecha:** 2026-11-27 ~06:30 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Regla:** clean-room (JSR-118 / JSR-135 / GCF / Nokia UI observable); Vendor/FreeJ2ME intacto; **sin push a GitHub**.

---

## Qué se implementó (overlay)

| Área | FQCNs propios |
|------|----------------|
| MIDlet | `MIDlet`, `MIDletStateChangeException` |
| RMS | `RecordStore`, `RecordEnumeration`, exceptions, `RecordFilter`/`Comparator`/`Listener` |
| MMAPI | `Manager`, `Player`, `Controllable`, `Control`, `MediaException`, `PlayerListener`, `VolumeControl` → `PlatformPlayer`/`AudioBridge` |
| Game API | `GameCanvas`, `Layer`, `Sprite`, `TiledLayer` (+ `LayerManager`) |
| Nokia UI | `DirectGraphics`, `DirectUtils`, `DeviceControl` |
| GCF | `Connector` + `Connection`/`HttpConnection`/… stubs; `ConnectionNotFoundException` |

RMS on-disk layout: `<dataPath>./rms/<suitename>/<store>` con `uint16` BE version/nextId/count + records — **compatible** con saves existentes (`Documents/rms/…`) y con la UI de savedata.

---

## Variante de classpath sin FreeJ2ME en estos paquetes

Script: `scripts/embedded-jvm/compile-fase2-own-packages.sh`  

Salida: `artifacts/freej2me-embedded-classpath-fase2/` (+ `OWNED_PACKAGES.txt`)

Tras strip + recompile overlay, **solo** clases CoreJ2 en:

- `javax.microedition.midlet|rms|media|lcdui/game|io`
- `com.nokia.mid.ui`

Build normal (`compile-freej2me-classpath.sh`) sigue generando Vendor+overlay (sin regresión).

---

## Verificación — variante FASE 2 en dispositivo

Instalación con classpath fase2 embebido.

### PruebaVibracion

- `corej2-lcdui: Display ready` / `Canvas 240x320`
- `START_APP_OK`, `lcdNonZeroPixels=76800`
- `startSession OK`
- qa-tap → `corej2-vibrate: 500ms`
- **Sin** `NoClassDefFoundError`

### Miami Nights EN v1.0.8

- Mismos marcadores MIDlet/LCDUI; `START_APP_OK`
- Pintura continua: `frames emitted=240` / `paintIndex=484` (~45 s) — bucle de menú/splash activo
- **Sin** `NoClassDefFoundError` / `ClassNotFoundException`
- Audio/RMS no disparados en la ventana QA automatizada (el juego difiere createPlayer/openRecordStore hasta más adentro del menú); el stack MMAPI/RMS propio quedó probado con Bounce (abajo) y el layout RMS es compatible con el suite `Documents/rms/Miami Nights/` (p.ej. `Miami Nights0`: version=2, nextid=2, 1 record)

### Bounce Tales (cobertura Game + media en el mismo classpath fase2)

- `corej2-game: GameCanvas 240x320`
- `corej2-media: createPlayer stream type=audio/midi`
- `corej2-audio: start MIDI` + `stopAll count=1` al salir (`qa-audio-stop`)
- `startSession OK` — **Sin** NCDF

### Build normal (regresión)

- `compile-freej2me-classpath.sh` → OK (424 classes)
- `xcodebuild` Debug → **BUILD SUCCEEDED**
- Reinstalado; PruebaVibracion `START_APP_OK` / `startSession OK`

---

## Artefactos

| Path | Uso |
|------|-----|
| `artifacts/freej2me-embedded-classpath/` | Build diaria (Vendor + overlay) |
| `artifacts/freej2me-embedded-classpath-fase2/` | Variante “owned packages only” |
| `scripts/embedded-jvm/compile-fase2-own-packages.sh` | Genera la variante |

Vendor/FreeJ2ME **no modificado**. Sin push.
