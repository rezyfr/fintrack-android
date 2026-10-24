#!/usr/bin/env python3
"""
G-SENSITIVE: No secrets, personal data or machine-local files in the repository.

Checks every tracked text file, and the messages of the commits that are not in the base branch yet:
  1. Secrets: JWTs, cloud and API keys, private keys, tokens, a Supabase project URL, a filled-in anon key.
  2. Personal identifiers: e-mail addresses (except example and no-reply ones), absolute home paths.
  3. Files that never belong in git: local.properties, .env, keystores, google-services.json, model files,
     database files, .claude/settings.local.json.
  4. Your own terms: names, merchants and amounts that came from real life. They are NOT stored in the
     repository, because that would publish them. Put one regular expression per line in
     governance/sensitive-terms.local.txt (ignored by git), or point FINTRACK_SENSITIVE_TERMS at a file.
     governance/sensitive-terms.example.txt shows the format. CI has no local file, so it runs checks 1 to 3.

Skipped: binary files, package-lock.json, and third-party skill data under .claude/.
To allow a finding, add the file to ALLOWED_FILES with a reason, or a pattern to ALLOWED_TEXT.
"""

import os
import pathlib
import re
import subprocess
import sys

ROOT = pathlib.Path(__file__).parent.parent

SECRET_PATTERNS = {
    "JWT": re.compile(r"eyJ[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{15,}\.[A-Za-z0-9_-]{10,}"),
    "AWS key": re.compile(r"\bAKIA[0-9A-Z]{16}\b"),
    "API key (sk-)": re.compile(r"\bsk-[A-Za-z0-9_-]{20,}"),
    "Google API key": re.compile(r"\bAIza[0-9A-Za-z_-]{35}\b"),
    "GitHub token": re.compile(r"\bgh[pousr]_[A-Za-z0-9]{30,}\b"),
    "Slack token": re.compile(r"\bxox[baprs]-[A-Za-z0-9-]{10,}"),
    "private key": re.compile(r"-----BEGIN [A-Z ]*PRIVATE KEY-----"),
    "Supabase project URL": re.compile(r"https://[a-z0-9]{15,}\.supabase\.(?:co|in)"),
    "filled-in anon key": re.compile(r"SUPABASE_ANON_KEY\s*=\s*[A-Za-z0-9._-]{30,}"),
    "filled-in API token": re.compile(r"API_TOKEN\s*=\s*[A-Za-z0-9._-]{24,}"),
}
EMAIL = re.compile(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(?:\.[A-Za-z0-9-]+)+")
HOME_PATH = re.compile(r"/Users/[A-Za-z0-9._-]+/|/home/[A-Za-z0-9._-]+/|[A-Za-z]:\\\\?Users\\\\?[A-Za-z0-9._-]+")

FORBIDDEN_FILES = re.compile(
    r"(^|/)(local\.properties|\.env|google-services\.json|settings\.local\.json)$"
    r"|\.(jks|keystore|pem|p12|litertlm|sqlite|sqlite3|db|db-wal|db-shm)$"
)

SKIP_PATH = re.compile(r"^(\.claude/|web/\.claude/)|(^|/)package-lock\.json$|\.(png|jpg|jpeg|webp|ttf|otf|jar|ico)$")

# e-mail addresses that are fine: examples, no-reply senders
ALLOWED_TEXT = re.compile(r"@example\.|@users\.noreply\.github\.com|noreply@|/Users/(?:you|yourname|user|username|name)/")

# path -> reason. Keep this short.
ALLOWED_FILES = {
    "governance/check_sensitive.py": "Holds the patterns this gate looks for",
}


def git(*args: str) -> str:
    return subprocess.run(["git", *args], cwd=ROOT, capture_output=True, text=True, check=False).stdout


def local_terms() -> list[re.Pattern]:
    path = pathlib.Path(os.environ.get("FINTRACK_SENSITIVE_TERMS", ROOT / "governance" / "sensitive-terms.local.txt"))
    if not path.is_file():
        return []
    terms = []
    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if line and not line.startswith("#"):
            terms.append(re.compile(line, re.IGNORECASE))
    return terms


def main() -> int:
    terms = local_terms()
    findings: list[str] = []

    files = [f for f in git("ls-files").splitlines() if f]
    for rel in files:
        if FORBIDDEN_FILES.search(rel):
            findings.append(f"{rel}: a file that must not be tracked")
            continue
        if SKIP_PATH.search(rel) or rel in ALLOWED_FILES:
            continue
        path = ROOT / rel
        try:
            data = path.read_bytes()
        except OSError:
            continue
        if b"\0" in data:
            continue
        for n, line in enumerate(data.decode("utf-8", errors="ignore").splitlines(), 1):
            hit = None
            for name, rx in SECRET_PATTERNS.items():
                if rx.search(line):
                    hit = name
                    break
            if not hit and HOME_PATH.search(line) and not ALLOWED_TEXT.search(line):
                hit = "absolute home path"
            if not hit:
                m = EMAIL.search(line)
                if m and not ALLOWED_TEXT.search(line):
                    hit = "e-mail address"
            if not hit:
                for t in terms:
                    if t.search(line):
                        hit = "a term from your local list"
                        break
            if hit:
                findings.append(f"{rel}:{n}: {hit}")

    # commit messages that are not in the base branch yet
    base = next((b for b in ("origin/master", "origin/main") if git("rev-parse", "--verify", "-q", b).strip()), None)
    if base:
        for commit in git("log", "--format=%h", f"{base}..HEAD").split():
            message = git("log", "-1", "--format=%B", commit)
            for rx in list(SECRET_PATTERNS.values()) + terms:
                if rx.search(message):
                    findings.append(f"commit {commit}: the message matches a secret or a term from your local list")
                    break

    note = "" if terms else " (no local term list: checks 1 to 3 only)"
    if findings:
        print(f"G-SENSITIVE FAIL: {len(findings)} finding(s){note}")
        for f in findings[:60]:
            print(f"  {f}")
        print("  Fix: remove the data, or replace it with a neutral value.")
        print("       A real exception goes into ALLOWED_FILES in governance/check_sensitive.py with a reason.")
        return 1
    print(f"G-SENSITIVE PASS: {len(files)} tracked files and the new commit messages are clean{note}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
