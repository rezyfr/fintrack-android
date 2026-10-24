#!/usr/bin/env bash
# Turn airplane mode on or off. Runs on the emulator only.
# Usage: set_airplane.sh on|off
set -eu
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
case "${1:?on or off}" in
  on)  adbe shell cmd connectivity airplane-mode enable ;;
  off) adbe shell cmd connectivity airplane-mode disable ;;
  *) echo "Use on or off." >&2; exit 1 ;;
esac
sleep 2
