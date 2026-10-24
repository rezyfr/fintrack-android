# Factory conventions

These gates check the product manifest (`factory/product/`), `factory/tasks/task.json` and the `// ac:` annotations.
They are part of the opt-in agent harness. CI does not run them. See `factory/README.md`.

| Gate    | Rule (one line)                                                          | Sensor                          |
| ------- | ------------------------------------------------------------------------ | ------------------------------- |
| G0      | `task.json` must target a story whose capability is Approved/In Progress. | `check_capability_status.py`    |
| G7      | Manifest graph (capabilities, stories, flows) has no broken links or orphans. | `check_story_map.py`       |
| G16     | Flow `actor==app` steps declare navigation; every `triggerElement` resolves to a real class. | `check_flow_integrity.py` |
| G31     | Every `story.acceptance[]` line is covered by a `// ac: <story-id>` annotation in source. | `check_ac_annotations.py` |
| G-NAME  | Manifest IDs are kebab-case and equal their filename; `task.feature` is the snake_case form of its capability id. | `check_naming.py` |
| G-REG   | `factory/product/registry.md` is a rendered index of the manifest; gate fails on any drift. | `check_registry.py` |

Run all gates, contributor and factory: `bash factory/check_all.sh`
Run with the retry loop: `bash factory/gate_retry_loop.sh`

The contributor gates are documented in `governance/docs/conventions.md`.

---

## G0 — Capability status

**Rule.** `factory/tasks/task.json.storyId` must resolve to a story file under
`factory/product/manifest/stories/`, that story's `capability` must resolve to a
capability file under `factory/product/manifest/capabilities/`, and the capability's
`status` must be `Approved` or `In Progress`.

