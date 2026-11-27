#!/usr/bin/env bash
# Driver v2: compila OpenJDK Mobile en una ruta SIN espacios (~/corej2-jvm),
# porque el build de OpenJDK no acepta espacios en la ruta ("Muse archivos"),
# y luego copia los artefactos de vuelta al proyecto.
# Se corre en segundo plano con nohup; el progreso queda en build-jvm.log.
set -x

PROJECT_DIR="$HOME/Desktop/Muse archivos/corej2"
BUILD_ROOT="$HOME/corej2-jvm"

mkdir -p "$BUILD_ROOT/ThirdParty/OpenJDKMobile" "$BUILD_ROOT/scripts"

# Scripts y pins frescos desde el proyecto
rsync -a --delete "$PROJECT_DIR/scripts/openjdk-mobile/" "$BUILD_ROOT/scripts/openjdk-mobile/"
cp "$PROJECT_DIR/ThirdParty/OpenJDKMobile/versions.env" "$BUILD_ROOT/ThirdParty/OpenJDKMobile/versions.env"

# Mover las fuentes ya descargadas (clon de ~1.5 GB) si están en el proyecto
if [ -d "$PROJECT_DIR/ThirdParty/OpenJDKMobile/src" ] && [ ! -d "$BUILD_ROOT/ThirdParty/OpenJDKMobile/src" ]; then
  mv "$PROJECT_DIR/ThirdParty/OpenJDKMobile/src" "$BUILD_ROOT/ThirdParty/OpenJDKMobile/src"
fi
# Copiar support/cache/out (pequeños) si existen en el proyecto
for d in support cache out; do
  if [ -d "$PROJECT_DIR/ThirdParty/OpenJDKMobile/$d" ] && [ ! -d "$BUILD_ROOT/ThirdParty/OpenJDKMobile/$d" ]; then
    cp -R "$PROJECT_DIR/ThirdParty/OpenJDKMobile/$d" "$BUILD_ROOT/ThirdParty/OpenJDKMobile/$d"
  fi
done

chmod +x "$BUILD_ROOT/scripts/openjdk-mobile/"*.sh
cd "$BUILD_ROOT" || exit 1
BOOT_JDK_HOME="$HOME/jdk24" ./scripts/openjdk-mobile/build-all.sh
BUILD_RC=$?
echo "build-all.sh exit: $BUILD_RC"
if [ "$BUILD_RC" -ne 0 ]; then
  echo "=== FALLO EN build-all.sh ==="
  exit "$BUILD_RC"
fi

# Copiar artefactos de vuelta al proyecto
mkdir -p "$PROJECT_DIR/artifacts"
rsync -a --delete "$BUILD_ROOT/artifacts/openjdk-mobile/" "$PROJECT_DIR/artifacts/openjdk-mobile/"

# Copiar el JDK explotado (java.home) a donde lo espera la fase "Copy Runtime"
JDK_SRC="$BUILD_ROOT/ThirdParty/OpenJDKMobile/src/mobile/build/ios-aarch64-zero-release/jdk"
JDK_DST="$PROJECT_DIR/ThirdParty/OpenJDKMobile/src/mobile/build/ios-aarch64-zero-release/jdk"
mkdir -p "$(dirname "$JDK_DST")"
rsync -a --delete "$JDK_SRC/" "$JDK_DST/"

# Classpath de FreeJ2ME + xcconfig de enlace, en el proyecto
cd "$PROJECT_DIR" || exit 1
chmod +x scripts/openjdk-mobile/*.sh scripts/embedded-jvm/*.sh
./scripts/embedded-jvm/compile-freej2me-classpath.sh
CP_RC=$?
echo "compile-freej2me-classpath.sh exit: $CP_RC"
if [ "$CP_RC" -ne 0 ]; then
  echo "=== FALLO EN compile-freej2me-classpath.sh ==="
  exit "$CP_RC"
fi

./scripts/openjdk-mobile/generate-xcode-link-xcconfig.sh
XC_RC=$?
echo "generate-xcode-link-xcconfig.sh exit: $XC_RC"
if [ "$XC_RC" -ne 0 ]; then
  echo "=== FALLO EN generate-xcode-link-xcconfig.sh ==="
  exit "$XC_RC"
fi

echo "=== TODO TERMINADO OK ==="
