# Inventario MIDP propia (clean-room) — FASE 0

**Fecha:** 2026-11-27 (America/Santo_Domingo)  
**Fuente:** escaneo de constant pool / method refs en `games/*.jar`  
**Regla:** superficie observable de los juegos; no copia de Vendor/FreeJ2ME.

---

## 1. JARs escaneados

- `Asphalt-3-Street-Rules-3D_ES_240x320.jar` — 32 tipos API, 131 métodos
- `Assassins-Creed_ES_640x360.jar` — 13 tipos API, 58 métodos
- `Bounce-Tales_ES_Multi.jar` — 16 tipos API, 54 métodos
- `Diamond-Rush_ES_240x400.jar` — 27 tipos API, 84 métodos
- `Gangstar-2-Kings-of-LA_ES_240x320.jar` — 15 tipos API, 65 métodos
- `Miami-Nights-Singles-in-the-City_J2ME_EN_v108.jar` — 18 tipos API, 61 métodos
- `Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar` — 18 tipos API, 60 métodos
- `Prince-of-Persia-Las-Arenas-Olvidadas_ES_640x360.jar` — 12 tipos API, 61 métodos
- `PruebaVibracion.jar` — 5 tipos API, 11 métodos
- `Real-Football-2011_ES_240x400.jar` — 18 tipos API, 69 métodos

---

## 2. Superficie API agregada (todos los JAR)

### Por paquete

| Paquete | # tipos | Tipos |
|---------|---------|-------|
| `com/nokia/mid` | 3 | `DeviceControl`, `DirectGraphics`, `DirectUtils` |
| `javax/microedition/io` | 7 | `Connection`, `ConnectionNotFoundException`, `Connector`, `ContentConnection`, `HttpConnection`, `InputConnection`, `OutputConnection` |
| `javax/microedition/lcdui` | 12 | `Alert`, `AlertType`, `Canvas`, `Command`, `CommandListener`, `Display`, `Displayable`, `Font`, `Form`, `Graphics`, `Image`, `TextField` |
| `javax/microedition/lcdui/game` | 3 | `GameCanvas`, `Layer`, `Sprite` |
| `javax/microedition/m3g` | 15 | `Appearance`, `Background`, `Camera`, `CompositingMode`, `Graphics3D`, `Image2D`, `IndexBuffer`, `PolygonMode`, `Sprite3D`, `Texture2D`, `Transform`, `Transformable`, `TriangleStripArray`, `VertexArray`, `VertexBuffer` |
| `javax/microedition/media` | 3 | `Controllable`, `Manager`, `Player` |
| `javax/microedition/media/control` | 1 | `VolumeControl` |
| `javax/microedition/midlet` | 2 | `MIDlet`, `MIDletStateChangeException` |
| `javax/microedition/rms` | 4 | `InvalidRecordIDException`, `RecordEnumeration`, `RecordStore`, `RecordStoreException` |

### Tipos ordenados por frecuencia (# JAR que los referencian)

