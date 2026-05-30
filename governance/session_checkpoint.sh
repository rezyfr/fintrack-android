#!/usr/bin/env bash
# Save mid-feature session state to governance/.session-checkpoint.json.
# Usage: bash governance/session_checkpoint.sh ["free-text note"]
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CHECKPOINT="${SCRIPT_DIR}/.session-checkpoint.json"
TASK_FILE="${REPO_ROOT}/tasks/task.json"
GATE_LOG="${SCRIPT_DIR}/gate-log.jsonl"

note="${1:-}"
ts=$(date -u +%FT%TZ)

branch=$(git -C "$REPO_ROOT" rev-parse --abbrev-ref HEAD 2>/dev/null || echo "")
head_sha=$(git -C "$REPO_ROOT" rev-parse --short HEAD 2>/dev/null || echo "")
dirty_count=$(git -C "$REPO_ROOT" status --porcelain 2>/dev/null | wc -l | tr -d ' ')
dirty_count=${dirty_count:-0}

if [[ -f "$TASK_FILE" ]]; then
  task_feature=$(jq -r '.feature // ""' "$TASK_FILE")
  task_story=$(jq -r '.storyId // ""' "$TASK_FILE")
else
  task_feature=""
  task_story=""
fi

if [[ -f "$GATE_LOG" && -s "$GATE_LOG" ]]; then
  last_gates=$(tail -n 1 "$GATE_LOG")
else
  last_gates="null"
fi

jq -n \
  --arg ts "$ts" \
  --arg branch "$branch" \
  --arg head_sha "$head_sha" \
  --argjson dirty_files "$dirty_count" \
  --arg task_feature "$task_feature" \
  --arg task_story "$task_story" \
  --argjson last_gates "$last_gates" \
  --arg note "$note" \
  '{
     ts: $ts,
     git: { branch: $branch, head_sha: $head_sha, dirty_files: $dirty_files },
     task: {
       feature: (if $task_feature == "" then null else $task_feature end),
       storyId: (if $task_story   == "" then null else $task_story   end)
     },
     last_gates: $last_gates,
     note: (if $note == "" then null else $note end)
   }' > "$CHECKPOINT"

echo "Checkpoint saved → ${CHECKPOINT#$REPO_ROOT/}"
[[ -n "$note" ]] && echo "  note: $note"
