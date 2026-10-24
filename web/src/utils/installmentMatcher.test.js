import { describe, it, expect } from 'vitest';
import { advanceInstallment, matchInstallment, parseSteps } from './installmentMatcher';

const plan = (over) => ({
  id: 7, merchant: 'Tiket.com', merchant_pattern: 'Tiket.com', wallet: 'MANDIRI_CC',
  installment_amount: 25920, total_installments: 12, current_step: 8, status: 'active', ...over,
});
const bcaA = plan({ id: 11, merchant_pattern: 'TIKET.COM 0%3', wallet: 'BCA_CC', installment_amount: 1081060, total_installments: 3, current_step: 2 });
const bcaB = plan({ id: 12, merchant_pattern: 'TIKET.COM 0%3', wallet: 'BCA_CC', installment_amount: 1713594, total_installments: 3, current_step: 2 });

describe('matchInstallment', () => {
  it('reads the step from the statement text', () => {
    const m = matchInstallment('TIKET.COM J 009/012', 'MANDIRI_CC', 25920, [plan()]);
    expect(m.newStep).toBe(9);
    expect(m.newStatus).toBe('active');
  });
  it('reads the KE ... DARI ... format and completes the plan', () => {
    const m = matchInstallment('CICILAN BCA KE 03 DARI 03, TIKET.COM 0%3', 'BCA_CC', 1081060, [bcaA, bcaB]);
    expect(m.installment.id).toBe(11);
    expect(m.newStep).toBe(3);
    expect(m.newStatus).toBe('completed');
  });
  it('uses the amount to pick between two plans with the same pattern', () => {
    const m = matchInstallment('CICILAN BCA KE 03 DARI 03, TIKET.COM 0%3', 'BCA_CC', 1713594, [bcaA, bcaB]);
    expect(m.installment.id).toBe(12);
  });
  it('never lowers a step that was fixed by hand', () => {
    expect(matchInstallment('TIKET.COM J 009/012', 'MANDIRI_CC', 25920, [plan({ current_step: 9 })]).newStep).toBe(9);
  });
  it('moves up by one when the text has no number', () => {
    expect(matchInstallment('Tiket.com', 'MANDIRI_CC', 25920, [plan()]).newStep).toBe(9);
  });
  it('ignores the pattern case', () => {
    expect(matchInstallment('tiket.COM j 009/012', 'MANDIRI_CC', 25920, [plan()]).installment.id).toBe(7);
  });
  it('gives no match for another wallet, another amount, or a completed plan', () => {
    expect(matchInstallment('TIKET.COM J 009/012', 'BCA_CC', 25920, [plan()])).toBeNull();
    expect(matchInstallment('TIKET.COM J 009/012', 'MANDIRI_CC', 99999, [plan()])).toBeNull();
    expect(matchInstallment('TIKET.COM J 012/012', 'MANDIRI_CC', 25920, [plan({ status: 'completed', current_step: 12 })])).toBeNull();
  });
  it('gives no match when two plans fit', () => {
    expect(matchInstallment('TIKET.COM J 009/012', 'MANDIRI_CC', 25920, [plan(), plan({ id: 99 })])).toBeNull();
  });
  it('gives no match for a step of another plan length', () => {
    expect(matchInstallment('TIKET.COM J 003/006', 'MANDIRI_CC', 25920, [plan()])).toBeNull();
  });
  it('completes the plan on the last step', () => {
    const m = matchInstallment('TIKET.COM J 012/012', 'MANDIRI_CC', 25920, [plan({ current_step: 11 })]);
    expect(m.newStep).toBe(12);
    expect(m.newStatus).toBe('completed');
  });
});

describe('advanceInstallment', () => {
  it('falls back to one step up for a plan picked by hand when the text has another plan length', () => {
    expect(advanceInstallment(plan({ current_step: 9 }), 'TIKET.COM J 003/006', false).newStep).toBe(10);
  });
  it('is strict by default', () => {
    expect(advanceInstallment(plan(), 'TIKET.COM J 003/006')).toBeNull();
  });
});

describe('parseSteps', () => {
  it('skips a date written as a step', () => {
    expect(parseSteps('PAYMENT 25/12')).toEqual([]);
  });
});