| #JAR | Tipo |
|------|------|
| 10 | `javax/microedition/lcdui/Display` |
| 10 | `javax/microedition/lcdui/Font` |
| 10 | `javax/microedition/lcdui/Graphics` |
| 10 | `javax/microedition/midlet/MIDlet` |
| 9 | `javax/microedition/lcdui/Canvas` |
| 9 | `javax/microedition/lcdui/Image` |
| 9 | `javax/microedition/media/Manager` |
| 9 | `javax/microedition/media/Player` |
| 9 | `javax/microedition/rms/RecordStore` |
| 8 | `javax/microedition/lcdui/CommandListener` |
| 8 | `javax/microedition/lcdui/Displayable` |
| 7 | `javax/microedition/lcdui/Command` |
| 6 | `javax/microedition/rms/RecordStoreException` |
| 5 | `javax/microedition/io/ConnectionNotFoundException` |
| 5 | `javax/microedition/media/control/VolumeControl` |
| 4 | `javax/microedition/media/Controllable` |
| 4 | `javax/microedition/rms/RecordEnumeration` |
| 3 | `javax/microedition/rms/InvalidRecordIDException` |
| 2 | `com/nokia/mid/ui/DirectGraphics` |
| 2 | `com/nokia/mid/ui/DirectUtils` |
| 2 | `javax/microedition/io/Connection` |
| 2 | `javax/microedition/io/Connector` |
| 2 | `javax/microedition/io/HttpConnection` |
| 2 | `javax/microedition/io/InputConnection` |
| 2 | `javax/microedition/io/OutputConnection` |
| 1 | `com/nokia/mid/ui/DeviceControl` |
| 1 | `javax/microedition/io/ContentConnection` |
| 1 | `javax/microedition/lcdui/Alert` |
| 1 | `javax/microedition/lcdui/AlertType` |
| 1 | `javax/microedition/lcdui/Form` |
| 1 | `javax/microedition/lcdui/TextField` |
| 1 | `javax/microedition/lcdui/game/GameCanvas` |
| 1 | `javax/microedition/lcdui/game/Layer` |
| 1 | `javax/microedition/lcdui/game/Sprite` |
| 1 | `javax/microedition/m3g/Appearance` |
| 1 | `javax/microedition/m3g/Background` |
| 1 | `javax/microedition/m3g/Camera` |
| 1 | `javax/microedition/m3g/CompositingMode` |
| 1 | `javax/microedition/m3g/Graphics3D` |
| 1 | `javax/microedition/m3g/Image2D` |
| 1 | `javax/microedition/m3g/IndexBuffer` |
| 1 | `javax/microedition/m3g/PolygonMode` |
| 1 | `javax/microedition/m3g/Sprite3D` |
| 1 | `javax/microedition/m3g/Texture2D` |
| 1 | `javax/microedition/m3g/Transform` |
| 1 | `javax/microedition/m3g/Transformable` |
| 1 | `javax/microedition/m3g/TriangleStripArray` |
| 1 | `javax/microedition/m3g/VertexArray` |
| 1 | `javax/microedition/m3g/VertexBuffer` |
| 1 | `javax/microedition/midlet/MIDletStateChangeException` |

### Métodos LCDUI referenciados (unión)

