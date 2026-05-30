#!/usr/bin/env bash
# Runs every gate in order. Exit 0 if all pass, 1 if any fail.
# Add new gates to the GATES array below.
set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

GATES=(
  "G1   architecture       check_architecture.py"
  "G3   android-tokens     check_android_tokens.py"
  "G-STR strings           check_strings.py"
  "G-ARCH solid-arch       check_architecture_solid.py"
  "G-DB room-migration     check_room_migration.py"
)

passed=()
failed=()

for entry in "${GATES[@]}"; do
  id=$(awk '{print $1}' <<<"$entry")
  name=$(awk '{print $2}' <<<"$entry")
  script=$(awk '{print $3}' <<<"$entry")
  path="${SCRIPT_DIR}/${script}"

  if [[ ! -f "$path" ]]; then
    echo "── ${id} ${name} ── SKIP (missing ${script})"
    failed+=("${id} ${name} (missing script)")
    continue
  fi

  echo "── ${id} ${name} ──"
  if python3 "$path"; then
    passed+=("${id} ${name}")
  else
    failed+=("${id} ${name}")
  fi
  echo
done

echo "════════════════════════════════════════"
echo "Passed: ${#passed[@]}"
for g in "${passed[@]}"; do echo "  PASS  ${g}"; done
echo "Failed: ${#failed[@]}"
for g in "${failed[@]}"; do echo "  FAIL  ${g}"; done
echo "════════════════════════════════════════"

if [[ ${#failed[@]} -gt 0 ]]; then
  exit 1
fi
echo "ALL GATES PASS"
exit 0
