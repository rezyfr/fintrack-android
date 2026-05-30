#!/usr/bin/env python3
"""
G16: Flow integrity gate.

For every flow file under product/manifest/flows/:
  - Each step with actor=="app" must declare either `noNavigation: true`
    or `requires: <page-id>`. (One of them, not neither.)
  - Each `triggerElement:` value, if present, must name a Kotlin
    class/object/interface that exists somewhere under
    app/src/main/java/com/fidriyanto/banktracker/.

A missing triggerElement on an app step is allowed; it only matters when
present.

Exit 0 on clean flows, 1 on any violation.
"""

from __future__ import annotations

import json
import re
import sys
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
FLOWS_DIR = REPO_ROOT / "product" / "manifest" / "flows"
APP_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

CLASS_DECL = re.compile(
    r"^\s*(?:(?:public|private|internal|open|final|abstract|sealed|data|enum)\s+)*"
    r"(?:class|object|interface)\s+(\w+)\b",
    re.MULTILINE,
)

# Top-level Kotlin function: starts at column 0 with optional visibility / inline / suspend
# modifiers. Captures Compose @Composable fun X(...) and any other top-level fun X(...).
# Annotations (e.g. @Composable) live on the preceding line so they don't break this match.
FUN_DECL = re.compile(
    r"^(?:(?:public|private|internal|inline|suspend)\s+)*fun\s+(\w+)\s*\(",
    re.MULTILINE,
)


def collect_app_class_names() -> set[str]:
    names: set[str] = set()
    if not APP_ROOT.exists():
        return names
    for kt in APP_ROOT.rglob("*.kt"):
        if any(seg in kt.parts for seg in ("test", "androidTest")):
            continue
        text = kt.read_text(encoding="utf-8", errors="ignore")
        for m in CLASS_DECL.finditer(text):
            names.add(m.group(1))
        for m in FUN_DECL.finditer(text):
            names.add(m.group(1))
    return names


def main() -> int:
    if not FLOWS_DIR.exists():
        print(f"G16 FAIL: flows dir missing at {FLOWS_DIR.relative_to(REPO_ROOT)}")
        return 1

    known_classes = collect_app_class_names()
    violations: list[str] = []

    for f in sorted(FLOWS_DIR.glob("*.json")):
        try:
            data = json.loads(f.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            violations.append(f"{f.relative_to(REPO_ROOT)}: parse error: {e}")
            continue

        steps = data.get("steps", [])
        for i, step in enumerate(steps):
            actor = step.get("actor")
            if actor != "app":
                continue
            has_nav = ("requires" in step) or (step.get("noNavigation") is True)
            if not has_nav:
                violations.append(
                    f"flows/{f.name}: step[{i}] actor=app must declare "
                    f"'requires:' or 'noNavigation: true'"
                )
            te = step.get("triggerElement")
            if te and te not in known_classes:
                violations.append(
                    f"flows/{f.name}: step[{i}].triggerElement '{te}' "
                    f"does not exist as a class / object / interface / top-level fun under app/"
                )

    if violations:
        print(f"G16 FAIL: {len(violations)} flow integrity issue(s)\n")
        for v in violations:
            print(f"  - {v}")
        return 1

    print(
        f"G16 PASS: {len(list(FLOWS_DIR.glob('*.json')))} flow(s) clean "
        f"(known classes scanned: {len(known_classes)})"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
