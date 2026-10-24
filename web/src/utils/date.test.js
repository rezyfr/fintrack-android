import { describe, it, expect } from 'vitest';
import { formatDisplayDate, parseDisplayDate } from './date';

describe('formatDisplayDate', () => {
  it('formats an ISO date as dd MMM yyyy', () => {
    expect(formatDisplayDate('2026-10-06')).toBe('06 Oct 2026');
    expect(formatDisplayDate('2026-01-31')).toBe('31 Jan 2026');
  });
  it('returns an empty string for empty or invalid input', () => {
    expect(formatDisplayDate('')).toBe('');
    expect(formatDisplayDate(undefined)).toBe('');
    expect(formatDisplayDate('2026-13-01')).toBe('');
  });
});

describe('parseDisplayDate', () => {
  it('reads dd MMM yyyy', () => {
    expect(parseDisplayDate('06 Oct 2026')).toBe('2026-10-06');
  });
  it('reads a day without a leading zero and any month case', () => {
    expect(parseDisplayDate('6 oct 2026')).toBe('2026-10-06');
    expect(parseDisplayDate('6 OCT 2026')).toBe('2026-10-06');
  });
  it('reads an ISO date', () => {
    expect(parseDisplayDate('2026-10-06')).toBe('2026-10-06');
  });
  it('rejects text that is not a real date', () => {
    expect(parseDisplayDate('31 Feb 2026')).toBeNull();
    expect(parseDisplayDate('06 Foo 2026')).toBeNull();
    expect(parseDisplayDate('hello')).toBeNull();
    expect(parseDisplayDate('')).toBeNull();
  });
});
