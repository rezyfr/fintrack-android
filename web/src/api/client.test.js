import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  getTransactions, addTransaction, updateTransaction, deleteTransaction, deleteTransactions, updateTransactionsCategory,
  getRecentFxRate, getWalletBalances, getAppSettings, saveAppSettings, saveAccount, deleteAccount,
  deleteCategory, deleteSubcategory, incrementInstallmentStep,
} from './client';

beforeEach(() => {
  global.fetch = vi.fn();
});

afterEach(() => {
  vi.restoreAllMocks();
});

const ok = (json) => ({ ok: true, status: 200, json: async () => json });
const page = (items, next = null) => ok({ items, next_offset: next });
const rows = (n) => Array.from({ length: n }, (_, i) => ({ id: i }));

describe('getTransactions', () => {
  it('sends the bearer token to the server address', async () => {
    global.fetch.mockResolvedValue(page([]));
    await getTransactions();
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toContain('https://api.test/v1/transactions');
    expect(options.headers.Authorization).toBe('Bearer test-token');
    expect(options.headers.apikey).toBeUndefined();
  });

  it('returns the rows of the page', async () => {
    global.fetch.mockResolvedValue(page([{ id: 1 }, { id: 2 }]));
    expect(await getTransactions()).toEqual([{ id: 1 }, { id: 2 }]);
  });

  it('turns the filters into query parameters', async () => {
    global.fetch.mockResolvedValue(page([]));
    await getTransactions({ wallet: 'BCA', txType: 'expense', category: 'Food & Drink', search: 'a b', amountMin: 5, amountMax: 90 });
    const url = new URL(global.fetch.mock.calls[0][0]);
    expect(Object.fromEntries(url.searchParams)).toEqual({
      involving_wallet: 'BCA', tx_type: 'expense', category: 'Food & Drink', search: 'a b', amount_min: '5', amount_max: '90', limit: '200',
    });
  });

  it('sends the month, and a date range replaces it', async () => {
    global.fetch.mockResolvedValue(page([]));
    await getTransactions({ month: '2026-05' });
    expect(new URL(global.fetch.mock.calls[0][0]).searchParams.get('month')).toBe('2026-05');

    global.fetch.mockClear();
    await getTransactions({ month: '2026-05', dateFrom: '2026-05-03' });
    const params = new URL(global.fetch.mock.calls[0][0]).searchParams;
    expect(params.get('month')).toBeNull();
    expect(params.get('date_from')).toBe('2026-05-03');
  });

  it('throws on a non-ok response', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 401 });
    await expect(getTransactions()).rejects.toThrow('API error: 401');
  });

  // ac: web-complete-range-fetch — a closed date range reads every page
  it('reads every page of a closed date range', async () => {
    global.fetch
      .mockResolvedValueOnce(page(rows(2), 2))
      .mockResolvedValueOnce(page(rows(2), 4))
      .mockResolvedValueOnce(page(rows(1), null));
    const result = await getTransactions({ dateFrom: '2026-09-26', dateTo: '2026-10-25', limit: 2 });
    expect(result).toHaveLength(5);
    expect(global.fetch).toHaveBeenCalledTimes(3);
    expect(global.fetch.mock.calls[1][0]).toContain('offset=2');
    expect(global.fetch.mock.calls[2][0]).toContain('offset=4');
  });

  it('stops an open-ended range at the limit', async () => {
    global.fetch.mockResolvedValue(page([{ id: 1 }, { id: 2 }], 2));
    const result = await getTransactions({ dateFrom: '2026-09-26', limit: 2 });
    expect(result).toHaveLength(2);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  it('asks for pages of at most 1000 rows when the limit is larger', async () => {
    global.fetch
      .mockResolvedValueOnce(page(rows(1000), 1000))
      .mockResolvedValueOnce(page(rows(1000), 2000))
      .mockResolvedValueOnce(page(rows(5), null));
    const result = await getTransactions({ wallet: 'CARD', limit: 2000 });
    expect(new URL(global.fetch.mock.calls[0][0]).searchParams.get('limit')).toBe('1000');
    expect(result).toHaveLength(2000);
    expect(global.fetch).toHaveBeenCalledTimes(2);
  });
});

describe('addTransaction', () => {
  it('posts the row as JSON', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 201, json: async () => ({}) });
    const row = { item: 'Food delivery', amount: 150, category: 'Food & Drink', tab: 'EXPENSES', date: '2026-05-01', note: null };
    await addTransaction(row);
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('https://api.test/v1/transactions');
    expect(options.method).toBe('POST');
    expect(options.headers['Content-Type']).toBe('application/json');
    expect(JSON.parse(options.body)).toEqual(row);
  });

  it('throws on a non-ok response', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 500 });
    await expect(addTransaction({})).rejects.toThrow('API error: 500');
  });
});

