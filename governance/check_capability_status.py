#!/usr/bin/env python3
"""
G0: tasks/task.json must reference a story whose capability is Approved or In Progress.

Blocks commits that target a capability in Draft / Unresolved / Done. Done is
treated as a violation because re-opening a shipped capability requires
explicit promotion back to In Progress (audit trail).
"""

from __future__ import annotations

import json
import sys
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
TASK_FILE = REPO_ROOT / "tasks" / "task.json"
STORIES_DIR = REPO_ROOT / "product" / "manifest" / "stories"
CAPS_DIR = REPO_ROOT / "product" / "manifest" / "capabilities"

ALLOWED_STATUSES = {"Approved", "In Progress"}


def fail(msg: str) -> int:
    print(f"G0 FAIL: {msg}")
    return 1


def main() -> int:
    if not TASK_FILE.exists():
        return fail(f"missing {TASK_FILE.relative_to(REPO_ROOT)}")

    try:
        task = json.loads(TASK_FILE.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return fail(f"{TASK_FILE.relative_to(REPO_ROOT)} is not valid JSON: {e}")

    story_id = task.get("storyId")
    if not story_id:
        return fail("task.json is missing 'storyId'")

    story_file = STORIES_DIR / f"{story_id}.json"
    if not story_file.exists():
        return fail(
            f"task.storyId '{story_id}' does not resolve to "
            f"{story_file.relative_to(REPO_ROOT)}"
        )

    try:
        story = json.loads(story_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return fail(f"{story_file.relative_to(REPO_ROOT)} is not valid JSON: {e}")

    cap_id = story.get("capability")
    if not cap_id:
        return fail(f"story '{story_id}' is missing 'capability'")

    cap_file = CAPS_DIR / f"{cap_id}.json"
    if not cap_file.exists():
        return fail(
            f"story.capability '{cap_id}' does not resolve to "
            f"{cap_file.relative_to(REPO_ROOT)}"
        )

    try:
        cap = json.loads(cap_file.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return fail(f"{cap_file.relative_to(REPO_ROOT)} is not valid JSON: {e}")

    status = cap.get("status")
    if status not in ALLOWED_STATUSES:
        return fail(
            f"capability '{cap_id}' status is '{status}'; "
            f"must be one of {sorted(ALLOWED_STATUSES)}"
        )

    print(
        f"G0 PASS: task->story '{story_id}' -> capability '{cap_id}' (status={status})"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
