# Feature Factory Blueprint

> A step-by-step guide to building a deterministic, agent-steered development harness
> on a new project. Read this once end-to-end, then build in the order given.

The pattern: **an agent is non-deterministic by default. Wrap it in guides + sensors so
every output is either steered before the act or caught after.** This repo (the
"factory") contains zero application code — only rules, gates, contracts, and a
manifest. The agent reads it and writes the application code in a separate repo.

---

## Mental Model

```
            ┌──────────────────────────────────────────┐
  Human ──► │  Manifest (capabilities / stories / flows)│ ◄── single source of truth
            └──────────────────────────────────────────┘
                              │
                              ▼
                       task.json (one per feature)
                              │
                              ▼
            ┌──────────────────────────────────────────┐
  Agent ──► │  Reads guides → writes code → runs gates │
            └──────────────────────────────────────────┘
                              │
                ┌─────────────┼─────────────┐
                ▼             ▼             ▼
              Guides       Sensors      Worklog
            (steer)        (catch)      (audit)
```

**Two control types — both required:**

| Type    | Role                      | Examples                                          |
| ------- | ------------------------- | ------------------------------------------------- |
| Guide   | Steer **before** the act  | `CLAUDE.md`, `conventions.md`, catalog, manifest  |
| Sensor  | Catch **after** the act   | `check_*.{py,sh,dart}` returning exit code 0 or 1 |

A convention without a sensor is a suggestion, not a rule.

---

## Directory Layout

Create this skeleton at the root of your governance repo:

```
my-factory/
├── CLAUDE.md                       # Agent entry point — read every session
├── config.json                     # Points to the downstream app repo
├── governance/
│   ├── checks/                     # Sensors (gates) — exit 0 pass, 1 fail
│   │   ├── check_all.sh            # Runs every gate in order
│   │   ├── check_*.py              # One file per gate
│   │   ├── gate_retry_loop.sh      # 3-strike retry with escalation
│   │   ├── session_resume.sh       # Restore checkpoint on session start
│   │   └── session_checkpoint.sh   # Save checkpoint mid-feature
│   ├── docs/
│   │   ├── conventions.md          # Detailed conventions per rule
│   │   ├── agentic-harness.md      # How to add/edit a gate
│   │   ├── meta-operational.md     # Escalation playbook
│   │   └── glossary.md             # Terms used by gates
│   ├── decisions/                  # Tech-lead decision records (one .md per ESCALATION)
│   └── worklog.jsonl               # Append-only audit trail
├── product/
│   ├── manifest/
│   │   ├── capabilities/           # one .json per capability
│   │   ├── stories/                # one .json per user story
│   │   └── flows/                  # one .json per user journey
│   ├── specs/                      # ID registry per module (declared IDs)
│   └── registry.md                 # Human-readable index of all IDs
├── design/
│   └── prototypes/                 # HTML/Figma exports referenced by capabilities
└── tasks/
    └── task.json                   # Current feature task (overwritten per feature)
```

---

## Phase 1 — Layer 1: The Rule Set (Guides)

The agent must read the rules every time it starts. The entry point is `CLAUDE.md`.

### Step 1.1 — Write `CLAUDE.md`

```markdown
# <Project> — Feature Factory

> Read this file at the start of every session before taking any action.

## What This Repo Is

This is the governance harness for <project>. It does not contain application
code — it contains the gates, conventions, and contracts that define what valid
output looks like.

The application code lives at the path in `config.json` (`app_path`).

## Mandatory Session Start

```bash
bash governance/checks/session_resume.sh
```

## Gate Commands

```bash
bash governance/checks/check_all.sh        # run all gates
python3 governance/checks/check_<name>.py  # run a single gate

LOOP_RESET=yes bash governance/checks/gate_retry_loop.sh  # start new cycle
bash governance/checks/gate_retry_loop.sh                 # continue cycle
```

## Non-Negotiable Rules

| Rule                                            | Gate |
| ----------------------------------------------- | ---- |
| Every <feature-folder> follows the standard tree | G1   |
| No literal colors / spacing / strings in code   | G3   |
| Every BLoC / ViewModel has a test file          | G4   |
| Story map links every feature folder to a story | G7   |
| ... (one row per gate)                          |      |

## Adding a New Feature

1. Add capability + story + flow under `product/manifest/`
2. Set capability status to `Approved`
3. Write `tasks/task.json` referencing the storyId
4. Agent reads task and produces feature; all gates must pass before commit

## Worklog Rule

Every session must append a `type=task` AND a `type=retrospective` entry
to `governance/worklog.jsonl`.

## Sub-File Load Map

| Task                          | Load first                       |
| ----------------------------- | -------------------------------- |
| Adding/editing a gate         | `governance/docs/agentic-harness.md` |
| Adding a manifest ID          | `governance/docs/conventions.md` |
| Gate keeps firing after 3 fixes | `governance/docs/meta-operational.md` |
```

