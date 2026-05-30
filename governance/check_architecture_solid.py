#!/usr/bin/env python3
"""
G-ARCH: SOLID + clean-architecture pyramid sensor.

Enforces this dependency direction (downward only, depend on interfaces):

  ui/<feature>/*ViewModel       -> *UseCase
  domain/usecase/*UseCase       -> *Repository (interface)
  data/repository/*RepositoryImpl -> *DataSource (interface)
  data/datasource/local/*LocalDataSource  -> *Dao
  data/datasource/remote/*RemoteDataSource -> *Service / *Api (Retrofit)

See /Users/socialplus/.claude-work/plans/imperative-swinging-wilkes.md for the
rule book (R1-R9). Exit 0 on pass, 1 on any violation.
"""

from __future__ import annotations

import re
import sys
import pathlib

REPO_ROOT = pathlib.Path(__file__).parent.parent
APP_ROOT = REPO_ROOT / "app" / "src" / "main" / "java"

# ----- Layer name -> allowed constructor-param suffix set --------------------

ALLOWED_PARAM_SUFFIXES = {
    "viewmodel":  {"UseCase", "SavedStateHandle"},
    "usecase":    {"Repository"},
    "repository": {"DataSource"},
    "local_ds":   {"Dao"},
    "remote_ds":  {"Service", "Api"},
}

RULE_MSG = {
    "viewmodel":  "R1: ViewModel may only inject *UseCase",
    "usecase":    "R2: UseCase may only inject *Repository (interface)",
    "repository": "R3: RepositoryImpl may only inject *DataSource (interface)",
    "local_ds":   "R4: LocalDataSource may only inject *Dao",
    "remote_ds":  "R4: RemoteDataSource may only inject *Service / *Api (use Retrofit)",
}

LINE_CAPS = {"viewmodel": 200, "usecase": 80, "repository": 150}

# ----- Banned infrastructure tokens, only allowed inside these dirs ----------

INFRA_BANNED = [
    (re.compile(r"\bokhttp3\b"),                 "okhttp3"),
    (re.compile(r"\bBuildConfig\.SUPABASE_"),    "BuildConfig.SUPABASE_*"),
]
INFRA_ALLOWED_DIRS = (
    "/data/datasource/remote/",
    "/notification/",
    "/service/",
    "/di/",
)

# ----- Banned raw HTTP patterns (R5b) — forbidden everywhere, use Retrofit ----

RETROFIT_REQUIRED = [
    (re.compile(r"\bRequest\.Builder\("),  "Request.Builder()"),
    (re.compile(r"\.newCall\("),           ".newCall()"),
]

# ----- Domain purity (R7) ----------------------------------------------------

DOMAIN_FORBIDDEN_PREFIXES = (
    "android.",
    "androidx.",
    "okhttp3.",
    "com.fidriyanto.banktracker.data.db.",
    "com.fidriyanto.banktracker.data.datasource.",
    "dagger.hilt.android.",
)

# ----- Helpers ---------------------------------------------------------------

CLASS_HEAD = re.compile(
    r"^[ \t]*"
    r"(?P<mods>(?:(?:public|private|internal|open|final|abstract|sealed|data|enum)\s+)*)"
    r"(?P<kind>class|object|interface)\s+(?P<name>\w+)",
    re.MULTILINE,
)

# Match the primary constructor of a specific class by name.
def _class_ctor_open(class_name: str) -> re.Pattern:
    return re.compile(rf"\bclass\s+{re.escape(class_name)}\b[^{{]*?\(")

PARAM_TYPE = re.compile(r":\s*([A-Za-z_][\w.]*)")

IMPORT_RE = re.compile(r"^\s*import\s+([\w.]+)", re.MULTILINE)


def rel_repo(p: pathlib.Path) -> str:
    return str(p.relative_to(REPO_ROOT))


def app_path_str(p: pathlib.Path) -> str:
    """Path relative to APP_ROOT with leading slash, forward slashes."""
    return "/" + str(p.relative_to(APP_ROOT)).replace("\\", "/")


def repo_path_str(p: pathlib.Path) -> str:
    return "/" + str(p.relative_to(REPO_ROOT)).replace("\\", "/")


def classify(path: pathlib.Path) -> str | None:
    """Return the layer id for a file, or None if it doesn't belong to any."""
    sp = app_path_str(path)
    stem = path.stem
    if "/ui/" in sp and stem.endswith("ViewModel"):
        return "viewmodel"
    if "/domain/usecase/" in sp and stem.endswith("UseCase"):
        return "usecase"
    if "/data/datasource/local/" in sp and stem.endswith("LocalDataSource"):
        return "local_ds"
    if "/data/datasource/remote/" in sp and stem.endswith("RemoteDataSource"):
        return "remote_ds"
    # Repository impl: any class declared under data/repository/.
    # Caller still needs to confirm a top-level class exists.
    if "/data/repository/" in sp:
        return "repository"
    return None


