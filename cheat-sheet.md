Here's the end-to-end recipe. Read it once; then the workflow is muscle memory.

## Caveat first

The pre-commit hook is wired and **G31 is currently red** (0/7 annotations for the transport story). Until you backfill those 7 `// ac: transport-provider-breakdown` comments in `app/src/`, every commit you try — for the existing work *or* a new feature — will be blocked. Two options:

- Add the 7 annotations now (small cleanup), then start fresh on the next feature with everything green.
- Start the next feature anyway; the harness is still useful for guiding you. Just plan to land both the transport annotations and the new feature's annotations in the same commit cycle.

## The recipe

### 1. Resume the session

```bash
bash governance/session_resume.sh
```

Tells you what you were doing, last gate result, dirty files. Always start here.

### 2. Author the manifest

Three JSON files. Kebab-case stems; the `id` field equals the stem (G-NAME).

```bash
product/manifest/capabilities/<feature-id>.json     # one per capability
product/manifest/stories/<story-id>.json            # one per user story
product/manifest/flows/<story-id>-flow.json         # one per journey
```

Shape (skeletons):

```json
// capability
{ "id": "monthly-budget-target",
  "title": "Monthly Budget Target",
  "status": "Approved",
  "owners": ["fidriyantoriz@social.plus"],
  "stories": ["set-monthly-budget"],
  "flows": ["set-monthly-budget-flow"] }
```

```json
// story
{ "id": "set-monthly-budget",
  "capability": "monthly-budget-target",
  "role": "user",
  "want": "to set a spending limit for the month",
  "soThat": "I get a visible warning when I approach it",
  "acceptance": [
    "a 'Budget' card appears at the top of the dashboard",
    "the card shows a progress bar of spent vs limit",
    "... up to ~7 lines, one assertion per line ..."
  ] }
```

```json
// flow — every actor:app step needs noNavigation OR requires (G16)
{ "id": "set-monthly-budget-flow",
  "steps": [
    { "actor": "user", "action": "opens the Dashboard" },
    { "actor": "app",  "action": "displays BudgetCard",
      "story": "set-monthly-budget",
      "noNavigation": true,
      "triggerElement": "BudgetCard" }
  ] }
```

### 3. Write task.json

```json
{ "feature": "monthly_budget_target",
  "storyId": "set-monthly-budget",
  "acceptanceCriteria": [ "...subset of the story's acceptance[]..." ],
  "designSpec": "design/prototypes/budget-card.html",
  "notes": "..." }
```

Key rule: `feature` is the kebab capability id with `-` → `_` (G-NAME R-NAME-2).

### 4. Regenerate the registry

```bash
python3 governance/check_registry.py --write
```

Runs in ~ms. Commits the new IDs into `product/registry.md`.

### 5. Sanity-check the manifest before writing code

```bash
LOOP_RESET=yes bash governance/gate_retry_loop.sh
```

Expected state at this point: G0 / G7 / G16 / G-NAME / G-REG green (manifest is internally consistent), **G31 red** for the new story (you haven't annotated anything yet — that's intentional, the gate is your TODO list). G-ARCH, G1, G3, G-STR depend on what's already in `app/`.

### 6. Implement under the pyramid

For each AC, drop a `// ac: <story-id>` comment in the code path that satisfies it. G31 just counts presence — anywhere in `app/src/` is fine.

The pyramid (G-ARCH enforces every hop):

```
ui/<feature>/<Feature>Screen.kt                      # @Composable; no DI rules
ui/<feature>/<Feature>ViewModel.kt                   # @Inject *UseCase only
domain/usecase/<Verb><Noun>UseCase.kt                # @Inject *Repository (interface) only
data/repository/<Feature>Repository.kt               # interface
data/repository/<Feature>RepositoryImpl.kt           # @Inject *DataSource (interface) only
data/datasource/<Feature>LocalDataSource.kt          # interface
data/datasource/local/<Feature>LocalDataSourceImpl.kt # @Inject *Dao only
data/datasource/<Feature>RemoteDataSource.kt         # interface (optional, if remote)
data/datasource/remote/<Feature>RemoteDataSourceImpl.kt # @Inject *Service/*Api only
data/db/<Entity>Entity.kt, <Feature>Dao.kt           # Room
di/AppModule.kt                                      # Hilt bindings — both Impls bound to interfaces
app/src/main/res/values/strings.xml                  # any user-visible copy
```

If the Room schema changes: bump `version` in `AppDatabase.kt` AND declare `MIGRATION_X_Y` AND register it in `AppModule.addMigrations(...)` (G-DB will tell you immediately if you miss one of the three).

If you add Text in a Composable, route it through `stringResource(R.string.…)` (G-STR).

### 7. Iterate until green

```bash
bash governance/gate_retry_loop.sh
```

Read the failure list, fix, re-run. Three failed attempts in a row escalates to UNRESOLVED — at that point write a decision record (`governance/decisions/<n>.md`) instead of swinging at the gate again.

Pause mid-feature:

```bash
bash governance/session_checkpoint.sh "stuck on G-ARCH R3 in BudgetRepositoryImpl"
```

### 8. Wrap the session

```bash
bash governance/worklog.sh task                          # auto-fills feature/story/gate-result
bash governance/worklog.sh retro <attempts> 0 "<notes>"  # close the loop
```

Both entries belong in the same session per blueprint Phase 5. The worklog is the audit trail that survives.

### 9. Commit

`git add` + `git commit`. The pre-commit hook reruns the whole suite as the safety net. If anything's red, fix and re-stage (a new commit, not `--amend`).

## Cheat sheet

| You did this             | Run this                                                                        |
| ------------------------ | ------------------------------------------------------------------------------- |
| Starting a session       | `bash governance/session_resume.sh`                                             |
| Edited any manifest JSON | `python3 governance/check_registry.py --write`                                  |
| Want to check one gate   | `python3 governance/check_<name>.py`                                            |
| Want to check all gates  | `bash governance/check_all.sh`                                                  |
| Want the 3-strike loop   | `LOOP_RESET=yes bash governance/gate_retry_loop.sh`                             |
| Pausing mid-feature      | `bash governance/session_checkpoint.sh "<note>"`                                |
| Finishing a session      | `bash governance/worklog.sh task && bash governance/worklog.sh retro N 0 "..."` |
| Gates fail 3x            | Write `governance/decisions/<n>.md`, then `LOOP_RESET=yes …`                    |

## Mental model in one line

> Write what the feature is (manifest) → write what you'll build (task.json) → write code that the gates accept → close the loop in the worklog.

If a gate is red, you have two valid moves: change the code to satisfy it, or change the gate. **Never disable a gate without a decision record** — that's how harnesses become decorative.