# Manual cases

These cases have no Maestro flow. Run them by hand before a release.

| Case | Reason |
|---|---|
| E2E-117 | Needs a fake model download server and a stub file path. Not built. |
| E2E-118 | Needs a fake model download server that returns 500. Not built. |
| E2E-119 | Needs a stub model file at the private model path. The path is not reachable from adb on a debug build without extra tooling. |
| E2E-121 | Needs a test build with a fake on-device engine. |
| E2E-122 | Needs a test build with a fake on-device engine. |
| E2E-133 | Needs a test build with a fake on-device engine. |
| E2E-135 | Needs an old APK in `.maestro/apks`. `scripts/install_old_apk.sh` exits with code 3 when none exists. |
| E2E-136 | Needs APKs at older database versions in `.maestro/apks`. Also run `python3 governance/check_room_migration.py`. |
| E2E-137 | Needs an old APK in `.maestro/apks`. |
| E2E-138 | Needs an old APK in `.maestro/apks`. |
| E2E-026 | Skipped, not manual. The code saves a captured row as synced at once (`TransactionRepositoryImpl.processNewNotification`), so no Pending row with Confirm and Dismiss exists. The doc row is stale. |
