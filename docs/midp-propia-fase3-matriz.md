# MIDP propia — FASE 3 — classpath puro + matriz XS Max

**Fecha:** 2026-11-27 ~07:30 America/Santo_Domingo  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Bundle:** `dev.abadx.corej2`  
**Regla:** clean-room (JSR-118 / JSR-135 / JSR-184 / GCF / Nokia observable). Vendor/FreeJ2ME **intacto**. **Sin push a GitHub**.

---

## 1. Variante pura (manda a partir de ahora)

| | |
|--|--|
| Script | `scripts/embedded-jvm/compile-fase3-pure.sh` |
| Salida | `artifacts/corej2-embedded-classpath-fase3/classes` (**280** `.class`) |
| Copy Runtime | embebe fase3 por defecto (`COREJ2_CLASSPATH_VARIANT=reference` → Vendor+overlay) |
| `org.recompile.freej2me` en producto | **0** |
| Vendor `javax/**` / OEM no usados | **ausentes** del producto |

Fuentes del classpath puro:

1. **ObjectWeb ASM** (BSD) — solo `Vendor/FreeJ2ME/src/org/objectweb/asm` (biblioteca de terceros; no es FreeJ2ME)
2. **Overlay CoreJ2** — MIDP / MMAPI / M3G / Nokia + bridges
3. **Bootstrap CoreJ2** — `LcdSupport`, `RunJarSupport`, …

Referencia (sigue existiendo): `compile-freej2me-classpath.sh` → `artifacts/freej2me-embedded-classpath/`.

---

## 2. Matriz XS Max (classpath fase3)

Criterios: arranca (`START_APP_OK`), pinta (`lcdNonZero` / `paintIndex`), entrada (`corej2-input`), audio (`corej2-media`/`corej2-audio`), RMS (`corej2-rms`).

| Juego | Arranca | Pinta | Entrada | Audio | RMS | Notas |
|-------|:-------:|:-----:|:-------:|:-----:|:---:|-------|
| PruebaVibracion | OK | OK | OK* | — | — | `corej2-vibrate` tras qa-tap; *input vía vibrate |
| Miami Nights EN v1.0.8 | OK | OK | OK | OK | —† | Menú/splash continuo |
| Miami Nights ES-ML v1.0.3 | OK | OK | OK | OK | OK | |
| Gangstar 2 | OK | OK | OK | OK | OK | `paintIndex`≥272 |
| Diamond Rush | OK | OK | OK | —† | OK | Form/Alert/TextField propios; audio no en ventana QA |
| Bounce Tales | OK | OK | OK | OK | —† | GameCanvas + MIDI |
| Real Football 2011 | OK | OK | OK | OK | OK | `setRecord(1)` en store vacío → crece IDs (fix fase3) |
| Prince of Persia | OK | OK | OK | parcial | OK | AVAudio prepare −10870 en un clip; sesión OK |
| Assassin's Creed | OK | OK | OK | OK | OK | NPE `loader==null` post-stop → null-safe `PlatformImage`/`Connector` |
| Asphalt 3 Street Rules 3D | OK | OK | OK | OK | OK | **M3G activo** (ver §3) |

† No disparado en la ventana QA automatizada (el título difiere createPlayer/openRecordStore).

**Ningún** `NoClassDefFoundError` / `ClassNotFoundException` de API MIDP en la matriz pura.

---

## 3. M3G / Asphalt

Tras `qa-key` FIRE (`-5`) el título entra en el path 3D:

- `corej2-m3g: setCamera` / `bindTarget PlatformGraphics 240x320`
- `render MeshVB` continuo (cientos–miles de llamadas)
- Diagnóstico: `MeshVB#1 dTris=14 nz=76800`, luego `nz≈26k–30k` con `totalTris` creciendo (p.ej. 19 584)

**Conclusión:** la escena 3D **sí pinta** en el framebuffer software (nz ≫ splash de 752 px).

Piezas aún incompletas / no bloqueantes para este título:

| Pieza | Estado |
|-------|--------|
| Tipos Asphalt-referenciados (`Appearance`, `Background`, `CompositingMode`, `PolygonMode`, `Sprite3D`, …) | Implementados clean-room |
| `Loader.load(.m3g)` | Stub (Asphalt **no** lo usa) |
| Luces / fog / material shading | Stubs / no lit |
| `Sprite3D` raster | API presente; path MeshVB es el dominante en Asphalt |
| Calidad/perf del rasterizer | Suficiente para “pinta”; no es M3G completo |

---

## 4. Auditoría clean-room

| Artefacto | Hallazgo | Acción |
|-----------|----------|--------|
| `MIDletLoader.java` | Tenía cabecera GPL FreeJ2ME | **Reescrito** clean-room (mismo FQCN; ASM rewrite de `Class.getResourceAsStream`) |
| `Mobile.java` | Solo existía en Vendor | **Nuevo** clean-room en Overlay |
| `AlertType`, `TextField`, `Item*`, M3G faltantes | Dependían de Vendor | **Nuevos** clean-room |
| `PlatformImage` / `Connector` | NPE si `loader==null` tras `stopMidlet` | Null-safe |
| `RecordStore.setRecord` | Football hacía setRecord(1) en store vacío | Crece espacio de IDs (comportamiento observable) |
| `Platform*` / `MobilePlatform` | FQCNs estables para JNI; implementación CoreJ2 | Se mantienen; no se recompila Vendor |

---

## 5. Provenance — clases en el producto (fase3)

| Origen | # `.class` | Paquetes |
|--------|----------:|----------|
| ObjectWeb ASM (BSD) | 159 | `org.objectweb.asm.**` |
| CoreJ2 M3G overlay | 28 | `javax.microedition.m3g.**` |
| CoreJ2 LCDUI (+ game) | 27 | `javax.microedition.lcdui.**` |
| CoreJ2 platform bridges | 14 | `org.recompile.mobile.**` |
| CoreJ2 GCF | 13 | `javax.microedition.io.**` |
| CoreJ2 RMS | 11 | `javax.microedition.rms.**` |
| CoreJ2 Bootstrap | 9 | `dev.abadx.corej2.bootstrap.**` + helpers |
| CoreJ2 MMAPI | 7 | `javax.microedition.media.**` |
| CoreJ2 overlay helpers | 5 | `dev.abadx.corej2.overlay.**` |
| Nokia UI | 3 | `com.nokia.mid.ui.**` |
| MIDlet | 2 | `javax.microedition.midlet.**` |
| OEM vibrate stubs | 2 | `com.samsung` / `com.siemens` |
| **Total** | **280** | |
| FreeJ2ME / `org.recompile.freej2me` | **0** | |
| OpenJDK | (módulo `java.base` en `java.home`, no en este classpath) | |

Inventario completo: `artifacts/corej2-embedded-classpath-fase3/CLASSES.txt`.

---

## 6. Artefactos

| Path | Uso |
|------|-----|
| `artifacts/corej2-embedded-classpath-fase3/` | **Producto** (puro) |
| `artifacts/freej2me-embedded-classpath/` | Referencia Vendor+overlay |
| `scripts/embedded-jvm/compile-fase3-pure.sh` | Genera el puro |
| `scripts/openjdk-mobile/copy-ios-runtime-into-app.sh` | Preferencia fase3 |
| `docs/midp-propia-fase3-matriz.md` | Este informe |

Vendor/FreeJ2ME no modificado. Sin push.
