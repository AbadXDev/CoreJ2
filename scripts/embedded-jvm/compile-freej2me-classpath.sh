#!/usr/bin/env bash
# Compile the embedded FreeJ2ME classpath for the in-process JVM:
#   Vendor/FreeJ2ME/src/**.java
#   CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Bootstrap/*.java
# Output: artifacts/freej2me-embedded-classpath/classes
# Also builds fixture JARs (DisplayProbeMIDlet.jar) under .../fixtures.
#
# Requires a Boot JDK (javac). Resolution order:
#   BOOT_JDK_HOME → JAVA_HOME → ~/jdk24 → /usr/libexec/java_home -v 24

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_ROOT="${REPO_ROOT}/artifacts/freej2me-embedded-classpath"
CLASSES_DIR="${OUT_ROOT}/classes"
FIXTURES_DIR="${OUT_ROOT}/fixtures"
FIXTURE_CLASSES_DIR="${OUT_ROOT}/fixture-classes"

log() { printf '[compile-freej2me-classpath] %s\n' "$*"; }
die() { printf '[compile-freej2me-classpath] ERROR: %s\n' "$*" >&2; exit 1; }

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
[[ -n "${JDK_HOME}" && -x "${JDK_HOME}/bin/javac" ]] || die "javac not found — set BOOT_JDK_HOME to a JDK 24 home"
JAVAC="${JDK_HOME}/bin/javac"
JAR="${JDK_HOME}/bin/jar"
log "Using javac from ${JDK_HOME}"

FREEJ2ME_SRC="${REPO_ROOT}/Vendor/FreeJ2ME/src"
BOOTSTRAP_SRC="${REPO_ROOT}/CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Bootstrap"
OVERLAY_SRC="${REPO_ROOT}/CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay"
FIXTURE_SRC="${REPO_ROOT}/CoreJ2/Features/Emulator/Runtime/Bootstrap/fixtures"
[[ -d "${FREEJ2ME_SRC}" ]] || die "missing ${FREEJ2ME_SRC} — run git submodule update --init"
[[ -d "${BOOTSTRAP_SRC}" ]] || die "missing ${BOOTSTRAP_SRC}"
[[ -d "${OVERLAY_SRC}" ]] || die "missing ${OVERLAY_SRC}"

rm -rf "${CLASSES_DIR}" "${FIXTURE_CLASSES_DIR}"
mkdir -p "${CLASSES_DIR}" "${FIXTURES_DIR}" "${FIXTURE_CLASSES_DIR}"

SOURCES_FILE="$(mktemp)"
OVERLAY_FILE="$(mktemp)"
trap 'rm -f "${SOURCES_FILE}" "${OVERLAY_FILE}"' EXIT
# Relative paths in the argfile: javac splits argfile entries on whitespace,
# so absolute paths under a directory containing spaces would break.
cd "${REPO_ROOT}"

# 1) Vendor FreeJ2ME only (still compiles against host AWT — never loaded on device).
find Vendor/FreeJ2ME/src -name '*.java' > "${SOURCES_FILE}"
SOURCE_COUNT="$(wc -l < "${SOURCES_FILE}" | tr -d ' ')"
log "Compiling ${SOURCE_COUNT} FreeJ2ME sources → ${CLASSES_DIR}"

"${JAVAC}" \
  --release 8 \
  -encoding UTF-8 \
  -nowarn \
  -d "${CLASSES_DIR}" \
  @"${SOURCES_FILE}"

# 2) AWT overlay — same FQCNs, overwrite Vendor Platform* / MobilePlatform / LayerManager.
find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Overlay -name '*.java' > "${OVERLAY_FILE}"
OVERLAY_COUNT="$(wc -l < "${OVERLAY_FILE}" | tr -d ' ')"
log "Compiling ${OVERLAY_COUNT} AWT-overlay sources (overwrite) → ${CLASSES_DIR}"

"${JAVAC}" \
  --release 8 \
  -encoding UTF-8 \
  -nowarn \
  -cp "${CLASSES_DIR}" \
  -d "${CLASSES_DIR}" \
  @"${OVERLAY_FILE}"

# 3) CoreJ2 Bootstrap (LcdSupport uses overlay getLCDImage()).
BOOTSTRAP_FILE="$(mktemp)"
trap 'rm -f "${SOURCES_FILE}" "${OVERLAY_FILE}" "${BOOTSTRAP_FILE}"' EXIT
find CoreJ2/Features/Emulator/Runtime/EmbeddedJVM/Bootstrap -name '*.java' > "${BOOTSTRAP_FILE}"
BOOTSTRAP_COUNT="$(wc -l < "${BOOTSTRAP_FILE}" | tr -d ' ')"
log "Compiling ${BOOTSTRAP_COUNT} Bootstrap sources → ${CLASSES_DIR}"

"${JAVAC}" \
  --release 8 \
  -encoding UTF-8 \
  -nowarn \
  -cp "${CLASSES_DIR}" \
  -d "${CLASSES_DIR}" \
  @"${BOOTSTRAP_FILE}"

CLASS_COUNT="$(find "${CLASSES_DIR}" -name '*.class' | wc -l | tr -d ' ')"
log "Compiled ${CLASS_COUNT} classes (overlay applied)"

# Fixture MIDlets (used by tests / diagnostics).
if [[ -d "${FIXTURE_SRC}" ]]; then
  FIXTURE_SOURCES="$(mktemp)"
  trap 'rm -f "${SOURCES_FILE}" "${FIXTURE_SOURCES}"' EXIT
  find CoreJ2/Features/Emulator/Runtime/Bootstrap/fixtures -name '*.java' > "${FIXTURE_SOURCES}"
  if [[ -s "${FIXTURE_SOURCES}" ]]; then
    log "Compiling fixture MIDlets"
    "${JAVAC}" \
      --release 8 \
      -encoding UTF-8 \
      -nowarn \
      -cp "${CLASSES_DIR}" \
      -d "${FIXTURE_CLASSES_DIR}" \
      @"${FIXTURE_SOURCES}"
    for main_class_file in "${FIXTURE_SRC}"/*.java; do
      [[ -f "${main_class_file}" ]] || continue
      base="$(basename "${main_class_file}" .java)"
      manifest="$(mktemp)"
      {
        echo "Manifest-Version: 1.0"
        echo "MIDlet-Name: ${base}"
        echo "MIDlet-Version: 1.0.0"
        echo "MIDlet-Vendor: CoreJ2"
        echo "MIDlet-1: ${base}, , ${base}"
        echo "MicroEdition-Profile: MIDP-2.0"
        echo "MicroEdition-Configuration: CLDC-1.1"
      } > "${manifest}"
      "${JAR}" cfm "${FIXTURES_DIR}/${base}.jar" "${manifest}" -C "${FIXTURE_CLASSES_DIR}" .
      rm -f "${manifest}"
      log "Built fixture ${FIXTURES_DIR}/${base}.jar"
    done
  fi
fi

log "Done."
