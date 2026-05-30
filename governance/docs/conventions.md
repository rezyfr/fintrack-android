# Conventions

This is the long-form rule book that the gate sensors enforce. Each section
documents one gate. The format mirrors factory-blueprint Phase 1.2:

> Rule. Why. Examples. Sensor. Scope.

If a rule lives here but no sensor enforces it, it is a suggestion, not a
rule. Every section below points to a real `governance/check_*.py` script.

| Gate    | Rule (one line)                                                          | Sensor                          |
| ------- | ------------------------------------------------------------------------ | ------------------------------- |
| G0      | `task.json` must target a story whose capability is Approved/In Progress. | `check_capability_status.py`    |
| G1      | UI layer may not touch Supabase or HTTP infrastructure directly.         | `check_architecture.py`         |
| G1-W    | Web files outside `web/src/api/` may not reference Supabase tokens or URLs. | `check_architecture_web.py`  |
| G3      | No hardcoded `Color(...)` literals outside `ui/theme/`.                  | `check_android_tokens.py`       |
| G-STR   | No hardcoded user-facing strings inside `Text(...)` composables.         | `check_strings.py`              |
| G-ARCH  | Layered architecture + SOLID dependency-inversion (R1-R9).               | `check_architecture_solid.py`   |
| G-DB    | Room database version bumps must come with a registered migration.       | `check_room_migration.py`       |
| G7      | Manifest graph (capabilities, stories, flows) has no broken links or orphans. | `check_story_map.py`       |
| G16     | Flow `actor==app` steps declare navigation; every `triggerElement` resolves to a real class. | `check_flow_integrity.py` |
| G31     | Every `story.acceptance[]` line is covered by a `// ac: <story-id>` annotation in source. | `check_ac_annotations.py` |
| G-NAME  | Manifest IDs are kebab-case and equal their filename; `task.feature` is the snake_case form of its capability id. | `check_naming.py` |
| G-REG   | `product/registry.md` is a rendered index of the manifest; gate fails on any drift. | `check_registry.py` |
| G-BUILD | The Android module must compile cleanly (`./gradlew :app:compileDebugKotlin`). | `check_build.sh` |

Run all gates: `bash governance/check_all.sh`
Run with retry loop: `bash governance/gate_retry_loop.sh`

---

## G0 — Capability status

**Rule.** `tasks/task.json.storyId` must resolve to a story file under
`product/manifest/stories/`, that story's `capability` must resolve to a
capability file under `product/manifest/capabilities/`, and the capability's
`status` must be `Approved` or `In Progress`.