def parse_constructor_params(text: str, class_name: str) -> list[tuple[str, str]] | None:
    """Return [(param_source, type_simple_name), ...] for class_name, or None."""
    m = _class_ctor_open(class_name).search(text)
    if not m:
        return None
    open_idx = m.end() - 1
    depth = 0
    body_start = open_idx + 1
    i = open_idx
    while i < len(text):
        c = text[i]
        if c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return _split_params(text[body_start:i])
        i += 1
    return None


def _split_params(body: str) -> list[tuple[str, str]]:
    out: list[tuple[str, str]] = []
    depth = angle = 0
    cur: list[str] = []
    for c in body:
        if c in "({":
            depth += 1
        elif c in ")}":
            depth -= 1
        elif c == "<":
            angle += 1
        elif c == ">":
            angle -= 1
        if c == "," and depth == 0 and angle == 0:
            _emit_param("".join(cur).strip(), out)
            cur = []
        else:
            cur.append(c)
    _emit_param("".join(cur).strip(), out)
    return out


def _emit_param(p: str, out: list[tuple[str, str]]) -> None:
    if not p:
        return
    m = PARAM_TYPE.search(p)
    if not m:
        return
    simple = m.group(1).rsplit(".", 1)[-1]
    out.append((p, simple))


def top_level_decls(text: str):
    """Yield (kind, name, is_public, mods) for top-level class/object/interface."""
    for m in CLASS_HEAD.finditer(text):
        mods = m.group("mods") or ""
        is_public = "private " not in mods and "internal " not in mods
        yield (m.group("kind"), m.group("name"), is_public, mods)


def is_main_type(kind: str, mods: str) -> bool:
    """A 'main' top-level type for SRP counting (R8).

    Excluded as cheap structural decls: object, data class, sealed class,
    enum class, abstract class. These commonly accompany a primary class in
    idiomatic Kotlin (UI state, sealed hierarchies, enums) without violating
    single-responsibility.
    """
    if kind == "interface":
        return True
    if kind != "class":
        return False
    for cheap in ("data ", "sealed ", "enum ", "abstract "):
        if cheap in mods:
            return False
    return True


def find_param_line(text: str, param_src: str) -> int:
    head = param_src.split(":")[0].strip()
    ident = head.split()[-1] if head else ""
    if not ident:
        return 1
    for i, line in enumerate(text.splitlines(), 1):
        if ident in line and ":" in line:
            return i
    return 1


# ----- Per-file checks --------------------------------------------------------