```
com/nokia/mid/ui/DirectGraphics.drawImage:(Ljavax/microedition/lcdui/Image;IIII)V
com/nokia/mid/ui/DirectUtils.getDirectGraphics:(Ljavax/microedition/lcdui/Graphics;)Lcom/nokia/mid/ui/DirectGraphics;
javax/microedition/lcdui/Alert.<init>:(Ljava/lang/String;)V
javax/microedition/lcdui/Alert.<init>:(Ljava/lang/String;Ljava/lang/String;Ljavax/microedition/lcdui/Image;Ljavax/microedition/lcdui/AlertType;)V
javax/microedition/lcdui/Alert.addCommand:(Ljavax/microedition/lcdui/Command;)V
javax/microedition/lcdui/Alert.setString:(Ljava/lang/String;)V
javax/microedition/lcdui/Alert.setTimeout:(I)V
javax/microedition/lcdui/Canvas.<init>:()V
javax/microedition/lcdui/Canvas.hideNotify:()V
javax/microedition/lcdui/Canvas.repaint:()V
javax/microedition/lcdui/Canvas.serviceRepaints:()V
javax/microedition/lcdui/Canvas.setFullScreenMode:(Z)V
javax/microedition/lcdui/Canvas.showNotify:()V
javax/microedition/lcdui/Canvas.sizeChanged:(II)V
javax/microedition/lcdui/Command.<init>:(Ljava/lang/String;II)V
javax/microedition/lcdui/Display.callSerially:(Ljava/lang/Runnable;)V
javax/microedition/lcdui/Display.getCurrent:()Ljavax/microedition/lcdui/Displayable;
javax/microedition/lcdui/Display.getDisplay:(Ljavax/microedition/midlet/MIDlet;)Ljavax/microedition/lcdui/Display;
javax/microedition/lcdui/Display.setCurrent:(Ljavax/microedition/lcdui/Displayable;)V
javax/microedition/lcdui/Display.vibrate:(I)Z
javax/microedition/lcdui/Displayable.addCommand:(Ljavax/microedition/lcdui/Command;)V
javax/microedition/lcdui/Displayable.getHeight:()I
javax/microedition/lcdui/Displayable.getWidth:()I
javax/microedition/lcdui/Displayable.removeCommand:(Ljavax/microedition/lcdui/Command;)V
javax/microedition/lcdui/Displayable.setCommandListener:(Ljavax/microedition/lcdui/CommandListener;)V
javax/microedition/lcdui/Font.getDefaultFont:()Ljavax/microedition/lcdui/Font;
javax/microedition/lcdui/Font.getFont:(III)Ljavax/microedition/lcdui/Font;
javax/microedition/lcdui/Font.getHeight:()I
javax/microedition/lcdui/Font.stringWidth:(Ljava/lang/String;)I
javax/microedition/lcdui/Font.substringWidth:(Ljava/lang/String;II)I
javax/microedition/lcdui/Form.<init>:(Ljava/lang/String;)V
javax/microedition/lcdui/Form.append:(Ljava/lang/String;)I
javax/microedition/lcdui/Form.append:(Ljavax/microedition/lcdui/Item;)I
javax/microedition/lcdui/Form.deleteAll:()V
javax/microedition/lcdui/Graphics.clipRect:(IIII)V
javax/microedition/lcdui/Graphics.drawArc:(IIIIII)V
javax/microedition/lcdui/Graphics.drawImage:(Ljavax/microedition/lcdui/Image;III)V
javax/microedition/lcdui/Graphics.drawLine:(IIII)V
javax/microedition/lcdui/Graphics.drawRGB:([IIIIIIIZ)V
javax/microedition/lcdui/Graphics.drawRect:(IIII)V
javax/microedition/lcdui/Graphics.drawRegion:(Ljavax/microedition/lcdui/Image;IIIIIIII)V
javax/microedition/lcdui/Graphics.drawRoundRect:(IIIIII)V
javax/microedition/lcdui/Graphics.drawString:(Ljava/lang/String;III)V
javax/microedition/lcdui/Graphics.drawSubstring:(Ljava/lang/String;IIIII)V
javax/microedition/lcdui/Graphics.fillArc:(IIIIII)V
javax/microedition/lcdui/Graphics.fillRect:(IIII)V
javax/microedition/lcdui/Graphics.fillRoundRect:(IIIIII)V
javax/microedition/lcdui/Graphics.fillTriangle:(IIIIII)V
javax/microedition/lcdui/Graphics.getClipHeight:()I
javax/microedition/lcdui/Graphics.getClipWidth:()I
javax/microedition/lcdui/Graphics.getClipX:()I
javax/microedition/lcdui/Graphics.getClipY:()I
javax/microedition/lcdui/Graphics.getColor:()I
javax/microedition/lcdui/Graphics.getDisplayColor:(I)I
javax/microedition/lcdui/Graphics.getStrokeStyle:()I
javax/microedition/lcdui/Graphics.getTranslateX:()I
javax/microedition/lcdui/Graphics.getTranslateY:()I
javax/microedition/lcdui/Graphics.setClip:(IIII)V
javax/microedition/lcdui/Graphics.setColor:(I)V
javax/microedition/lcdui/Graphics.setColor:(III)V
javax/microedition/lcdui/Graphics.setFont:(Ljavax/microedition/lcdui/Font;)V
javax/microedition/lcdui/Graphics.setStrokeStyle:(I)V
javax/microedition/lcdui/Graphics.translate:(II)V
javax/microedition/lcdui/Image.createImage:(II)Ljavax/microedition/lcdui/Image;
javax/microedition/lcdui/Image.createImage:(Ljava/lang/String;)Ljavax/microedition/lcdui/Image;
javax/microedition/lcdui/Image.createImage:(Ljavax/microedition/lcdui/Image;)Ljavax/microedition/lcdui/Image;
javax/microedition/lcdui/Image.createImage:(Ljavax/microedition/lcdui/Image;IIIII)Ljavax/microedition/lcdui/Image;
javax/microedition/lcdui/Image.createImage:([BII)Ljavax/microedition/lcdui/Image;
javax/microedition/lcdui/Image.createRGBImage:([IIIZ)Ljavax/microedition/lcdui/Image;
javax/microedition/lcdui/Image.getGraphics:()Ljavax/microedition/lcdui/Graphics;
javax/microedition/lcdui/Image.getHeight:()I
javax/microedition/lcdui/Image.getRGB:([IIIIIII)V
javax/microedition/lcdui/Image.getWidth:()I
javax/microedition/lcdui/TextField.<init>:(Ljava/lang/String;Ljava/lang/String;II)V
javax/microedition/lcdui/TextField.getString:()Ljava/lang/String;
javax/microedition/lcdui/game/GameCanvas.<init>:(Z)V
javax/microedition/lcdui/game/GameCanvas.paint:(Ljavax/microedition/lcdui/Graphics;)V
javax/microedition/lcdui/game/GameCanvas.setFullScreenMode:(Z)V
javax/microedition/lcdui/game/Layer.getHeight:()I
javax/microedition/lcdui/game/Layer.getWidth:()I
javax/microedition/lcdui/game/Layer.setPosition:(II)V
javax/microedition/lcdui/game/Sprite.<init>:(Ljavax/microedition/lcdui/Image;II)V
javax/microedition/lcdui/game/Sprite.paint:(Ljavax/microedition/lcdui/Graphics;)V
javax/microedition/lcdui/game/Sprite.setFrame:(I)V
javax/microedition/lcdui/game/Sprite.setRefPixelPosition:(II)V
```

