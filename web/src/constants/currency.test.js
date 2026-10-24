import { vi } from 'vitest';
import {
  DEFAULT_CURRENCY_SETTINGS, symbolFor, isZeroDecimalCurrency, formatMoney, currencyHeading,
  getCurrencySettings, saveCurrencySettings, getCurrencyVersion, subscribeCurrency,
  applyAppSettings, currenciesInUse, labelFor, fallbackRateFor, isHome,
} from './currency';
import { walletCurrency, deriveTab, currencySymbol, applyCatalog } from './transaction';

// saveAppSettings is the real function here. Its request goes through this fetch mock.
const fetchMock = vi.fn(() => Promise.resolve({ ok: true }));
const savedBody = () => JSON.parse(fetchMock.mock.calls.at(-1)[1].body);

function memoryStorage(initial = {}) {
  const data = { ...initial };
  return {
    getItem: k => (k in data ? data[k] : null),
    setItem: (k, v) => { data[k] = String(v); },
    data,
  };
}

beforeEach(() => {
  vi.stubGlobal('localStorage', memoryStorage());
  fetchMock.mockClear();
  vi.stubGlobal('fetch', fetchMock);
});
afterEach(() => {
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  applyCatalog({});
  vi.unstubAllGlobals();
  vi.resetModules();
});

// ac: web-configurable-currency-pair — defaults match Android so an existing installation looks unchanged
it('defaults to IDR/Indonesia with a THB/Thailand override and the legacy cycle start day 26', () => {
  expect(getCurrencySettings()).toEqual({
    homeCode: 'IDR', homeLabel: 'Indonesia', cycleStartDay: 26,
    currencies: [{ code: 'THB', label: 'Thailand', rate: null }],
  });
});

// ac: web-configurable-currency-pair — currency symbols and decimal places come from one lookup
it('looks up symbols and shows the code for an unknown currency', () => {
  expect(symbolFor('IDR')).toBe('Rp ');
  expect(symbolFor('THB')).toBe('฿');
  expect(symbolFor('USD')).toBe('$');
  expect(symbolFor('CHF')).toBe('CHF ');
});

it('formats zero-decimal currencies without decimals and others with two', () => {
  expect(isZeroDecimalCurrency('JPY')).toBe(true);
  expect(formatMoney('IDR', 1234567)).toBe('Rp 1,234,567');
  expect(formatMoney('THB', 1234.5)).toBe('฿1,234.50');
  expect(formatMoney('CHF', 10)).toBe('CHF 10.00');
});

// ac: web-configurable-currency-pair — section headings use the configured labels
it('builds a section heading from the configured label and code', () => {
  expect(currencyHeading('THB')).toBe('Thailand — ฿ THB');
  expect(currencyHeading('IDR')).toBe('Indonesia — Rp IDR');
  saveCurrencySettings({ homeCode: 'usd', homeLabel: 'United States', currencies: [{ code: 'eur', label: 'Europe', rate: '' }] });
  expect(currencyHeading('USD')).toBe('United States — $ USD');
  expect(currencyHeading('EUR')).toBe('Europe — € EUR');
  expect(currencyHeading('JPY')).toBe('¥ JPY');
});

// ac: web-configurable-currency-pair — the pair is stored in this browser's localStorage
it('saves the pair to localStorage with uppercase codes and notifies subscribers', () => {
  const listener = vi.fn();
  const unsubscribe = subscribeCurrency(listener);
  const before = getCurrencyVersion();
  const error = saveCurrencySettings({ homeCode: ' usd ', homeLabel: ' US ', currencies: [{ code: 'eur', label: 'EU', rate: '1.1' }] });
  expect(error).toBeNull();
  expect(getCurrencySettings()).toMatchObject({
    homeCode: 'USD', homeLabel: 'US', currencies: [{ code: 'EUR', label: 'EU', rate: 1.1 }],
  });
  expect(JSON.parse(localStorage.data.currencySettings)).toMatchObject({ homeCode: 'USD', currencies: [{ code: 'EUR' }] });
  expect(listener).toHaveBeenCalledTimes(1);
  expect(getCurrencyVersion()).toBe(before + 1);
  unsubscribe();
});

