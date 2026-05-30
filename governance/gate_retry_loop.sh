#!/usr/bin/env bash
# 3-strike retry harness per factory-blueprint Phase 2.4.
#
# Usage:
#   LOOP_RESET=yes bash governance/gate_retry_loop.sh   # start a new cycle
#   bash governance/gate_retry_loop.sh                  # continue cycle
#
# Exit codes:
#   0  all gates green on this attempt
#   1  gates failed; cycle still has attempts left — fix and re-run
#   2  UNRESOLVED — exhausted MAX_ATTEMPTS; tech-lead handoff required
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
STATE_FILE="${SCRIPT_DIR}/.gate-loop-state"
MAX_ATTEMPTS=3

if [[ "${LOOP_RESET:-}" == "yes" ]]; then
  echo 0 > "$STATE_FILE"
fi

attempt=$(cat "$STATE_FILE" 2>/dev/null || echo 0)
attempt=$((attempt + 1))
echo "$attempt" > "$STATE_FILE"

echo "── Gate attempt ${attempt} / ${MAX_ATTEMPTS} ──"
if bash "${SCRIPT_DIR}/check_all.sh"; then
  echo "PASS — all gates green on attempt ${attempt}"
  rm -f "$STATE_FILE"
  exit 0
fi

if [[ $attempt -ge $MAX_ATTEMPTS ]]; then
  echo "UNRESOLVED — gates still red after ${MAX_ATTEMPTS} attempts."
  echo "             Escalate: write a decision record under governance/decisions/"
  echo "             and reset with: LOOP_RESET=yes bash governance/gate_retry_loop.sh"
  rm -f "$STATE_FILE"
  exit 2
fi

remaining=$((MAX_ATTEMPTS - attempt))
echo "FAIL — gate(s) red; ${remaining} attempt(s) remaining."
echo "       Self-correct, then re-run: bash governance/gate_retry_loop.sh"
exit 1
