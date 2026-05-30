#!/usr/bin/env python3
"""
G31: Acceptance-criteria annotation gate.

For every story under product/manifest/stories/, the number of
  // ac: <story-id>             (Kotlin / JS line comment)
  /* ac: <story-id> */          (JSX block comment, also matches the {/* */} form)
annotations found across app/src/**.kt AND web/src/**.{js,jsx,ts,tsx}
must be >= len(acceptance[]).

Each acceptance line is meant to map to at least one annotation. The
sensor counts presence, not per-line identity — that's enough to force
the developer to wire each AC through implementation without forcing a
brittle 1:1 string match.

Annotations may live on either platform (Android, Web) — a feature shipped
on both can satisfy its ACs from either codebase.

Exit 0 on satisfied counts, 1 if any story is under-annotated.
"""

from __future__ import annotations

import json
import re
import sys
import collections
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
STORIES_DIR = REPO_ROOT / "product" / "manifest" / "stories"
APP_SRC = REPO_ROOT / "app" / "src"
WEB_SRC = REPO_ROOT / "web" / "src"

# Match both `// ac: id` and `/* ac: id` (covers `{/* ac: id */}` in JSX too).
ANNOTATION_RE = re.compile(r"(?://|/\*)\s*ac:\s*([A-Za-z0-9_-]+)")

WEB_EXTS = (".js", ".jsx", ".ts", ".tsx")


def scan_dir(root: pathlib.Path, exts: tuple, counts: collections.Counter) -> int:
    """Walk root, counting AC annotations per story id. Returns file count scanned."""
    n = 0
    if not root.exists():
        return n
    for ext in exts:
        for f in root.rglob(f"*{ext}"):
            # Skip dependencies and build outputs.
            if any(seg in f.parts for seg in ("node_modules", "dist", "build")):
                continue
            n += 1
            text = f.read_text(encoding="utf-8", errors="ignore")
            for m in ANNOTATION_RE.finditer(text):
                counts[m.group(1)] += 1
    return n


def main() -> int:
    if not STORIES_DIR.exists():
        print(f"G31 FAIL: stories dir missing at {STORIES_DIR.relative_to(REPO_ROOT)}")
        return 1

    counts: collections.Counter = collections.Counter()
    kt_count  = scan_dir(APP_SRC, (".kt",),  counts)
    web_count = scan_dir(WEB_SRC, WEB_EXTS,  counts)

    if kt_count == 0 and web_count == 0:
        print("G31 FAIL: no source files found under app/src/ or web/src/")
        return 1

    violations: list[str] = []
    summary: list[tuple[str, int, int]] = []  # (story_id, required, found)

    for f in sorted(STORIES_DIR.glob("*.json")):
        try:
            story = json.loads(f.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            violations.append(f"{f.relative_to(REPO_ROOT)}: parse error: {e}")
            continue
        sid = story.get("id") or f.stem
        acs = story.get("acceptance") or []
        required = len(acs)
        found = counts.get(sid, 0)
        summary.append((sid, required, found))
        if found < required:
            missing = required - found
            violations.append(
                f"story '{sid}': {found}/{required} 'ac: {sid}' "
                f"annotation(s) found; {missing} more needed "
                f"across app/src/ and web/src/"
            )

    if violations:
        print(f"G31 FAIL: {len(violations)} story/-ies under-annotated\n")
        for v in violations:
            print(f"  - {v}")
        print()
        print("  Fix: add `// ac: <story-id>` (Kotlin / JS) or `/* ac: <story-id> */`")
        print("       (JSX) comments in the code paths that implement each")
        print("       acceptance criterion. One per AC minimum.")
        return 1

    total = sum(r for _, r, _ in summary)
    print(
        f"G31 PASS: {len(summary)} story/-ies, "
        f"{total} acceptance line(s) all covered "
        f"(scanned {kt_count} .kt + {web_count} web files)"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