it('rejects a home code that is not three letters and a blank home label without changing the settings', () => {
  for (const code of ['  ', 'US', 'USDX', 'U5D']) {
    expect(saveCurrencySettings({ ...DEFAULT_CURRENCY_SETTINGS, homeCode: code })).toMatch('three letters');
  }
  expect(saveCurrencySettings({ ...DEFAULT_CURRENCY_SETTINGS, homeLabel: '  ' })).toMatch('home currency label is required');
  expect(getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);
});

it('drops an override for the new home currency', () => {
  expect(saveCurrencySettings({ ...DEFAULT_CURRENCY_SETTINGS, homeCode: 'thb' })).toBeNull();
  expect(getCurrencySettings().currencies).toEqual([]);
});

// ac: web-configurable-currency-pair — missing or unreadable storage falls back to the defaults
it('reads the legacy stored pair on load as one override', async () => {
  vi.stubGlobal('localStorage', memoryStorage({
    currencySettings: JSON.stringify({ homeCode: 'USD', homeLabel: 'US', foreignCode: 'EUR', foreignLabel: 'EU' }),
  }));
  vi.resetModules();
  const fresh = await import('./currency');
  expect(fresh.getCurrencySettings()).toMatchObject({
    homeCode: 'USD', homeLabel: 'US', currencies: [{ code: 'EUR', label: 'EU', rate: null }],
  });
});

it('reads stored overrides on load', async () => {
  vi.stubGlobal('localStorage', memoryStorage({
    currencySettings: JSON.stringify({
      homeCode: 'USD', homeLabel: 'US', cycleStartDay: 5,
      currencies: [{ code: 'EUR', label: 'EU', rate: 1.1 }, { code: 'GBP', label: 'UK', rate: null }],
    }),
  }));
  vi.resetModules();
  const fresh = await import('./currency');
  expect(fresh.getCurrencySettings()).toEqual({
    homeCode: 'USD', homeLabel: 'US', cycleStartDay: 5,
    currencies: [{ code: 'EUR', label: 'EU', rate: 1.1 }, { code: 'GBP', label: 'UK', rate: null }],
  });
});

it('falls back to the defaults when storage is invalid or throws', async () => {
  vi.stubGlobal('localStorage', memoryStorage({ currencySettings: '{not json' }));
  vi.resetModules();
  expect((await import('./currency')).getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);

  vi.stubGlobal('localStorage', { getItem() { throw new Error('blocked'); }, setItem() { throw new Error('blocked'); } });
  vi.resetModules();
  const fresh = await import('./currency');
  expect(fresh.getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);
  expect(fresh.saveCurrencySettings({ ...DEFAULT_CURRENCY_SETTINGS, homeCode: 'USD' })).toBeNull();
  expect(fresh.getCurrencySettings().homeCode).toBe('USD');
});

// ac: web-configurable-currency-pair — a wallet's currency comes from its account, and an unknown
// wallet counts as the home currency
it('reads a wallet currency from its account and falls back to home', () => {
  applyCatalog({ accounts: [{ id: 'KBANK', name: 'Kasikorn', currency: 'THB' }] });
  expect(walletCurrency('KBANK')).toBe('THB');
  expect(walletCurrency('MISSING')).toBe('IDR');
  expect(currencySymbol('KBANK')).toBe('฿');
});

// ac: web-configurable-currency-pair — the ledger tab follows the configured foreign currency
it('routes the ledger tab by the home currency of the wallet', () => {
  applyCatalog({
    accounts: [
      { id: 'US_BANK', name: 'US Bank', currency: 'USD' },
      { id: 'EU_BANK', name: 'EU Bank', currency: 'EUR' },
    ],
  });
  saveCurrencySettings({ homeCode: 'EUR', homeLabel: 'Europe', currencies: [{ code: 'USD', label: 'US', rate: null }] });
  expect(deriveTab('US_BANK', 'expense')).toBe('EXPENSES');
  expect(deriveTab('US_BANK', 'income')).toBe('INCOME');
  expect(deriveTab('EU_BANK', 'expense')).toBe('IDR_EXPENSES');
  expect(deriveTab('EU_BANK', 'income')).toBe('IDR_INCOME');
});

