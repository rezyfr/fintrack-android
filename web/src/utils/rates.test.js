import { vi } from 'vitest';
import { buildRateTable, loadRateTable, toHomeAmount } from './rates';
import * as api from '../api/client';
import { applyCatalog } from '../constants/transaction';
import { DEFAULT_CURRENCY_SETTINGS, saveCurrencySettings } from '../constants/currency';

vi.mock('../api/client');

const ACCOUNTS = [
  { id: 'BCA', name: 'BCA', currency: 'IDR' },
  { id: 'BBL', name: 'BBL', currency: 'THB' },
  { id: 'USD1', name: 'US', currency: 'USD' },
  { id: 'USD2', name: 'US 2', currency: 'USD' },
];

beforeEach(() => {
  vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {} });
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })));
  applyCatalog({ accounts: ACCOUNTS });
});
afterEach(() => {
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

// ac: exchange-rate-from-transfers - one table, home at 1: live rate, else override, else legacy 535, else 1
describe('rate table', () => {
  it('uses the legacy 535 for THB, 1 for a currency with no rate, and 1 for home', () => {
    expect(buildRateTable()).toEqual({ IDR: 1, THB: 535, USD: 1 });
  });

  it('prefers an override over the fallback, and a live rate over the override', () => {
    saveCurrencySettings({
      homeCode: 'IDR', homeLabel: 'Indonesia',
      currencies: [{ code: 'THB', label: 'Thailand', rate: 480 }, { code: 'USD', label: 'US', rate: 16000 }],
    });
    expect(buildRateTable()).toEqual({ IDR: 1, THB: 480, USD: 16000 });
    expect(buildRateTable({ THB: 500, USD: null })).toEqual({ IDR: 1, THB: 500, USD: 16000 });
  });

  it('uses 1 for every currency when home is not IDR and no override has a rate', () => {
    saveCurrencySettings({ homeCode: 'EUR', homeLabel: 'Europe', currencies: [] });
    expect(buildRateTable()).toEqual({ EUR: 1, IDR: 1, THB: 1, USD: 1 });
  });

  it('reads the live rate of each non-home currency from transfers into home wallets', async () => {
    api.getRecentFxRate.mockImplementation(async ({ fromWallets, toWallets }) => {
      expect(toWallets).toEqual(['BCA']);
      return fromWallets[0] === 'BBL' ? 520 : null;
    });
    const loaded = await loadRateTable();
    expect(api.getRecentFxRate).toHaveBeenCalledWith({ fromWallets: ['BBL'], toWallets: ['BCA'] });
    expect(api.getRecentFxRate).toHaveBeenCalledWith({ fromWallets: ['USD1', 'USD2'], toWallets: ['BCA'] });
    expect(loaded.table).toEqual({ IDR: 1, THB: 520, USD: 1 });
    expect(loaded.derived).toEqual(['THB']);
  });

  it('falls back when the read fails', async () => {
    api.getRecentFxRate.mockRejectedValue(new Error('offline'));
    const loaded = await loadRateTable();
    expect(loaded.table).toEqual({ IDR: 1, THB: 535, USD: 1 });
    expect(loaded.derived).toEqual([]);
  });

  it('converts an amount to home with the rate of its currency', () => {
    expect(toHomeAmount(10, 'THB', { IDR: 1, THB: 500 })).toBe(5000);
    expect(toHomeAmount(10, 'IDR', { IDR: 1, THB: 500 })).toBe(10);
  });
});
