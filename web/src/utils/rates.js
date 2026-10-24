import { getRecentFxRate } from '../api/client';
import { WALLETS } from '../constants/transaction';
import { currenciesInUse, fallbackRateFor, getCurrencySettings } from '../constants/currency';

// ac: exchange-rate-from-transfers - the rate table maps each currency in use to its home units per
// one unit, with the home currency at 1. The live rate of a currency is the median of the last 10
// transfers from one of its wallets to a home wallet. Without such a transfer it is the fallback rate.
// derived maps a code to its live rate or null.
export function buildRateTable(derived = {}) {
  const table = { [getCurrencySettings().homeCode]: 1 };
  for (const code of currenciesInUse()) {
    if (code in table) continue;
    table[code] = derived[code] ?? fallbackRateFor(code);
  }
  return table;
}

// Reads the live rate of every non-home currency in use. A failed read counts as no live rate.
// Returns { table, derived } where derived lists the codes whose rate came from transfers.
export async function loadRateTable() {
  const { homeCode } = getCurrencySettings();
  const toWallets = WALLETS.filter(w => w.currency === homeCode).map(w => w.id);
  const codes = currenciesInUse().filter(c => c !== homeCode);
  const live = await Promise.all(codes.map(async code => {
    try {
      return await getRecentFxRate({
        fromWallets: WALLETS.filter(w => w.currency === code).map(w => w.id),
        toWallets,
      });
    } catch {
      return null;
    }
  }));
  const derived = {};
  codes.forEach((code, i) => { derived[code] = live[i] ?? null; });
  return { table: buildRateTable(derived), derived: codes.filter(c => derived[c] != null) };
}

// An amount in a currency, converted to the home currency. A currency missing from the table
// converts at its fallback rate.
export function toHomeAmount(amount, code, table) {
  return amount * (table[code] ?? fallbackRateFor(code));
}
