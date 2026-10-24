#!/usr/bin/env bash
# Turn the notification listener of the app on or off. Runs on the emulator only.
# Usage: set_listener.sh on|off
set -eu
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
listener="$APP_ID/$APP_ID.service.BankNotificationService"
case "${1:?on or off}" in
  on)  adbe shell cmd notification allow_listener "$listener" ;;
  off) adbe shell cmd notification disallow_listener "$listener" ;;
  *) echo "Use on or off." >&2; exit 1 ;;
esac
sleep 1
