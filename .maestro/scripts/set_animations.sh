#!/usr/bin/env bash
# Turn system animations off or back on. Runs on the emulator only.
# Usage: set_animations.sh off|on
set -eu
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
case "${1:?on or off}" in
  off) v=0 ;;
  on)  v=1 ;;
  *) echo "Use on or off." >&2; exit 1 ;;
esac
for key in window_animation_scale transition_animation_scale animator_duration_scale; do
  adbe shell settings put global "$key" "$v"
done
