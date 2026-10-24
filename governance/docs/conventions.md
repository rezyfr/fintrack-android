# Conventions

This is the long-form rule book for the gates that CI and `governance/check_all.sh` run.
Each section documents one gate: the rule, why it exists, pass and fail examples, the sensor, and its scope.

If a rule lives here but no sensor enforces it, it is a suggestion, not a rule.
Every section below points to a real `governance/check_*` script.

| Gate    | Rule (one line)                                                          | Sensor                          |
| ------- | ------------------------------------------------------------------------ | ------------------------------- |
| G1      | UI layer may not touch the backend or HTTP infrastructure directly.         | `check_architecture.py`         |
| G1-W    | Web files outside `web/src/api/` may not reference the server address or token. | `check_architecture_web.py`  |
| G3      | No hardcoded `Color(...)` literals outside `ui/theme/`.                  | `check_android_tokens.py`       |
| G-TYPE  | No hardcoded text sizes or font families outside `ui/theme/`.            | `check_type_scale.py`           |
| G-STR   | No hardcoded user-facing strings inside `Text(...)` composables.         | `check_strings.py`              |
| G-ARCH  | Layered architecture + SOLID dependency-inversion (R1-R9).               | `check_architecture_solid.py`   |
| G-DB    | Room database version bumps must come with a registered migration.       | `check_room_migration.py`       |
| G-NEUTRAL | App code does not name a specific currency, bank account or city.        | `check_neutrality.py`           |
| G-SENSITIVE | No secrets, personal e-mail addresses, home paths or private files are tracked. | `check_sensitive.py`            |
| G-BUILD | The Android module must compile cleanly (`./gradlew :app:compileDebugKotlin`). | `check_build.sh` |
| G-FMT   | Android Kotlin source must be Spotless-clean (`./gradlew :app:spotlessCheck`). | `check_format.sh` |

Run all gates: `bash governance/check_all.sh`

The opt-in agent harness in `factory/` adds manifest gates on top of these. See `factory/README.md`.

---

## G1 — UI layer architecture

**Rule.** Files under `app/.../ui/` (excluding tests) may not reference
`OkHttpClient`, `Request.Builder()`, `BuildConfig.API_BASE_URL`, or
`BuildConfig.API_TOKEN`.

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
            .url("${BuildConfig.API_BASE_URL}/...")  // G1 violation
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
directories, may not reference `VITE_API_BASE_URL` or `VITE_API_TOKEN`.
They also may not bring back `VITE_SUPABASE_URL`, `VITE_SUPABASE_ANON_KEY`
or hardcoded `https://*.supabase.co` URLs.
Components consume the API only via imports from `web/src/api/`.

**Why.** Same reasoning as G1 on the Android side: the UI layer is the
wrong place to know about transport details. The api/ folder is the
single seam where the server enters the React codebase; widening that
surface to components creates duplicate request paths, makes mocking
brittle, and leaks env-token references into render trees.

**Pass.**
```jsx
// web/src/components/TransactionList.jsx
import { getTransactions } from "../api/client";
```

**Fail.**
```jsx
// web/src/components/TransactionList.jsx
const url = import.meta.env.VITE_API_BASE_URL;            // G1-W fail
const token = import.meta.env.VITE_API_TOKEN;             // G1-W fail
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

## G-TYPE — Android type scale

**Rule.** UI code outside `ui/theme/` may not set `fontSize`, `lineHeight` or
`fontFamily`, and may not build a `TextStyle(...)` from scratch. Text takes its
metrics from one role in `AppTypography`.

**Why.** Before this gate the app wrote 273 `fontSize` values across 22 distinct
sizes, and 206 of them sat between 11sp and 14sp. A heading, a row name, a money
figure and a caption all rendered at nearly the same size, so no screen had a
hierarchy. One scale in `ui/theme/Type.kt` fixes that, and only holds if call
sites cannot opt out.

**Pass.**
```kotlin
Text("…", style = MaterialTheme.typography.titleMedium)
Text("…", style = MaterialTheme.typography.bodySmall, color = onSurfaceVariant)
val entry = MaterialTheme.typography.displayLarge.copy(textAlign = TextAlign.Start)
```

**Fail.**
```kotlin
Text("…", fontSize = 13.sp)                          // G-TYPE violation
Text("…", fontFamily = Fraunces, fontSize = 22.sp)   // G-TYPE violation
val big = TextStyle(fontSize = 46.sp)                // G-TYPE violation
```

**Allowed.** `fontWeight` stays. It is a deliberate override on top of a role,
not a second scale.

**Sensor.** `governance/check_type_scale.py`. Regex scan. Comments skipped.

**Scope.** Any `.kt` under `app/src/main/java/.../`. Skipped: `ui/theme/`,
`test/`, `androidTest/`. The roles, and the two oversized onboarding styles
`SetupNumeral` and `SetupSymbol`, live in `ui/theme/Type.kt`.

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
| **R5** | `okhttp3`, `BuildConfig.API_*`, and `Request.Builder()` may appear only in `data/datasource/remote/`, `notification/`, `service/`, `di/`. |
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
    private val fetcher: HttpTransactionFetcher // R1 violation
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

## G-FMT — Format

**Rule.** `./gradlew :app:spotlessCheck` must exit 0. Spotless runs ktlint
against every `.kt` file under `app/src/` and every `*.gradle.kts` file,
using the rule selection in `app/build.gradle.kts`'s `editorConfigOverride`
(and mirrored, for IDE-only guidance, in the root `.editorconfig`).

**Why.** Before this gate, the codebase had no enforced formatting
convention: indentation drifted, manual column-alignment (`val x   = 1`)
competed with single-space style from file to file, and nothing caught
either. G-ARCH checks *architecture* (layering, naming, size caps); G-FMT
checks *whitespace* — a different axis that no other gate covered.

The rule selection is deliberately narrower than ktlint's full default
ruleset. The whole `standard` rule set starts disabled, then specific
rules are re-enabled one at a time: `indent`, `final-newline`,
`no-trailing-whitespaces`, `no-multi-spaces`, `no-consecutive-blank-lines`,
`no-blank-line-before-rbrace`, `no-empty-first-line-in-class-body`,
`no-leading-empty-lines-in-method-blocks`, `spacing`, `no-semicolons`,
`string-template-indent`. Enabling ktlint's full default ruleset instead
would force trailing commas, function-signature line-wrapping, and
naming-convention checks onto a codebase that never had them — a much
bigger, noisier, unrequested change than "make the whitespace
consistent." Add more rules to the enabled list only as a deliberate,
separate decision, not as a side effect of a version bump.

Applying a plain `.editorconfig` alone did not work: Spotless's `ktlint()`
step did not pick up per-rule `ktlint_standard_*` overrides from a root
`.editorconfig` file in testing (confirmed by an unchanged violation set
across three different `.editorconfig` rewrites). The rule selection that
actually takes effect lives in the `editorConfigOverride(...)` map passed
directly to `ktlint()` in `app/build.gradle.kts`; the `.editorconfig` file
is kept only for IDE-level hints (indent size, final newline, trim
trailing whitespace) and must be kept in sync with the Gradle config by
hand if either changes.

**Pass.** `./gradlew :app:spotlessCheck` succeeds; the gate prints
`G-FMT PASS: :app:spotlessCheck succeeded`.

**Fail.** Spotless reports one or more files with formatting violations
and a unified diff of the expected change per file.
```
The following files had format violations:
    src/main/java/.../FooViewModel.kt
        @@ -12,7 +12,6 @@
        ...
