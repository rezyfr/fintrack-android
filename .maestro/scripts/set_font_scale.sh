#!/usr/bin/env bash
# Set the system font scale. Runs on the emulator only.
# Usage: set_font_scale.sh 1.3     (use 1.0 to reset)
set -eu
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
scale="${1:?scale missing}"
case "$scale" in
  [0-9]|[0-9].[0-9]|[0-9].[0-9][0-9]) ;;
  *) echo "The scale must look like 1.3." >&2; exit 1 ;;
esac
adbe shell settings put system font_scale "$scale"
sleep 1