---

## 3. Por juego

### Asphalt 3 3D

`Asphalt-3-Street-Rules-3D_ES_240x320.jar`

**Tipos:**
- `com/nokia/mid/ui/DirectGraphics`
- `com/nokia/mid/ui/DirectUtils`
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/m3g/Appearance`
- `javax/microedition/m3g/Background`
- `javax/microedition/m3g/Camera`
- `javax/microedition/m3g/CompositingMode`
- `javax/microedition/m3g/Graphics3D`
- `javax/microedition/m3g/Image2D`
- `javax/microedition/m3g/IndexBuffer`
- `javax/microedition/m3g/PolygonMode`
- `javax/microedition/m3g/Sprite3D`
- `javax/microedition/m3g/Texture2D`
- `javax/microedition/m3g/Transform`
- `javax/microedition/m3g/Transformable`
- `javax/microedition/m3g/TriangleStripArray`
- `javax/microedition/m3g/VertexArray`
- `javax/microedition/m3g/VertexBuffer`
- `javax/microedition/media/Controllable`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/media/control/VolumeControl`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/InvalidRecordIDException`
- `javax/microedition/rms/RecordEnumeration`
- `javax/microedition/rms/RecordStore`
- `javax/microedition/rms/RecordStoreException`

### Assassin's Creed

`Assassins-Creed_ES_640x360.jar`

**Tipos:**
- `javax/microedition/io/ConnectionNotFoundException`
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/RecordStore`

### Bounce Tales

`Bounce-Tales_ES_Multi.jar`

