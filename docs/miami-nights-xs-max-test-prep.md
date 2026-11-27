# Preparación: Miami Nights en iPhone XS Max

**Fecha:** 2026-11-27 ~00:56 AST (America/Santo_Domingo)  
**Dispositivo:** iPhone XS Max A12+ (`00008020-001645C20221002E`)  
**JAR:** `games/Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar` (509 KB)

---

## 1) Flujo exacto de importación de JARs

**No** escanea `Documents` automáticamente.  
**No** declara `CFBundleDocumentTypes` / “Abrir con…” desde la app Archivos.  
**Sí** usa el **document picker del sistema** (`.fileImporter`).

### Cadena

1. **UI** — `LibraryView` toolbar **Import Game** → `isImportPresented = true`
2. **Picker** — `.fileImporter(allowedContentTypes: UTType jar, allowsMultipleSelection: false)`
3. **VM** — `LibraryViewModel.handleImportResult` → `importSelectedJAR(from:)`
4. **Engine** — `DefaultImportEngine` → `DefaultImportPipeline.run`
5. **Copia** — `FileImportService.importJAR`:
   - `startAccessingSecurityScopedResource()` sobre la URL del picker
   - Destino: `Documents/CoreJ2/Library/<UUID>/game.jar`
   - Devuelve `InstalledGame` provisional (título = nombre del archivo sin `.jar`)
6. **Steps** — Manifest → Hash → DuplicateDetection → Artwork
7. **Persistencia** — `repository.save` (SwiftData) + refresh de la lista
8. **Jugar** — tap en el juego → `selectGame` → `EmulatorView` → bridge/runtime carga `game.jarURL`

La biblioteca interna vive bajo Documents, pero **solo** tras un import vía picker. Meter un JAR suelto en Documents **no** lo registra en la librería.

---

## 2) Copia al iPhone (hecha)

```bash
xcrun devicectl device copy to \
  --device 00008020-001645C20221002E \
  --domain-type appDataContainer \
  --domain-identifier dev.abadx.corej2 \
  --destination "Documents/Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar" \
  --source "games/Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar"
```

**Importante:** el destino debe incluir el **nombre de archivo**.  
`--destination Documents/` reemplaza `Documents` por un **archivo** (bug de uso de la CLI).

También se activó **`UIFileSharingEnabled`** (para que Files / el picker vean “On My iPhone → CoreJ2”):

- `/Users/sakamoto/Desktop/Muse archivos/corej2/CoreJ2/Info.plist`
- `INFOPLIST_FILE = CoreJ2/Info.plist` en Debug/Release del target

App reinstalada; JAR verificado en Documents (509 KB).

---

## 3) Consola al launch

```text
Launched application with dev.abadx.corej2 bundle identifier.
Waiting for the application to terminate…
App terminated due to signal 15.   # SIGTERM al cortar el bridge --console a los ~12s
```

- **Sin errores de JVM/JNI/OpenJDK** en stdout en el arranque en frío.
- Esperado: la JVM embebida solo se crea al **lanzar un juego**, no al abrir la Library.
- App dejada corriendo de nuevo (PID ~1184). Sin crash reports de CoreJ2/CoreJ2.
- Log: `/Users/sakamoto/Desktop/Muse archivos/corej2/build/device/launch-console.log`

---

## 4) Pasos exactos en el iPhone (usuario)

1. Abre **CoreJ2** (ya debería estar en primer plano).
2. En **Library**, toca **Import Game** (arriba a la derecha).
3. En el picker de Archivos:
   - **Browse** → **On My iPhone** → **CoreJ2**
   - Selecciona `Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar`
4. Espera el alert **Import Successful** → **OK**.
5. En la lista, **toca el juego** para abrirlo en el emulador.
6. (Opcional) Si aparece el picker de perfil de dispositivo, elige el perfil 240×320 / el detectado y confirma.

**Limitación v1:** una sesión de emulador por proceso de la app; si paras y quieres otro juego, puede hacer falta reiniciar CoreJ2.

---

## Archivos tocados (local, sin push)

- `CoreJ2/Info.plist` (nuevo; `UIFileSharingEnabled`)
- `CoreJ2.xcodeproj/project.pbxproj` (`INFOPLIST_FILE` + key File Sharing)
- Vendor/FreeJ2ME: intacto · GitHub: sin push
