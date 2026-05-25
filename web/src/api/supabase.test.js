import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { getTransactions, addTransaction } from './supabase';

beforeEach(() => {
  global.fetch = vi.fn();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('getTransactions', () => {
  it('fetches with correct auth headers', async () => {
    global.fetch.mockResolvedValue({ ok: true, json: async () => [] });
    await getTransactions();
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toContain('https://test.supabase.co/rest/v1/transactions');
    expect(options.headers.apikey).toBe('test-anon-key');
    expect(options.headers.Authorization).toBe('Bearer test-anon-key');
  });

  it('applies tab filter', async () => {
    global.fetch.mockResolvedValue({ ok: true, json: async () => [] });
    await getTransactions({ tab: 'EXPENSES' });
    const [url] = global.fetch.mock.calls[0];
    expect(url).toContain('tab=eq.EXPENSES');
  });

  it('applies month filter with correct date range', async () => {
    global.fetch.mockResolvedValue({ ok: true, json: async () => [] });
    await getTransactions({ month: '2026-05' });
    const [url] = global.fetch.mock.calls[0];
    expect(url).toContain('date=gte.2026-05-01');
    expect(url).toContain('date=lte.2026-05-31');
  });

  it('throws on non-ok response', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 401 });
    await expect(getTransactions()).rejects.toThrow('Supabase error: 401');
  });
});

describe('addTransaction', () => {
  it('posts with correct headers and body', async () => {
    global.fetch.mockResolvedValue({ ok: true });
    const row = {
      merchant: 'Grab',
      item: 'Food delivery',
      amount: 150,
      category: 'Food & Drink',
      channel: 'eWallet',
      tab: 'EXPENSES',
      date: '2026-05-01',
      note: null,
    };
    await addTransaction(row);
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toContain('https://test.supabase.co/rest/v1/transactions');
    expect(options.method).toBe('POST');
    expect(options.headers['Prefer']).toBe('return=minimal');
    expect(JSON.parse(options.body)).toEqual(row);
  });

  it('throws on non-ok response', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 500 });
    await expect(addTransaction({})).rejects.toThrow('Supabase error: 500');
  });
});
