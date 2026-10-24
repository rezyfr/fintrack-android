#!/usr/bin/env bash
# G-BUILD: the Android module must compile.
#
# Structural sensors don't catch compile errors. This gate runs
# `./gradlew :app:compileDebugKotlin` and exits 0 on success, 1 on any
# Kotlin compilation error.
#
# Compile-only (not assembleDebug) for speed: typically 3-15 seconds when
# cached vs 30+ seconds for a full assemble.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GRADLEW="${REPO_ROOT}/gradlew"

if [[ ! -x "$GRADLEW" ]]; then
  echo "G-BUILD FAIL: gradlew not found at ${GRADLEW}"
  exit 1
fi

LOG=$(mktemp -t check_build.XXXXXX)
trap 'rm -f "$LOG"' EXIT

if (cd "$REPO_ROOT" && ./gradlew :app:compileDebugKotlin --no-daemon -q) > "$LOG" 2>&1; then
  echo "G-BUILD PASS: :app:compileDebugKotlin succeeded"
  exit 0
fi

echo "G-BUILD FAIL: :app:compileDebugKotlin failed"
echo
# Surface only the Kotlin compiler error lines so the agent sees what to fix.
grep -E '^(e:|w:|FAILURE|Caused by|\* What went wrong|error:)' "$LOG" | head -40
echo
echo "  Full log saved during run; re-run manually for context:"
echo "    ./gradlew :app:compileDebugKotlin"
exit 1
