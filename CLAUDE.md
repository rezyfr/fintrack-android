# FinTrack

`fintrack-android` is a personal finance tracker: an Android app (`app/`) and a React web app (`web/`). Both call the FinTrack server through its REST API. The server is a separate project.
The `governance/` folder holds the gates that check the architecture, build and format rules.
CI runs the same gates.

## Architecture

The Android app follows a layered pyramid:

```
ViewModel  ->  UseCase  ->  Repository  ->  DataSource  ->  Dao / Service
 (ui/)        (domain/    (data/         (data/         (data/db/,
               usecase/)   repository/)   datasource/)   sheets/, ...)
```

A layer injects only the layer directly below it.
The `G-ARCH` gate enforces this with the class-name suffixes (`*UseCase`, `*Repository`, `*DataSource`).

## Approach

- Read existing files before writing. Do not re-read unless changed.
- Match the style of the surrounding code.
- No emojis or em-dashes.
- Do not guess APIs, versions, flags, commit SHAs or package names. Verify by reading code or docs.

## Gate commands

```bash
bash governance/check_all.sh              # run every contributor gate
python3 governance/check_<name>.py        # run a single gate
./gradlew :app:spotlessApply              # fix Kotlin formatting
```

Gates exit 0 on pass and 1 on fail.

## Rules the gates enforce

| Rule | Gate |
|---|---|
| UI layer must not touch the backend or HTTP infrastructure | G1 |
| Web UI layer must route server calls through `web/src/api/` | G1-W |
| No hardcoded `Color(...)` literals outside `ui/theme/` | G3 |
| No hardcoded text sizes or font families outside `ui/theme/` | G-TYPE |
| No hardcoded user-facing strings inside `Text(...)` composables | G-STR |
| Layered architecture and dependency inversion (R1 to R9) | G-ARCH |
| Room `version` bumps come with a registered, unbroken migration chain | G-DB |
| App code does not name a specific currency, bank account or city | G-NEUTRAL |
| No secrets, personal e-mail addresses, home paths or private files in tracked files or new commit messages | G-SENSITIVE |
| Android module must compile (`./gradlew :app:compileDebugKotlin`) | G-BUILD |
| Kotlin source must be Spotless-clean (`./gradlew :app:spotlessCheck`) | G-FMT |

The full rule book with pass and fail examples is in `governance/docs/conventions.md`.

## Adding a feature

1. Implement it under the existing layers.
2. Run `bash governance/check_all.sh` and fix every failure.
3. Add or update tests. Web tests run with `npm test` in `web/`.
4. Open a pull request.

## Optional: the factory harness

`factory/` holds an opt-in workflow that traces every change from a product manifest to code.
It has its own gates, a task file and a worklog.
Do not follow it unless the user turns it on. See `factory/README.md`.
