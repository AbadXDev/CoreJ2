# CoreJ2 — crash al lanzar Miami Nights (XS Max)

**Fecha:** 2026-11-27 ~02:02 AST (America/Santo_Domingo)  
**Dispositivo:** iPhone XS Max (`00008020-001645C20221002E`) · iOS 18.7.1  
**JAR:** Miami Nights Singles in the City (J2ME)  
**Resultado:** **bloqueado en `JNI_CreateJavaVM`** — la app llega a crear la VM y muere por **CODESIGNING Invalid Page** (SIGKILL). Miami Nights **aún no abre**.

---

## 1) Resumen ejecutivo

Se diagnosticó y corrigió la cadena de arranque hasta el borde de HotSpot Zero. El fallo que el usuario veía al tocar el juego ya no es “la app ni siquiera intenta la JVM”: ahora `corej2_embedded_jvm_create` corre, encuentra `lib/modules`, y **dentro de `Arguments::parse` el proceso salta a `PC=0` y iOS lo mata por codesign**.

Eso requiere decisión / trabajo de VM (parches OpenJDK Mobile o stubs W^X más profundos), no un cambio mínimo más de Swift/UI.

**Vendor/FreeJ2ME:** intacto. **Sin push** a GitHub (el árbol local no es un repo git).

---

## 2) Picker / copia `devicectl` (punto 1 del usuario)

| Hallazgo | Detalle |
|----------|---------|
| Causa típica | La copia a `Documents/` del contenedor **anterior** se pierde al reinstalar; o el JAR queda en Documents pero el picker no lo lista. |
| Fix en app | Auto-import de `Documents/*.jar` en `LibraryViewModel.loadGames()` + UTIs amplios en el `fileImporter`. |
| Fix herramienta | `scripts/device/copy-jar-to-app-documents.sh` (garantiza directorio Documents). |
| Paths rotos | Tras reinstalar, SwiftData guarda `jarPath` absolutos del contenedor viejo → `jarNotFound`. |
| Fix | `DuplicateDetectionStep` permite re-import si el JAR del hash ya no existe; la library filtra juegos sin archivo. |

Para futuras copias: instalar la app **primero**, luego `copy-jar-to-app-documents.sh`, abrir la app (auto-import). No hace falta AirDrop.

---

## 3) Crash reports (punto 2)

Extraídos vía `devicectl … systemCrashLogs` → `build/device/crashlogs3/`.

| Campo | Valor |
|-------|--------|
| Señal | **SIGKILL** |
| Namespace | **CODESIGNING** / **Invalid Page** |
| Excepción | `EXC_BAD_ACCESS` · `KERN_PROTECTION_FAILURE at 0x0` |
| Thread | cola `CoreJ2.EmbeddedJVM` (no MainActor) |
| Frames | `corej2_embedded_jvm_create` → `JNI_CreateJavaVM` → `Threads::create_vm` → **`Arguments::parse`** → **PC=0** |

Consola (`devicectl … --console`):

```text
corej2_embedded_jvm_create: modules magic=dadafeca … Documents/Documents/lib/modules
App terminated due to signal 9.
```

Ya **no** aparece `Failed setting boot class path` (eso se corrigió antes del SIGKILL).

---

## 4) Causas corregidas en esta iteración

### A. `lib/modules` ausente
El JDK exploded de Zero no trae jimage. HotSpot abortaba al crear la VM.  
**Fix:** `scripts/openjdk-mobile/build-jlink-java-home.sh` + `copy-ios-runtime-into-app.sh` instala `lib/modules` (~25 MiB).

### B. Layout iOS de OpenJDK Mobile
En `os_bsd.cpp` / `os.cpp` (commit `421bb8f0…`):

- `java.home` forzado a `$HOME/Documents` (ignora `-Djava.home`)
- boot path: `$JAVA_HOME/Documents/lib/modules` → **`$HOME/Documents/Documents/lib/modules`**

**Fix:** `MaterializedJavaHome` copia el jimage (+ conf/jvm.cfg) a esa ruta antes de `CreateJavaVM`.

### C. Arranque UI / autolaunch
- `activeEmulatorViewModel` ahora es `@Published` (sin eso, `navigationDestination` a veces no montaba `EmulatorView`).
- Autolaunch con delay 800 ms + log en `Documents/jvm-diag.log`.
- Bootstrap embebido async (sin bloquear MainActor).

