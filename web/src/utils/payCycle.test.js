import { describe, it, expect } from 'vitest';
import { payCycle, cycleDays, calendarWeeks } from './payCycle';


// ac: cycle-calendar-daily-totals — weekday-aligned grid over one pay cycle
describe('calendarWeeks', () => {
  const weeks = calendarWeeks('2026-09-26', '2026-10-25');
  it('is a rectangle of whole 7-day weeks', () => {
    expect(weeks.every(w => w.length === 7)).toBe(true);
  });
  it('starts on a Sunday and ends on a Saturday', () => {
    expect(new Date(weeks[0][0].iso + 'T00:00').getDay()).toBe(0);
    const last = weeks[weeks.length - 1];
    expect(new Date(last[6].iso + 'T00:00').getDay()).toBe(6);
  });
  it('marks the cycle boundary days inCycle and the pad days not', () => {
    const flat = weeks.flat();
    expect(flat.find(c => c.iso === '2026-09-26').inCycle).toBe(true);
    expect(flat.find(c => c.iso === '2026-10-25').inCycle).toBe(true);
    expect(flat.find(c => c.iso === '2026-09-25').inCycle).toBe(false);
    expect(flat.find(c => c.iso === '2026-10-26').inCycle).toBe(false);
  });
  it('includes every day of the cycle exactly once', () => {
    const inCycle = weeks.flat().filter(c => c.inCycle).map(c => c.iso);
    expect(inCycle.length).toBe(30); // 26 Sep .. 25 Oct inclusive
    expect(new Set(inCycle).size).toBe(30);
  });
});