**Why.** Code is downstream of the manifest. Starting work on a feature
whose capability is still `Draft` (PM hasn't signed off), `Unresolved`
(blocked by tech-lead decision), or `Done` (shipped — re-opening requires
explicit promotion) is the most common source of orphan work.

**Pass.**
```json
// factory/product/manifest/capabilities/transport-provider-insight.json
{ "id": "transport-provider-insight", "status": "Approved", ... }

// factory/tasks/task.json
{ "storyId": "transport-provider-breakdown", ... }
```

**Fail.**
```json
{ "status": "Draft" }      // G0 fail
{ "status": "Done" }       // G0 fail
{ "storyId": "ghost" }     // G0 fail — story file does not exist
```

**Sensor.** `factory/check_capability_status.py`. Reads task.json, walks
to the story, then to the capability, then checks status against the allowed
set.

**Scope.** `factory/tasks/task.json` + `factory/product/manifest/stories/` +
`factory/product/manifest/capabilities/`.

---

## G7 — Story-map traceability

**Rule.** The manifest graph must close.

- Every `id` in `capability.stories[]` resolves to a file under
  `factory/product/manifest/stories/`.
- Every `id` in `capability.flows[]` resolves to a file under
  `factory/product/manifest/flows/`.
- Every `story.capability` resolves AND that capability lists this story in
  its `stories[]` (bidirectional).
- Every `flow.steps[].story` (when present) resolves.
- `factory/tasks/task.json.storyId` resolves.
- No orphan stories (stories not referenced by any capability) or orphan
  flows.

**Why.** Without this gate, manifest files drift apart silently: a renamed
story leaves a dangling capability reference, a deleted flow leaves a
dangling AC trail, a refactor produces files no one reads. The pyramid stops
being a contract.

**Pass.** The current `transport-provider-insight` capability lists
`transport-provider-breakdown` story and `transport-provider-breakdown-flow`
flow; both files exist; the story's `capability` points back; no orphans.

**Fail.** Removing a story from `capability.stories[]` while keeping the
story file → orphan warning. Renaming a flow file without updating
`capability.flows[]` → broken-link warning. `task.json` pointing at a
deleted story → broken-link warning.

**Sensor.** `factory/check_story_map.py`. Loads all manifest JSON,
builds sets of known IDs, walks references, reports orphans separately.

**Scope.** `factory/product/manifest/{capabilities,stories,flows}/*.json` and
`factory/tasks/task.json`.

---

## G16 — Flow integrity

**Rule.** For every flow under `factory/product/manifest/flows/`:

- Each step with `actor: "app"` must declare either `noNavigation: true`
  OR `requires: <page-id>`. Neither is a violation; navigation intent must
  be explicit.
- Each `triggerElement: "<Name>"` (when present on a step) must name a
  Kotlin class, object, interface, or top-level function that exists
  somewhere under `app/src/main/java/com/fidriyanto/banktracker/`.
  Compose UI primitives are functions (`@Composable fun X()`), so the
  sensor accepts those too. Stale element names silently rot otherwise.

A step without a `triggerElement` is allowed; the rule only fires when one
is declared.

**Why.** Flows describe the UI's runtime contract step by step. If an app
step is silent about navigation, the agent cannot tell whether to add a
route or stay on the current screen. If a `triggerElement` references a
class that was renamed last week, the flow lies about the codebase. Both
failure modes are silent without this gate.

**Pass.**
```json
{ "actor": "app",
  "action": "displays TransportBreakdownCard below CategoryBreakdownCard",
  "story": "transport-provider-breakdown",
  "noNavigation": true,
  "triggerElement": "TransportBreakdownCard" }
```

**Fail.**
```json
{ "actor": "app", "action": "shows something" }
// G16 fail — no requires/noNavigation
```
```json
{ "actor": "app", "noNavigation": true, "triggerElement": "GhostScreen" }
// G16 fail — GhostScreen class does not exist
```

**Sensor.** `factory/check_flow_integrity.py`. Scans every `.kt`
under `app/src/main/java/` once to build a set of known class/object/
interface names, then walks every flow's `steps[]`.

**Scope.** `factory/product/manifest/flows/*.json` + `app/src/main/java/.../**.kt`.
Tests and androidTest sources are skipped.

---

## G31 — Acceptance-criteria annotations

**Rule.** For every story file, the number of
`// ac: <story-id>` annotations across `app/src/**/*.kt` must be at least
`len(story.acceptance)`. Each acceptance line must have a corresponding
code anchor.

**Why.** Acceptance criteria need to be wired through implementation, not
copied into a PR description and forgotten. The annotation forces the
developer to mark the *exact* code location that satisfies each line —
which makes review, refactor, and regression hunting traceable.

The gate counts presence rather than per-line identity (the AC strings are
prose, brittle to match verbatim) — enough to force coverage without forcing
a 1:1 string contract.

**Pass.**
```kotlin
// ui/dashboard/TransportBreakdownCard.kt
@Composable
fun TransportBreakdownCard(...) {
    // ac: transport-provider-breakdown
    val top = providers.take(3)
    // ac: transport-provider-breakdown
    val other = providers.drop(3)
    ...
}
```
(7 ACs for the story require ≥7 annotations across app/src/.)

**Fail.** No `// ac: transport-provider-breakdown` annotations present →
0/7 covered → G31 fail. Adding only 3 → 3/7 covered → G31 fail. Adding
8 → 7/7 covered (extras are fine) → G31 pass.

**Sensor.** `factory/check_ac_annotations.py`. Counts regex hits per
story id across `app/src/**/*.kt`.

**Scope.** `factory/product/manifest/stories/*.json` + `app/src/**/*.kt`. Tests
are scanned too — annotations in test sources count toward the required
total.

---

## G-NAME — Naming convention

**Rule.** Two enforced sub-rules plus a documented (unenforced) baseline.

**R-NAME-1 (enforced).** Every file under
`factory/product/manifest/{capabilities,stories,flows}/` must:
- have a kebab-case stem: `^[a-z][a-z0-9-]*[a-z0-9]$`
- contain an `id` field whose value equals the stem

**R-NAME-2 (enforced).** `factory/tasks/task.json.feature` must be the snake_case
form of the resolved capability's id. Concretely: walk
`task.storyId -> story.capability`, then `feature ==
capability.id.replace("-", "_")`. The `feature` value itself must match
snake_case: `^[a-z][a-z0-9_]*[a-z0-9]$`.

**Why.** Manifest IDs are URL-slug-shaped identifiers (filenames, JSON
keys, references across files) — kebab-case is the Web/CLI idiom for
those. Code-level identifiers (Kotlin packages, folder names, struct
fields) use underscores instead — that's the Kotlin/Python idiom. Both
conventions are correct *for their surface*; the bug is when one drifts
from the other. R-NAME-2 forces the developer to keep the
`capability.id` ↔ `task.feature` ↔ feature-folder triple in lockstep.

**Pass.**
```json
// factory/product/manifest/capabilities/transport-provider-insight.json
{ "id": "transport-provider-insight", ... }

// factory/tasks/task.json
{ "feature": "transport_provider_insight",
  "storyId": "transport-provider-breakdown" }
```

**Fail.**
```text
product/manifest/capabilities/TransportProviderInsight.json    # PascalCase stem
product/manifest/stories/transport_provider_breakdown.json     # snake stem
{ "id": "transport-provider-insight-v2" } in transport-provider-insight.json   # id != stem
tasks/task.json: { "feature": "transport-provider-insight", ... }              # kebab, not snake
tasks/task.json: { "feature": "transport_provider" }                           # doesn't match capability
```

**Sensor.** `factory/check_naming.py`.

**Scope.** `factory/product/manifest/{capabilities,stories,flows}/*.json` and
`factory/tasks/task.json`.

### Documented baseline (not yet enforced)

The following conventions are how the codebase is shaped today; promote
into G-NAME (or its own gate) as failures appear in real runs.

| Surface | Convention | Example |
|---------|-----------|---------|
| Flow id suffix | end in `-flow` | `transport-provider-breakdown-flow` |
| Feature folder under `ui/` | snake_case, equals `task.feature` | `ui/transport_provider_insight/` |
| Kotlin class per layer | suffix per G-ARCH | `*ViewModel`, `*UseCase`, `*Repository`, `*RepositoryImpl`, `*LocalDataSource`, `*RemoteDataSource`, `*Dao` |
| String resource key | `<screen>_<purpose>`, snake_case | `add_amount_label`, `settings_dark_theme` |
| Room migration object | `MIGRATION_<from>_<to>` | `MIGRATION_5_6` (G-DB) |
| AC annotation | `// ac: <story-id>` | `// ac: transport-provider-breakdown` (G31) |
| Shared resource keys | `action_<verb>` for global verbs | `action_save`, `action_dismiss`, `action_retry` |

The kebab-vs-snake split is deliberate: IDs identify *concepts* across
files (kebab); code identifies *constructs* inside the build (snake or
camel, per Kotlin convention). Both stay aligned via the conversion in
R-NAME-2.

---

## G-REG — Product registry sync

**Rule.** `factory/product/registry.md` is the rendered index of the manifest:
one table for capabilities, one for stories, one for flows, and one for
story→source AC coverage. The sensor renders the registry in-memory from
the current manifest and compares to the on-disk file. Any difference,
including whitespace, is a violation.

The file is generated — never hand-edit it. Regenerate with:
```bash
python3 factory/check_registry.py --write
```

**Why.** A human-readable index is only useful if it stays accurate. A
hand-maintained registry drifts within days as stories are renamed and
capabilities promote. Treating the file as a build artifact (sensor +
regenerator) keeps drift impossible. The registry also surfaces the
G31 AC coverage at a glance, so reviewers can see exactly how many
acceptance criteria still lack `// ac:` annotations.

**Pass.** `factory/product/registry.md` byte-for-byte equals
`render(manifest/, ac-annotation-counts)`.

**Fail.** Manifest changes without re-running the generator; hand-editing
the registry; deleted, added, or renamed manifest files without regen.
Sensor reports the first differing line and the regen command.

**Sensor.** `factory/check_registry.py`. Same script doubles as the
generator (`--write`). Both modes share the same `render(...)` function
so a sensor pass guarantees the on-disk file matches what `--write`
would produce.

**Scope.** `factory/product/registry.md`, `factory/product/manifest/{capabilities,stories,flows}/*.json`,
and `app/src/**/*.kt` (for AC coverage counts).

---

## How to add a new factory gate

Follow factory blueprint "How to Add a New Gate" (`factory/blueprint.md`):

1. Spot a failure mode the agent shipped subtly wrong; existing gates did not catch.
2. Document the rule here with pass and fail examples.
3. Write `factory/check_<rule>.py`. Exit 1 on violation and list every offender.
4. Add a row to the table at the top of this file and to `GATES=(...)` in `factory/check_all.sh`.
5. Fix the existing codebase until the new gate is green before you merge the gate.
6. Run `bash factory/gate_retry_loop.sh` on the next feature to verify the gate fires correctly.
