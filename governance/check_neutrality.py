#!/usr/bin/env python3
"""
G-NEUTRAL: The app code does not name a specific currency, bank account or city.

Why: a user picks their own home currency, accounts and pay cycle on first run. A currency code, a
wallet id or a city in the code would tie the app to one person's setup.

Flags these in non-test app code (Android main sources, web sources, SQL migrations):
  - the currency codes IDR and THB as string literals
  - the wallet ids BBL, BCA, BCA_CC, MANDIRI, MANDIRI_CC, and the bank names Bualuang and myBCA
  - the country labels Indonesia and Thailand, the time zone Asia/Bangkok, the symbols Rp and the baht sign
  - the fixed exchange rate 535

Comment lines are skipped. Test files are skipped.

The ALLOWED table lists the few files that may keep them, each with the reason. To add a file, add
a row with a reason a reviewer can accept. Prefer reading the value from the user's settings and accounts.
"""

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).parent.parent

SCAN = [
    (ROOT / "app" / "src" / "main", {".kt", ".xml"}),
    (ROOT / "web" / "src", {".js", ".jsx"}),
]

PATTERN = re.compile(
    r"""["'](?:IDR|THB)["']"""
    r"""|\b(?:BBL|BCA|BCA_CC|MANDIRI|MANDIRI_CC|Bualuang|myBCA)\b"""
    r"""|\b(?:Indonesia|Thailand)\b|Asia/Bangkok|\b535\b|\bRp\b|฿"""
)

# path (relative to the repo root) -> why it may name a currency, bank or city
ALLOWED = {
    "app/src/main/java/com/fidriyanto/banktracker/domain/model/CurrencySettings.kt":
        "Defaults for an install that never saved settings, the shared symbol table, and the zero-decimal list",
    "app/src/main/java/com/fidriyanto/banktracker/domain/model/ExchangeRate.kt":
        "The fallback rate an existing IDR and THB install always used",
    "app/src/main/java/com/fidriyanto/banktracker/domain/model/KnownBank.kt":
        "The built-in bank patterns. A user maps each to an account, or turns it off",
    "app/src/main/java/com/fidriyanto/banktracker/notification/NotificationParser.kt":
        "The built-in BCA and Bangkok Bank notification patterns",
    "app/src/main/java/com/fidriyanto/banktracker/domain/usecase/CurrencyDetect.kt":
        "Currency words and symbols in notification text, for example the Thai word for baht",
    "app/src/main/java/com/fidriyanto/banktracker/ui/setup/SetupPages.kt":
        "Currency suggestion chips on the setup page. The user can type any code",
    "web/src/constants/currency.js":
        "Defaults for an install that never saved settings, the symbol table, and the legacy fallback rate",
    "web/src/components/TransactionList.jsx":
        "Reads the ledger tab label kept from the first version. The IDR tab holds home currency rows",
}


def is_comment(line: str) -> bool:
    s = line.strip()
    return s.startswith(("//", "*", "/*", "<!--", "#", "--")) or s.startswith("{/*")


def skip(path: pathlib.Path) -> bool:
    name = path.name
    return ("/test/" in path.as_posix() or "/androidTest/" in path.as_posix()
            or ".test." in name or name in {"test-setup.js", "test-fixtures.js"})


def main() -> int:
    hits = []
    for base, exts in SCAN:
        for path in sorted(base.rglob("*")):
            if path.suffix not in exts or not path.is_file() or skip(path):
                continue
            rel = path.relative_to(ROOT).as_posix()
            if rel in ALLOWED:
                continue
            for n, line in enumerate(path.read_text(encoding="utf-8", errors="ignore").splitlines(), 1):
                if not is_comment(line) and PATTERN.search(line):
                    hits.append((rel, n, line.strip()))
    if hits:
        print(f"G-NEUTRAL FAIL: {len(hits)} place(s) name a specific currency, bank or city")
        for rel, n, text in hits[:40]:
            print(f"  {rel}:{n}\n    {text[:140]}")
        print("  Fix: read the value from the user's settings or accounts.")
        print("       If the file must keep it, add it to ALLOWED in governance/check_neutrality.py with a reason.")
        return 1
    print(f"G-NEUTRAL PASS: no specific currency, bank or city outside the {len(ALLOWED)} allowed files")
    return 0


if __name__ == "__main__":
    sys.exit(main())
