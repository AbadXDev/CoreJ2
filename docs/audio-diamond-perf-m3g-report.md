# Audio stop · Diamond Rush · rendimiento · M3G — XS Max

**Device:** iPhone XS Max `00008020-001645C20221002E` (America/Santo_Domingo)  
**Bundle:** `dev.abadx.corej2`  
**Vendor/FreeJ2ME:** intact  
**GitHub push:** none

---

## 1) Audio que sigue al salir

### Causa

`stopMidlet` destruía el MIDlet pero **no cerraba** los `PlatformPlayer` / slots de `AVMIDIPlayer`. Miami (y cualquier MIDI en loop) dejaba el hilo de `scheduleMidiLoop` reiniciando la pista aunque la UI volviera a Library.

### Fix

| Capa | Cambio |
|------|--------|
| `AudioBridge.stopAll()` | Para y `close` de todos los ids vivos |
| `MobilePlatform.stopMidlet` | Llama `stopAll` antes y después de `destroyCurrent` |
| `MMAAudioEngine.stopAll()` | Vacía slots Swift + `generation++` para matar loops MIDI |
| `DefaultPlatformBootstrap.stopMidletSync` | `audioEngine.stopAll()` por si Java se saltó alguno |

### Verificación

**Bounce Tales** (MIDI automático al arrancar):

```
corej2-audio: create id=1 … start MIDI id=1
qa-audio-stop dismiss
corej2-audio: stopAll count=1
corej2-audio: close id=1
corej2-audio: stopAll swift slots=0
```

Al volver a biblioteca el audio corta. Gangstar sin música activada en menú no crea players (count=0); el mismo camino de `stopAll` aplica cuando sí hay audio.

---

## 2) Diamond Rush — congelado en idioma

### Causa

La lista de idiomas calcula el índice **dentro de `paint()`** a partir de las coords del toque. `pointerPressed` hacía `repaint()`, pero el hit test podía no quedar aplicado antes de `pointerReleased` → `Index: -1` → tap sin efecto (parece freeze). Además `Connector.open` HTTP sin `HttpConnection` podía romper menús posteriores.

### Fix

- `MobilePlatform.pointerPressed/Released`: `Canvas.repaint()` síncrono antes de entregar el release.
- `Connector` stub `HttpConnection` (200 + EOF) para ads/scores.
- Locale `/locale/es-es.properties` ya cargaba bien una vez el índice era válido.

### Verificación (qa-tap `120,280`)

```
pointerPressed 120,280
pointerReleased 120,280
Index: 5
Loading setting: main menu.play='jugar'
```

Pasa el selector (español) al menú principal.

---

## 3) Rendimiento / calor

### Antes (comportamiento)

Cada `painter.run()` → JNI `extractPixels` (copia completa ARGB) + FNV checksum + evento → `CGImage`. Juegos con repaint agresivo saturaban CPU.

### Después

| Optimización | Detalle |
|--------------|---------|
| `LcdSupport.extractPixelsIfChanged` | Hash de contenido; `null` si no cambió |
| Frame pacing ~30 fps | Descarta extracts si &lt; 33 ms desde el último emit |
| Pack LE sin `ByteBuffer.allocate` por pixel loop | Menos presión GC en Java |

### Números (Bounce Tales, ~20 s en XS Max)

| Métrica | Valor |
|---------|-------|
| `paintIndex` (callbacks painter) | 878 |
| Frames emitidos a UI | 240 |
| Saltados (LCD sin cambio) | 397 |
| Saltados (pacing) | 241 |
| Ratio emitido / paint | **~27 %** |
| Ritmo UI aprox. | ~12 fps sostenido (tope de pacing 30) |

Antes, esos 878 paints habrían forzado ~878 copias full-frame. Ahora ~240. El teléfono debería calentarse menos en menús/idle con LCD estático.

Logs: `corej2-perf: frames emitted=… skippedUnchanged=… paced=…`

---

## 4) Motor 3D (M3G) — Asphalt 3

### Qué había

FreeJ2ME `javax.microedition.m3g.*` en Vendor es **stub** (constructores que no guardan datos, `Group.nodes` null, `Node.rendering=false`, `Transform` vacío, `Graphics3D.render` no-op). Asphalt construye geometría en código (no trae `.m3g` en el JAR).

### Qué se añadió (solo overlay)

Almacenamiento real + raster software:

- `Transform`, `Transformable`, `Node`, `Group`, `Mesh`
- `VertexArray`, `VertexBuffer`, `TriangleStripArray` / `IndexBuffer`
- `Camera`, `Texture2D`, `Image2D` (+ `Image.getRGB` funcional)
- `Graphics3D` → `SoftwareM3GRenderer` (triangle strips, depth opcional, texturas básicas)

### Estado en dispositivo

| Antes | Ahora |
|-------|-------|
| `lcdNonZeroPixels=0` | **`lcdNonZeroPixels=752`** |

Hay paint distinto de negro puro. En las ventanas de QA (~14–18 s + taps de menú) **aún no aparecen** logs `corej2-m3g: bindTarget/render` — Asphalt no entra al render 3D de carrera en ese tramo (se queda en flujo 2D/menú).

### Qué falta para “jugable”

1. Llevar Asphalt hasta la escena 3D (menús/softkeys/táctil específicos) y capturar la traza `bindTarget` → `render`.
2. Completar APIs que Asphalt usa en inmediato (`render(VB,IB,Appearance,Transform)` ya cableado; validar strips/UV/Material).
3. `Sprite3D`, fog, lights reales, sorting por layer, culling correcto.
4. Posible ruta `com.mascotcapsule.micro3d` si algún build la mezcla (este JAR ES usa `javax.microedition.m3g`).

Documentado: **subconjunto software operativo; Asphalt ya no está en negro total (752 px); carrera 3D completa pendiente de instrumentación in-game.**

---

## Rebuild / install

- `compile-freej2me-classpath.sh` → OK  
- `xcodebuild` Debug → **BUILD SUCCEEDED**  
- Instalado en XS Max; app relanzada limpia (sin flags QA)