**Tipos:**
- `com/nokia/mid/ui/DeviceControl`
- `com/nokia/mid/ui/DirectGraphics`
- `com/nokia/mid/ui/DirectUtils`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/lcdui/game/GameCanvas`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/media/control/VolumeControl`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/midlet/MIDletStateChangeException`
- `javax/microedition/rms/RecordStore`
- `javax/microedition/rms/RecordStoreException`

### Diamond Rush

`Diamond-Rush_ES_240x400.jar`

**Tipos:**
- `javax/microedition/io/Connection`
- `javax/microedition/io/ConnectionNotFoundException`
- `javax/microedition/io/Connector`
- `javax/microedition/io/ContentConnection`
- `javax/microedition/io/HttpConnection`
- `javax/microedition/io/InputConnection`
- `javax/microedition/io/OutputConnection`
- `javax/microedition/lcdui/Alert`
- `javax/microedition/lcdui/AlertType`
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Form`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/lcdui/TextField`
- `javax/microedition/lcdui/game/Layer`
- `javax/microedition/lcdui/game/Sprite`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/RecordEnumeration`
- `javax/microedition/rms/RecordStore`
- `javax/microedition/rms/RecordStoreException`

### Gangstar 2

`Gangstar-2-Kings-of-LA_ES_240x320.jar`

**Tipos:**
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/media/Controllable`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/media/control/VolumeControl`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/RecordStore`
- `javax/microedition/rms/RecordStoreException`

### Miami EN v1.0.8

`Miami-Nights-Singles-in-the-City_J2ME_EN_v108.jar`

**Tipos:**
- `javax/microedition/io/ConnectionNotFoundException`
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/media/Controllable`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/media/control/VolumeControl`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/InvalidRecordIDException`
- `javax/microedition/rms/RecordEnumeration`
- `javax/microedition/rms/RecordStore`
- `javax/microedition/rms/RecordStoreException`

### Miami ES-ML v1.0.3

`Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar`

**Tipos:**
- `javax/microedition/io/ConnectionNotFoundException`
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/media/Controllable`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/media/control/VolumeControl`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/InvalidRecordIDException`
- `javax/microedition/rms/RecordEnumeration`
- `javax/microedition/rms/RecordStore`
- `javax/microedition/rms/RecordStoreException`

### Prince of Persia

`Prince-of-Persia-Las-Arenas-Olvidadas_ES_640x360.jar`

**Tipos:**
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/RecordStore`

### PruebaVibracion

`PruebaVibracion.jar`

