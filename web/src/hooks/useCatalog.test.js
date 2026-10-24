import { vi } from 'vitest';
import { refreshCatalog } from './useCatalog';
import * as api from '../api/client';
import { getCurrencySettings, saveCurrencySettings, DEFAULT_CURRENCY_SETTINGS } from '../constants/currency';
import { WALLETS } from '../constants/transaction';

vi.mock('../api/client');

beforeEach(() => {
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })));
  api.getAccounts.mockResolvedValue([{ id: 'MAIN', name: 'Main', currency: 'USD', type: 'checking' }]);
  api.getCategories.mockResolvedValue([]);
  api.getSubcategories.mockResolvedValue([]);
});
afterEach(() => {
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

// ac: web-first-run-setup - an app_settings row is applied over the legacy defaults
it('applies the app_settings row to the currency settings', async () => {
  api.getAppSettings.mockResolvedValue({
    id: 1, home_code: 'USD', home_label: 'United States', foreign_code: null, foreign_label: null,
    cycle_start_day: 1, fallback_rate: null,
  });
  await refreshCatalog();
  expect(getCurrencySettings()).toEqual({
    homeCode: 'USD', homeLabel: 'United States', cycleStartDay: 1, currencies: [],
  });
  expect(WALLETS.map(w => w.id)).toEqual(['MAIN']);
});

// ac: web-first-run-setup - no row, or a failed read, keeps the stored or legacy settings
it('keeps the current settings when there is no row or the read fails', async () => {
  api.getAppSettings.mockResolvedValue(null);
  await refreshCatalog();
  expect(getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);
  api.getAppSettings.mockRejectedValue(new Error('missing table'));
  await refreshCatalog();
  expect(getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);
  expect(WALLETS.map(w => w.id)).toEqual(['MAIN']);
});
