#!/usr/bin/env python3
"""
G-NAME: Naming convention sensor (manifest IDs + task.feature alignment).

R-NAME-1  Every file under product/manifest/{capabilities,stories,flows}/
          must have a kebab-case stem (^[a-z][a-z0-9-]*[a-z0-9]$) and its
          JSON `id` field must equal that stem.

R-NAME-2  tasks/task.json.feature must equal capability.id with hyphens
          replaced by underscores (kebab -> snake), where the capability is
          reached via task.storyId -> story.capability. Also requires
          task.feature itself to be snake_case
          (^[a-z][a-z0-9_]*[a-z0-9]$).

Other naming conventions (resource keys, file suffixes, Kotlin classes)
are documented in governance/docs/conventions.md but not enforced here.
Promote rules into G-NAME as failures appear in real runs.

Exit 0 on pass, 1 on any violation.
"""

from __future__ import annotations

import json
import re
import sys
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
MANIFEST = REPO_ROOT / "product" / "manifest"
TASK_FILE = REPO_ROOT / "tasks" / "task.json"

KEBAB_RE = re.compile(r"^[a-z][a-z0-9-]*[a-z0-9]$")
SNAKE_RE = re.compile(r"^[a-z][a-z0-9_]*[a-z0-9]$")

MANIFEST_DIRS = ("capabilities", "stories", "flows")


def load_json(path: pathlib.Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return ("__parse_error__", str(e))


def check_manifest_ids(violations: list) -> None:
    for sub in MANIFEST_DIRS:
        d = MANIFEST / sub
        if not d.exists():
            continue
        for f in sorted(d.glob("*.json")):
            stem = f.stem
            if not KEBAB_RE.match(stem):
                violations.append(
                    f"{f.relative_to(REPO_ROOT)}: file stem '{stem}' is not "
                    f"kebab-case (^[a-z][a-z0-9-]*[a-z0-9]$)"
                )
                continue
            data = load_json(f)
            if isinstance(data, tuple):
                violations.append(
                    f"{f.relative_to(REPO_ROOT)}: parse error: {data[1]}"
                )
                continue
            jid = data.get("id")
            if jid != stem:
                violations.append(
                    f"{f.relative_to(REPO_ROOT)}: 'id' field is "
                    f"'{jid}', expected '{stem}' (must match file stem)"
                )


def check_task_feature_alignment(violations: list) -> None:
    if not TASK_FILE.exists():
        return  # G0 / G7 own that failure
    task = load_json(TASK_FILE)
    if isinstance(task, tuple):
        return  # G0 / G7 own parse errors

    feature = task.get("feature")
    story_id = task.get("storyId")
    if not feature or not story_id:
        return  # missing fields are G0's problem

    if not SNAKE_RE.match(feature):
        violations.append(
            f"tasks/task.json: feature '{feature}' is not snake_case "
            f"(^[a-z][a-z0-9_]*[a-z0-9]$)"
        )

    story_path = MANIFEST / "stories" / f"{story_id}.json"
    if not story_path.exists():
        return  # G0 / G7 own this
    story = load_json(story_path)
    if isinstance(story, tuple):
        return
    cap_id = story.get("capability")
    if not cap_id:
        return

    expected_feature = cap_id.replace("-", "_")
    if feature != expected_feature:
        violations.append(
            f"tasks/task.json: feature '{feature}' does not match "
            f"capability '{cap_id}' (expected '{expected_feature}' — "
            f"snake_case of the kebab-case capability id)"
        )


def main() -> int:
    if not MANIFEST.exists():
        print(f"G-NAME FAIL: manifest dir missing at {MANIFEST.relative_to(REPO_ROOT)}")
        return 1

    violations: list = []
    check_manifest_ids(violations)
    check_task_feature_alignment(violations)

    if violations:
        print(f"G-NAME FAIL: {len(violations)} naming violation(s)\n")
        for v in violations:
            print(f"  - {v}")
        return 1

    print("G-NAME PASS: manifest IDs kebab-case, task.feature aligned with capability")
    return 0


if __name__ == "__main__":
    sys.exit(main())