**Tipos:**
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/midlet/MIDlet`

### Real Football 2011

`Real-Football-2011_ES_240x400.jar`

**Tipos:**
- `javax/microedition/io/Connection`
- `javax/microedition/io/ConnectionNotFoundException`
- `javax/microedition/io/Connector`
- `javax/microedition/io/HttpConnection`
- `javax/microedition/io/InputConnection`
- `javax/microedition/io/OutputConnection`
- `javax/microedition/lcdui/Canvas`
- `javax/microedition/lcdui/Command`
- `javax/microedition/lcdui/CommandListener`
- `javax/microedition/lcdui/Display`
- `javax/microedition/lcdui/Displayable`
- `javax/microedition/lcdui/Font`
- `javax/microedition/lcdui/Graphics`
- `javax/microedition/lcdui/Image`
- `javax/microedition/media/Manager`
- `javax/microedition/media/Player`
- `javax/microedition/midlet/MIDlet`
- `javax/microedition/rms/RecordStore`

---

## 4. Cobertura actual de nuestro overlay (antes de FASE 1)

| Área | Estado | Notas |
|------|--------|-------|
| LCDUI Display / vibrate | **FASE 1 clean-room** | `VibrationBridge`; log `corej2-lcdui: Display ready` |
| Image / PNG / BMP | Overlay propio (`PngDecoder`, `BmpDecoder`, `PlatformImage`) | |
| Graphics / Font | Clean-room `Graphics`/`Font` + `PlatformGraphics`/`BitmapFont` | |
| Canvas / Displayable | **FASE 1 clean-room** | paint → `PlatformImage` → flush |
| Command / Form / Alert | **FASE 1 mínimos clean-room** | Diamond Form/Alert cubiertos a nivel básico |
| MMAPI Manager/Player | Overlay `PlatformPlayer` + `AudioBridge` | |
| RMS | Swift `RMSStoreService` + Vendor RecordStore | |
| Connector / HTTP | Overlay stubs | |
| M3G | Overlay software en curso | Asphalt |
| Nokia DirectGraphics | Vendor + PlatformGraphics | |
| OEM vibrate (Nokia/Samsung/Siemens) | Overlay bridges | |

Archivos overlay actuales (referencia):

```
com/nokia/mid/ui/DeviceControl.java
com/samsung/util/Vibration.java
com/siemens/mp/game/Vibrator.java
javax/microedition/io/Connector.java
javax/microedition/lcdui/Display.java
javax/microedition/lcdui/Image.java
javax/microedition/lcdui/game/LayerManager.java
javax/microedition/m3g/Camera.java
javax/microedition/m3g/Graphics3D.java
javax/microedition/m3g/Group.java
javax/microedition/m3g/Image2D.java
javax/microedition/m3g/IndexBuffer.java
javax/microedition/m3g/Mesh.java
javax/microedition/m3g/Node.java
javax/microedition/m3g/Texture2D.java
javax/microedition/m3g/Transform.java
javax/microedition/m3g/Transformable.java
javax/microedition/m3g/TriangleStripArray.java
javax/microedition/m3g/VertexArray.java
javax/microedition/m3g/VertexBuffer.java
javax/microedition/midlet/MIDlet.java
dev/abadx/corej2/bootstrap/AudioBridge.java
dev/abadx/corej2/bootstrap/VibrationBridge.java
dev/abadx/corej2/overlay/BitmapFont.java
dev/abadx/corej2/overlay/BmpDecoder.java
dev/abadx/corej2/overlay/IntArgbImage.java
dev/abadx/corej2/overlay/PngDecoder.java
dev/abadx/corej2/overlay/m3g/SoftwareM3GRenderer.java
org/recompile/mobile/MIDletLoader.java
org/recompile/mobile/MobilePlatform.java
org/recompile/mobile/PlatformFont.java
org/recompile/mobile/PlatformGraphics.java
org/recompile/mobile/PlatformImage.java
org/recompile/mobile/PlatformPlayer.java
```

---

## 5. Orden de implementación por capas

| Fase | Capa | Objetivo verificable |
|------|------|----------------------|
| **0** | Inventario | Este documento |
| **1** | LCDUI núcleo clean-room | `Display`, `Displayable`, `Canvas`, `Graphics`, `Image`, `Font`, `Command`, `CommandListener`, Alert/Form mínimos. PruebaVibracion + Canvas probe usan solo nuestras clases para esas APIs; Miami sin regresión |
| **2** | MIDlet + RMS + MMAPI + Game + Nokia + GCF | **Hecho** — ver `midp-propia-fase2-verify.md` |
| **3** | (absorbió FASE 2) | MIDlet+RMS ya en overlay; retirar restos Vendor en siguientes |
| **4** | (absorbió FASE 2) | Manager/Player/VolumeControl propios |
| **5** | GCF / OEM | Connector realista, Nokia/Samsung/Siemens |
| **6** | M3G | Completar software renderer; retirar stubs Vendor |
| **7** | Retirada FreeJ2ME | Classpath sin Vendor; App Store |

---

## 6. Criterio clean-room

- Implementar desde MIDP 2.0 (JSR-118), CLDC 1.1, MMAPI (JSR-135), M3G (JSR-184) y comportamiento observable de los JAR.

- No copiar ni plantillar desde `Vendor/FreeJ2ME`.

- Vendor permanece intacto en el árbol; el classpath de app usa nuestros `.class` que pisan los mismos FQCN.

- Puentes nativos propios: `AudioBridge`, `VibrationBridge`, LCD painter, RMS Swift.
