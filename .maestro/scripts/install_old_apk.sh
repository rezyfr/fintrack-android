#!/usr/bin/env bash
# Install an older release APK so an upgrade flow can run. Runs on the emulator only.
# Usage: install_old_apk.sh [path]   (default: the newest APK in .maestro/apks)
# The script does not download anything. Put the APK in .maestro/apks yourself.
# Exit code 3 means there is no old APK, so the caller marks the upgrade case as manual.
set -eu
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
apk="${1:-}"
if [ -z "$apk" ]; then
  apk="$(ls -t "$MAESTRO_DIR"/apks/*.apk 2>/dev/null | head -1 || true)"
fi
if [ -z "$apk" ] || [ ! -f "$apk" ]; then
  echo "No old APK found in $MAESTRO_DIR/apks. Skipping the upgrade cases." >&2
  exit 3
fi
adbe uninstall "$APP_ID" >/dev/null 2>&1 || true
adbe install "$apk"
