// Calls the fake server from a flow. Env: SERVER_PATH (for example /__seed/base), SERVER_BODY (optional JSON text).
var base = 'http://localhost:' + (typeof SERVER_PORT === 'undefined' ? '8765' : SERVER_PORT)
var response = http.post(base + SERVER_PATH, { body: typeof SERVER_BODY === 'undefined' ? '' : SERVER_BODY })
if (!response.ok) {
  throw new Error('Fake server call failed: ' + SERVER_PATH + ' status ' + response.status)
}
output.body = response.body
