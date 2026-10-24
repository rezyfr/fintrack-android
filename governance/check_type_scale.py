#!/usr/bin/env python3
"""
G-TYPE: No hardcoded text sizes or font families in Android UI code outside ui/theme/.

Every piece of text takes its size, weight, font and line height from one role in
AppTypography, so the screens keep one hierarchy.

Flags:
  - fontSize = 13.sp      (use style = MaterialTheme.typography.<role>)
  - lineHeight = 18.sp    (the role sets it)
  - fontFamily = Fraunces (the role picks the font)
  - TextStyle(...)        (build from a role: MaterialTheme.typography.<role>.copy(...))

Allowed: fontWeight, as a deliberate override on top of a role.

Exempt:
  - ui/theme/            — the only place raw text metrics are allowed
  - src/test/            — test files
  - src/androidTest/     — instrumentation test files
"""

import sys
import re
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent

UI_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

RULES = (
    (re.compile(r"\bfontSize\s*="), "fontSize"),
    (re.compile(r"\blineHeight\s*="), "lineHeight"),
    (re.compile(r"\bfontFamily\s*="), "fontFamily"),
    (re.compile(r"\bTextStyle\s*\("), "TextStyle(...)"),
)

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
            for pattern, what in RULES:
                if pattern.search(line):
                    violations.append((str(kt.relative_to(REPO_ROOT)), lineno, what, stripped))
                    break

    if violations:
        print(f"G-TYPE FAIL: {len(violations)} hardcoded text metric(s) found in UI code\n")
        for path, lineno, what, snippet in violations:
            print(f"  {path}:{lineno}  ({what})")
            print(f"    {snippet}")
        print("\n  Fix: use style = MaterialTheme.typography.<role> instead.")
        print("  To vary one property, copy a role: MaterialTheme.typography.bodyMedium.copy(...)")
        print("  The roles are declared in app/src/main/java/.../ui/theme/Type.kt")
        return 1

    print("G-TYPE PASS: every text size comes from a typography role")
    return 0


if __name__ == "__main__":
    sys.exit(main())
