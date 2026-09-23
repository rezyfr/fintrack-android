import { describe, it, expect } from 'vitest';
import {
  latestCutoff, previousCutoff, dueDateAfter, installmentPortion, minimumPayment, daysUntil,
} from './cardBilling';

describe('latestCutoff', () => {
  it('uses this month once the cutoff day has passed', () => {
    expect(latestCutoff(3, new Date(2026, 8, 23))).toBe('2026-09-03');
  });
  it('uses last month when the cutoff day is still ahead', () => {
    expect(latestCutoff(11, new Date(2026, 8, 5))).toBe('2026-08-11');
  });
});

describe('previousCutoff', () => {
  it('steps back exactly one statement', () => {
    expect(previousCutoff('2026-09-03', 3)).toBe('2026-08-03');
  });
});

// ac: card-statement-due-and-minimum — due date is the due day in the first month after the cutoff
describe('dueDateAfter', () => {
  it('lands the same month when the due day is after the cutoff day', () => {
    // BCA: closes 3 Oct, due 19 Oct
    expect(dueDateAfter('2026-10-03', 19)).toBe('2026-10-19');
  });
  it('lands the next month when the due day is on or before the cutoff day', () => {
    // Mandiri: closes 11 Sep, due 1 Oct
    expect(dueDateAfter('2026-09-11', 1)).toBe('2026-10-01');
  });
});

// ac: card-statement-due-and-minimum — installment charges inside the statement window
describe('installmentPortion', () => {
  const rows = [
    { tx_type: 'expense', date: '2026-09-25', item: 'CICILAN BCA KE 03 DARI 03, TIKET.COM', amount: 1081060 },
    { tx_type: 'expense', date: '2026-09-25', item: 'TIKET.COM J 010/012', amount: 25920 },
    { tx_type: 'expense', date: '2026-09-20', item: 'Naga groceries', amount: 500000 },
    { tx_type: 'expense', date: '2026-08-30', item: 'CICILAN BCA KE 02 DARI 03', amount: 1081060 }, // prior window
  ];
  it('sums only installment-tagged charges in (from, to]', () => {
    expect(installmentPortion(rows, '2026-09-03', '2026-10-03')).toBe(1081060 + 25920);
  });
  it('excludes a plain grocery charge', () => {
    const only = installmentPortion([rows[2]], '2026-09-03', '2026-10-03');
    expect(only).toBe(0);
  });
});

// ac: card-statement-due-and-minimum — the two minimum-payment rules
describe('minimumPayment', () => {
  it('BCA: installment in full plus 5% of the rest', () => {
    // Verified against the Sep statement: bill 3,825,917, installment 2,794,654 -> minimum 2,846,217
    const m = minimumPayment({
      statementBalance: 3825917, installment: 2794654, minPercent: 5, minFullInstallments: true,
    });
    expect(Math.round(m)).toBe(2846217);
  });
  it('Mandiri: plain 5% of the balance', () => {
    // Verified: bill 20,953,446 -> minimum 1,047,672 (statement rounds to 1,047,680)
    const m = minimumPayment({
      statementBalance: 20953446, installment: 0, minPercent: 5, minFullInstallments: false,
    });
    expect(Math.round(m)).toBe(1047672);
  });
  it('never exceeds the balance when the installment alone is larger', () => {
    const m = minimumPayment({
      statementBalance: 100000, installment: 100000, minPercent: 5, minFullInstallments: true,
    });
    expect(m).toBe(100000);
  });
});

describe('daysUntil', () => {
  it('counts whole days to the due date', () => {
    expect(daysUntil('2026-10-19', new Date(2026, 9, 15))).toBe(4);
  });
  it('is negative once the due date has passed', () => {
    expect(daysUntil('2026-09-19', new Date(2026, 8, 22))).toBe(-3);
  });
});
