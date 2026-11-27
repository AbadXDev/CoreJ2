# FASE 3 ronda 4 — Perf / Asphalt UV / iconos / Real Football 2011

**Fecha:** 2026-11-27 ~10:15 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Bundle:** `dev.abadx.corej2`  
**Classpath:** puro fase3 (282 classes)  
**Vendor/FreeJ2ME:** intacto  
**Branch:** `cursor/fase3-ronda4-perf-icons-rf2011-7520`

---

## 1) Prince of Persia + Assassin's Creed (calor / FPS)

### Causas

1. `Canvas.repaint` hacía **spin** de `pendingRepaint` cuando el juego llama `repaint()` desde `paint()` (típico en touch 640×360) → pintaba sin techo y calentaba el XS Max.  
2. Logs `Canvas.paint begin/end` + `flush` en cada frame.  
3. Emisión de frames a ~30 fps en LCD 640×360 (muchos píxeles).

### Cambios

| Pieza | Cambio |
|-------|--------|
| `Canvas.repaint` | Máx. **2** passes por llamada; `dt` medido por pass |
| Logs paint | Solo si `dt≥40ms` o cada 60 frames |
| `DefaultPlatformBootstrap` | Pace ~20 fps si ≥200k px (640×360), ~25 fps si ≥90k (240×400) |

### Verificación XS Max

| Juego | Perfil | landscape | paint median | Notas |
|-------|--------|-----------|--------------|-------|
| Prince | `nokia-s60-640x360` | **true** | **~40 ms** | Antes el log acumulaba miles de ms por el spin |
| Assassin's Creed | `nokia-s60-640x360` | **true** | **~45 ms** | startSession OK |

---

## 2) Asphalt — UV + calor

### Cambios M3G

1. **Clip poligonal vs W=ε** (`emitNearClipped`) — tris que cruzan el near plane se recortan a 1–2 tris en vez de descartarse o streakear.  
2. Tessellation **selectiva** más barata (`longest>2`, depth≤4, ndc grande).  
3. **Sin `flushToPlatform` por MeshVB** — Asphalt hace cientos de meshes/frame; el flush queda en `releaseTarget` / `render(World)`.

### Perf carrera (dumps)

- Paint en carrera: **~350–400 ms** (antes ~900–1100 ms con flush por mesh).  
- Frames: `docs/asphalt-m3g-frames-r4/` — Vegas con **Monte Carlo** / skyline legibles; pista con líneas visibles (banding residual menor).

---

## 3) Diamond Rush — icono de biblioteca

### Causa raíz

`JARArchiveReader` solo leía **local headers**. El `icon.png` de Diamond usa **ZIP data descriptor** (`flags&8`, csize/usize=0 en el local header) → la extracción fallaba y no existía `artwork/icon.png`.

Además, `AsyncImage(file://…)` es poco fiable para iconos locales/palette.

### Cambios

| Pieza | Cambio |
|-------|--------|
| `JARArchiveReader` | Lee vía **central directory** (tamaños + offset correctos) |
| `ArtworkStep` | Fallbacks `icon.png` / `Icon.png`; API estática compartida |
| `LibraryViewModel` | `backfillMissingArtwork()` al `loadGames()` |
| `LibraryView` | `UIImage(contentsOfFile:)` en lugar de AsyncImage |

### Verificación

- Tras install: `Library/…/artwork/icon.png` = **9570 bytes, 50×50 RGBA** (match del JAR).  
- Evidencia: `media/library-icons-r4/diamond-rush-icon.png`.

---

## 4) Real Football 2011 — no inicia

### Hallazgos en XS Max (flujo `selectGame` = mismo path que tap en biblioteca)

| Check | Resultado |
|-------|-----------|
| Perfil | **`samsung-240x400` 240×400** (desde basename `…_240x400.jar`) |
| `Canvas` | 240×400 |
| `startSession` | **OK** (`GloftRF11` + `START_APP_OK`) |
| LCD | `lcdNonZeroPixels=25000` (hay imagen) |

### Causas que lo hacían parecer “muerto”

1. Perfil incorrecto histórico → UI de 240×400 en viewport equivocado.  
2. Spikes de paint de **varios segundos** al cargar assets + spin de `pendingRepaint` (mismo bug que Prince).

### Cambios

- `repairFilenameResolutionProfiles()` en `loadGames` — realinea SwiftData al `WIDTHxHEIGHT` del JAR en Documents.  
- Cap de paint + pacing 240×400 (~25 fps).  
- Tras fix: arranca con perfil correcto; median paint ~95 ms (spikes de carga residuales al abrir menús).

---

## 5) Build / install

| Paso | Resultado |
|------|-----------|
| `compile-fase3-pure.sh` | 282 classes |
| `xcodebuild` Debug → XS Max | **BUILD SUCCEEDED** |
| Install | CoreJ2.app en el XS Max |

---

## 6) Archivos tocados (sin Vendor)

- `JARArchiveReader.swift`, `ArtworkStep.swift`, `LibraryView.swift`, `LibraryViewModel.swift`
- `Canvas.java`, `Graphics3D.java`, `SoftwareM3GRenderer.java`
- `DefaultPlatformBootstrap.swift`
