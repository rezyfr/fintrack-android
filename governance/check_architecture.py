#!/usr/bin/env python3
"""
G1: UI layer must not directly access Supabase or HTTP infrastructure.

The rule: Screens and ViewModels may only call Repositories or Fetchers.
Direct use of OkHttpClient, BuildConfig.SUPABASE_URL/KEY, or Request.Builder
in the ui/ package is a layering violation.

Exempt:
  - app/src/main/java/.../sheets/     — fetcher/syncer implementations live here (allowed)
  - app/src/main/java/.../di/         — dependency injection modules (allowed)
  - src/test/ and src/androidTest/
"""

import sys
import re
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
JAVA_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

# Patterns that must not appear in ui/ files
BANNED = [
    (re.compile(r"BuildConfig\.SUPABASE_URL"),   "BuildConfig.SUPABASE_URL — use repository injection"),
    (re.compile(r"BuildConfig\.SUPABASE_ANON_KEY"), "BuildConfig.SUPABASE_ANON_KEY — use repository injection"),
    (re.compile(r"\bOkHttpClient\b"),             "OkHttpClient — belongs in sheets/ or di/, not ui/"),
    (re.compile(r"Request\.Builder\(\)"),         "Request.Builder() — HTTP calls belong in sheets/ layer"),
]

SKIP_DIRS = {"test", "androidTest"}


def is_ui_file(path: pathlib.Path) -> bool:
    parts = path.parts
    return "ui" in parts and not any(s in parts for s in SKIP_DIRS)


def main() -> int:
    violations = []
    for kt in JAVA_ROOT.rglob("*.kt"):
        if not is_ui_file(kt):
            continue
        text = kt.read_text(encoding="utf-8", errors="ignore")
        for lineno, line in enumerate(text.splitlines(), 1):
            if line.strip().startswith("//"):
                continue
            for pattern, reason in BANNED:
                if pattern.search(line):
                    violations.append((str(kt.relative_to(REPO_ROOT)), lineno, line.strip(), reason))
                    break

    if violations:
        print(f"G1 FAIL: {len(violations)} architecture violation(s) in ui/ layer\n")
        for path, lineno, snippet, reason in violations:
            print(f"  {path}:{lineno}")
            print(f"    {snippet}")
            print(f"    → {reason}")
        return 1

    print("G1 PASS: no Supabase/HTTP access in ui/ layer")
    return 0


if __name__ == "__main__":
    sys.exit(main())
