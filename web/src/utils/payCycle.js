// ac: insights-pay-cycle-periods — a period runs from pay day through the day before the next one,
// so the cycle holding a date ends on the 25th of that month, or of the next month once the 26th has
// passed. Shared by Insights and Budget so both agree on where a cycle begins and ends.
export const PAY_CYCLE_START_DAY = 26;

export function isoDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// offset 0 is the cycle in progress, 1 the one before it, and so on.
export function payCycle(offset = 0, now = new Date()) {
  const monthsBack = offset + (now.getDate() >= PAY_CYCLE_START_DAY ? 0 : 1);
  const start = new Date(now.getFullYear(), now.getMonth() - monthsBack, PAY_CYCLE_START_DAY);
  const end   = new Date(start.getFullYear(), start.getMonth() + 1, PAY_CYCLE_START_DAY - 1);
  return { from: isoDate(start), to: isoDate(end) };
}

// Every date in [from, to] as an ISO string, so a chart can walk the cycle day by day.
export function cycleDays(from, to) {
  const out = [];
  const [fy, fm, fd] = from.split('-').map(Number);
  const [ty, tm, td] = to.split('-').map(Number);
  const end = new Date(ty, tm - 1, td);
  for (let d = new Date(fy, fm - 1, fd); d <= end; d.setDate(d.getDate() + 1)) {
    out.push(isoDate(d));
  }
  return out;
}

// ac: cycle-calendar-daily-totals — a weekday-aligned grid covering a pay cycle. Pads to whole
// weeks (Sunday-first) so the cycle sits inside a rectangular month grid; padding days are marked
// inCycle:false so the view can dim them and leave them out of totals.
export function calendarWeeks(from, to) {
  const days = cycleDays(from, to);
  const inCycle = new Set(days);
  const [fy, fm, fd] = from.split('-').map(Number);
  const start = new Date(fy, fm - 1, fd);
  start.setDate(start.getDate() - start.getDay()); // back to the Sunday on/before the cycle start

  const [ty, tm, td] = to.split('-').map(Number);
  const end = new Date(ty, tm - 1, td);
  end.setDate(end.getDate() + (6 - end.getDay())); // forward to the Saturday on/after the cycle end

  const weeks = [];
  let week = [];
  for (let d = new Date(start); d <= end; d.setDate(d.getDate() + 1)) {
    const day = isoDate(d);
    week.push({ iso: day, inCycle: inCycle.has(day) });
    if (week.length === 7) { weeks.push(week); week = []; }
  }
  if (week.length) weeks.push(week);
  return weeks;
}
