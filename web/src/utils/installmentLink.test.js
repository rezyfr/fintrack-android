import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import * as api from '../api/client';
import { addWithLink, flushPendingSteps, updateWithLink } from './installmentLink';

vi.mock('../api/client');

const plan = {
  id: 7, merchant: 'Tiket.com', merchant_pattern: 'Tiket.com', wallet: 'MANDIRI_CC',
  installment_amount: 25920, total_installments: 12, current_step: 9, status: 'active',
};

function memoryStorage() {
  const m = new Map();
  return { getItem: (k) => (m.has(k) ? m.get(k) : null), setItem: (k, v) => { m.set(k, v); } };
}

const expense = (item = 'TIKET.COM J 010/012') => ({
  item, amount: 25920, category: 'Travel', date: '2026-09-30', wallet: 'MANDIRI_CC', tx_type: 'expense', tab: 'IDR_EXPENSES',
});

beforeEach(() => {
  api.getInstallments.mockResolvedValue([plan]);
  api.addTransaction.mockResolvedValue(undefined);
  api.updateTransaction.mockResolvedValue(undefined);
  api.incrementInstallmentStep.mockResolvedValue(undefined);
});
afterEach(() => vi.resetAllMocks());

describe('addWithLink', () => {
  it('sends a matching expense with its installment and moves the step', async () => {
    const out = await addWithLink(expense(), memoryStorage());
    expect(api.addTransaction).toHaveBeenCalledWith(expect.objectContaining({ installment_id: 7 }));
    expect(api.incrementInstallmentStep).toHaveBeenCalledWith(7, 10, 'active');
    expect(out.stepSent).toBe(true);
  });

  it('sends a transaction without a match unlinked and moves no step', async () => {
    await addWithLink({ ...expense('Groceries'), amount: 500 }, memoryStorage());
    expect(api.addTransaction.mock.calls[0][0]).not.toHaveProperty('installment_id');
    expect(api.incrementInstallmentStep).not.toHaveBeenCalled();
  });

  it('never links an income', async () => {
    await addWithLink({ ...expense(), tx_type: 'income' }, memoryStorage());
    expect(api.addTransaction.mock.calls[0][0]).not.toHaveProperty('installment_id');
  });

  it('saves the transaction without a link when the installments cannot be read', async () => {
    api.getInstallments.mockRejectedValue(new Error('offline'));
    await addWithLink(expense(), memoryStorage());
    expect(api.addTransaction.mock.calls[0][0]).not.toHaveProperty('installment_id');
  });

  it('queues a failed step update and the transaction still saves', async () => {
    const storage = memoryStorage();
    api.incrementInstallmentStep.mockRejectedValue(new Error('offline'));
    const out = await addWithLink(expense(), storage);
    expect(out.stepSent).toBe(false);
    expect(JSON.parse(storage.getItem('pending_installment_steps'))[7].newStep).toBe(10);
  });
});

describe('flushPendingSteps', () => {
  it('sends a queued step and clears it', async () => {
    const storage = memoryStorage();
    storage.setItem('pending_installment_steps', JSON.stringify({ 7: { newStep: 10, newStatus: 'active' } }));
    await flushPendingSteps(storage);
    expect(api.incrementInstallmentStep).toHaveBeenCalledWith(7, 10, 'active');
    expect(JSON.parse(storage.getItem('pending_installment_steps'))).toEqual({});
  });

  it('keeps a queued step while the send keeps failing', async () => {
    const storage = memoryStorage();
    storage.setItem('pending_installment_steps', JSON.stringify({ 7: { newStep: 10, newStatus: 'active' } }));
    api.incrementInstallmentStep.mockRejectedValue(new Error('offline'));
    await flushPendingSteps(storage);
    expect(Object.keys(JSON.parse(storage.getItem('pending_installment_steps')))).toEqual(['7']);
  });
});

describe('updateWithLink', () => {
  it('links an unlinked card expense when it matches', async () => {
    const out = await updateWithLink(5, expense(), { autoLink: true }, memoryStorage());
    expect(api.updateTransaction).toHaveBeenCalledWith(5, expect.objectContaining({ installment_id: 7 }));
    expect(out.linkedId).toBe(7);
    expect(api.incrementInstallmentStep).toHaveBeenCalledWith(7, 10, 'active');
  });

  it('changes no link and no step for an already linked transaction', async () => {
    const out = await updateWithLink(5, expense(), {}, memoryStorage());
    expect(api.updateTransaction.mock.calls[0][1]).not.toHaveProperty('installment_id');
    expect(out.linkedId).toBeUndefined();
    expect(api.incrementInstallmentStep).not.toHaveBeenCalled();
  });

  it('links a plan picked by hand even when the text has no step', async () => {
    await updateWithLink(5, expense('Something else'), { installmentId: 7 }, memoryStorage());
    expect(api.updateTransaction).toHaveBeenCalledWith(5, expect.objectContaining({ installment_id: 7 }));
    expect(api.incrementInstallmentStep).toHaveBeenCalledWith(7, 10, 'active');
  });

  it('sends a null link to unlink and moves no step', async () => {
    const out = await updateWithLink(5, expense(), { unlink: true }, memoryStorage());
    expect(api.updateTransaction).toHaveBeenCalledWith(5, expect.objectContaining({ installment_id: null }));
    expect(out.linkedId).toBeNull();
    expect(api.incrementInstallmentStep).not.toHaveBeenCalled();
  });
});
