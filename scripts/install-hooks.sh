#!/usr/bin/env bash
# Copy scripts/hooks/* into .git/hooks/. Run once per fresh clone.
set -e

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOKS_SRC="${REPO_ROOT}/scripts/hooks"
HOOKS_DST="${REPO_ROOT}/.git/hooks"

if [[ ! -d "$HOOKS_SRC" ]]; then
  echo "scripts/hooks/ not found at ${HOOKS_SRC}" >&2
  exit 1
fi

installed=0
for src in "$HOOKS_SRC"/*; do
  [[ -f "$src" ]] || continue
  name=$(basename "$src")
  cp "$src" "${HOOKS_DST}/${name}"
  chmod +x "${HOOKS_DST}/${name}"
  echo "Installed: ${name}"
  installed=$((installed + 1))
done

if [[ $installed -eq 0 ]]; then
  echo "No hooks found in ${HOOKS_SRC}" >&2
  exit 1
fi
echo "Done. ${installed} hook(s) installed into ${HOOKS_DST}"
