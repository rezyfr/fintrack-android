#!/usr/bin/env bash
# Print session-resume info: checkpoint + task brief + drift check + gate log + git status.
# Exit 0 always. Informational only.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
CHECKPOINT="${SCRIPT_DIR}/.session-checkpoint.json"
TASK_FILE="${REPO_ROOT}/tasks/task.json"
GATE_LOG="${SCRIPT_DIR}/gate-log.jsonl"

cp_story=""
cur_story=""

echo "── 1. Checkpoint ──"
if [[ -f "$CHECKPOINT" ]]; then
  jq -r '
    "  ts:       \(.ts)\n" +
    "  branch:   \(.git.branch)  HEAD \(.git.head_sha)  (\(.git.dirty_files) dirty file(s))\n" +
    "  task:     \(.task.feature // "—") / \(.task.storyId // "—")\n" +
    "  note:     \(.note // "—")"
  ' "$CHECKPOINT"
  cp_story=$(jq -r '.task.storyId // ""' "$CHECKPOINT")
else
  echo "  (no checkpoint yet)"
fi
echo

echo "── 2. Task brief ──"
if [[ -f "$TASK_FILE" ]]; then
  jq -r '
    "  feature:  \(.feature)\n" +
    "  storyId:  \(.storyId)\n" +
    "  first AC: \(.acceptanceCriteria[0] // "—")"
  ' "$TASK_FILE"
  cur_story=$(jq -r '.storyId // ""' "$TASK_FILE")
else
  echo "  (no tasks/task.json)"
fi
echo

echo "── 3. Task drift ──"
if [[ -n "$cp_story" && -n "$cur_story" && "$cp_story" != "$cur_story" ]]; then
  echo "  WARN: task changed since checkpoint (was '$cp_story', now '$cur_story')"
elif [[ -n "$cp_story" && -n "$cur_story" ]]; then
  echo "  OK: task unchanged ($cur_story)"
else
  echo "  (skipped — no checkpoint or task to compare)"
fi
echo

echo "── 4. Gate log (last 5) ──"
if [[ -f "$GATE_LOG" && -s "$GATE_LOG" ]]; then
  tail -n 5 "$GATE_LOG" \
    | jq -r '"  \(.ts)  exit=\(.exit)  failed=\(.failed | tojson)"'
else
  echo "  (no gate-log yet)"
fi
echo

echo "── 5. Git status ──"
status_out=$(git -C "$REPO_ROOT" status --short)
if [[ -z "$status_out" ]]; then
  echo "  (working tree clean)"
else
  echo "$status_out" | head -n 20 | sed 's/^/  /'
fi

exit 0
