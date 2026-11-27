# FASE 3 ronda 5 — Real Football 2011 viewport (240×400 completo)

**Fecha:** 2026-11-27 ~10:35 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Bundle:** `dev.abadx.corej2`  
**Classpath:** puro fase3  
**Vendor/FreeJ2ME:** intacto  
**Branch:** `cursor/fase3-ronda5-rf2011-viewport-7520`

---

## Problema

Leonel: RF2011 arranca y el touch funciona, pero **solo se ve una parte de la pantalla**. Perfil ya `samsung-240x400`; el LCD tiene imagen → sospecha del viewport (aspect fill vs fit / contenedor con otra relación de aspecto).

---

## Reproducción XS Max

1. Autolaunch `Real-Football-2011_ES_240x400.jar` → perfil `samsung-240x400`, `Canvas 240x400`, `START_APP_OK`.
2. Screenshot **before** (`media/rf2011-viewport-r5/before-screen.png`): splash Gameloft **muy ampliado / recortado**.
3. Dump LCD `frame-*-240x400.png` vía `Documents/qa-lcd-dump` → framebuffer real **240×400**.

### Causa raíz (viewport)

Log before/fix:

| Momento | `geo` (GeometryReader) | `fitted` |
|---------|------------------------|----------|
| Bug | **382×888** (casi pantalla entera; keypad aún no reservado) | **382×636** (más alto que el hueco visible sobre el keypad) |
| Fix | **382×458** (hueco real sobre keypad) | **274×458** (aspect 0.600, cabe entero) |

El `VStack` solo montaba el keypad cuando `activeSession != nil`. En el primer layout el LCD medía **toda** la altura; `fittedLCDSize` devolvía un rect más alto que el área que luego queda visible → el padre **recortaba** arriba/abajo (equivalente visual a un aspect-fill).

---

## Cambios

| Pieza | Cambio |
|-------|--------|
| `EmulatorView` | Keypad **siempre reservado** (opacity baja hasta sesión); LCD con fit estricto; `clipped()`; log `corej2-viewport` |
| `EmulatorLCDImageView` | `UIImageView` + `.scaleAspectFit` (nunca fill) |
| `fittedLCDSize` | API estática + tests (`EmulatorViewLayoutTests`) |
| `EmulatorViewModel` | Dump PNG opcional si existe `Documents/qa-lcd-dump` |

---

## Verificación

| Check | Resultado |
|-------|-----------|
| `xcodebuild` Debug → XS Max | **BUILD SUCCEEDED** |
| Install + autolaunch RF2011 | `startSession OK`, landscape=false |
| Viewport log | `geo=382x458 fitted=274x458 aspect=0.6000` |
| Screenshot after | `media/rf2011-viewport-r5/after-screen.png` — LCD letterboxeado, **frame completo** visible |
| LCD dump vs vista | `compare-dump-vs-view.png` — misma imagen 240×400 (splash/menú) |

### Nota sobre el contenido del framebuffer

Los dumps `lcd-frame-1-splash.png` / `lcd-frame-12-sound.png` muestran que **dentro** del 240×400 el juego ya pinta UI que toca el borde derecho (p.ej. texto del diálogo de sonido). Eso **no** es recorte adicional del viewport tras el fix: la vista muestra el LCD entero; el clipping residual está en el paint del MIDlet/overlay y queda fuera del alcance de este viewport pass.

---

## Evidencia

- `docs/media/rf2011-viewport-r5/before-screen.png`
- `docs/media/rf2011-viewport-r5/after-screen.png`
- `docs/media/rf2011-viewport-r5/compare-dump-vs-view.png`
- `docs/media/rf2011-viewport-r5/lcd-frame-1-splash.png`
- `docs/media/rf2011-viewport-r5/lcd-frame-12-sound.png`
