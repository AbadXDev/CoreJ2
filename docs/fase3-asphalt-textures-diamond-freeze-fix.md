# FASE 3 — Asphalt texturas M3G + Diamond freeze — XS Max

**Fecha:** 2026-11-27 ~08:17 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Bundle:** `dev.abadx.corej2`  
**Classpath:** puro fase3 (`artifacts/corej2-embedded-classpath-fase3/`, 281 classes)  
**Vendor/FreeJ2ME:** intacto  
**GitHub push:** feature branch only (si aplica); sin merge a main en este informe

---

## 1) Asphalt 3: Street Rules 3D — texturas rotas / negro

### Problema

En la matriz FASE 3 el framebuffer M3G tenía `nz≈26k–76k` (no era splash vacío), pero en juego real Leonel veía la carrera **negra** y con **texturas rotas**.

### Causa / fix (pipeline overlay M3G)

| Pieza | Cambio |
|-------|--------|
| `Image2D` | Pack/unpack correcto por formato (incl. LA bpp=2), `set` rectangular, `sampleARGB` con wrap + nearest/bilinear |
| `SoftwareM3GRenderer` | UV perspective-correct; wrap/filter; blend REPLACE / MODULATE / DECAL / BLEND / ADD |
| `Graphics3D` | Clear respeta `colorClear` / `depthClear` + Background `Image2D`; no fuerza negro opaco sobre texturas |
| Evidencia | `frameStats()`: `nz` + `meanRGB` + **varianza** + flag `tex0` |

### Evidencia de frame (XS Max, build instalada, `asphalt5.log`)

Entrada a carrera vía `qa-key` (`-5` / `53`). Muestras `corej2-m3g: render MeshVB`:

| Mesh | nz | meanRGB | var | tex0 |
|------|---:|---------|----:|:----:|
| #1 | 75943 | 141/142/149 | 13004 | false |
| #3 | 76601 | 132/125/112 | 5107 | **true** |
| #120 | 76800 | 155/114/58 | 5780 | true |
| #360 | 76800 | 163/164/165 | 6659 | true |
| #480 | 76800 | 145/149/150 | 6124 | true |
| #840 | 76797 | 134/88/74 | 3512 | true |

- **8/10** renders con `tex0=true`
- Varianza **min/avg/max = 1754 / 4203 / 6659** (no flat black: un clear negro daría mean≈0 y var≈0)
- Medias con canales distintos (p.ej. 155/114/58, 134/88/74) ⇒ color texturado real

**Meta:** la carrera 3D pinta con texturas (varianza de color real, no solo píxeles no-cero).

---

## 2) Diamond Rush — freeze tras el primer toque

### Repro

Pointer reales (`qa-tap`, no solo autolaunch). Tras el primer release el MIDlet hacía trabajo pesado (locale + RMS) en el hilo que servía input → taps posteriores muertos. Tras mover `pointerReleased` a worker, el cuelgue se movió a **post-idioma**.

### Causa raíz (post-idioma)

`RecordEnumeration` con `keepUpdated=true`:

1. `hasNextElement()` / `nextRecordId()` llamaban `rebuild()` → `build()` hacía **`index = 0` siempre**
2. `nextRecordId()` delegaba en `hasNextElement()` → cada avance **reiniciaba** el cursor
3. `while (hasNextElement()) { nextRecord(); }` leía el **primer** record forever → worker Java bloqueado
4. Worker serializado ⇒ ningún `pointerReleased` posterior → “congelado” en UI

Log pre-fix (`diamond7`): `enumerate keepUpdated=true` sin `worker end`; paints seguían (~25 ms) pero releases posteriores sin worker.

### Fix

En `RecordStore.EnumerationImpl`:

- `build(resetIndex)`: keepUpdated refresh **preserva** el cursor (solo clamp)
- `rebuild()` API explícita sí reinicia a 0
- `nextRecordId()` ya **no** llama `hasNextElement()` (evita rebuild+reset mid-advance)

Complementos previos que se mantienen: paint síncrono de hit-test en release, worker daemon serializado/coalesced, stub HTTP `Connector`.

### Verificación (XS Max, `diamond10.log`)

Flujo con pointers reales:

```
setCurrent app.t   → menú principal
pointer 120,120     → Play
setCurrent app.h   → selección de nivel
… taps nivel …
corej2-media: createPlayer stream type=audio/wav
setCurrent app.f   → gameplay
```

| Métrica | Resultado |
|---------|-----------|
| `pointerReleased-worker begin/end` | **18 / 18** (sin worker colgado) |
| Pantallas | `app.t` → `app.h` → **`app.f`** |
| Audio gameplay | `createPlayer` `audio/wav` |
| Paints tras `app.f` | **233** (avg ~57 ms) — tablero activo |

**Meta:** tocando se entra al gameplay; el freeze del primer toque queda resuelto en causa raíz RMS.

---

## 3) Build / install

| Paso | Resultado |
|------|-----------|
| `scripts/embedded-jvm/compile-fase3-pure.sh` | 281 classes, provenance OK |
| `xcodebuild` Debug → XS Max | **BUILD SUCCEEDED** |
| `devicectl install` | CoreJ2.app instalada en `00008020-001645C20221002E` |

Vendor/FreeJ2ME **no** tocado.

---

## 4) Archivos tocados (overlay / bootstrap only)

- `javax/microedition/m3g/Image2D.java`
- `javax/microedition/m3g/Graphics3D.java`
- `dev/abadx/corej2/overlay/m3g/SoftwareM3GRenderer.java`
- `javax/microedition/rms/RecordStore.java` (**enumerate keepUpdated**)
- `org/recompile/mobile/MobilePlatform.java` (worker pointer)
- `javax/microedition/lcdui/Canvas.java` / `Display.java` / `Image.java` (diag + reentrancy)
- `javax/microedition/io/Connector.java` (stub HTTP)

---

## 5) Logs locales de evidencia

| Log | Contenido |
|-----|-----------|
| `/tmp/fase3-fix/asphalt5.log` | Frame stats texturados |
| `/tmp/fase3-fix/diamond7.log` | Hang pre-fix en enumerate |
| `/tmp/fase3-fix/diamond10.log` | Play → nivel → `app.f` gameplay |
