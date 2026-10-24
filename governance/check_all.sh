#!/usr/bin/env bash
# Runs every contributor gate in order. Exit 0 if all pass, 1 if any fail.
# CI runs the same script (.github/workflows/ci.yml).
# Add new gates to the GATES array below.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GATES=(
  "G1   architecture       check_architecture.py"
  "G3   android-tokens     check_android_tokens.py"
  "G-TYPE type-scale       check_type_scale.py"
  "G-STR strings           check_strings.py"
  "G-ARCH solid-arch       check_architecture_solid.py"
  "G-DB room-migration     check_room_migration.py"
  "G1-W web-architecture   check_architecture_web.py"
  "G-NEUTRAL neutrality    check_neutrality.py"
  "G-SENSITIVE sensitive   check_sensitive.py"
  "G-BUILD android-compile check_build.sh"
  "G-FMT format            check_format.sh"
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

if [[ ${#failed_ids[@]} -gt 0 ]]; then
  exit 1
fi
echo "ALL GATES PASS"
