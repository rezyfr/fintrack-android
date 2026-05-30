#!/usr/bin/env python3
"""
G-DB: Room database version must have a complete, registered migration chain.

Checks:
  1. AppDatabase.kt declares version = N
  2. A MIGRATION_{N-1}_{N} object exists in AppDatabase.kt
  3. Every MIGRATION_X_Y object in AppDatabase.kt is listed in AppModule.kt's addMigrations(...)
  4. The migration chain is unbroken from the minimum source version to N

Catches the two most common mistakes:
  - Bumping version without adding a migration
  - Declaring a migration in AppDatabase but forgetting to register it in AppModule
"""

from __future__ import annotations

import sys
import re
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
JAVA_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"


def find_file(name: str) -> pathlib.Path | None:
    matches = list(JAVA_ROOT.rglob(name))
    return matches[0] if matches else None


def main() -> int:
    db_file = find_file("AppDatabase.kt")
    mod_file = find_file("AppModule.kt")

    if not db_file:
        print("G-DB FAIL: AppDatabase.kt not found")
        return 1
    if not mod_file:
        print("G-DB FAIL: AppModule.kt not found")
        return 1

    db_text  = db_file.read_text(encoding="utf-8")
    mod_text = mod_file.read_text(encoding="utf-8")

    # 1. Extract current version
    version_match = re.search(r"\bversion\s*=\s*(\d+)", db_text)
    if not version_match:
        print("G-DB FAIL: could not parse 'version = N' from AppDatabase.kt")
        return 1
    version = int(version_match.group(1))

    # 2. Extract all MIGRATION_X_Y objects declared in AppDatabase
    declared = set()
    for m in re.finditer(r"\bMIGRATION_(\d+)_(\d+)\b", db_text):
        declared.add((int(m.group(1)), int(m.group(2))))

    # 3. Extract all MIGRATION_X_Y listed in addMigrations() in AppModule
    registered = set()
    add_block = re.search(r"addMigrations\(([^)]+)\)", mod_text, re.DOTALL)
    if add_block:
        for m in re.finditer(r"MIGRATION_(\d+)_(\d+)", add_block.group(1)):
            registered.add((int(m.group(1)), int(m.group(2))))

    failures = []

    # 4. Latest version must have a migration
    if version > 1 and (version - 1, version) not in declared:
        failures.append(
            f"No MIGRATION_{version-1}_{version} found in AppDatabase.kt "
            f"(version is {version})"
        )

    # 5. Chain completeness — every consecutive step must be covered
    if declared:
        min_src = min(x for x, _ in declared)
        for step in range(min_src, version):
            if (step, step + 1) not in declared:
                failures.append(
                    f"Migration gap: MIGRATION_{step}_{step+1} is missing "
                    f"(chain must be unbroken from {min_src} to {version})"
                )

    # 6. Every declared migration must be registered in AppModule
    for pair in sorted(declared):
        if pair not in registered:
            failures.append(
                f"MIGRATION_{pair[0]}_{pair[1]} declared in AppDatabase "
                f"but not registered in AppModule.addMigrations()"
            )

    if failures:
        print(f"G-DB FAIL: {len(failures)} Room migration issue(s)\n")
        for f in failures:
            print(f"  • {f}")
        return 1

    chain = " → ".join(str(v) for v in range(
        min((x for x, _ in declared), default=version), version + 1
    ))
    print(f"G-DB PASS: version={version}, migration chain [{chain}] complete and registered")
    return 0


if __name__ == "__main__":
    sys.exit(main())