### Step 1.2 — Write the conventions doc

`governance/docs/conventions.md` is the long-form rule book. For each gate, document:

- **Rule** — one sentence.
- **Why** — the consistency/quality reason.
- **Examples** — both ✓ pass and ✗ fail snippets.
- **Sensor** — which `check_*.py` enforces it.
- **Scope** — paths the rule applies to; documented skip-lists if any.

### Step 1.3 — Define the catalog (widget / component base classes)

In the downstream app, every UI primitive must extend a catalog base class.
Document the mapping in `governance/docs/conventions.md`:

| Forbidden raw widget        | Catalog replacement          |
| --------------------------- | ---------------------------- |
| `CircleAvatar`              | `UserAvatarElement`          |
| `ElevatedButton`            | `PrimaryButtonElement`       |
| `Color(0xFF…)` literals     | `AppColors.<token>`          |

Why catalog: one change to `PrimaryButtonElement` propagates everywhere.

---

## Phase 2 — Layer 2: Sensors (Gates)

Every rule needs a script that returns exit code 1 on violation. Start with five
gates; add more as failure modes appear in real runs.

### Step 2.1 — Minimum Viable Gate Set

| Gate | Purpose                                                  |
| ---- | -------------------------------------------------------- |
| G0   | Capability status — block if Draft / Unresolved           |
| G1   | Architecture — folder structure, dependency direction    |
| G2   | Conventions — base class extension, error-return shape   |
| G3   | Tokens — no literal colors/spacing/strings in feature code |
| G4   | TDD — every BLoC/ViewModel has a test file               |
| G7   | Story map — every feature folder has `story_map.json`    |

### Step 2.2 — Gate script template (`governance/checks/check_<rule>.py`)

```python
#!/usr/bin/env python3
"""
G<N>: <one-sentence rule>

Scans <app_path>/<scope> for <pattern>. Exits 1 on any violation.
"""
import sys, re, json, pathlib

def main(app_path: str) -> int:
    root = pathlib.Path(app_path)
    violations = []

    for f in root.rglob("*.kt"):       # or *.dart, *.ts, etc.
        if should_skip(f):
            continue
        text = f.read_text(encoding="utf-8", errors="ignore")
        for lineno, line in enumerate(text.splitlines(), 1):
            if BAD_PATTERN.search(line):
                violations.append((str(f), lineno, line.strip()))

    if violations:
        print(f"G<N> FAIL: {len(violations)} violation(s)")
        for path, lineno, line in violations[:50]:
            print(f"  {path}:{lineno}: {line}")
        return 1

    print(f"G<N> PASS")
    return 0

BAD_PATTERN = re.compile(r"...")  # the actual rule

SKIP_DIRS = {"base", "theme", "test"}

def should_skip(p: pathlib.Path) -> bool:
    return any(part in SKIP_DIRS for part in p.parts)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: check_<rule>.py <app_path>"); sys.exit(2)
    sys.exit(main(sys.argv[1]))
```

**Gate authoring rules:**

1. **Binary exit codes.** 0 = pass, 1 = fail, 2 = misuse. No "warning" states.
2. **Print every violation with `path:line:snippet`** so the agent can fix without re-scanning.
3. **AST-based when possible.** Regex catches the first iteration; an aliased
   import (`import Color as C`) defeats it. Promote to AST when an agent
   manages to slip a violation past the regex.
4. **Skip lists are tech debt.** Track them in the gate as a top-level
   `SKIP_FILES` set with a comment dating the entry. Each session should aim to
   shrink, not grow, the list.

### Step 2.3 — Wire `check_all.sh`

