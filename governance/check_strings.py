#!/usr/bin/env python3
"""
G-STR: No hardcoded user-facing string literals in Android UI code.

Flags string literals passed to Compose Text composables in ui/ files.
User-facing copy must be declared in res/values/strings.xml and read via
stringResource(R.string.<key>).

Matched call shapes:
  Text("literal")                              positional first arg
  Text("literal", ...)                          positional first arg
  Text(text = "literal", ...)                  named text= arg
  label = { Text("literal") }                  trailing/lambda Text
  Text("Total: ${state.amount}")               template strings (still flagged)

A string is considered user-facing if it contains at least one ASCII letter.
Pure-symbol / punctuation / digit strings (e.g. "●", " · ", "%s") are exempt
because they are formatting helpers, not translatable copy.

Exempt paths:
  - ui/theme/         — token declarations may carry display names
  - src/test/         — test files
  - src/androidTest/  — instrumentation test files
"""

import sys
import re
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
UI_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

SKIP_DIRS = {"theme", "test", "androidTest"}

# Capture a string literal that follows `Text(` either positionally or as `text =`.
# Group 1 is the string body (without surrounding quotes).
TEXT_CALL = re.compile(
    r"""\bText\s*\(\s*                # Text(
        (?:text\s*=\s*)?              # optional named text =
        "((?:\\.|[^"\\])*)"           # capture string body
    """,
    re.VERBOSE,
)

LETTER = re.compile(r"[A-Za-z]")


def should_skip(path: pathlib.Path) -> bool:
    return any(part in SKIP_DIRS for part in path.parts)


def is_user_facing(literal: str) -> bool:
    return bool(LETTER.search(literal))


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
            for m in TEXT_CALL.finditer(line):
                literal = m.group(1)
                if is_user_facing(literal):
                    violations.append(
                        (str(kt.relative_to(REPO_ROOT)), lineno, stripped, literal)
                    )

    if violations:
        print(f"G-STR FAIL: {len(violations)} hardcoded string literal(s) in UI code\n")
        for path, lineno, snippet, literal in violations[:200]:
            print(f"  {path}:{lineno}")
            print(f"    {snippet}")
            print(f"    literal: \"{literal}\"")
        if len(violations) > 200:
            print(f"\n  ... and {len(violations) - 200} more")
        print(
            "\n  Fix: move the string to app/src/main/res/values/strings.xml and read it"
            "\n       via stringResource(R.string.<key>) in the composable."
        )
        return 1

    print("G-STR PASS: no hardcoded user-facing strings in Text(...) calls")
    return 0


if __name__ == "__main__":
    sys.exit(main())
