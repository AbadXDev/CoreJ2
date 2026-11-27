# FASE 3 ronda 2 — Asphalt frames reales + cruceta deslizable

**Fecha:** 2026-11-27 ~09:00 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Bundle:** `dev.abadx.corej2`  
**Classpath:** puro fase3 (282 classes)  
**Vendor/FreeJ2ME:** intacto  

---

## 0) Diamond Rush

Confirmado por Leonel: **jugable**. Sin cambios adicionales en esta ronda.

---

## 1) Asphalt 3D — inspección visual de frames (no solo métricas)

### Cómo se capturó

Flag `Documents/qa-m3g-dump` → el overlay escribe BMP en `Documents/m3g-dumps/`:

- `frame-N-240x320.bmp` — framebuffer M3D tras entrar a carrera (`qa-key` −5/53)
- `tex-N-WxH-fmt99.bmp` — primeras texturas RGB muestreadas

Convertidos a PNG en `docs/asphalt-m3g-frames/`.

### Hallazgo visual

| Artefacto | Qué se ve |
|-----------|-----------|
| `tex-road-atlas.png` | Atlas de carretera **coherente** (carriles, líneas, bordes amarillos) — decode RGB OK |
| `tex-palm.png` | Atlas palmera (verde + tronco) OK |
| `race-desert-palm.png` | Cielo azul + palmera con textura usable; **banda central con streaks horizontales**; suelo amarillo sólido |
| `race-track-perspective.png` | Geometría de pista con punto de fuga; **streaks radiales** en asfalto/muros; bandera a cuadros arriba |
| `race-tunnel.png` | Túnel/muros con ruido/shear en UV; checker arriba |
| `race-cityscape-readable-signs.png` | Skyline nocturno con letreros **legibles** (“Monte Carlo”, “LANCE BURTON”) → el decode de Image2D funciona en meshes “normales” |

**Conclusión visual:** no es “todo negro”. El decode de formatos RGB de Asphalt está bien (atlas y carteles). Lo roto es el **mapeo UV en superficies grandes con tiling extremo** (`v` hasta ~75–108 con `scale=1/128`), que produce streaks / smear. El auto se ve en uso real (Leonel); en dumps automatizados a veces queda fuera de frame.

### Cambios M3G aplicados

| Pieza | Cambio |
|-------|--------|
| `VertexArray.getFloat` | Bytes de texcoord como **unsigned** (Asphalt) |
| `Image2D.sampleARGB` | Siempre normalizado JSR-184 `(0,0)=arriba-izq` |
| `SoftwareM3GRenderer` | Aplica **Texture2D composite transform** + proyección `q`; blend FUNC_* con blendColor; compositing ALPHA/…; UV perspectiva con fallback **afín si span UV > 2.5** (tris de carretera muy tiled) |
| `ArgbBmpDump` / `Graphics3D` | Dumps BMP de frame + textura bajo `qa-m3g-dump` |
| Log | `uvRange u=[0,1] v=[0,107…] scale=0.0078125 blend=REPLACE wrap=REPEAT` |

### Estado vs meta

- **Mejor:** evidencia visual real; texturas fuente correctas; algunos meshes (palmera, carteles) se reconocen.  
- **Pendiente:** la carrera **aún no es jugable** a nivel texturas de pista/muros (streaks). Siguiente candidatos: tessellation UV / corrección perspectiva más robusta en tris de alto tiling, Sprite3D/HUD del checker superior.

---

## 2) Cruceta deslizable

### Comportamiento

`EmulatorKeypadView` sustituye los 4 botones discretos por un **SlidingDPad** continuo:

- Arrastrar sin levantar → `keyReleased` de ejes que salen + `keyPressed` de ejes nuevos  
- Zona entre dos direcciones → **diagonal** (p.ej. left+up)  
- Mantener → `keyRepeated` (~100 ms tras 350 ms)  
- Centro (deadzone) → sin teclas  

### Verificación XS Max

`Documents/qa-dpad-slide` + gestos reales sobre la cruceta:

```
qa-dpad-slide down left     → keyPressed -3
qa-dpad-slide diagonal      → keyRepeated -3 ; keyPressed -1
qa-dpad-slide up-only       → keyReleased -3 ; keyRepeated -1
qa-dpad-slide release up    → keyReleased -1
```

Tras el script, toques físicos en dispositivo mostraron diagonales `(-2,-4)` down+right y slides left/up — el gesto continuo de la UI responde.

---

## 3) Build / install

| Paso | Resultado |
|------|-----------|
| `compile-fase3-pure.sh` | 282 classes |
| `xcodebuild` Debug → XS Max | **BUILD SUCCEEDED** |
| Install | CoreJ2.app en el XS Max |

---

## 4) Archivos tocados (sin Vendor)

- `overlay/m3g/SoftwareM3GRenderer.java`, `ArgbBmpDump.java`
- `m3g/Image2D.java`, `Graphics3D.java`, `VertexArray.java`
- `EmulatorKeypadView.swift` (SlidingDPad)
- `LibraryViewModel.swift` (`qa-dpad-slide`)
- `MobilePlatform.java` (log `keyRepeated`)

---

## 5) Evidencia

Frames/texturas: `docs/asphalt-m3g-frames/`  
Logs locales: `/tmp/fase3-fix/asphalt11.log`, `dpad-slide2.log`