```bash
#!/usr/bin/env bash
set -uo pipefail

APP_PATH=$(jq -r .app_path config.json)
GATES=(
  check_capability_status   # G0
  check_architecture        # G1
  check_conventions         # G2
  check_tokens              # G3
  check_tdd                 # G4
  check_story_map           # G7
)

failed=()
for g in "${GATES[@]}"; do
  if ! python3 "governance/checks/${g}.py" "$APP_PATH"; then
    failed+=("$g")
  fi
done

if [[ ${#failed[@]} -gt 0 ]]; then
  printf '\nFAILED GATES: %s\n' "${failed[*]}"
  exit 1
fi
echo "ALL GATES PASS"
```

### Step 2.4 — The retry loop (`gate_retry_loop.sh`)

```bash
#!/usr/bin/env bash
# Tracks attempt count. Max 3 retries before escalating to UNRESOLVED.
set -uo pipefail

STATE_FILE=".gate-loop-state"
MAX_ATTEMPTS=3

if [[ "${LOOP_RESET:-}" == "yes" ]]; then
  echo 0 > "$STATE_FILE"
fi

attempt=$(cat "$STATE_FILE" 2>/dev/null || echo 0)
attempt=$((attempt + 1))
echo "$attempt" > "$STATE_FILE"

echo "── Gate attempt $attempt / $MAX_ATTEMPTS ──"
if bash governance/checks/check_all.sh; then
  rm "$STATE_FILE"
  echo "✓ All gates green on attempt $attempt"
  exit 0
fi

if [[ $attempt -ge $MAX_ATTEMPTS ]]; then
  echo "✗ UNRESOLVED — escalate to human after $MAX_ATTEMPTS attempts"
  rm "$STATE_FILE"
  exit 2
fi
echo "✗ Gate failed — self-correct and re-run gate_retry_loop.sh"
exit 1
```

**Why 3 attempts:** beyond 3, manual review is faster than continued
automation. Prevents infinite loops burning tokens on unfixable problems.

### Step 2.5 — Pre-commit hook

Add the gate chain to the downstream app's pre-commit hook so the wall holds even
if the agent forgets:

```bash
# .git/hooks/pre-commit in the app repo
bash ../my-factory/governance/checks/check_all.sh
```

---

## Phase 3 — Layer 3: The Manifest (Source of Truth)

Code is downstream of the manifest. The agent cannot start work on a capability
whose status is not `Approved`. Every line of code traces back to a story.

### Step 3.1 — Capability schema

`product/manifest/capabilities/<capability-id>.json`:

```json
{
  "id": "user-profile",
  "title": "User Profile",
  "status": "Approved",
  "owners": ["pm@company.com"],
  "stories": ["view-user-profile", "block-user", "edit-user-profile"],
  "flows": ["view-profile-flow", "block-user-flow"],
  "designSpec": "design/prototypes/user-profile-v2.html",
  "prototypeTokenMap": {
    "bg_deep":   "ProfilePage.page_bg",
    "teal":      "ProfileHeaderElement.accent",
    "grad_warm": "ProfileCtaElement.cta_gradient"
  }
}
```

**Status lifecycle:**

```
Draft  ─►  Approved  ─►  In Progress  ─►  Done
   ▲                                          │
   └──── on UNRESOLVED ◄──────────────────────┘
```

`G0` blocks any commit whose `task.json` references a capability not in `Approved` or `In Progress`.

### Step 3.2 — Story schema

`product/manifest/stories/<story-id>.json`:

```json
{
  "id": "block-user",
  "capability": "user-profile",
  "role": "user",
  "want": "to block another user from their profile page",
  "soThat": "I can prevent unwanted interactions",
  "acceptance": [
    "a menu button is shown on the app bar when viewing another user's profile",
    "tapping the menu button shows options including Block",
    "confirming Block shows a blocked state on the profile header",
    "the Block option is not shown when viewing own profile"
  ]
}
```

**The forcing function:** each item in `acceptance[]` becomes a `// ac: <story-id>`
annotation in source code AND a corresponding test assertion. Gate `G31` verifies
every AC line has at least one annotation.

### Step 3.3 — Flow schema

`product/manifest/flows/<flow-id>.json`:

