// Works out how the date picker labels yesterday. Output: output.label (a text pattern) and output.prevMonth ('true' when
// yesterday falls in the month before today, so the picker needs one click on the previous month arrow).
var months = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
var today = new Date()
var y = new Date(today.getFullYear(), today.getMonth(), today.getDate() - 1)
output.label = '.*, ' + months[y.getMonth()] + ' ' + y.getDate() + ', ' + y.getFullYear()
output.prevMonth = String(y.getMonth() !== today.getMonth())
