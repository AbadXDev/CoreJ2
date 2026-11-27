#!/usr/bin/env bash
# Build a classpath variant where FASE-2 packages contain ONLY CoreJ2 clean-room
# overlays (no residual FreeJ2ME .class files for those packages).
#
# Packages owned by this variant:
#   javax.microedition.midlet
#   javax.microedition.rms
#   javax.microedition.media (+ control)
#   javax.microedition.lcdui.game
#   javax.microedition.io
#   com.nokia.mid.ui
#
# Output: artifacts/freej2me-embedded-classpath-fase2/classes
# Also refreshes the normal artifacts/freej2me-embedded-classpath (no regression).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${REPO_ROOT}"

log() { printf '[compile-fase2-own] %s\n' "$*"; }

# 1) Normal classpath (Vendor + overlay) — regression baseline.
bash scripts/embedded-jvm/compile-freej2me-classpath.sh

NORMAL="${REPO_ROOT}/artifacts/freej2me-embedded-classpath/classes"
OUT_ROOT="${REPO_ROOT}/artifacts/freej2me-embedded-classpath-fase2"
OUT="${OUT_ROOT}/classes"
rm -rf "${OUT_ROOT}"
mkdir -p "${OUT}"
cp -R "${NORMAL}/." "${OUT}/"

# 2) Strip Vendor leftovers from owned packages, then recompile overlays into OUT.
OWN_DIRS=(
  "javax/microedition/midlet"
  "javax/microedition/rms"
  "javax/microedition/media"
  "javax/microedition/lcdui/game"
  "javax/microedition/io"
  "com/nokia/mid/ui"
)

for d in "${OWN_DIRS[@]}"; do
  if [[ -d "${OUT}/${d}" ]]; then
    log "stripping ${d}"
    rm -rf "${OUT}/${d}"
  fi
done

resolve_jdk_home() {
  if [[ -n "${BOOT_JDK_HOME:-}" && -x "${BOOT_JDK_HOME}/bin/javac" ]]; then
    printf '%s\n' "${BOOT_JDK_HOME}"; return 0
  fi
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/javac" ]]; then
    printf '%s\n' "${JAVA_HOME}"; return 0
  fi
  if [[ -x "${HOME}/jdk24/bin/javac" ]]; then
    printf '%s\n' "${HOME}/jdk24"; return 0
  fi
  /usr/libexec/java_home -v 24 2>/dev/null || true
}

JDK_HOME="$(resolve_jdk_home)"
JAVAC="${JDK_HOME}/bin/javac"

OVERLAY_LIST="$(mktemp)"
trap 'rm -f "${OVERLAY_LIST}"' EXIT

# Collect overlay sources that live under owned packages (plus PlatformPlayer which
# implements media.Player and is required by Manager).
{
  find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/javax/microedition/midlet -name '*.java' 2>/dev/null || true
  find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/javax/microedition/rms -name '*.java' 2>/dev/null || true
  find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/javax/microedition/media -name '*.java' 2>/dev/null || true
  find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/javax/microedition/lcdui/game -name '*.java' 2>/dev/null || true
  find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/javax/microedition/io -name '*.java' 2>/dev/null || true
  find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/com/nokia/mid/ui -name '*.java' 2>/dev/null || true
  echo "CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/org/recompile/mobile/PlatformPlayer.java"
  echo "CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/dev/abadx/corej2/bootstrap/AudioBridge.java"
  echo "CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay/dev/abadx/corej2/bootstrap/VibrationBridge.java"
} > "${OVERLAY_LIST}"

COUNT="$(wc -l < "${OVERLAY_LIST}" | tr -d ' ')"
log "recompiling ${COUNT} owned-package overlay sources → ${OUT}"

"${JAVAC}" \
  --release 8 \
  -encoding UTF-8 \
  -nowarn \
  -cp "${OUT}" \
  -d "${OUT}" \
  @"${OVERLAY_LIST}"

# Marker file listing owned packages
mkdir -p "${OUT_ROOT}"
cat > "${OUT_ROOT}/OWNED_PACKAGES.txt" <<'MARK'
javax.microedition.midlet
javax.microedition.rms
javax.microedition.media
javax.microedition.lcdui.game
javax.microedition.io
com.nokia.mid.ui
MARK

log "fase2 owned-package classpath ready: ${OUT}"
find "${OUT}/javax/microedition/midlet" "${OUT}/javax/microedition/rms" \
  "${OUT}/javax/microedition/media" "${OUT}/javax/microedition/lcdui/game" \
  "${OUT}/javax/microedition/io" "${OUT}/com/nokia/mid/ui" \
  -name '*.class' 2>/dev/null | wc -l | awk '{print "[compile-fase2-own] owned class files:", $1}'
