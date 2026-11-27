#!/usr/bin/env bash
# Apply CoreJ2 OpenJDK Mobile patches onto the checked-out source tree.
# Idempotent: skips a patch that is already applied (git apply --check / reverse).

set -euo pipefail
# shellcheck source=common.sh
source "$(cd "$(dirname "$0")" && pwd)/common.sh"

PATCH_DIR="${REPO_ROOT}/patches/openjdk-mobile"
[[ -d "${OJDK_SRC_DIR}" ]] || ojdk_die "sources missing — run 01-fetch-sources.sh first"
[[ -d "${PATCH_DIR}" ]] || ojdk_die "missing ${PATCH_DIR}"

shopt -s nullglob
patches=("${PATCH_DIR}"/*.patch)
if [[ ${#patches[@]} -eq 0 ]]; then
  ojdk_log "No patches under ${PATCH_DIR}"
  exit 0
fi

cd "${OJDK_SRC_DIR}"
for p in "${patches[@]}"; do
  base="$(basename "${p}")"
  if git apply --check "${p}" 2>/dev/null; then
    ojdk_log "Applying ${base}"
    git apply "${p}"
  elif git apply --reverse --check "${p}" 2>/dev/null; then
    ojdk_log "Already applied: ${base}"
  else
    ojdk_die "Cannot apply ${base} (neither forward nor reverse check passed)"
  fi
done

ojdk_log "OpenJDK Mobile patches up to date"