Run './gradlew spotlessApply' to fix all violations.
```

**Sensor.** `governance/check_format.sh`. Bash, mirrors `check_build.sh`'s
shape: wraps `./gradlew :app:spotlessCheck`, surfaces the violation
summary on failure, points at the fix command
(`./gradlew :app:spotlessApply`).

**Scope.** `app/src/**/*.kt` and `*.gradle.kts` at the repo root and in
`app/`. Web (`web/`) is out of scope — it already has ESLint as a
dependency; wiring it into a gate is a separate decision.

---

## How to add a new gate

1. Find a failure mode that existing gates did not catch.
2. Document the rule here with pass and fail examples.
3. Write `governance/check_<rule>.py`. Exit 1 on violation and list every offender.
4. Add a row to the table at the top of this file and to `GATES=(...)` in `governance/check_all.sh`.
5. Fix the existing codebase until the new gate is green before you merge the gate. A red baseline blocks every contributor.

---

## G-NEUTRAL — No specific currency, bank or city

**Rule.** Non-test code in `app/src/main` and `web/src` may not use the currency codes
`IDR` and `THB` as string literals, the wallet ids `BBL`, `BCA`, `BCA_CC`, `MANDIRI` and `MANDIRI_CC`,
the names `Bualuang` and `myBCA`, the labels `Indonesia` and `Thailand`, the time zone `Asia/Bangkok`,
the symbols `Rp` and the baht sign, or the fixed rate `535`. Comment lines and tests are skipped.

**Why.** A user picks the home currency, the accounts and the pay cycle on first run. A currency, a
wallet id or a city in the code ties the app to one person's setup.

**Pass.**
```kotlin
val code = CurrencySettingsStore.current.homeCode
val name = AccountStore.nameOf(wallet)
```

**Fail.**
```kotlin
if (wallet == "BBL") ...            // G-NEUTRAL violation
val rate = if (code == "THB") 535.0 else 1.0
```

**Sensor.** `governance/check_neutrality.py`. Regex scan. The `ALLOWED` table in the script lists the
few files that may keep these values, each with a reason: the defaults for an install that never saved
settings, the shared symbol table, the built-in BCA and Bangkok Bank notification patterns, the currency
words in notification text, the setup suggestion chips, and the legacy ledger tab label.

**Scope.** Android main sources (`.kt`, `.xml`), web sources (`.js`, `.jsx`).

## G-SENSITIVE: No sensitive data

The gate checks every tracked text file and the messages of commits not yet in `origin/master`.

| Check | Example that fails |
|---|---|
| Secret patterns | A JWT, an AWS key, a Supabase project URL, a filled-in `SUPABASE_ANON_KEY`, a filled-in `API_TOKEN` |
| Personal e-mail address | `jane@gmail.com` (use `owner@example.com`) |
| Absolute home path | `/Users/you/...` (use `<repo>/...`) |
| Private tracked file | `local.properties`, `.env`, `google-services.json`, a keystore, a model file |
| Personal term | Any regular expression in `governance/sensitive-terms.local.txt` |

Copy `governance/sensitive-terms.example.txt` to `governance/sensitive-terms.local.txt` and add your own names, merchants and amounts.
Git ignores that file. CI has no local list, so it runs the first four checks only.
