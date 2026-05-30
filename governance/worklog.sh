#!/usr/bin/env bash
# Append a worklog entry per factory-blueprint Phase 5.
#
# Usage:
#   bash governance/worklog.sh task
#     -> appends {"type":"task", ...} auto-filled from tasks/task.json and
#        the last line of governance/gate-log.jsonl
#
#   bash governance/worklog.sh retro <attempts> <escalations> "<notes>"
#     -> appends {"type":"retrospective", ...} with the given counts and notes
#
# Both entry types are paired per session — gate W8 (future) will require it.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
WORKLOG="${SCRIPT_DIR}/worklog.jsonl"
TASK_FILE="${REPO_ROOT}/tasks/task.json"
GATE_LOG="${SCRIPT_DIR}/gate-log.jsonl"

usage() {
  cat <<'EOF'
usage:
  worklog.sh task
  worklog.sh retro <attempts> <escalations> "<notes>"
EOF
  exit 2
}

subcmd="${1:-}"
case "$subcmd" in
  task)
    if [[ ! -f "$TASK_FILE" ]]; then
      echo "worklog.sh task: tasks/task.json not found" >&2
      exit 1
    fi
    feature=$(jq -r '.feature // ""' "$TASK_FILE")
    story=$(jq -r '.storyId // ""' "$TASK_FILE")
    if [[ -f "$GATE_LOG" && -s "$GATE_LOG" ]]; then
      gate_results=$(tail -n 1 "$GATE_LOG")
    else
      gate_results="null"
    fi
    ts=$(date -u +%FT%TZ)
    jq -c -n \
      --arg ts "$ts" \
      --arg feature "$feature" \
      --arg story "$story" \
      --argjson gate_results "$gate_results" \
      '{ts:$ts, type:"task", feature:$feature, storyId:$story, gateResults:$gate_results}' \
      >> "$WORKLOG"
    echo "Worklog (task) appended → ${WORKLOG#$REPO_ROOT/}"
    ;;
  retro)
    [[ $# -ge 4 ]] || usage
    attempts="$2"
    escalations="$3"
    notes="$4"
    # Validate integers.
    [[ "$attempts" =~ ^[0-9]+$ ]] || { echo "attempts must be integer" >&2; exit 2; }
    [[ "$escalations" =~ ^[0-9]+$ ]] || { echo "escalations must be integer" >&2; exit 2; }
    if [[ -f "$TASK_FILE" ]]; then
      feature=$(jq -r '.feature // ""' "$TASK_FILE")
    else
      feature=""
    fi
    ts=$(date -u +%FT%TZ)
    jq -c -n \
      --arg ts "$ts" \
      --arg feature "$feature" \
      --argjson attempts "$attempts" \
      --argjson escalations "$escalations" \
      --arg notes "$notes" \
      '{ts:$ts, type:"retrospective", feature:$feature, attempts:$attempts, escalations:$escalations, notes:$notes}' \
      >> "$WORKLOG"
    echo "Worklog (retro) appended → ${WORKLOG#$REPO_ROOT/}"
    ;;
  ""|-h|--help)
    usage
    ;;
  *)
    echo "unknown subcommand: $subcmd" >&2
    usage
    ;;
esac
