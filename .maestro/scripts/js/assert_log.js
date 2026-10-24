// Checks the request log of the fake server.
// Env: LOG_METHOD (GET, POST, PATCH, DELETE), LOG_TABLE, LOG_CONTAINS (optional text), LOG_EXPECT ('some' or 'none', default 'some').
var base = 'http://localhost:' + (typeof SERVER_PORT === 'undefined' ? '8765' : SERVER_PORT)
var response = http.get(base + '/__log')
var lines = JSON.parse(response.body)
var prefix = LOG_METHOD + ' ' + LOG_TABLE + '?'
var contains = typeof LOG_CONTAINS === 'undefined' ? '' : LOG_CONTAINS
var expect = typeof LOG_EXPECT === 'undefined' ? 'some' : LOG_EXPECT
var hits = lines.filter(function (l) { return l.indexOf(prefix) === 0 && l.indexOf(contains) !== -1 })
if (expect === 'some' && hits.length === 0) {
  throw new Error('No ' + LOG_METHOD + ' ' + LOG_TABLE + ' request containing "' + contains + '" in the server log')
}
if (expect === 'none' && hits.length > 0) {
  throw new Error('Unexpected ' + LOG_METHOD + ' ' + LOG_TABLE + ' request: ' + hits[0])
}
