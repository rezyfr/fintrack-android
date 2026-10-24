#!/usr/bin/env bash
# Post a notification as if a bank app sent it. Runs on the emulator only.
# Usage: post_notification.sh "<title>" "<text>" [tag]
# Example: post_notification.sh "myBCA" "You spent IDR 45,000 at Food and Beverage"
set -eu
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
title="${1:?title missing}"
text="${2:?text missing}"
# One fixed tag replaces the earlier notification. Android drops new posts from a package after 50 of them.
tag="${3:-e2e}"
listener="$APP_ID/$APP_ID.service.BankNotificationService"

adbe shell cmd notification allow_listener "$listener" >/dev/null
# adb joins its arguments and the phone shell splits them again, so quote each value for the phone shell.
q() { printf "'%s'" "$(printf '%s' "$1" | sed "s/'/'\\\\''/g")"; }
adbe shell "cmd notification post -S bigtext -t $(q "$title") $(q "$tag") $(q "$text")"
# The listener reads the notification in its own thread. Give it a moment before the next step.
sleep 2
