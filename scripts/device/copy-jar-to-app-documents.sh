#!/usr/bin/env bash
# Copy a JAR into CoreJ2 Documents so Files / fileImporter can see it
# (requires UIFileSharingEnabled on the installed build).
#
# Usage:
#   ./scripts/device/copy-jar-to-app-documents.sh [path/to/game.jar] [device-udid]
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
JAR="${1:-$ROOT/games/Miami-Nights-Singles-in-the-City_J2ME_ES-ML_240x320_v103.jar}"
UDID="${2:-00008020-001645C20221002E}"
BUNDLE_ID="dev.abadx.corej2"
NAME="$(basename "$JAR")"

[[ -f "$JAR" ]] || { echo "error: JAR not found: $JAR" >&2; exit 1; }

# Ensure Documents is a directory (a prior bad copy can replace it with a file).
STAGE="$(mktemp -d)"
cleanup() { rm -rf "$STAGE"; }
trap cleanup EXIT
mkdir -p "$STAGE/Documents"

xcrun devicectl device copy to \
  --device "$UDID" \
  --domain-type appDataContainer \
  --domain-identifier "$BUNDLE_ID" \
  --destination / \
  --source "$STAGE/Documents" \
  --remove-existing-content false >/dev/null

xcrun devicectl device copy to \
  --device "$UDID" \
  --domain-type appDataContainer \
  --domain-identifier "$BUNDLE_ID" \
  --destination "Documents/${NAME}" \
  --source "$JAR"

echo "OK: Documents/${NAME} on ${UDID}"
xcrun devicectl device info files \
  --device "$UDID" \
  --domain-type appDataContainer \
  --domain-identifier "$BUNDLE_ID" \
  --subdirectory Documents \
  --no-recurse
