#!/usr/bin/env bash
# Runs every gate in order. Exit 0 if all pass, 1 if any fail.
# Append one JSON line to governance/gate-log.jsonl per run.
# Add new gates to the GATES array below.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GATE_LOG="${SCRIPT_DIR}/gate-log.jsonl"

GATES=(
  "G0   capability-status  check_capability_status.py"
  "G1   architecture       check_architecture.py"
  "G3   android-tokens     check_android_tokens.py"
  "G-STR strings           check_strings.py"
  "G-ARCH solid-arch       check_architecture_solid.py"
  "G-DB room-migration     check_room_migration.py"
  "G7   story-map          check_story_map.py"
  "G16  flow-integrity     check_flow_integrity.py"
  "G31  ac-annotations     check_ac_annotations.py"
  "G1-W web-architecture   check_architecture_web.py"
  "G-NAME naming           check_naming.py"
  "G-REG registry          check_registry.py"
  "G-BUILD android-compile check_build.sh"
)

passed_ids=()
failed_ids=()

for entry in "${GATES[@]}"; do
  read -r id name script <<<"$entry"
  path="${SCRIPT_DIR}/${script}"

  if [[ ! -f "$path" ]]; then
    echo "── ${id} ${name} ── SKIP (missing ${script})"
    failed_ids+=("$id")
    continue
  fi

  echo "── ${id} ${name} ──"
  case "$script" in
    *.py) runner=(python3 "$path") ;;
    *.sh) runner=(bash "$path") ;;
    *)
      echo "  UNSUPPORTED runner for ${script}"
      failed_ids+=("$id")
      echo
      continue
      ;;
  esac
  if "${runner[@]}"; then
    passed_ids+=("$id")
  else
    failed_ids+=("$id")
  fi
  echo
done

echo "════════════════════════════════════════"
echo "Passed: ${#passed_ids[@]}"
for g in "${passed_ids[@]}"; do echo "  PASS  ${g}"; done
echo "Failed: ${#failed_ids[@]}"
for g in "${failed_ids[@]}"; do echo "  FAIL  ${g}"; done
echo "════════════════════════════════════════"

exit_code=0
[[ ${#failed_ids[@]} -gt 0 ]] && exit_code=1

# Append one-line audit entry. Empty arrays serialise as [] cleanly.
ts=$(date -u +%FT%TZ)
passed_json=$(printf '%s\n' "${passed_ids[@]:-}" | jq -R . | jq -sc 'map(select(length>0))')
failed_json=$(printf '%s\n' "${failed_ids[@]:-}" | jq -R . | jq -sc 'map(select(length>0))')
jq -c -n \
  --arg ts "$ts" \
  --argjson exit "$exit_code" \
  --argjson passed "$passed_json" \
  --argjson failed "$failed_json" \
  '{ts:$ts, exit:$exit, passed:$passed, failed:$failed}' \
  >> "$GATE_LOG"

[[ $exit_code -eq 0 ]] && echo "ALL GATES PASS"
exit $exit_code
