// Runs a helper script on the host through the fake server, for steps only adb can do.
// Env: SCRIPT_NAME (post_notification, set_listener, set_airplane, set_font_scale), SCRIPT_ARGS (JSON array text).
var base = 'http://localhost:' + (typeof SERVER_PORT === 'undefined' ? '8765' : SERVER_PORT)
var args = typeof SCRIPT_ARGS === 'undefined' ? '[]' : SCRIPT_ARGS
var response = http.post(base + '/__run/' + SCRIPT_NAME, { body: '{"args":' + args + '}' })
if (!response.ok) {
  throw new Error('Helper script ' + SCRIPT_NAME + ' failed: ' + response.body)
}