### D. `jvm.cfg` dangling (iteración previa)
Symlink roto → reemplazado por cfg Zero-only en Copy Runtime.

---

## 5) Bloqueo que necesita decisión

**Síntoma estable:** tras materializar modules, `JNI_CreateJavaVM` entra a `Arguments::parse` y el proceso es asesinado por codesign (`Invalid Page`, ejecución en `0x0`).

**Qué ya se descartó:**

- Falta de `lib/modules` / boot class path  
- JAR inexistente (stale path)  
- Watchdog MainActor  
- Opciones `-XX:+UseSerialGC` / `-Xrs` (reproducido con opciones mínimas)

**Pistas técnicas:**

- `os::current_thread_enable_wx` en `os_bsd_zero.o` ya es no-op (no llama `pthread_jit_write_protect_np`).
- `OpenJDKMobileWXStubs.cpp` en el App Target está **vacío** (solo comentarios); el link no pide stubs nuevos, pero el kill de codesign sigue.
- El salto a `PC=0` sugiere **llamada a puntero nulo** durante el parse de argumentos de la VM (no un jetsam clásico).

**Opciones para el usuario / siguiente epic:**

1. Instrumentar / parchear OpenJDK Mobile (`Arguments::parse` / init iOS) para localizar el call site nulo.  
2. Revisar si hace falta un objeto `os_bsd_aarch64` distinto o un stub fuerte que **reemplace** símbolos W^X/JIT en el link.  
3. Validar en otro dispositivo / build de Zero más reciente del repo `openjdk/mobile`.  
4. Aceptar el bloqueo de bring-up embebido y priorizar otro camino de runtime (fuera de alcance mínimo).

---

## 6) Archivos tocados (mínimos)

| Path | Rol |
|------|-----|
| `scripts/openjdk-mobile/build-jlink-java-home.sh` | Nuevo — genera jimage |
| `scripts/openjdk-mobile/copy-ios-runtime-into-app.sh` | Instala `lib/modules` + conf |
| `CoreJ2/.../MaterializedJavaHome.swift` | Nuevo — layout iOS Documents |
| `CoreJ2/App/AppDependencyContainer.swift` | Usa materialize |
| `CoreJ2/.../EmbeddedJVMNative.c` | Logs + preflight modules |
| `CoreJ2/.../LibraryViewModel.swift` | Autolaunch, filter JARs vivos |
| `CoreJ2/.../DuplicateDetectionStep.swift` | Re-import si path muerto |
| `CoreJ2/.../EmulatorView*.swift` | Logs de launch |
| `artifacts/openjdk-mobile/jlink-ios-java.home/` | Artefacto jlink |

---

## 7) Cómo reproducir el estado actual

```bash
# 1) Asegurar jimage
./scripts/openjdk-mobile/build-jlink-java-home.sh

# 2) Build + install
xcodebuild -project CoreJ2.xcodeproj -scheme CoreJ2 -configuration Debug \
  -destination 'platform=iOS,id=00008020-001645C20221002E' \
  -derivedDataPath build/DerivedData -allowProvisioningUpdates build
xcrun devicectl device install app --device 00008020-001645C20221002E \
  build/DerivedData/Build/Products/Debug-iphoneos/CoreJ2.app

# 3) JAR + autolaunch
./scripts/device/copy-jar-to-app-documents.sh
: > /tmp/autolaunch && xcrun devicectl device copy to --device … \
  --domain-type appDataContainer --domain-identifier dev.abadx.corej2 \
  --destination Documents/autolaunch --source /tmp/autolaunch

# 4) Consola
xcrun devicectl device process launch --device … --terminate-existing --activate --console \
  dev.abadx.corej2
```

Expectativa actual: logs de materialize + `modules magic=dadafeca`, luego **signal 9**.

---

## 8) Veredicto

| Pregunta | Respuesta |
|----------|-----------|
| ¿Picker/devicectl Documents? | **Corregido** para el flujo futuro (auto-import + script + repair de paths). |
| ¿Crash al tocar el juego? | **Causa raíz acotada:** codesign kill en `Arguments::parse` tras boot path OK. |
| ¿Miami Nights abre? | **No** — bloqueo de VM/iOS, no de UI/JAR. |
| ¿Siguiente paso? | Decisión del usuario sobre parche OpenJDK Mobile / stubs W^X / alternativa de runtime. |
