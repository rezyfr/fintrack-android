#!/usr/bin/env bash
# Shared helpers for the Maestro scripts. Source this file, do not run it.
# Safety rule: every adb and maestro call must use the emulator, never a real phone.

MAESTRO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPO_ROOT="$(cd "$MAESTRO_DIR/.." && pwd)"
export ANDROID_SERIAL="${ANDROID_SERIAL:-emulator-5554}"
DEVICE="$ANDROID_SERIAL"
APP_ID="com.fidriyanto.banktracker"
FAKE_PORT="${FAKE_PORT:-8765}"
FAKE_URL="http://localhost:$FAKE_PORT"
BACKUP_DIR="$MAESTRO_DIR/reports/.backup"
LOCAL_PROPS="$REPO_ROOT/local.properties"
DEBUG_MANIFEST="$REPO_ROOT/app/src/debug/AndroidManifest.xml"

case "$DEVICE" in
  emulator-*) ;;
  *) echo "Refusing to run on $DEVICE. Use an emulator." >&2; exit 2 ;;
esac

adbe() { adb -s "$DEVICE" "$@"; }

# Put the test settings in place. The originals are copied to $BACKUP_DIR first.
swap_config() {
  mkdir -p "$BACKUP_DIR"
  if [ -e "$BACKUP_DIR/local.properties" ] || [ -e "$BACKUP_DIR/AndroidManifest.xml" ]; then
    echo "Found a backup from an earlier run. Restoring it first." >&2
    restore_config
  fi
  cp "$LOCAL_PROPS" "$BACKUP_DIR/local.properties"
  cp "$DEBUG_MANIFEST" "$BACKUP_DIR/AndroidManifest.xml"
  local tmp
  tmp="$(mktemp)"
  grep -v '^API_BASE_URL=' "$LOCAL_PROPS" | grep -v '^API_TOKEN=' > "$tmp"
  {
    cat "$tmp"
    echo "API_BASE_URL=http://10.0.2.2:$FAKE_PORT"
    echo "API_TOKEN=test"
  } > "$LOCAL_PROPS"
  rm -f "$tmp"
  if ! grep -q usesCleartextTraffic "$DEBUG_MANIFEST"; then
    sed -i.bak 's|<application>|<application android:usesCleartextTraffic="true">|' "$DEBUG_MANIFEST"
    rm -f "$DEBUG_MANIFEST.bak"
  fi
}

# Put the original settings back. Safe to call more than once.
restore_config() {
  if [ -e "$BACKUP_DIR/local.properties" ]; then
    cp "$BACKUP_DIR/local.properties" "$LOCAL_PROPS" && rm -f "$BACKUP_DIR/local.properties"
  fi
  if [ -e "$BACKUP_DIR/AndroidManifest.xml" ]; then
    cp "$BACKUP_DIR/AndroidManifest.xml" "$DEBUG_MANIFEST" && rm -f "$BACKUP_DIR/AndroidManifest.xml"
  fi
}

fake_post() { curl -s -X POST "$FAKE_URL$1" ${2:+-d "$2"} >/dev/null; }

start_fake_server() {
  mkdir -p "$MAESTRO_DIR/reports"
  if curl -s "$FAKE_URL/__health" >/dev/null 2>&1; then return 0; fi
  python3 "$MAESTRO_DIR/scripts/fake-server/server.py" --port "$FAKE_PORT" --log "$MAESTRO_DIR/reports/requests.log" \
    >"$MAESTRO_DIR/reports/fake-server.out" 2>&1 &
  echo $! > "$MAESTRO_DIR/reports/fake-server.pid"
  for _ in $(seq 1 20); do
    curl -s "$FAKE_URL/__health" >/dev/null 2>&1 && return 0
    sleep 0.3
  done
  echo "The fake server did not start." >&2
  return 1
}

stop_fake_server() {
  if [ -f "$MAESTRO_DIR/reports/fake-server.pid" ]; then
    kill "$(cat "$MAESTRO_DIR/reports/fake-server.pid")" 2>/dev/null || true
    rm -f "$MAESTRO_DIR/reports/fake-server.pid"
  fi
}
