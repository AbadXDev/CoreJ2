# Renombrado JavaOne → CoreJ2 + AppIcon (Mezcla C)

**Fecha:** 2026-11-27 (America/Santo_Domingo)  
**Dispositivo:** iPhone XS Max `00008020-001645C20221002E`  
**Resultado:** OK — build, identidad nueva, icono empaquetado, Miami Nights menú jugable; `grep -ri javaone` = 0.

---

## 1. Renombrado

| Antes | Después |
|-------|---------|
| `JavaOne.xcodeproj` | `CoreJ2.xcodeproj` |
| `JavaOne/` | `CoreJ2/` |
| `JavaOneTests` / `JavaOneUITests` | `CoreJ2Tests` / `CoreJ2UITests` |
| `JavaOneApp.swift` | `CoreJ2App.swift` |
| Bundle `com.javaonelabs.JavaOne` | `dev.abadx.corej2` (+Tests/UITests) |
| Display name | `CoreJ2` |
| Paquete Java `org.javaone.*` | `dev.abadx.corej2.*` (bootstrap + overlay) |
| Símbolos C `javaone_*` / macros `JAVAONE_*` | `corej2_*` / `COREJ2_*` |
| Log tag OpenJDK `javaone-jvm` | `corej2-jvm:` (misma longitud en binarios) |

Notas históricas del nombre viejo (p. ej. CHANGELOG «originally developed as JavaOne») eliminadas. Snapshot obsoleto `corej2.zip` (contenía texto `javaone`) eliminado. Symlinks rotos que apuntaban a `JavaOne/…` retargeteados a `CoreJ2/…`.

### Meta verificable

```text
grep -ri javaone --exclude-dir=build --exclude-dir=DerivedData --exclude-dir=.git .
→ 0 resultados (contenido)
find … -iname '*javaone*'
→ 0 rutas (fuera de build/DerivedData)
```

Vendor/FreeJ2ME intacto. Sin push a GitHub.

---

## 2. AppIcon (Mezcla C)

- Maestro: `assets/branding/corej2-appicon-1024.png` (1024×1024).
- Asset catalog: `CoreJ2/Assets.xcassets/AppIcon.appiconset/` con tamaños iPhone/iPad/marketing generados desde el maestro.
- Info.plist del `.app`: `CFBundleIconName=AppIcon`, `CFBundleDisplayName=CoreJ2`, `CFBundleIdentifier=dev.abadx.corej2`.
- En el producto: `AppIcon60x60@2x.png` + `Assets.car` presentes.

---

## 3. Verificación XS Max

| Check | Resultado |
|-------|-----------|
| `xcodebuild` scheme `CoreJ2` Debug device | **BUILD SUCCEEDED** |
| Install | `dev.abadx.corej2` → `CoreJ2.app` (sandbox fresco) |
| Identidad vieja | `com.javaonelabs.JavaOne` desinstalada tras la verificación |
| Icono | AppIcon en catálogo / Infos del bundle; iOS usa `AppIcon` en SpringBoard |
| Miami Nights EN v1.0.8 | Autolaunch OK |

### Miami (log clave)

- `autolaunch armed for Miami Nights v=1.0.8`
- `corej2-lcdui: Display ready` / `Canvas 240x320`
- `START_APP_OK`
- `lcdNonZeroPixels=76800`
- `startSession OK Miami Nights`
- Bucle de paint continuo (`paintIndex` ≥ 244 en ~25 s; spam de menú del juego)

Sin regresión de arranque respecto a la sesión FASE 2 previa.

---

## 4. Alcance no tocado

- Vendor / FreeJ2ME: sin cambios de integración.
- Sin push remoto.