```json
{
  "id": "block-user-flow",
  "steps": [
    { "actor": "user", "action": "taps the overflow menu on another user's profile" },
    {
      "actor": "app",
      "action": "shows a bottom sheet with Block option",
      "story": "block-user",
      "noNavigation": true,
      "triggerElement": "ProfileOverflowMenuElement"
    },
    { "actor": "user", "action": "taps Block and confirms the dialog" },
    {
      "actor": "app",
      "action": "updates the profile header to blocked state",
      "story": "block-user",
      "noNavigation": true
    }
  ]
}
```

**Flow gate (G16) verifies:**

- Every `actor=="app"` step declares either `requires: <new-page-id>` or `noNavigation: true`.
- Every `triggerElement` is a class that actually exists in the app.

**Flow coverage gate (G8):** every flow has a matching widget test in `test/flows/<flow-id>_test.dart`.

### Step 3.4 — The traceability chain

```
Capability (Approved)
   └─► Story (acceptance[])
          └─► Flow (steps[] with triggerElement)
                 └─► task.json (storyId)
                        └─► code (// ac: <story-id>)
                               └─► test (assertion + label)
                                      └─► gates PASS → status = Done → commit allowed
```

If any link is missing, a gate blocks. Orphan code is impossible.

---

## Phase 4 — Task Schema (One File per Feature)

Every feature starts with a `task.json` written by the PM or designer — not a chat
message, not a verbal brief.

`tasks/task.json`:

```json
{
  "feature": "user_profile",
  "storyId": "block-user",
  "acceptanceCriteria": [
    "a menu button is shown on the app bar when viewing another user's profile",
    "the Block option is not shown when viewing own profile"
  ],
  "designSpec": "design/prototypes/user-profile-v2.html",
  "notes": "Block confirmation must use a destructive dialog variant."
}
```

**Constraints enforced by gates:**

- `storyId` must exist in `product/manifest/stories/` — otherwise G7 fails.
- `acceptanceCriteria` is a subset of the story's `acceptance[]`.
- `feature` becomes the folder name under `features/<feature>/` in the app.

**Human input per feature = 2 actions:**

1. Write the manifest entries (capability + story + flow).
2. Write `tasks/task.json`.

Everything else is the agent's job.

---

## Phase 5 — Worklog & Audit Trail

`governance/worklog.jsonl` is append-only. Every session writes:

```json
{"ts":"2026-05-30T09:14:00Z","type":"task","feature":"user_profile","storyId":"block-user","gateResults":{"G1":"pass","G3":"pass","G4":"fail","G7":"pass"}}
{"ts":"2026-05-30T09:42:11Z","type":"retrospective","feature":"user_profile","attempts":2,"escalations":0,"notes":"Token import alias slipped past G3 regex on first attempt — promoted to AST."}
```

Gate `W8` requires every `type=task` entry to be paired with a `type=retrospective`.

The worklog is your audit trail. When a feature ships broken, the worklog tells you
exactly which gate let it through, on which attempt, and what the retro said.

---

## Phase 6 — Skills as Departments (Advanced)

Once the basic harness is stable, factor agent work into specialist sub-agents.
Each skill = preprompt + identity contract + output contract. The factory orchestrates
them in order.

| Skill                    | Role                                         | Reads                  | Writes                                |
| ------------------------ | -------------------------------------------- | ---------------------- | ------------------------------------- |
| `capability-extractor`   | PRD + prototype → manifest                   | PRD `.md`, prototype   | `manifest/{capabilities,stories,flows}` |
| `design-extractor`       | Prototype HTML → token JSONL                 | prototype `.html`      | `design/tokens.jsonl`                 |
| `designer`               | Tokens → `AppColors.*` / catalog components  | `tokens.jsonl`         | `app/theme/`, `app/catalog/`          |
| `app-implementor`        | Capability + designSpec → BLoC + widgets + tests | task.json, manifest    | `app/features/<feature>/`             |
| `qa-engineer`            | Runs `check_all.sh` → structured report      | gate output            | structured PASS/FAIL JSON             |
| `tech-lead`              | Resolves UNRESOLVED → decision record        | gate log, worklog      | `governance/decisions/<n>.md`         |

Pipeline triggered by a single command (e.g. `/start-factory`):

```
PRD + HTML ──► capability-extractor ──┐
                                       ├──► designer ──► implementor ──► qa ──► PR
PRD + HTML ──► design-extractor    ────┘
```

Handoff checks run between every stage. Failure re-forks the **producer**, not the consumer.
Retries > 3 emit `UNRESOLVED`; the factory stops and the tech-lead writes a decision record.

