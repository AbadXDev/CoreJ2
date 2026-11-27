# FASE 3 ronda 3 — Controles, landscape 640×360, Asphalt UV

**Fecha:** 2026-11-27 ~09:30 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Bundle:** `dev.abadx.corej2`  
**Classpath:** puro fase3 (282 classes)  
**Vendor/FreeJ2ME:** intacto  

---

## 1) Ajustes → Controles: estilo de cruceta

Nueva preferencia persistida en `UserDefaults` (`corej2.settings.dpadStyle`):

| Opción | Comportamiento |
|--------|----------------|
| **Palanca** | Joystick virtual 8 direcciones por ángulo |
| **Cruceta deslizable** | Arrastre continuo (default) |
| **Cruceta clásica** | 4 botones discretos como antes |

UI: `SettingsView` → Controles → picker “Estilo de cruceta”.

---

## 2) Cruceta deslizable — diagonales + histéresis

`AngleDPadView` (estilo sliding/joystick):

- Sectores diagonales **más anchos** (~55°) que los cardinales (~35°)
- Histéresis angular (+22°) y de radio (enter 0.18 / exit 0.08)
- Deslizar de un cardinal al adyacente pasa por la diagonal sin zona muerta que suelte todas las teclas

QA en XS Max (`qa-dpad-slide` durante Prince):

```
qa-dpad-slide down left
qa-dpad-slide diagonal left+up
qa-dpad-slide up-only (release left)
qa-dpad-slide release up
qa-dpad-slide done
```

---

## 3) Landscape para juegos 640×360

- `DeviceProfile.prefersLandscapeLayout` cuando `lcdWidth > lcdHeight`
- `EmulatorView` usa layout horizontal (LCD centro, cruceta izq, FIRE/soft/num derecha)
- `AppOrientationLock` + `UIApplicationDelegate.supportedInterfaceOrientationsFor`

### Verificación XS Max

| Juego | Canvas | `landscape=` |
|-------|--------|--------------|
| Prince of Persia Las Arenas Olvidadas | 640×360 | **true** |
| Assassin's Creed | 640×360 | **true** |
| Asphalt 3 (control) | 240×320 | false |

---

## 4) Asphalt — UV pista/muros

### Cambios en `SoftwareM3GRenderer`

1. **Rechazo W≤ε** — tris que cruzan el near plane / W≈0 provocaban el “túnel radial” de streaks; se descartan si algún clip-W ≤ 1e-3  
2. **Perspectiva siempre** (cuando PolygonMode lo pide) — se eliminó el fallback afín en spans UV grandes (era la fuente de streaks horizontales/radiales)  
3. **Tessellation binaria** del borde UV más largo en tris grandes en pantalla (depth ≤ 7, umbral densidado)

### Resultado visual (dumps reales)

Comparado con ronda 2 (túnel de streaks sin escena legible):

| Frame | Qué se ve ahora |
|-------|-----------------|
| `race-coastal-r3.png` | Costa tropical: playa, mar, palmeras, edificios, montaña, cielo; geometría de pista correcta; **asfalto aún con banding/streaks** |
| `race-coastal-skyline-r3.png` | Skyline + palmera legibles |
| `race-vegas-signs-r3.png` | Noche Vegas: letreros **“VEGAS”** y **“Monte Carlo”** legibles; pista mejor pero no perfecta |

**Veredicto:** salto grande (escena jugable/reconocible; carteles OK). El tiling extremo de la pista (`v` ~75–108) sigue imperfecto — falta clip poligonal completo contra W=ε (hoy se descarta el tri) y/o más subdivisión selectiva.

Evidencia: `docs/asphalt-m3g-frames-r3/` y `media/asphalt-m3g-frames-r3/`.

---

## 5) Build / install

| Paso | Resultado |
|------|-----------|
| `compile-fase3-pure.sh` | 282 classes, 0 FreeJ2ME |
| `xcodebuild` Debug → XS Max | **BUILD SUCCEEDED** |
| Install | CoreJ2.app en el XS Max |

---

## 6) Archivos tocados (sin Vendor)

- `CoreJ2/Features/Settings/AppSettings.swift`, `SettingsView.swift`
- `CoreJ2/Features/Emulator/Views/EmulatorKeypadView.swift`, `EmulatorView.swift`
- `CoreJ2/App/CoreJ2App.swift` (`AppOrientationLock`)
- `CoreJ2/Features/Library/Models/DeviceProfile.swift`
- `overlay/m3g/SoftwareM3GRenderer.java`
