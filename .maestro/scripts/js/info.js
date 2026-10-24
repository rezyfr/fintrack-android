// Reads facts the fake server worked out for the loaded seed set.
// Output: output.range (the Home cycle range text), output.today (ISO date), output.day (cycle start day).
var base = 'http://localhost:' + (typeof SERVER_PORT === 'undefined' ? '8765' : SERVER_PORT)
var info = JSON.parse(http.get(base + '/__info').body)
output.range = info.cycle_range
output.today = info.today
output.day = String(info.cycle_start_day)
