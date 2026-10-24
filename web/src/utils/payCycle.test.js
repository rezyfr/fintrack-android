import { describe, it, expect, vi } from 'vitest';
import { payCycle, cycleDays, calendarWeeks, LEGACY_CYCLE_START_DAY } from './payCycle';
import { getCurrencySettings, saveCurrencySettings } from '../constants/currency';

beforeEach(() => vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true }))));
afterEach(() => vi.unstubAllGlobals());

const withStartDay = (day) => saveCurrencySettings({ ...getCurrencySettings(), cycleStartDay: day });
afterEach(() => withStartDay(LEGACY_CYCLE_START_DAY));

// ac: web-first-run-setup - the cycle follows the start day setting
describe('payCycle', () => {
  it('uses the legacy start day 26 by default', () => {
    expect(payCycle(0, new Date(2026, 9, 11))).toEqual({ from: '2026-09-26', to: '2026-10-25' });
    expect(payCycle(0, new Date(2026, 9, 26))).toEqual({ from: '2026-10-26', to: '2026-11-25' });
    expect(payCycle(1, new Date(2026, 9, 11))).toEqual({ from: '2026-08-26', to: '2026-09-25' });
    expect(payCycle(0, new Date(2026, 0, 5))).toEqual({ from: '2025-12-26', to: '2026-01-25' });
  });

  it('is a calendar month for start day 1', () => {
    withStartDay(1);
    expect(payCycle(0, new Date(2026, 9, 11))).toEqual({ from: '2026-10-01', to: '2026-10-31' });
    expect(payCycle(0, new Date(2026, 9, 1))).toEqual({ from: '2026-10-01', to: '2026-10-31' });
    expect(payCycle(1, new Date(2026, 9, 11))).toEqual({ from: '2026-09-01', to: '2026-09-30' });
    expect(payCycle(0, new Date(2028, 1, 29))).toEqual({ from: '2028-02-01', to: '2028-02-29' });
    expect(payCycle(-1, new Date(2026, 11, 20))).toEqual({ from: '2027-01-01', to: '2027-01-31' });
  });

  it('works for start day 15', () => {
    withStartDay(15);
    expect(payCycle(0, new Date(2026, 9, 11))).toEqual({ from: '2026-09-15', to: '2026-10-14' });
    expect(payCycle(0, new Date(2026, 9, 15))).toEqual({ from: '2026-10-15', to: '2026-11-14' });
    expect(payCycle(2, new Date(2026, 9, 20))).toEqual({ from: '2026-08-15', to: '2026-09-14' });
  });

  it('works for start day 28 across February', () => {
    withStartDay(28);
    expect(payCycle(0, new Date(2027, 1, 10))).toEqual({ from: '2027-01-28', to: '2027-02-27' });
  });
});


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
