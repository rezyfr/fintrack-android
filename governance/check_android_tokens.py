#!/usr/bin/env python3
"""
G3: No hardcoded Color literals in Android UI code outside ui/theme/.

Flags:
  - Color.White / Color.Black / Color.Gray / Color.Red etc. (use MaterialTheme or LocalAppColors instead)
  - Color(0xFF...) literal constructors (declare in ui/theme/Color.kt instead)

Exempt:
  - ui/theme/            — the only place raw Color values are allowed
  - src/test/            — test files
  - src/androidTest/     — instrumentation test files
"""

import sys
import re
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent

UI_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

# Named color constants that must not appear in UI code (Color.Transparent is OK — semantic)
COLOR_NAMED = re.compile(
    r"\bColor\.(White|Black|Gray|DarkGray|LightGray|Red|Green|Blue|Yellow|Cyan|Magenta)\b"
)

# Literal hex constructors, e.g. Color(0xFFCCAABB) or Color(0x14FFFFFF)
COLOR_LITERAL = re.compile(r"\bColor\(0x[0-9A-Fa-f]+\b")

SKIP_DIRS = {"theme", "test", "androidTest"}


def should_skip(path: pathlib.Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)


def main() -> int:
    violations = []
    for kt in UI_ROOT.rglob("*.kt"):
        if should_skip(kt):
            continue
        text = kt.read_text(encoding="utf-8", errors="ignore")
        for lineno, line in enumerate(text.splitlines(), 1):
            stripped = line.strip()
            if stripped.startswith("//"):
                continue
            for pattern in (COLOR_NAMED, COLOR_LITERAL):
                if pattern.search(line):
                    violations.append((str(kt.relative_to(REPO_ROOT)), lineno, stripped))
                    break

    if violations:
        print(f"G3 FAIL: {len(violations)} hardcoded Color literal(s) found in UI code\n")
        for path, lineno, snippet in violations:
            print(f"  {path}:{lineno}")
            print(f"    {snippet}")
        print("\n  Fix: use MaterialTheme.colorScheme.* or LocalAppColors.current.* instead.")
        print("  Raw Color values belong only in app/src/main/java/.../ui/theme/")
        return 1

    print("G3 PASS: no hardcoded Color literals outside ui/theme/")
    return 0


if __name__ == "__main__":
    sys.exit(main())