// ac: web-first-run-setup - no other currency is fine, and then the app has one currency
it('saves no overrides and reports one currency in use', () => {
  applyCatalog({ accounts: [{ id: 'MAIN', name: 'Main', currency: 'USD' }] });
  expect(saveCurrencySettings({ homeCode: 'usd', homeLabel: 'US', currencies: [] })).toBeNull();
  expect(currenciesInUse()).toEqual(['USD']);
  expect(getCurrencySettings()).toMatchObject({ homeCode: 'USD', currencies: [] });
  expect(savedBody()).toEqual({ home_code: 'USD', home_label: 'US', cycle_start_day: 26, currencies: [] });
  expect(deriveTab('MAIN', 'expense')).toBe('IDR_EXPENSES');
});

it('with no accounts only the home currency is in use', () => {
  applyCatalog({ accounts: [] });
  expect(currenciesInUse()).toEqual(['IDR']);
});

// ac: web-configurable-currency-pair - the currencies in use are the home currency, then the account
// currencies in account order. An override alone does not make a currency.
it('lists the home currency first and then each account currency once, in account order', () => {
  applyCatalog({
    accounts: [
      { id: 'A', name: 'A', currency: 'THB' },
      { id: 'B', name: 'B', currency: 'IDR' },
      { id: 'C', name: 'C', currency: 'USD' },
      { id: 'D', name: 'D', currency: 'THB' },
      { id: 'E', name: 'E', currency: 'EUR' },
    ],
  });
  expect(currenciesInUse()).toEqual(['IDR', 'THB', 'USD', 'EUR']);
  applyCatalog({ accounts: [{ id: 'B', name: 'B', currency: 'IDR' }] });
  expect(currenciesInUse()).toEqual(['IDR']);
  expect(isHome('IDR')).toBe(true);
  expect(isHome('THB')).toBe(false);
});

it('labels a currency by its override, the home label, or its code', () => {
  saveCurrencySettings({ homeCode: 'IDR', homeLabel: 'Home', currencies: [{ code: 'THB', label: 'Trip', rate: null }] });
  expect(labelFor('IDR')).toBe('Home');
  expect(labelFor('THB')).toBe('Trip');
  expect(labelFor('USD')).toBe('USD');
});

// ac: exchange-rate-from-transfers - the fallback rate is the override, then 535 for IDR home and THB, then 1
it('picks the fallback rate: override, legacy 535, then 1', () => {
  expect(fallbackRateFor('IDR')).toBe(1);
  expect(fallbackRateFor('THB')).toBe(535);
  expect(fallbackRateFor('USD')).toBe(1);
  saveCurrencySettings({
    homeCode: 'IDR', homeLabel: 'Indonesia',
    currencies: [{ code: 'THB', label: 'Thailand', rate: 480 }, { code: 'USD', label: 'US', rate: '16000' }],
  });
  expect(fallbackRateFor('THB')).toBe(480);
  expect(fallbackRateFor('USD')).toBe(16000);
  saveCurrencySettings({ homeCode: 'USD', homeLabel: 'US', currencies: [{ code: 'THB', label: 'Thailand', rate: null }] });
  expect(fallbackRateFor('THB')).toBe(1);
  expect(fallbackRateFor('USD')).toBe(1);
});

// ac: web-first-run-setup - the cycle start day is a whole number from 1 to 28 and a rate is above 0
it('validates the cycle start day and each rate', () => {
  for (const day of [0, 29, 1.5, 'x', '']) {
    expect(saveCurrencySettings({ ...DEFAULT_CURRENCY_SETTINGS, cycleStartDay: day })).toMatch('cycle start day');
  }
  const withRate = rate => ({ ...DEFAULT_CURRENCY_SETTINGS, currencies: [{ code: 'THB', label: 'Thailand', rate }] });
  expect(saveCurrencySettings(withRate('-2'))).toMatch('exchange rate for THB');
  expect(saveCurrencySettings(withRate('abc'))).toMatch('exchange rate for THB');
  expect(getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);
  expect(saveCurrencySettings({ ...withRate('12.5'), cycleStartDay: '1' })).toBeNull();
  expect(getCurrencySettings()).toMatchObject({ cycleStartDay: 1, currencies: [{ code: 'THB', rate: 12.5 }] });
  expect(saveCurrencySettings(withRate(''))).toBeNull();
  expect(getCurrencySettings().currencies[0].rate).toBeNull();
});

