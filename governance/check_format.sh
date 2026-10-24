#!/usr/bin/env bash
# G-FMT: Android Kotlin source must be Spotless-clean.
#
# Runs `./gradlew :app:spotlessCheck` and exits 0 on success, 1 on any
# formatting violation. Fix with `./gradlew :app:spotlessApply`.
#
# Scope: indentation, trailing whitespace, spacing, and a handful of
# blank-line rules — see .editorconfig and the editorConfigOverride in
# app/build.gradle.kts for exactly which ktlint rules are enabled. This is
# deliberately narrower than ktlint's full default ruleset (no forced
# trailing commas, no forced line-wrapping, no naming-convention checks);
# see G-FMT in governance/docs/conventions.md for why.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GRADLEW="${REPO_ROOT}/gradlew"

if [[ ! -x "$GRADLEW" ]]; then
  echo "G-FMT FAIL: gradlew not found at ${GRADLEW}"
  exit 1
fi

LOG=$(mktemp -t check_format.XXXXXX)
trap 'rm -f "$LOG"' EXIT

if (cd "$REPO_ROOT" && ./gradlew :app:spotlessCheck --no-daemon -q) > "$LOG" 2>&1; then
  echo "G-FMT PASS: :app:spotlessCheck succeeded"
  exit 0
fi

echo "G-FMT FAIL: :app:spotlessCheck failed"
echo
head -60 "$LOG"
echo
echo "  Fix with:"
echo "    ./gradlew :app:spotlessApply"
exit 1
