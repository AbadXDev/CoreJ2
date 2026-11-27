#!/usr/bin/env bash
# Build a portable `lib/modules` jimage (+ conf) from the iOS Zero exploded
# modules tree. HotSpot requires `java.home/lib/modules` at JNI_CreateJavaVM.
#
# Output: artifacts/openjdk-mobile/jlink-ios-java.home/
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
MODS="${REPO_ROOT}/ThirdParty/OpenJDKMobile/src/mobile/build/ios-aarch64-zero-release/jdk/modules"
OUT="${REPO_ROOT}/artifacts/openjdk-mobile/jlink-ios-java.home"
WORKDIR="${REPO_ROOT}/build/device/jmod-work"

JMOD_BIN="${JMOD_BIN:-}"
JLINK_BIN="${JLINK_BIN:-}"
if [[ -z "${JMOD_BIN}" ]]; then
  for c in \
    /opt/homebrew/opt/openjdk@25/bin/jmod \
    /opt/homebrew/opt/openjdk/bin/jmod \
    "$(command -v jmod || true)"; do
    [[ -n "${c}" && -x "${c}" ]] || continue
    major="$("${c%/jmod}/java" -version 2>&1 | sed -n 's/.*version \"\([0-9]*\).*/\1/p' | head -1)"
    if [[ "${major}" -ge 25 ]]; then
      JMOD_BIN="${c}"
      JLINK_BIN="${c%/jmod}/jlink"
      break
    fi
  done
fi
[[ -n "${JMOD_BIN}" && -x "${JMOD_BIN}" && -x "${JLINK_BIN}" ]] || {
  echo "error: need JDK 25+ jmod/jlink (brew install openjdk@25)" >&2
  exit 1
}
[[ -d "${MODS}/java.base" ]] || {
  echo "error: exploded modules missing at ${MODS}" >&2
  exit 1
}

MODULES=(java.base java.logging java.datatransfer java.xml java.prefs java.desktop)

rm -rf "${WORKDIR}" "${OUT}"
mkdir -p "${WORKDIR}/jmods"

echo "build-jlink-java-home: packaging ${#MODULES[@]} modules via ${JMOD_BIN}"
for m in "${MODULES[@]}"; do
  "${JMOD_BIN}" create \
    --class-path "${MODS}/${m}" \
    --module-version "25" \
    --target-platform "macos-aarch64" \
    "${WORKDIR}/jmods/${m}.jmod"
done

echo "build-jlink-java-home: jlink → ${OUT}"
"${JLINK_BIN}" --module-path "${WORKDIR}/jmods" \
  --add-modules "java.base,java.logging,java.datatransfer,java.xml,java.prefs,java.desktop" \
  --output "${OUT}" \
  --no-header-files --no-man-pages --strip-debug \
  --compress=zip-0

# Host jlink may omit conf when jmods were class-path only; merge from JDK 25.
HOST_HOME="$("${JLINK_BIN%/jlink}/java" -XshowSettings:properties -version 2>&1 | sed -n 's/.*java.home = //p' | head -1)"
if [[ -d "${HOST_HOME}/conf" ]]; then
  rsync -a "${HOST_HOME}/conf/" "${OUT}/conf/"
fi
SRC_NET="${REPO_ROOT}/ThirdParty/OpenJDKMobile/src/mobile/build/ios-aarch64-zero-release/jdk/conf/net.properties"
[[ -f "${SRC_NET}" ]] && cp -f "${SRC_NET}" "${OUT}/conf/net.properties"

mkdir -p "${OUT}/lib"
cat > "${OUT}/lib/jvm.cfg" <<'EOF'
# CoreJ2: Zero interpreter only (OpenJDK Mobile static link).
-zero KNOWN
EOF

# Drop any host native bits; device links Zero statically.
rm -rf "${OUT}/bin" "${OUT}/legal" 2>/dev/null || true
find "${OUT}/lib" -maxdepth 1 \( -name '*.dylib' -o -name 'server' -o -name 'jli' -o -name 'jspawnhelper' \) -exec rm -rf {} + 2>/dev/null || true

[[ -f "${OUT}/lib/modules" ]] || {
  echo "error: jlink did not produce lib/modules" >&2
  exit 1
}
echo "build-jlink-java-home: OK ($(du -sh "${OUT}" | awk '{print $1}'), modules=$(wc -c < "${OUT}/lib/modules" | tr -d ' ') bytes)"
