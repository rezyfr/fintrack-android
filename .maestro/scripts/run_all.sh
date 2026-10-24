#!/usr/bin/env bash
# Run the Maestro flows on the emulator against the fake server.
#
# Usage: run_all.sh [options]
#   --tags pr,nightly   run only flows with one of these tags (default: all flows)
#   --flow E2E-010      run only flows whose file name contains this text
#   --no-build          reuse the APK that is already built
#   --no-install        reuse the APK that is already installed
#   --no-retry          do not run a failed flow a second time
#   --keep-server       leave the fake server running at the end
#
# Each flow starts with header comments that tell this script how to prepare the server:
#   # seed: base                     seed set to load (see data/seeds.py). No line means an empty server.
#   # fault: POST accounts=500       fault switch: <route>=<mode>. Modes: 500, 401, slow, empty.
#   # adb: set_font_scale.sh 1.3     helper script from scripts/ to run before the flow
#   # after: set_font_scale.sh 1.0   helper script to run after the flow
#   # manual: <reason>               do not run the flow, report it as manual
#
# Safety: only emulator-5554 is used. The settings of local.properties and the debug manifest are
# put back when the script ends, also after a failure.
set -u
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

TAGS=""; ONLY=""; BUILD=1; INSTALL=1; KEEP=0; RETRY=1
while [ $# -gt 0 ]; do
  case "$1" in
    --tags) TAGS="$2"; shift 2 ;;
    --flow) ONLY="$2"; shift 2 ;;
    --no-build) BUILD=0; shift ;;
    --no-install) INSTALL=0; shift ;;
    --no-retry) RETRY=0; shift ;;
    --keep-server) KEEP=1; shift ;;
    *) echo "Unknown option $1" >&2; exit 2 ;;
  esac
done

REPORTS="$MAESTRO_DIR/reports"
mkdir -p "$REPORTS"
# One run at a time: two runs would fight over the same emulator.
LOCK="$REPORTS/run.lock"
if [ -e "$LOCK" ] && kill -0 "$(cat "$LOCK")" 2>/dev/null; then
  echo "Another run is active (pid $(cat "$LOCK"))." >&2
  exit 2
fi
echo $$ > "$LOCK"
SUMMARY="$REPORTS/summary.tsv"
: > "$SUMMARY"

cleanup() {
  rm -f "$LOCK"
  restore_config
  adbe shell cmd connectivity airplane-mode disable >/dev/null 2>&1 || true
  adbe shell settings put system font_scale 1.0 >/dev/null 2>&1 || true
  adbe shell settings put system accelerometer_rotation 0 >/dev/null 2>&1 || true
  bash "$MAESTRO_DIR/scripts/set_animations.sh" on >/dev/null 2>&1 || true
  [ "$KEEP" -eq 1 ] || stop_fake_server
}
trap cleanup EXIT INT TERM

if ! adb devices | grep -q "^$DEVICE[[:space:]]*device"; then
  echo "The emulator $DEVICE is not running." >&2
  exit 2
fi

# 1 and 2: the test settings go in only while Gradle builds. cleanup puts them back.
if [ "$BUILD" -eq 1 ]; then
  bash "$MAESTRO_DIR/scripts/build_test_apk.sh" || { echo "Build failed." >&2; exit 1; }
fi
APK="$REPO_ROOT/app/build/outputs/apk/debug/app-debug.apk"
# 3: install on the emulator
if [ "$INSTALL" -eq 1 ]; then
  adbe install -r "$APK" >/dev/null || { echo "Install failed." >&2; exit 1; }
fi
bash "$MAESTRO_DIR/scripts/set_animations.sh" off

# 4: the fake server
start_fake_server || exit 1

header() { grep -E "^# $2:" "$1" | sed -E "s/^# $2:[[:space:]]*//"; }

flow_tags() {
  awk '/^tags:/{t=1;next} /^---/{exit} t&&/^[[:space:]]*-/{gsub(/^[[:space:]]*-[[:space:]]*/,"");print} t&&!/^[[:space:]]*-/{t=0}' "$1"
}

has_tag() {
  [ -z "$TAGS" ] && return 0
  local want have
  for want in $(echo "$TAGS" | tr ',' ' '); do
    for have in $(flow_tags "$1"); do [ "$want" = "$have" ] && return 0; done
  done
  return 1
}

prepare() {
  local f="$1" line route mode
  fake_post /__reset
  local seed; seed="$(header "$f" seed | head -1)"
  [ -n "$seed" ] && fake_post "/__seed/$seed"
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    route="${line%=*}"; mode="${line##*=}"
    fake_post /__fault "{\"route\":\"$route\",\"mode\":\"$mode\"}"
  done < <(header "$f" fault)
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    # shellcheck disable=SC2086
    bash "$MAESTRO_DIR/scripts/${line%% *}" ${line#* } || echo "Setup step failed: $line" >&2
  done < <(header "$f" adb)
}

finish() {
  local f="$1" line
  while IFS= read -r line; do
    [ -z "$line" ] && continue
    # shellcheck disable=SC2086
    bash "$MAESTRO_DIR/scripts/${line%% *}" ${line#* } >/dev/null 2>&1 || true
  done < <(header "$f" after)
  fake_post /__fault/clear
}

run_flow() {
  local f="$1" name="$2"
  # A flow that fails inside the notification shade would leave it open for the next flow.
  adb -s "$DEVICE" shell cmd statusbar collapse >/dev/null 2>&1 || true
  prepare "$f"
  (cd "$MAESTRO_DIR" && maestro --device "$DEVICE" test --format junit --output "$REPORTS/$name.xml" \
      --debug-output "$REPORTS/debug/$name" --flatten-debug-output "$f") > "$REPORTS/$name.log" 2>&1
  local code=$?
  finish "$f"
  return $code
}

pass=0; fail=0; flaky=0; manual=0
FLOWS=()
while IFS= read -r f; do FLOWS+=("$f"); done < <(find "$MAESTRO_DIR/flows" -name '*.yaml' | sort)
for f in "${FLOWS[@]}"; do
  name="$(basename "$f" .yaml)"
  [ -n "$ONLY" ] && [[ "$name" != *"$ONLY"* ]] && continue
  has_tag "$f" || continue
  reason="$(header "$f" manual | head -1)"
  if [ -n "$reason" ]; then
    printf '%s\tmanual\t%s\n' "$name" "$reason" >> "$SUMMARY"; manual=$((manual+1)); continue
  fi
  echo "== $name"
  if run_flow "$f" "$name"; then
    printf '%s\tpass\t\n' "$name" >> "$SUMMARY"; pass=$((pass+1))
  elif [ "$RETRY" -eq 1 ] && run_flow "$f" "$name"; then
    printf '%s\tflaky\tpassed on the second run\n' "$name" >> "$SUMMARY"; flaky=$((flaky+1))
  else
    printf '%s\tfail\tsee reports/%s.log\n' "$name" "$name" >> "$SUMMARY"; fail=$((fail+1))
  fi
done

echo
column -t -s "$(printf '\t')" "$SUMMARY"
echo
echo "pass=$pass flaky=$flaky fail=$fail manual=$manual"
[ "$fail" -eq 0 ]
