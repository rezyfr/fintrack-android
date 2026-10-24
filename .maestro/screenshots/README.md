# README screenshots

These flows take the screenshots in `docs/screenshots`. They use the fake server and demo data only.

1. Set a clean status bar on the emulator:
   `adb -s emulator-5554 shell settings put global sysui_demo_allowed 1`, then send the `com.android.systemui.demo` broadcasts `enter`, `clock` (hhmm 0941), `battery` (level 100), `network` (wifi show, mobile hide).
2. Copy `light.yaml`, `dark.yaml` and `features.yaml` into `flows/pr/`. `light.yaml` sets its own data. `dark.yaml` needs the seed `showcase`.
3. Run `bash .maestro/scripts/run_all.sh --flow features` and `--flow light`. The `features` flow takes the budget chart and the schedules. It needs the seed `showcase` and creates the schedules through the screens. Then run `adb -s emulator-5554 shell cmd uimode night yes` and `bash .maestro/scripts/run_all.sh --flow dark --no-build --no-install`. Switch back with `cmd uimode night no`.
4. Run `python3 .maestro/screenshots/build_images.py` to crop the screenshots and build the strips and the banner.
5. Remove the three copied flows from `flows/pr/`.