// ac: web-first-run-setup - saving also saves the shared settings on the server, and a failure only logs
it('saves the settings with the currency list', async () => {
  saveCurrencySettings({
    ...DEFAULT_CURRENCY_SETTINGS, cycleStartDay: 15,
    currencies: [{ code: 'THB', label: 'Thailand', rate: 500 }, { code: 'USD', label: 'US', rate: null }],
  });
  expect(fetchMock.mock.calls.at(-1)[0]).toBe('https://api.test/v1/settings');
  expect(fetchMock.mock.calls.at(-1)[1].method).toBe('PUT');
  expect(savedBody()).toEqual({
    home_code: 'IDR', home_label: 'Indonesia', cycle_start_day: 15,
    currencies: [{ code: 'THB', label: 'Thailand', rate: 500 }, { code: 'USD', label: 'US', rate: null }],
  });
  const warn = vi.spyOn(console, 'warn').mockImplementation(() => {});
  fetchMock.mockRejectedValueOnce(new Error('offline'));
  expect(saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS)).toBeNull();
  await vi.waitFor(() => expect(warn).toHaveBeenCalled());
  warn.mockRestore();
});

// ac: web-first-run-setup - a missing row changes nothing
it('keeps the settings when there is no app_settings row', () => {
  applyAppSettings(null);
  expect(getCurrencySettings()).toEqual(DEFAULT_CURRENCY_SETTINGS);
});

// ac: web-first-run-setup - the currencies column of the row is used when it is present
it('reads the currencies column of an app_settings row', () => {
  applyAppSettings({
    id: 1, home_code: 'eur', home_label: 'Europe', cycle_start_day: 1,
    currencies: [
      { code: 'usd', label: 'US', rate: 0.9 }, { code: 'GBP', label: '', rate: 0 },
      { code: 'EUR', label: 'same as home', rate: 2 }, { code: ' ', label: 'blank', rate: 3 },
    ],
    foreign_code: 'THB', foreign_label: 'Thailand', fallback_rate: 5,
  });
  expect(getCurrencySettings()).toEqual({
    homeCode: 'EUR', homeLabel: 'Europe', cycleStartDay: 1,
    currencies: [{ code: 'USD', label: 'US', rate: 0.9 }, { code: 'GBP', label: 'GBP', rate: null }],
  });
  expect(currencyHeading('EUR')).toBe('Europe — € EUR');
});

// ac: web-first-run-setup - settings without a currency list mean no other currency
it('reads settings without a currency list as no other currency', () => {
  applyAppSettings({ home_code: 'usd', home_label: 'US', cycle_start_day: 1 });
  expect(getCurrencySettings()).toEqual({ homeCode: 'USD', homeLabel: 'US', cycleStartDay: 1, currencies: [] });
  applyAppSettings({ home_code: 'IDR', home_label: 'Indonesia', cycle_start_day: 26, currencies: [{ code: 'THB', label: null, rate: null }] });
  expect(getCurrencySettings().currencies).toEqual([{ code: 'THB', label: 'THB', rate: null }]);
});

// ac: web-first-run-setup - with neither a row nor storage, the legacy defaults apply
it('applies the legacy defaults with neither a row nor storage', async () => {
  vi.stubGlobal('localStorage', memoryStorage());
  vi.resetModules();
  const fresh = await import('./currency');
  expect(fresh.getCurrencySettings()).toEqual(fresh.DEFAULT_CURRENCY_SETTINGS);
  expect(fresh.getCurrencySettings()).toEqual({
    homeCode: 'IDR', homeLabel: 'Indonesia', cycleStartDay: 26, currencies: [{ code: 'THB', label: 'Thailand', rate: null }],
  });
});
