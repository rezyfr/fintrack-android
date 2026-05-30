# FinTrack: Feature Factory

> Read this file at the start of every session before taking any action.

## What This Repo Is

`fintrack-android` is both the application and its governance harness: the Android app lives under `app/`, while `governance/` holds the gates, conventions, and contracts that define what valid output looks like. The architecture pyramid is:

```
ViewModel  ->  UseCase  ->  Repository  ->  DataSource  ->  Dao / Service
 (ui/)        (domain/    (data/         (data/         (data/db/,
               usecase/)   repository/)   datasource/)   sheets/, ...)
```

Every line of code in `app/` must trace back to `tasks/task.json` and respect the gates in `governance/`.

## Mandatory Session Start

```bash
bash governance/session_resume.sh
```

This prints the last checkpoint, current task brief, task-drift check, last 5 gate runs, and `git status`. Run it before reading anything else.

## Approach

- Read existing files before writing. Don't re-read unless changed.
- Thorough in reasoning, concise in output.
- Skip files over 100KB unless required.
- No sycophantic openers or closing fluff.
- No emojis or em-dashes.
- Do not guess APIs, versions, flags, commit SHAs, or package names. Verify by reading code or docs before asserting.

## Gate Commands

```bash
bash governance/check_all.sh                              # run every gate
python3 governance/check_<name>.py                        # run a single gate

LOOP_RESET=yes bash governance/gate_retry_loop.sh         # start a new 3-strike cycle
bash governance/gate_retry_loop.sh                        # continue cycle
```

Every `check_all.sh` run appends a one-line audit entry to `governance/gate-log.jsonl`. Gates exit 0 on pass, 1 on fail; the retry loop additionally returns 2 on UNRESOLVED.

## Non-Negotiable Rules

| Rule                                                                    | Gate    |
| ----------------------------------------------------------------------- | ------- |
| `task.json` must target a story whose capability is Approved/In Progress | G0      |
| UI layer must not touch Supabase or HTTP infrastructure                 | G1      |
| Web UI layer must route Supabase calls through `web/src/api/`            | G1-W    |
| No hardcoded `Color(...)` literals outside `ui/theme/`                  | G3      |
| No hardcoded user-facing strings inside `Text(...)` composables         | G-STR   |
| Layered architecture + SOLID dependency-inversion (R1-R9)               | G-ARCH  |
| Room `version` bumps come with a registered, unbroken migration chain   | G-DB    |
| Manifest graph (capabilities/stories/flows) has no broken links/orphans | G7      |
| Flow `actor==app` steps declare navigation; `triggerElement` resolves   | G16     |
| Every story.acceptance[] line is covered by a `// ac: <id>` annotation  | G31     |
| Manifest IDs are kebab-case + filename; `task.feature` is snake_case of capability | G-NAME |
| `product/registry.md` is in sync with the manifest (regenerate, don't hand-edit) | G-REG  |
| Android module must compile (`./gradlew :app:compileDebugKotlin`)        | G-BUILD |

Full rule book with pass / fail examples: `governance/docs/conventions.md`.

## Adding a New Feature

1. Write or update `tasks/task.json` with `feature`, `storyId`, and `acceptanceCriteria`.
2. Implement under the existing pyramid layers; obey the gates as you write, not after.
3. Drive `bash governance/gate_retry_loop.sh` until green.
4. Append session-end entries with `bash governance/worklog.sh task` and `bash governance/worklog.sh retro <attempts> <escalations> "<notes>"`.
5. Open the PR. Reviewers read the result, not the steps.

## Pausing Mid-Feature

```bash
bash governance/session_checkpoint.sh "where I left off"
```

Saves `governance/.session-checkpoint.json` (gitignored). Pick up with `session_resume.sh` next session.

## Worklog Rule

Every session must append BOTH a `type=task` and a `type=retrospective` entry to `governance/worklog.jsonl`. The worklog is the cross-session audit trail and is committed to git. Use `governance/worklog.sh` rather than hand-editing.

## Sub-File Load Map

| Task                                       | Load first                                      |
| ------------------------------------------ | ----------------------------------------------- |
| Understand a gate or add a new one         | `governance/docs/conventions.md`                |
| Understand the harness pattern itself      | `factory-blueprint.md`                          |
| Wire up a new feature end-to-end           | `tasks/task.json`, then this file               |
| Audit what happened last session           | `governance/worklog.jsonl`, `governance/gate-log.jsonl` |
| Resume after an interruption               | `bash governance/session_resume.sh`             |