---

## Phase 7 — Session Lifecycle

Every agent session follows the same shape:

```
1. session_resume.sh         # restore last checkpoint, print gate log tail
2. read CLAUDE.md            # mandatory entry point
3. read tasks/task.json      # the brief
4. confirm storyId Approved  # G0 sanity check before any write
5. write code in TDD order   # tests first or alongside (G4/G5)
6. gate_retry_loop.sh        # iterate until green or UNRESOLVED
7. session_checkpoint.sh     # save state mid-feature if pausing
8. append worklog entries    # task + retrospective (W8)
9. open PR                   # human reviews the result, not the steps
```

---

## How to Add a New Gate (when a failure mode appears)

1. **Spot the failure** — agent shipped something subtly wrong; existing gates didn't catch it.
2. **Document the rule** in `governance/docs/conventions.md` with ✓/✗ examples.
3. **Write the sensor** as `governance/checks/check_<rule>.py` — exit 1 on violation, list every offender.
4. **Add a row to `CLAUDE.md` rule table** and to `check_all.sh`.
5. **Backfill the existing codebase** until the new gate is green before merging the gate itself. (Never commit a red baseline.)
6. **Run `gate_retry_loop.sh`** on the next feature to verify the gate fires correctly.

---

## How to Add a New Feature (the user's actual workflow)

1. Write or update the capability JSON in `product/manifest/capabilities/`.
   - Set `status: "Approved"` only after PM signoff.
2. Write the story JSON in `product/manifest/stories/` with full `acceptance[]`.
3. Write the flow JSON in `product/manifest/flows/`, including `triggerElement` for every app step.
4. Drop the design prototype in `design/prototypes/` and reference it in `designSpec`.
5. Write `tasks/task.json` referencing the `storyId`.
6. Tell the agent: `read tasks/task.json and implement`. That's it.

The factory does the rest.

---

## When the Harness Pays Off

| Scenario        | Without harness                          | With harness                                    |
| --------------- | ---------------------------------------- | ----------------------------------------------- |
| Onboarding      | Months of tribal knowledge transfer       | Agent reads `CLAUDE.md` → knows every rule      |
| 10 → 100 features | Conventions drift                        | Gates enforce conventions at commit time         |
| Untested code   | "Will write tests later"                 | G4/G5 make untested code structurally impossible |
| Code ↔ intent   | No traceable link                        | Manifest → `// ac:` → G31 traces back            |
| UI consistency  | Drifts per developer                     | Catalog + G12 enforce widget-level consistency   |
| Refactor        | Hand-grep every call site                | Token / catalog change propagates; gates catch stragglers |
| Post-mortem     | "Who knows when this broke"              | Worklog shows gate, attempt, retro              |

---

## What This Is Not

| ✗ Not                                            | ✓ Is                                              |
| ------------------------------------------------ | ------------------------------------------------- |
| A code generator for throwaway scaffolding       | Deterministic infrastructure                       |
| A copilot needing human review at each step      | An agent with structurally enforced boundaries     |
| A chatbot wrapper around the codebase            | A pipeline where every rule has a sensor           |
| A replacement for architecture decisions         | Humans define intent; agent executes within it     |

---

## Starting Rule

> **Start with the rule you break most often. Write the sensor first.**

A bloated harness costs as much as an insufficient one. Add gates as failure modes
appear in real runs; remove gates that have become redundant as the model improves.

---

## Build Order Checklist

- [ ] Create directory skeleton (Phase 0)
- [ ] Write `CLAUDE.md` + `config.json` (Phase 1.1)
- [ ] Write `conventions.md` for the first 5 rules (Phase 1.2)
- [ ] Implement 5 minimum-viable gates + `check_all.sh` (Phase 2.1–2.3)
- [ ] Implement `gate_retry_loop.sh` (Phase 2.4)
- [ ] Wire pre-commit hook in the downstream app (Phase 2.5)
- [ ] Write one capability + one story + one flow as a smoke test (Phase 3)
- [ ] Write `tasks/task.json` for that story (Phase 4)
- [ ] Run the agent end-to-end on the smoke-test feature
- [ ] Append the first `task` + `retrospective` to `worklog.jsonl` (Phase 5)
- [ ] Iterate: each gate added must come with a passing baseline.

When all checkboxes are green, the factory is live.