**Why.** Code is downstream of the manifest. Starting work on a feature
whose capability is still `Draft` (PM hasn't signed off), `Unresolved`
(blocked by tech-lead decision), or `Done` (shipped — re-opening requires
explicit promotion) is the most common source of orphan work.

**Pass.**
```json
// product/manifest/capabilities/transport-provider-insight.json
{ "id": "transport-provider-insight", "status": "Approved", ... }

// tasks/task.json
{ "storyId": "transport-provider-breakdown", ... }
```

**Fail.**
```json
{ "status": "Draft" }      // G0 fail
{ "status": "Done" }       // G0 fail
{ "storyId": "ghost" }     // G0 fail — story file does not exist
```

**Sensor.** `governance/check_capability_status.py`. Reads task.json, walks
to the story, then to the capability, then checks status against the allowed
set.

**Scope.** `tasks/task.json` + `product/manifest/stories/` +
`product/manifest/capabilities/`.

---

## G1 — UI layer architecture

**Rule.** Files under `app/.../ui/` (excluding tests) may not reference
`OkHttpClient`, `Request.Builder()`, `BuildConfig.SUPABASE_URL`, or
`BuildConfig.SUPABASE_ANON_KEY`.

**Why.** The UI layer is the wrong place to know about transport details. Once
HTTP leaks into a Composable or a ViewModel, swapping the backend, mocking
for tests, and reasoning about lifecycles all get harder. Repositories or
DataSources are the authorised owners of those concerns.

**Pass.**
```kotlin
// ui/dashboard/DashboardViewModel.kt
class DashboardViewModel @Inject constructor(
    private val getMonthlyOverview: GetMonthlyOverviewUseCase,
) : ViewModel() { ... }
```

**Fail.**
```kotlin
// ui/feed/FeedViewModel.kt
class FeedViewModel @Inject constructor(
    private val http: OkHttpClient,                  // G1 violation
) : ViewModel() {
    fun refresh() {
        val req = Request.Builder()                  // G1 violation
            .url("${BuildConfig.SUPABASE_URL}/...")  // G1 violation
            .build()
    }
}
```

**Sensor.** `governance/check_architecture.py`. Single-line regex scan.
Comments are skipped.

**Scope.** Any `.kt` under `app/src/main/java/.../ui/`. Skipped: `test/`,
`androidTest/`. Exemptions: `sheets/`, `di/` (infrastructure wiring).

---

## G1-W — Web UI layer architecture

**Rule.** Files under `web/src/`, excluding `web/src/api/` and dependency
directories, may not reference `VITE_SUPABASE_URL`,
`VITE_SUPABASE_ANON_KEY`, or hardcoded `https://*.supabase.co` URLs.
Components consume the API only via imports from `web/src/api/`.

**Why.** Same reasoning as G1 on the Android side: the UI layer is the
wrong place to know about transport details. The api/ folder is the
single seam where Supabase enters the React codebase; widening that
surface to components creates duplicate request paths, makes mocking
brittle, and leaks env-token references into render trees.

**Pass.**
```jsx
// web/src/components/TransactionList.jsx
import { fetchTransactions } from "../api/supabase";
```

**Fail.**
```jsx
// web/src/components/TransactionList.jsx
const url = import.meta.env.VITE_SUPABASE_URL;            // G1-W fail
fetch(`https://abc.supabase.co/rest/v1/transactions`);    // G1-W fail
```

**Sensor.** `governance/check_architecture_web.py`. Line-level regex
scan over `web/src/**/*.{js,jsx,ts,tsx}`. Comments (`//` or `*` lines)
are skipped.

**Scope.** `web/src/**` except `web/src/api/`, `node_modules/`, `dist/`,
`build/`. Test files outside `web/src/api/` are NOT exempt — they should
mock the api/ layer rather than reach for the raw tokens.

---

## G3 — Android colour tokens

**Rule.** UI code outside `ui/theme/` may not use `Color.White / Black /
Gray / DarkGray / LightGray / Red / Green / Blue / Yellow / Cyan / Magenta`
or `Color(0x...)` literal constructors.

**Why.** Hardcoded colours defeat theming and dark-mode. A single repaint
should change one token in `ui/theme/`, not hundreds of call sites. Semantic
colours (`MaterialTheme.colorScheme.*`, `LocalAppColors.current.*`) keep that
contract.

**Pass.**
```kotlin
Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
val warning = LocalAppColors.current.warning
```

**Fail.**
```kotlin
Text("…", color = Color.Gray)            // G3 violation
val accent = Color(0xFFCCAABB)           // G3 violation
```

**Sensor.** `governance/check_android_tokens.py`. Regex scan. Comments skipped.
`Color.Transparent` is allowed because it carries semantic meaning, not a hue.

**Scope.** Any `.kt` under `app/src/main/java/.../`. Skipped: `ui/theme/`,
`test/`, `androidTest/`. Raw `Color` values are *required* inside
`ui/theme/Color.kt` and `ui/theme/AppColors.kt`; that is where the tokens
themselves live.

---

## G-STR — String literals

**Rule.** User-facing string literals passed to a Compose `Text(...)` call
inside `ui/` must come from `res/values/strings.xml` via
`stringResource(R.string.<key>)`.

**Why.** Hardcoded copy can't be translated, can't be A/B tested, and
encourages duplicated strings drifting out of sync. Resources are the only
place where strings get a stable, reviewable surface.

A literal is considered user-facing when it contains at least one ASCII
letter. Pure-symbol strings (`"●"`, `" · "`, `"%s"`) are decorative and pass.

**Pass.**
```kotlin
Text(stringResource(R.string.add_title))
Text(stringResource(R.string.add_amount_label, currency))   // %1$s format arg
Text("●", color = dotColor)                                 // pure symbol — OK
```

**Fail.**
```kotlin
Text("Settings")                              // G-STR violation
Text("Amount (${state.wallet.currency})")     // G-STR violation (template)
label = { Text("Description") }               // G-STR violation
```

**Sensor.** `governance/check_strings.py`. Matches `Text(...)` and
`label = { Text(...) }` patterns on a single line. Multi-line `Text(\n
"literal")` and string literals inside `if`/`when` expressions are known
blind spots — promote to AST if the team starts slipping violations past
them.

**Scope.** Any `.kt` under `app/src/main/java/.../ui/`. Skipped:
`ui/theme/`, `test/`, `androidTest/`. Resource keys live in
`app/src/main/res/values/strings.xml`.

---

## G-ARCH — Layered architecture + SOLID

**Rule.** Code must follow the canonical pyramid

```
ViewModel  →  UseCase  →  Repository  →  DataSource  →  Dao / Service
 (ui/)       (domain/    (data/         (data/         (data/db/,
              usecase/)   repository/)   datasource/)   sheets/, …)
```

with every layer depending on the *interface* of the layer below — never on
its concrete implementation, never on a layer two hops away, never on a
layer above. The sensor enforces nine sub-rules:

| Sub-rule | Statement |
|----------|-----------|
| **R1** | A `*ViewModel.kt` may inject only types ending in `UseCase` (plus `SavedStateHandle`). |
| **R2** | A `*UseCase.kt` may inject only types ending in `Repository` (the interface). |
| **R3** | A `*RepositoryImpl.kt` may inject only types ending in `DataSource` (the interface). |
| **R4** | `*LocalDataSource.kt` injects only `*Dao`. `*RemoteDataSource.kt` injects only `*Service` / `*Api`. |
| **R5** | `okhttp3`, `BuildConfig.SUPABASE_*`, and `Request.Builder()` may appear only in `data/datasource/remote/`, `notification/`, `service/`, `di/`. |
| **R6** | Each repository name `X` must have BOTH `interface XRepository` AND `class XRepositoryImpl`. |
| **R7** | Files under `domain/` may not import `android.*`, `androidx.*`, `okhttp3.*`, `data.db.*`, `data.datasource.*`, or `dagger.hilt.android.*`. |
| **R8** | A file in `ui/`, `domain/`, or `data/` declares at most one *main* public type (interface or non-data, non-sealed, non-enum, non-abstract class). |
| **R9** | Soft size caps: `*ViewModel.kt` ≤ 200 lines, `*UseCase.kt` ≤ 80, `*RepositoryImpl.kt` ≤ 150. |

**Why.** Dependency-inversion (R1-R4, R6) makes layers swappable for tests
and feature flags. Domain purity (R7) lets business rules be reasoned about
without Android in scope. Infra confinement (R5) keeps the HTTP surface
small and reviewable. SRP proxies (R8, R9) push large classes into smaller
ones before they become unmaintainable.

**Pass.**
```kotlin
// ui/dashboard/DashboardViewModel.kt
class DashboardViewModel @Inject constructor(
    private val getMonthlyOverview: GetMonthlyOverviewUseCase,   // R1 OK
) : ViewModel()

// data/repository/TransactionRepository.kt
interface TransactionRepository { ... }                          // R6 OK

// data/repository/TransactionRepositoryImpl.kt
class TransactionRepositoryImpl @Inject constructor(
    private val local: TransactionLocalDataSource,               // R3 OK
    private val sync:  TransactionSyncDataSource,                // R3 OK
) : TransactionRepository
```

**Fail.**
```kotlin
// ui/feed/FeedViewModel.kt
class FeedViewModel @Inject constructor(
    private val repo:    TransactionRepository,     // R1 violation
    private val fetcher: SupabaseTransactionFetcher // R1 violation
) : ViewModel()
```

**Sensor.** `governance/check_architecture_solid.py`. Regex-based
constructor and import scan; per-file plus a cross-file pairing pass for R6.
Promote to AST if aliased imports or split constructors slip violations
past the regex.

**Scope.** Layer classification by file path + filename suffix:
- `ui/**/*ViewModel.kt` → viewmodel
- `domain/usecase/**/*UseCase.kt` → usecase
- `data/repository/**/*.kt` → repository
- `data/datasource/local/**/*LocalDataSource.kt` → local data source
- `data/datasource/remote/**/*RemoteDataSource.kt` → remote data source

Files outside these paths are not subject to R1-R4. R5 (infra confinement),
R7 (domain purity), R8 (one-main-type), and R9 (size caps) apply across
their respective scopes regardless of layer.

---

## G-DB — Room migration chain

**Rule.** When `AppDatabase.kt` declares `version = N`, an unbroken
migration chain from `MIGRATION_(N_min)_(N_min+1)` through
`MIGRATION_(N-1)_N` must be present in `AppDatabase.kt`, and every
`MIGRATION_X_Y` object declared there must also appear in
`AppModule.addMigrations(...)`.

**Why.** The two most common Room mistakes are (a) bumping `version` without
writing the migration, which crashes the app on first install upgrade, and
(b) writing the migration but forgetting to register it in the Hilt module,
which silently falls back to destructive recreation. The gate catches both
before the change reaches a user device.

**Pass.**
```kotlin
// data/db/AppDatabase.kt
@Database(entities = [...], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) { override fun migrate(db: SupportSQLiteDatabase) { ... } }
        // ... MIGRATION_4_5, MIGRATION_3_4, MIGRATION_2_3 also present ...
    }
}

// di/AppModule.kt
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
    .build()
```

**Fail.**
```kotlin
@Database(entities = [...], version = 6)   // bumped, but no MIGRATION_5_6 → G-DB fail
```
```kotlin
// MIGRATION_5_6 declared in AppDatabase but missing from addMigrations(...) → G-DB fail
```

**Sensor.** `governance/check_room_migration.py`. Reads `AppDatabase.kt` and
`AppModule.kt` once, parses `version = N`, the set of declared
`MIGRATION_X_Y` objects, and the set of registered ones; verifies the chain
is unbroken from the minimum source version to `N` and that every declared
migration is also registered.

**Scope.** `app/src/main/java/.../data/db/AppDatabase.kt` and
`app/src/main/java/.../di/AppModule.kt`. Both files must exist; the gate
fails fast if either is missing.

---

## G7 — Story-map traceability

**Rule.** The manifest graph must close.

- Every `id` in `capability.stories[]` resolves to a file under
  `product/manifest/stories/`.
- Every `id` in `capability.flows[]` resolves to a file under
  `product/manifest/flows/`.
- Every `story.capability` resolves AND that capability lists this story in
  its `stories[]` (bidirectional).
- Every `flow.steps[].story` (when present) resolves.
- `tasks/task.json.storyId` resolves.
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

**Sensor.** `governance/check_story_map.py`. Loads all manifest JSON,
builds sets of known IDs, walks references, reports orphans separately.

**Scope.** `product/manifest/{capabilities,stories,flows}/*.json` and
`tasks/task.json`.

---

## G16 — Flow integrity

**Rule.** For every flow under `product/manifest/flows/`:

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

**Sensor.** `governance/check_flow_integrity.py`. Scans every `.kt`
under `app/src/main/java/` once to build a set of known class/object/
interface names, then walks every flow's `steps[]`.

**Scope.** `product/manifest/flows/*.json` + `app/src/main/java/.../**.kt`.
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

**Sensor.** `governance/check_ac_annotations.py`. Counts regex hits per
story id across `app/src/**/*.kt`.

**Scope.** `product/manifest/stories/*.json` + `app/src/**/*.kt`. Tests
are scanned too — annotations in test sources count toward the required
total.

---

## G-NAME — Naming convention

**Rule.** Two enforced sub-rules plus a documented (unenforced) baseline.

**R-NAME-1 (enforced).** Every file under
`product/manifest/{capabilities,stories,flows}/` must:
- have a kebab-case stem: `^[a-z][a-z0-9-]*[a-z0-9]$`
- contain an `id` field whose value equals the stem

**R-NAME-2 (enforced).** `tasks/task.json.feature` must be the snake_case
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
// product/manifest/capabilities/transport-provider-insight.json
{ "id": "transport-provider-insight", ... }

// tasks/task.json
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

**Sensor.** `governance/check_naming.py`.

**Scope.** `product/manifest/{capabilities,stories,flows}/*.json` and
`tasks/task.json`.

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

**Rule.** `product/registry.md` is the rendered index of the manifest:
one table for capabilities, one for stories, one for flows, and one for
story→source AC coverage. The sensor renders the registry in-memory from
the current manifest and compares to the on-disk file. Any difference,
including whitespace, is a violation.

The file is generated — never hand-edit it. Regenerate with:
```bash
python3 governance/check_registry.py --write
```

**Why.** A human-readable index is only useful if it stays accurate. A
hand-maintained registry drifts within days as stories are renamed and
capabilities promote. Treating the file as a build artifact (sensor +
regenerator) keeps drift impossible. The registry also surfaces the
G31 AC coverage at a glance, so reviewers can see exactly how many
acceptance criteria still lack `// ac:` annotations.

**Pass.** `product/registry.md` byte-for-byte equals
`render(manifest/, ac-annotation-counts)`.

**Fail.** Manifest changes without re-running the generator; hand-editing
the registry; deleted, added, or renamed manifest files without regen.
Sensor reports the first differing line and the regen command.

**Sensor.** `governance/check_registry.py`. Same script doubles as the
generator (`--write`). Both modes share the same `render(...)` function
so a sensor pass guarantees the on-disk file matches what `--write`
would produce.

**Scope.** `product/registry.md`, `product/manifest/{capabilities,stories,flows}/*.json`,
and `app/src/**/*.kt` (for AC coverage counts).

---

## G-BUILD — Android compile

**Rule.** `./gradlew :app:compileDebugKotlin` must exit 0. Any Kotlin
compilation error fails the gate.

**Why.** Every other sensor is structural — they check rules over file
contents but never invoke the Kotlin compiler. A change can satisfy all
the rule-based gates and still be unbuildable: wrong import path, missing
brace, calling a scope-extension by fully qualified name, an
`@Composable` invocation outside a composable context. Without this gate
those errors only surface at IDE time or at the user's `./gradlew`. The
gate puts compile correctness on the same audit line as everything else.

The gate is compile-only (`compileDebugKotlin`), not a full
`assembleDebug`, to keep retry-loop iterations cheap. A typical cold run
is 5–15 seconds; cached runs are under 3.

**Pass.** Build succeeds; the gate prints
`G-BUILD PASS: :app:compileDebugKotlin succeeded`.

**Fail.** Build emits `e:` (error) lines. The gate echoes the first ~40
lines of compiler output and points at the regen command:
```
./gradlew :app:compileDebugKotlin
```

**Sensor.** `governance/check_build.sh`. Bash, ~25 lines. Wraps the
gradle invocation, captures stdout/stderr to a temp log, surfaces
compiler error lines on failure.

**Scope.** `app/` Kotlin sources only. Web (`web/`) has its own future
companion gate (G-BUILD-W) when the team is ready to layer it in.

---

## How to add a new gate

Follow factory-blueprint "How to Add a New Gate":

1. Spot a failure mode the agent shipped subtly wrong; existing gates didn't catch.
2. Document the rule here with ✓ / ✗ examples.
3. Write `governance/check_<rule>.py` — exit 1 on violation, list every offender.
4. Add a row to the table at the top of this file and to `GATES=(…)` in `governance/check_all.sh`.
5. Backfill the existing codebase until the new gate is green BEFORE merging the gate itself. Red baselines block every future commit.
6. Run `bash governance/gate_retry_loop.sh` on the next feature to verify the gate fires correctly.
