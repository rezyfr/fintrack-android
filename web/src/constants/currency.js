import { saveAppSettings } from '../api/client';
import { WALLETS } from './transaction';

// ac: web-configurable-currency-pair - the currencies this app tracks and how they are labeled.
// The home currency is always one of them. Every other currency comes from the accounts: a currency
// is in use when an account holds it. `currencies` is a list of overrides for those other currencies:
// { code, label, rate }. The label is the book name, and the rate is the fallback rate in home units
// per one unit of that currency, or null.
// The defaults match CurrencySettings.kt on Android and what the web app originally shipped
// hardcoded, so an existing installation looks identical until the settings are saved.
export const DEFAULT_CURRENCY_SETTINGS = {
  homeCode: 'IDR',
  homeLabel: 'Indonesia',
  cycleStartDay: 26,
  currencies: [{ code: 'THB', label: 'Thailand', rate: null }],
};

const STORAGE_KEY = 'currencySettings';

// Common currency symbols. This is the same table Android uses (CurrencySettings.kt), with a
// trailing space after "Rp" to keep the existing web format ("Rp 1,000"). A code that is not in the
// table is shown as the code itself (for example "CHF 10"), not a guessed symbol.
const CURRENCY_SYMBOLS = {
  IDR: 'Rp ', THB: '฿', USD: '$', EUR: '€', GBP: '£', SGD: 'S$', JPY: '¥', MYR: 'RM',
  AUD: 'A$', VND: '₫', PHP: '₱', CNY: '¥',
};

// Currencies conventionally shown with no decimal places. Same set as Android.
const ZERO_DECIMAL_CURRENCIES = new Set(['IDR', 'JPY', 'VND', 'KRW']);

// ac: web-configurable-currency-pair — currency symbols come from one shared lookup instead of
// separate hardcoded THB/IDR branches in each view
export function symbolFor(code) {
  return CURRENCY_SYMBOLS[code] ?? `${code} `;
}

export function isZeroDecimalCurrency(code) {
  return ZERO_DECIMAL_CURRENCIES.has(code);
}

// Formats an amount in a currency: symbol first, no decimals for zero-decimal currencies, two
// decimals otherwise. Pass an absolute value when the caller shows the sign separately.
export function formatMoney(code, amount) {
  const decimals = isZeroDecimalCurrency(code) ? 0 : 2;
  return `${symbolFor(code)}${Number(amount).toLocaleString('en', {
    minimumFractionDigits: decimals,
    maximumFractionDigits: decimals,
  })}`;
}

// ac: web-configurable-currency-pair — the heading for a currency section, for example
// "Thailand - ฿ THB". It uses the configured label for the home currency or an override, and the bare
// code for any other currency.
export function currencyHeading(code) {
  const label = configuredLabel(code);
  const mark = `${symbolFor(code).trim()} ${code}`;
  return label ? `${label} — ${mark}` : mark;
}

function validDay(v) {
  return Number.isInteger(v) && v >= 1 && v <= 28;
}

function validRate(v) {
  return typeof v === 'number' && Number.isFinite(v) && v > 0;
}

const text = v => String(v ?? '').trim();

// Cleans an override list: uppercase codes, no blank code, no duplicate, no override for the home
// currency. A rate that is not a number above 0 becomes null. A blank label becomes the code.
function normalizeOverrides(list, homeCode) {
  const seen = new Set([homeCode]);
  const out = [];
  for (const item of Array.isArray(list) ? list : []) {
    const code = text(item?.code).toUpperCase();
    if (!code || seen.has(code)) continue;
    seen.add(code);
    const rate = Number(item.rate);
    out.push({ code, label: text(item.label) || code, rate: validRate(rate) ? rate : null });
  }
  return out;
}

// The one override of the older settings: a foreign code, its label and the single fallback rate.
// An empty foreign code means no override, and a rate of 0 or null means no rate.
function legacyOverrides(foreignCode, foreignLabel, rate, homeCode) {
  return normalizeOverrides([{ code: foreignCode, label: foreignLabel, rate }], homeCode);
}

// ac: web-configurable-currency-pair - the settings are cached in this browser (localStorage) and the
// shared copy lives in the app_settings table. Missing, unreadable, or incomplete storage falls back
// to the defaults. The older key holds homeCode, homeLabel, foreignCode and foreignLabel, and maps to
// one override.
function readStored() {
  try {
    const raw = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (raw && typeof raw.homeCode === 'string' && typeof raw.homeLabel === 'string'
      && raw.homeCode.trim() && raw.homeLabel.trim()) {
      const homeCode = raw.homeCode.trim().toUpperCase();
      const currencies = Array.isArray(raw.currencies)
        ? normalizeOverrides(raw.currencies, homeCode)
        : legacyOverrides(raw.foreignCode, raw.foreignLabel, raw.fallbackRate, homeCode);
      return {
        homeCode,
        homeLabel: raw.homeLabel.trim(),
        cycleStartDay: validDay(raw.cycleStartDay) ? raw.cycleStartDay : DEFAULT_CURRENCY_SETTINGS.cycleStartDay,
        currencies,
      };
    }
  } catch {
    // localStorage can be unavailable (private mode) or hold invalid JSON
  }
  return cloneDefaults();
}