describe('changing and deleting transactions', () => {
  it('patches one transaction by id', async () => {
    global.fetch.mockResolvedValue(ok({}));
    await updateTransaction(7, { amount: 40, installment_id: null });
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('https://api.test/v1/transactions/7');
    expect(options.method).toBe('PATCH');
    expect(JSON.parse(options.body)).toEqual({ amount: 40, installment_id: null });
  });

  it('sends no content type on a request without a body', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 204 });
    await deleteTransactions([1]);
    await deleteTransaction(2);
    for (const [, options] of global.fetch.mock.calls) {
      expect(options.body).toBeUndefined();
      expect(options.headers['Content-Type']).toBeUndefined();
    }
  });

  it('counts a transaction that is already gone as deleted', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 404 });
    await expect(deleteTransaction(7)).resolves.toBeUndefined();
    expect(global.fetch.mock.calls[0][1].method).toBe('DELETE');
  });

  it('deletes many transactions in batches of 500', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 204 });
    const ids = Array.from({ length: 1001 }, (_, i) => i + 1);
    await deleteTransactions(ids);
    expect(global.fetch).toHaveBeenCalledTimes(3);
    const sent = global.fetch.mock.calls.map(([url]) => new URL(url).searchParams.get('ids').split(',').length);
    expect(sent).toEqual([500, 500, 1]);
    await deleteTransactions([]);
    expect(global.fetch).toHaveBeenCalledTimes(3);
  });

  it('changes the category of many transactions in one patch', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 204 });
    await updateTransactionsCategory([1, 2], 'Gifts');
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('https://api.test/v1/transactions');
    expect(options.method).toBe('PATCH');
    expect(JSON.parse(options.body)).toEqual({ ids: [1, 2], category: 'Gifts' });
  });
});

describe('other endpoints', () => {
  it('lists the wallet balances ordered by id', async () => {
    global.fetch.mockResolvedValue(ok([{ id: 'B' }, { id: 'A' }]));
    expect((await getWalletBalances()).map((b) => b.id)).toEqual(['A', 'B']);
    expect(global.fetch.mock.calls[0][0]).toBe('https://api.test/v1/wallet-balances');
  });

  it('answers null when the settings are not saved yet', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 404 });
    expect(await getAppSettings()).toBeNull();
  });

  it('puts the settings', async () => {
    global.fetch.mockResolvedValue(ok({}));
    const settings = { home_code: 'IDR', home_label: 'Rupiah', cycle_start_day: 25, currencies: [] };
    await saveAppSettings(settings);
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('https://api.test/v1/settings');
    expect(options.method).toBe('PUT');
    expect(JSON.parse(options.body)).toEqual(settings);
  });

  it('puts an account by id and keeps the id out of the body', async () => {
    global.fetch.mockResolvedValue(ok({}));
    await saveAccount({ id: 'MAIN_BANK', name: 'Main Bank', currency: 'IDR' });
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('https://api.test/v1/accounts/MAIN_BANK');
    expect(options.method).toBe('PUT');
    expect(JSON.parse(options.body)).toEqual({ name: 'Main Bank', currency: 'IDR' });
  });

  it('counts a missing account, category or subcategory as deleted', async () => {
    global.fetch.mockResolvedValue({ ok: false, status: 404 });
    await expect(deleteAccount('X')).resolves.toBeUndefined();
    await expect(deleteCategory('Pets / Animals')).resolves.toBeUndefined();
    await expect(deleteSubcategory('Pets / Animals', 'Vet visits')).resolves.toBeUndefined();
  });

  it('writes names with a slash in the query, not in the path', async () => {
    global.fetch.mockResolvedValue({ ok: true, status: 204 });
    await deleteCategory('Pets / Animals');
    await deleteSubcategory('Pets / Animals', 'Cafe / coffee');
    const [first] = global.fetch.mock.calls[0];
    const [second] = global.fetch.mock.calls[1];
    expect(new URL(first).pathname).toBe('/v1/categories');
    expect(new URL(first).searchParams.get('name')).toBe('Pets / Animals');
    expect(new URL(second).searchParams.get('name')).toBe('Cafe / coffee');
  });

  it('patches an installment step and status', async () => {
    global.fetch.mockResolvedValue(ok({}));
    await incrementInstallmentStep(3, 5, 'active');
    const [url, options] = global.fetch.mock.calls[0];
    expect(url).toBe('https://api.test/v1/installments/3');
    expect(JSON.parse(options.body)).toEqual({ current_step: 5, status: 'active' });
  });
});

// ac: exchange-rate-from-transfers - the rate of a currency is the median of its last 10 transfers
// into the home currency, from wallets of any source currency
describe('getRecentFxRate', () => {
  const rates = (list) => ok({ rates: list });

  it('returns the median of the transfer rates', async () => {
    global.fetch.mockResolvedValue(rates([500, 540, 520, 900, 10]));
    expect(await getRecentFxRate({ fromWallets: ['USD1', 'USD2'], toWallets: ['BCA'] })).toBe(520);
    const params = new URL(global.fetch.mock.calls[0][0]).searchParams;
    expect(params.get('from_wallets')).toBe('USD1,USD2');
    expect(params.get('to_wallets')).toBe('BCA');
    expect(params.get('limit')).toBe('10');
  });

  it('returns null with no transfer or with no wallet on either side', async () => {
    global.fetch.mockResolvedValue(rates([]));
    expect(await getRecentFxRate({ fromWallets: ['BBL'], toWallets: ['BCA'] })).toBeNull();
    expect(await getRecentFxRate({ fromWallets: [], toWallets: ['BCA'] })).toBeNull();
    expect(await getRecentFxRate({ fromWallets: ['BBL'], toWallets: [] })).toBeNull();
  });
});
