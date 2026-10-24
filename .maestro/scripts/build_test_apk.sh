#!/usr/bin/env bash
# Build the debug APK against the fake server. The real settings are restored at the end, also on failure.
set -u
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
trap restore_config EXIT INT TERM
swap_config
cd "$REPO_ROOT" || exit 1
./gradlew :app:assembleDebug -q
status=$?
restore_config
exit $status