function cloneDefaults() {
  return { ...DEFAULT_CURRENCY_SETTINGS, currencies: DEFAULT_CURRENCY_SETTINGS.currencies.map(c => ({ ...c })) };
}

let settings = readStored();
let version = 0;
const listeners = new Set();

export function getCurrencySettings() {
  return settings;
}

export function subscribeCurrency(listener) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

export function getCurrencyVersion() {
  return version;
}

export function isHome(code) {
  return code === settings.homeCode;
}

// The currencies in use: the home currency first, then every distinct currency of the accounts, in
// account order. Nothing else defines a currency.
export function currenciesInUse() {
  const codes = [settings.homeCode];
  for (const w of WALLETS) {
    if (w.currency && !codes.includes(w.currency)) codes.push(w.currency);
  }
  return codes;
}

function overrideFor(code) {
  return settings.currencies.find(c => c.code === code);
}

// The label the user set for the home currency or an override, or null.
function configuredLabel(code) {
  if (isHome(code)) return settings.homeLabel;
  return overrideFor(code)?.label || null;
}

// The book name of a currency: its label, or the code when it has none.
export function labelFor(code) {
  return configuredLabel(code) || code;
}

// How many home units one unit of the currency buys when no transfer gives a real rate. The saved
// rate wins. An installation on the legacy IDR home and THB keeps the 535 it always used. Any other
// currency converts 1 to 1. The home currency is always 1.
export function fallbackRateFor(code) {
  if (isHome(code)) return 1;
  const rate = overrideFor(code)?.rate;
  if (validRate(rate)) return rate;
  return settings.homeCode === 'IDR' && code === 'THB' ? 535 : 1;
}

function commit(next) {
  settings = next;
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  } catch {
    // The settings still apply for this session when storage is unavailable
  }
  version += 1;
  listeners.forEach(l => l());
}

// ac: web-first-run-setup - applies the settings read from the server. Missing settings keep
// whatever the store holds. The settings on the server are the shared copy, so they win over the browser cache.
export function applyAppSettings(row) {
  if (!row || !row.home_code) return;
  const homeCode = text(row.home_code).toUpperCase();
  const currencies = normalizeOverrides(Array.isArray(row.currencies) ? row.currencies : [], homeCode);
  commit({
    homeCode,
    homeLabel: text(row.home_label) || homeCode,
    cycleStartDay: validDay(row.cycle_start_day) ? row.cycle_start_day : settings.cycleStartDay,
    currencies,
  });
}

// Codes are stored uppercase. Returns an error message, or null when the settings were saved.
// next.currencies lists overrides, and a rate may be a number, a numeric string, or empty (no rate).
// An override for a code that is not in the list is kept, so a label survives while no account uses
// the currency. A next value without cycleStartDay or currencies keeps the current one.
// The shared copy on the server is saved on a best effort basis: a failure is logged and ignored.
export function saveCurrencySettings(next) {
  const homeCode = text(next.homeCode).toUpperCase();
  const homeLabel = text(next.homeLabel);
  const cycleStartDay = next.cycleStartDay === undefined ? settings.cycleStartDay : Number(text(next.cycleStartDay));
  if (!/^[A-Z]{3}$/.test(homeCode)) return 'The home currency code must be three letters';
  if (!homeLabel) return 'The home currency label is required';
  if (!validDay(cycleStartDay)) return 'The cycle start day must be a whole number from 1 to 28';
  const given = next.currencies === undefined ? settings.currencies : next.currencies;
  const rates = [];
  for (const c of given) {
    const raw = c.rate == null ? '' : text(c.rate);
    const rate = raw === '' ? null : Number(raw);
    if (rate !== null && !validRate(rate)) return `The exchange rate for ${text(c.code).toUpperCase()} must be a number above 0`;
    rates.push(rate);
  }
  const currencies = normalizeOverrides(given.map((c, i) => ({ ...c, rate: rates[i] })), homeCode);
  commit({ homeCode, homeLabel, cycleStartDay, currencies });
  saveAppSettings({
    home_code: homeCode,
    home_label: homeLabel,
    cycle_start_day: cycleStartDay,
    currencies,
  }).catch(e => console.warn('Could not save app settings', e));
  return null;
}

// ac: web-insights-parity — a short amount for tight rows, the same as Android's formatMoneyCompact:
// up to 999 as formatMoney, then thousands as "k" and millions as "M" with at most one decimal.
export function formatMoneyCompact(code, amount) {
  const v = Math.abs(amount);
  if (v < 1000) return (amount < 0 ? '-' : '') + formatMoney(code, v);
  const [scaled, unit] = v < 1_000_000 ? [v / 1000, 'k'] : [v / 1_000_000, 'M'];
  const body = scaled < 100 ? scaled.toFixed(1).replace(/\.0$/, '') : scaled.toFixed(0);
  return `${amount < 0 ? '-' : ''}${symbolFor(code)}${body}${unit}`;
}
