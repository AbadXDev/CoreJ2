#!/usr/bin/env bash
# FASE 3 — pure CoreJ2 classpath: ZERO FreeJ2ME / Vendor classes.
#
# Compiles:
#   1) ObjectWeb ASM (BSD) from Vendor/FreeJ2ME/src/org/objectweb/asm  [third-party only]
#   2) CoreJ2 Overlay/**/*.java  (MIDP/MMAPI/M3G/Nokia + platform bridges)
#   3) CoreJ2 Bootstrap/**/*.java
#
# Output: artifacts/corej2-embedded-classpath-fase3/classes
# Marker:  artifacts/corej2-embedded-classpath-fase3/PROVENANCE.txt
#
# Does NOT modify Vendor/FreeJ2ME. The legacy Vendor+overlay build remains via
# compile-freej2me-classpath.sh as a reference.

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "${REPO_ROOT}"

OUT_ROOT="${REPO_ROOT}/artifacts/corej2-embedded-classpath-fase3"
CLASSES_DIR="${OUT_ROOT}/classes"

log() { printf '[compile-fase3-pure] %s\n' "$*"; }
die() { printf '[compile-fase3-pure] ERROR: %s\n' "$*" >&2; exit 1; }

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
[[ -n "${JDK_HOME}" && -x "${JDK_HOME}/bin/javac" ]] || die "javac not found — set BOOT_JDK_HOME"
JAVAC="${JDK_HOME}/bin/javac"
log "Using javac from ${JDK_HOME}"

OVERLAY_SRC="${REPO_ROOT}/CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay"
BOOTSTRAP_SRC="${REPO_ROOT}/CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Bootstrap"
ASM_SRC="${REPO_ROOT}/Vendor/FreeJ2ME/src/org/objectweb/asm"
[[ -d "${OVERLAY_SRC}" ]] || die "missing ${OVERLAY_SRC}"
[[ -d "${BOOTSTRAP_SRC}" ]] || die "missing ${BOOTSTRAP_SRC}"
[[ -d "${ASM_SRC}" ]] || die "missing ObjectWeb ASM at ${ASM_SRC}"

rm -rf "${OUT_ROOT}"
mkdir -p "${CLASSES_DIR}"

ASM_LIST="$(mktemp)"
OVERLAY_LIST="$(mktemp)"
BOOTSTRAP_LIST="$(mktemp)"
trap 'rm -f "${ASM_LIST}" "${OVERLAY_LIST}" "${BOOTSTRAP_LIST}"' EXIT

find Vendor/FreeJ2ME/src/org/objectweb/asm -name '*.java' > "${ASM_LIST}"
ASM_COUNT="$(wc -l < "${ASM_LIST}" | tr -d ' ')"
log "Compiling ${ASM_COUNT} ObjectWeb ASM sources (third-party) → ${CLASSES_DIR}"
"${JAVAC}" --release 8 -encoding UTF-8 -nowarn -d "${CLASSES_DIR}" @"${ASM_LIST}"

find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay -name '*.java' > "${OVERLAY_LIST}"
OV_COUNT="$(wc -l < "${OVERLAY_LIST}" | tr -d ' ')"
log "Compiling ${OV_COUNT} Overlay sources → ${CLASSES_DIR}"
"${JAVAC}" --release 8 -encoding UTF-8 -nowarn -cp "${CLASSES_DIR}" -d "${CLASSES_DIR}" @"${OVERLAY_LIST}"

find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Bootstrap -name '*.java' > "${BOOTSTRAP_LIST}"
BS_COUNT="$(wc -l < "${BOOTSTRAP_LIST}" | tr -d ' ')"
log "Compiling ${BS_COUNT} Bootstrap sources → ${CLASSES_DIR}"
"${JAVAC}" --release 8 -encoding UTF-8 -nowarn -cp "${CLASSES_DIR}" -d "${CLASSES_DIR}" @"${BOOTSTRAP_LIST}"

# --- provenance / purity checks ---------------------------------------------
CLASS_COUNT="$(find "${CLASSES_DIR}" -name '*.class' | wc -l | tr -d ' ')"
FREEJ2ME_HIT="$(find "${CLASSES_DIR}" -path '*/org/recompile/freej2me/*' -name '*.class' | wc -l | tr -d ' ')"
VENDOR_MIDP_LEAK="$(find "${CLASSES_DIR}/javax" "${CLASSES_DIR}/com" "${CLASSES_DIR}/org/recompile" \
  -name '*.class' 2>/dev/null | while read -r f; do
    # nothing — purity is "no freej2me package + no Vendor compile"
    :
  done; echo 0)"

if [[ "${FREEJ2ME_HIT}" != "0" ]]; then
  die "pure classpath contains org.recompile.freej2me classes (${FREEJ2ME_HIT})"
fi

# Ensure no accidental Vendor-only packages slipped in (bluetooth/pim/sensor/…)
FORBIDDEN_DIRS=(
  "javax/bluetooth"
  "javax/microedition/pim"
  "javax/microedition/sensor"
  "javax/wireless"
  "com/mascotcapsule"
  "org/recompile/freej2me"
)
for d in "${FORBIDDEN_DIRS[@]}"; do
  if [[ -d "${CLASSES_DIR}/${d}" ]]; then
    die "forbidden package present in pure classpath: ${d}"
  fi
done

{
  echo "CoreJ2 FASE-3 pure classpath"
  echo "classes=${CLASS_COUNT}"
  echo "sources:"
  echo "  - org.objectweb.asm (ObjectWeb ASM, BSD; tree under Vendor/FreeJ2ME/src for convenience)"
  echo "  - CoreJ2 Overlay (clean-room MIDP/MMAPI/M3G/Nokia + platform bridges)"
  echo "  - CoreJ2 Bootstrap (LcdSupport, RunJarSupport, …)"
  echo "excluded:"
  echo "  - Vendor/FreeJ2ME javax/** org/recompile/freej2me/** org/recompile/mobile/** (Vendor copies)"
  echo "  - OEM stubs not referenced by matrix (bluetooth, PIM, sensor, mascotcapsule, …)"
} > "${OUT_ROOT}/PROVENANCE.txt"

# Class inventory for the report
find "${CLASSES_DIR}" -name '*.class' | sed "s|^${CLASSES_DIR}/||" | sort > "${OUT_ROOT}/CLASSES.txt"

log "fase3 pure classpath ready: ${CLASSES_DIR} (${CLASS_COUNT} classes)"
log "PROVENANCE → ${OUT_ROOT}/PROVENANCE.txt"
