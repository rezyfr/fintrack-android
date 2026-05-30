#!/usr/bin/env python3
"""
G1-W: Web UI layer must not touch Supabase infrastructure directly.

The rule: files under web/src/ (excluding web/src/api/ and dependency dirs)
may not reference the Supabase environment tokens or hardcoded Supabase URLs.
Components consume the API via imports from web/src/api/.

Mirrors G1 on the Android side. Catches the same failure mode (transport
leaking into the UI layer) for the React codebase.

Banned tokens (outside web/src/api/):
  - import.meta.env.VITE_SUPABASE_URL
  - import.meta.env.VITE_SUPABASE_ANON_KEY
  - VITE_SUPABASE_URL / VITE_SUPABASE_ANON_KEY      (anywhere they appear)
  - hardcoded URLs matching https://*.supabase.co/...

Exit 0 on clean, 1 on any violation.
"""

from __future__ import annotations

import re
import sys
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
WEB_SRC = REPO_ROOT / "web" / "src"
API_DIR_SEGMENT = "/web/src/api/"
SKIP_DIRS = ("node_modules", "dist", "build")
EXTS = (".js", ".jsx", ".ts", ".tsx")

BANNED = [
    (re.compile(r"\bVITE_SUPABASE_URL\b"),       "VITE_SUPABASE_URL"),
    (re.compile(r"\bVITE_SUPABASE_ANON_KEY\b"),  "VITE_SUPABASE_ANON_KEY"),
    (re.compile(r"https://[^\s\"']*\.supabase\.co"), "hardcoded supabase.co URL"),
]


def is_exempt(path: pathlib.Path) -> bool:
    rel = "/" + str(path.relative_to(REPO_ROOT)).replace("\\", "/")
    if API_DIR_SEGMENT in rel:
        return True
    return any(seg in path.parts for seg in SKIP_DIRS)


def main() -> int:
    if not WEB_SRC.exists():
        print(f"G1-W FAIL: {WEB_SRC.relative_to(REPO_ROOT)} not found")
        return 1

    violations = []
    for ext in EXTS:
        for f in WEB_SRC.rglob(f"*{ext}"):
            if is_exempt(f):
                continue
            text = f.read_text(encoding="utf-8", errors="ignore")
            for lineno, line in enumerate(text.splitlines(), 1):
                stripped = line.strip()
                if stripped.startswith("//") or stripped.startswith("*"):
                    continue
                for pat, label in BANNED:
                    if pat.search(line):
                        violations.append(
                            (str(f.relative_to(REPO_ROOT)), lineno, stripped, label)
                        )
                        break

    if violations:
        print(f"G1-W FAIL: {len(violations)} Supabase access(es) outside web/src/api/\n")
        for path, lineno, snippet, label in violations:
            print(f"  {path}:{lineno}")
            print(f"    {snippet}")
            print(f"    -> {label} — route through web/src/api/")
        return 1

    print("G1-W PASS: no Supabase tokens / URLs outside web/src/api/")
    return 0


if __name__ == "__main__":
    sys.exit(main())