def check_file(path: pathlib.Path, violations: list) -> None:
    text = path.read_text(encoding="utf-8", errors="ignore")
    rel = rel_repo(path)
    rps = repo_path_str(path)
    sp = app_path_str(path)
    layer = classify(path)

    # R5: banned infra tokens outside allowed dirs
    if not any(d in rps for d in INFRA_ALLOWED_DIRS):
        for lineno, line in enumerate(text.splitlines(), 1):
            stripped = line.strip()
            if stripped.startswith("//"):
                continue
            for pat, label in INFRA_BANNED:
                if pat.search(line):
                    violations.append(
                        (rel, lineno, stripped,
                         f"R5: `{label}` only allowed in data/datasource/remote/, sheets/, "
                         f"notification/, service/, categorization/")
                    )
                    break

    # R5b: raw OkHttp call patterns forbidden everywhere — use Retrofit services
    for lineno, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        if stripped.startswith("//"):
            continue
        for pat, label in RETROFIT_REQUIRED:
            if pat.search(line):
                violations.append(
                    (rel, lineno, stripped,
                     f"R5b: `{label}` forbidden everywhere; inject a Retrofit *Service / *Api")
                )
                break

    # R7: domain purity
    if "/domain/" in sp:
        for m in IMPORT_RE.finditer(text):
            imp = m.group(1)
            for prefix in DOMAIN_FORBIDDEN_PREFIXES:
                if imp.startswith(prefix):
                    lineno = text.count("\n", 0, m.start()) + 1
                    violations.append(
                        (rel, lineno, f"import {imp}",
                         f"R7: domain/ may not import `{prefix}*`")
                    )
                    break

    # R8: at most one "main" public type per file (ui/domain/data).
    # Data classes, sealed classes, enums, objects, and abstract classes are
    # treated as cheap supporting decls and do NOT count.
    if any(seg in sp for seg in ("/ui/", "/domain/", "/data/")):
        main_types = [d for d in top_level_decls(text)
                      if d[2] and is_main_type(d[0], d[3])]
        if len(main_types) > 1:
            names = ", ".join(d[1] for d in main_types)
            violations.append(
                (rel, 1, "",
                 f"R8: file declares {len(main_types)} main public types ({names}); expected 1")
            )

    # R9: line caps
    if layer in LINE_CAPS:
        n = len(text.splitlines())
        cap = LINE_CAPS[layer]
        if n > cap:
            violations.append(
                (rel, n, "", f"R9: {layer} file is {n} lines > {cap} cap")
            )

    # R1-R4: layer-specific constructor injection rules
    if layer:
        if layer == "repository":
            has_class = any(d[0] == "class" and d[2] for d in top_level_decls(text))
            if has_class:
                _check_constructor(path, text, layer, violations)
        else:
            _check_constructor(path, text, layer, violations)

    # R6 (per-file): naming convention in data/repository/
    if "/data/repository/" in sp:
        decls = [d for d in top_level_decls(text) if d[2]]
        kinds = {d[0] for d in decls if d[0] in ("class", "interface")}
        stem = path.stem
        if stem.endswith("RepositoryImpl"):
            if "class" not in kinds:
                violations.append(
                    (rel, 1, "",
                     f"R6: `{stem}.kt` should declare a class")
                )
        elif stem.endswith("Repository"):
            if "interface" not in kinds:
                violations.append(
                    (rel, 1, "",
                     f"R6: `{stem}.kt` must declare an interface; "
                     f"concrete impl belongs in `{stem}Impl.kt`")
                )

    # Cross-file repository pairing (R6) is checked once at the end in main().


def _check_constructor(path: pathlib.Path, text: str, layer: str, violations: list) -> None:
    params = parse_constructor_params(text, path.stem)
    if params is None:
        return  # interface / object / no ctor / class name doesn't match file stem
    allowed = ALLOWED_PARAM_SUFFIXES[layer]
    msg = RULE_MSG[layer]
    for param_src, simple in params:
        if not any(simple.endswith(suf) for suf in allowed):
            lineno = find_param_line(text, param_src)
            violations.append(
                (rel_repo(path), lineno, param_src.strip(),
                 f"{msg} (got `{simple}`)")
            )


# ----- Repo-wide R6 cross-file pairing ---------------------------------------

def check_repository_pairing(violations: list) -> None:
    repo_dir = APP_ROOT / "com" / "fidriyanto" / "banktracker" / "data" / "repository"
    if not repo_dir.exists():
        return
    impls: set[str] = set()
    ifaces: set[str] = set()
    for kt in repo_dir.glob("*.kt"):
        text = kt.read_text(encoding="utf-8", errors="ignore")
        for kind, name, is_pub, _mods in top_level_decls(text):
            if not is_pub:
                continue
            if kind == "class" and name.endswith("RepositoryImpl"):
                impls.add(name[:-4])  # FooRepositoryImpl -> FooRepository
            if kind == "interface" and name.endswith("Repository"):
                ifaces.add(name)
    rel = str(repo_dir.relative_to(REPO_ROOT))
    for name in sorted(impls - ifaces):
        violations.append(
            (rel, 1, "", f"R6: `{name}Impl` exists but no `interface {name}` declared")
        )
    for name in sorted(ifaces - impls):
        violations.append(
            (rel, 1, "", f"R6: `interface {name}` exists but no `class {name}Impl` declared")
        )


# ----- Entry point -----------------------------------------------------------

def main() -> int:
    if not APP_ROOT.exists():
        print(f"G-ARCH FAIL: app root not found at {APP_ROOT}")
        return 1

    violations: list = []
    for kt in APP_ROOT.rglob("*.kt"):
        if any(seg in kt.parts for seg in ("test", "androidTest")):
            continue
        check_file(kt, violations)

    check_repository_pairing(violations)

    if not violations:
        print("G-ARCH PASS: layered architecture & SOLID rules satisfied")
        return 0

    print(f"G-ARCH FAIL: {len(violations)} architecture rule violation(s)\n")
    for path, lineno, snippet, msg in violations:
        print(f"  {path}:{lineno}")
        if snippet:
            print(f"    {snippet}")
        print(f"    -> {msg}")
    return 1


if __name__ == "__main__":
    sys.exit(main())
