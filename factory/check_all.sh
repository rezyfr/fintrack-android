#!/usr/bin/env bash
# Opt-in agent harness: runs the contributor gates (governance/) and then the manifest gates (factory/).
# Exit 0 if all pass, 1 if any fail. Appends one JSON line to factory/gate-log.jsonl per run.
# Add new manifest gates to the GATES array below.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
GATE_LOG="${SCRIPT_DIR}/gate-log.jsonl"

# Same order as before the harness was split: the manifest gates run first.
GATES=(
  "G0   capability-status  factory/check_capability_status.py"
  "G1   architecture       governance/check_architecture.py"
  "G3   android-tokens     governance/check_android_tokens.py"
  "G-STR strings           governance/check_strings.py"
  "G-ARCH solid-arch       governance/check_architecture_solid.py"
  "G-DB room-migration     governance/check_room_migration.py"
  "G7   story-map          factory/check_story_map.py"
  "G16  flow-integrity     factory/check_flow_integrity.py"
  "G31  ac-annotations     factory/check_ac_annotations.py"
  "G1-W web-architecture   governance/check_architecture_web.py"
  "G-NAME naming           factory/check_naming.py"
  "G-REG registry          factory/check_registry.py"
  "G-BUILD android-compile governance/check_build.sh"
  "G-FMT format            governance/check_format.sh"
)

passed_ids=()
failed_ids=()

for entry in "${GATES[@]}"; do
  read -r id name rel <<<"$entry"
  path="${REPO_ROOT}/${rel}"
  script="$(basename "$rel")"

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
