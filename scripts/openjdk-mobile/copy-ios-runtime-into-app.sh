#!/usr/bin/env bash
# Xcode build phase "Copy Runtime": embed the OpenJDK Mobile java.home and the
# compiled FreeJ2ME classpath into the app bundle (device builds only).
#
# Expected bundle layout (see docs/Architecture/E6-US003):
#   CoreJ2.app/OpenJDKMobile/java.home/    exploded JDK from the mobile build
#   CoreJ2.app/OpenJDKMobile/classpath/    FreeJ2ME + dev.abadx.corej2.bootstrap
#   CoreJ2.app/fixtures/                   DisplayProbeMIDlet.jar et al.

set -euo pipefail

if [[ "${PLATFORM_NAME:-}" != "iphoneos" ]]; then
  echo "Copy Runtime: simulator build — skipping embedded JVM resources."
  exit 0
fi

APP_DIR="${TARGET_BUILD_DIR}/${PRODUCT_NAME}.app"
DEST_ROOT="${APP_DIR}/OpenJDKMobile"
mkdir -p "${DEST_ROOT}"

# --- java.home ---------------------------------------------------------------
JAVA_HOME_SRC="${SRCROOT}/ThirdParty/OpenJDKMobile/src/mobile/build/ios-aarch64-zero-release/jdk"
if [[ ! -d "${JAVA_HOME_SRC}" ]]; then
  JAVA_HOME_SRC="$(find "${SRCROOT}/ThirdParty/OpenJDKMobile/src/mobile/build" -maxdepth 2 -type d -name jdk 2>/dev/null | head -1 || true)"
fi
[[ -n "${JAVA_HOME_SRC}" && -d "${JAVA_HOME_SRC}" ]] || {
  echo "error: exploded JDK (java.home) not found — run scripts/openjdk-mobile/build-all.sh first" >&2
  exit 1
}
echo "Copy Runtime: java.home ← ${JAVA_HOME_SRC}"
rsync -a --delete "${JAVA_HOME_SRC}/" "${DEST_ROOT}/java.home/"

# The Zero exploded image ships lib/jvm.cfg as a dangling symlink into the
# build support tree (../../support/modules_libs/...). Replace it with a real
# Zero-only cfg so JNI_CreateJavaVM can resolve the VM flavor on device.
JVM_CFG="${DEST_ROOT}/java.home/lib/jvm.cfg"
mkdir -p "${DEST_ROOT}/java.home/lib"
rm -f "${JVM_CFG}"
cat > "${JVM_CFG}" <<'EOF'
# CoreJ2: Zero interpreter only (OpenJDK Mobile static link).
-zero KNOWN
EOF
echo "Copy Runtime: wrote ${JVM_CFG}"

# Exploded `jdk/` from `static-libs-image` has no `lib/modules` jimage. HotSpot
# aborts during JNI_CreateJavaVM without it. Prefer a pre-built jlink image
# (see scripts/openjdk-mobile/build-jlink-java-home.sh).
JLINK_HOME="${SRCROOT}/artifacts/openjdk-mobile/jlink-ios-java.home"
MODULES_IMG="${JLINK_HOME}/lib/modules"
if [[ -f "${MODULES_IMG}" ]]; then
  cp -f "${MODULES_IMG}" "${DEST_ROOT}/java.home/lib/modules"
  echo "Copy Runtime: installed lib/modules from jlink image ($(wc -c < "${MODULES_IMG}" | tr -d ' ') bytes)"
  # Exploded tree only ships net.properties; merge jlink/host conf for security.
  if [[ -d "${JLINK_HOME}/conf" ]]; then
    rsync -a "${JLINK_HOME}/conf/" "${DEST_ROOT}/java.home/conf/"
    echo "Copy Runtime: merged conf/ from jlink image"
  fi
else
  echo "error: missing ${MODULES_IMG} — run scripts/openjdk-mobile/build-jlink-java-home.sh" >&2
  exit 1
fi
# --- Embedded classpath (FASE 3 pure is default; Vendor+overlay = reference) -
# COREJ2_CLASSPATH_VARIANT=reference → artifacts/freej2me-embedded-classpath
# otherwise → artifacts/corej2-embedded-classpath-fase3 (zero FreeJ2ME classes)
VARIANT="${COREJ2_CLASSPATH_VARIANT:-fase3}"
if [[ "${VARIANT}" == "reference" || "${VARIANT}" == "freej2me" ]]; then
  CLASSPATH_SRC="${SRCROOT}/artifacts/freej2me-embedded-classpath/classes"
  FIXTURES_SRC="${SRCROOT}/artifacts/freej2me-embedded-classpath/fixtures"
else
  CLASSPATH_SRC="${SRCROOT}/artifacts/corej2-embedded-classpath-fase3/classes"
  FIXTURES_SRC="${SRCROOT}/artifacts/freej2me-embedded-classpath/fixtures"
fi
[[ -d "${CLASSPATH_SRC}" ]] || {
  echo "error: classpath not found at ${CLASSPATH_SRC}" >&2
  echo "hint: run scripts/embedded-jvm/compile-fase3-pure.sh (or compile-freej2me-classpath.sh for reference)" >&2
  exit 1
}
# Purity gate for the default variant
if [[ "${VARIANT}" != "reference" && "${VARIANT}" != "freej2me" ]]; then
  if find "${CLASSPATH_SRC}" -path '*/org/recompile/freej2me/*' -name '*.class' | grep -q .; then
    echo "error: fase3 classpath still contains org.recompile.freej2me" >&2
    exit 1
  fi
fi
echo "Copy Runtime: classpath ← ${CLASSPATH_SRC} (variant=${VARIANT})"
rsync -a --delete "${CLASSPATH_SRC}/" "${DEST_ROOT}/classpath/"

# --- Fixtures ----------------------------------------------------------------
if [[ -d "${FIXTURES_SRC}" ]]; then
  echo "Copy Runtime: fixtures ← ${FIXTURES_SRC}"
  mkdir -p "${APP_DIR}/fixtures"
  rsync -a --delete "${FIXTURES_SRC}/" "${APP_DIR}/fixtures/"
fi

echo "Copy Runtime: done."
