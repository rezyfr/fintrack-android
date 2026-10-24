// Works out the Home cycle range text for a pay cycle start day, the same way the app does.
// Env: START_DAY. Output: output.range (for example "27 Sep - 26 Oct"), output.from, output.to.
var names = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']
var day = parseInt(START_DAY, 10)
var today = new Date()
var back = today.getDate() >= day ? 0 : 1
var start = new Date(today.getFullYear(), today.getMonth() - back, day)
var end = new Date(start.getFullYear(), start.getMonth() + 1, day - 1)
function fmt(d) { return d.getDate() + ' ' + names[d.getMonth()] }
output.from = fmt(start)
output.to = fmt(end)
output.range = fmt(start) + ' - ' + fmt(end)
