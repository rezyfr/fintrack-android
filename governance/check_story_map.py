#!/usr/bin/env python3
"""
G7: Story-map traceability gate.

Verifies the full link graph across product/manifest/:
  capability.stories[]   -> every id resolves to product/manifest/stories/<id>.json
  capability.flows[]     -> every id resolves to product/manifest/flows/<id>.json
  story.capability       -> resolves AND is listed in that capability's stories[]
  flow.steps[].story     -> resolves to a known story id
  tasks/task.json.storyId-> resolves to a known story id

Also reports orphans: stories not referenced by any capability, flows not
referenced by any capability.

Exit 0 on clean graph, 1 on any broken link or orphan.
"""

from __future__ import annotations

import json
import sys
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
MANIFEST = REPO_ROOT / "product" / "manifest"
CAPS_DIR = MANIFEST / "capabilities"
STORIES_DIR = MANIFEST / "stories"
FLOWS_DIR = MANIFEST / "flows"
TASK_FILE = REPO_ROOT / "tasks" / "task.json"


def load_json(path: pathlib.Path):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        return ("__parse_error__", str(e))


def main() -> int:
    if not MANIFEST.exists():
        print(f"G7 FAIL: manifest dir missing at {MANIFEST.relative_to(REPO_ROOT)}")
        return 1

    cap_files = sorted(CAPS_DIR.glob("*.json")) if CAPS_DIR.exists() else []
    story_files = sorted(STORIES_DIR.glob("*.json")) if STORIES_DIR.exists() else []
    flow_files = sorted(FLOWS_DIR.glob("*.json")) if FLOWS_DIR.exists() else []

    known_caps   = {f.stem for f in cap_files}
    known_stories= {f.stem for f in story_files}
    known_flows  = {f.stem for f in flow_files}

    violations: list[str] = []

    # Track which stories/flows each capability references so we can detect orphans.
    referenced_stories: set[str] = set()
    referenced_flows: set[str] = set()

    # Capability ->  (stories[], flows[])
    cap_data: dict[str, dict] = {}

    for f in cap_files:
        data = load_json(f)
        if isinstance(data, tuple):
            violations.append(f"{f.relative_to(REPO_ROOT)}: parse error: {data[1]}")
            continue
        cap_id = data.get("id") or f.stem
        cap_data[cap_id] = data

        for sid in data.get("stories", []):
            referenced_stories.add(sid)
            if sid not in known_stories:
                violations.append(
                    f"capabilities/{f.name}: stories[] references unknown story '{sid}'"
                )
        for fid in data.get("flows", []):
            referenced_flows.add(fid)
            if fid not in known_flows:
                violations.append(
                    f"capabilities/{f.name}: flows[] references unknown flow '{fid}'"
                )

    # Stories must reference a real capability AND be listed in that capability's stories[].
    for f in story_files:
        data = load_json(f)
        if isinstance(data, tuple):
            violations.append(f"{f.relative_to(REPO_ROOT)}: parse error: {data[1]}")
            continue
        sid = data.get("id") or f.stem
        cap_id = data.get("capability")
        if not cap_id:
            violations.append(f"stories/{f.name}: missing 'capability'")
            continue
        if cap_id not in known_caps:
            violations.append(
                f"stories/{f.name}: capability '{cap_id}' does not resolve"
            )
            continue
        cap_stories = cap_data.get(cap_id, {}).get("stories", [])
        if sid not in cap_stories:
            violations.append(
                f"stories/{f.name}: not listed in capability '{cap_id}'.stories[]"
            )

    # Flows: every step's `story` (when present) must resolve.
    for f in flow_files:
        data = load_json(f)
        if isinstance(data, tuple):
            violations.append(f"{f.relative_to(REPO_ROOT)}: parse error: {data[1]}")
            continue
        for i, step in enumerate(data.get("steps", [])):
            ref = step.get("story")
            if ref and ref not in known_stories:
                violations.append(
                    f"flows/{f.name}: step[{i}].story '{ref}' does not resolve"
                )

    # Orphans.
    orphan_stories = sorted(known_stories - referenced_stories)
    for sid in orphan_stories:
        violations.append(f"stories/{sid}.json: orphan — no capability references it")
    orphan_flows = sorted(known_flows - referenced_flows)
    for fid in orphan_flows:
        violations.append(f"flows/{fid}.json: orphan — no capability references it")

    # task.json.storyId must resolve.
    if TASK_FILE.exists():
        task = load_json(TASK_FILE)
        if isinstance(task, tuple):
            violations.append(f"tasks/task.json: parse error: {task[1]}")
        else:
            sid = task.get("storyId")
            if not sid:
                violations.append("tasks/task.json: missing 'storyId'")
            elif sid not in known_stories:
                violations.append(
                    f"tasks/task.json: storyId '{sid}' does not resolve"
                )
    else:
        violations.append("tasks/task.json: missing")

    if violations:
        print(f"G7 FAIL: {len(violations)} traceability issue(s)\n")
        for v in violations:
            print(f"  - {v}")
        return 1

    print(
        f"G7 PASS: {len(known_caps)} capability/-ies, "
        f"{len(known_stories)} story/-ies, {len(known_flows)} flow(s); "
        f"graph clean"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
