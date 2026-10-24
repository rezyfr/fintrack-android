import { moneyFlow } from './moneyFlow';
import { walletCurrency, isInvestmentWallet } from '../constants/transaction';

const WALLET_ZONE = '__wallet__';
const COMBINED_ZONE = '__all__';
const TOP_EXPENSES = 5;

const amountOf = r => Number(r.amount) || 0;
const sum = rows => rows.reduce((t, r) => t + amountOf(r), 0);

// ac: web-insights-parity - each row in the home currency: a row from a wallet in another currency is
// multiplied by the rate of that currency, and a transfer into one converts its received amount with
// the rate of the destination wallet's currency. rates maps a code to home units per one unit, with
// the home currency at 1. Same as Android's convertToHome.
export function convertToHome(rows, rates) {
  const rateOf = w => (w == null ? 1 : rates[walletCurrency(w)] ?? 1);
  return rows.map(r => ({
    ...r,
    amount: amountOf(r) * rateOf(r.wallet),
    to_amount: r.to_amount == null ? r.to_amount : Number(r.to_amount) * rateOf(r.to_wallet),
  }));
}

// ac: web-insights-parity — the expenses of each category, largest first, with the previous cycle's
// spend and, when any row has one, the split by subcategory. Same rules as Android's
// DashboardSummaryBuilder.
function categoryBreakdown(expenses, prevExpenses) {
  const total = sum(expenses);
  const byCat = new Map();
  for (const r of expenses) {
    const cat = r.category || 'Other';
    if (!byCat.has(cat)) byCat.set(cat, []);
    byCat.get(cat).push(r);
  }
  const prevByCat = new Map();
  for (const r of prevExpenses) {
    const cat = r.category || 'Other';
    prevByCat.set(cat, (prevByCat.get(cat) || 0) + amountOf(r));
  }
  return [...byCat.entries()]
    .map(([category, rows]) => {
      const amount = sum(rows);
      const subTotals = new Map();
      for (const r of rows) {
        const sub = r.subcategory && r.subcategory.trim() ? r.subcategory : 'None';
        subTotals.set(sub, (subTotals.get(sub) || 0) + amountOf(r));
      }
      const subs = [...subTotals.entries()]
        .map(([name, amt]) => ({ name, amount: amt, share: amount > 0 ? amt / amount : 0 }))
        .sort((a, b) => b.amount - a.amount);
      return {
        category,
        amount,
        share: total > 0 ? amount / total : 0,
        previous: prevByCat.get(category) || 0,
        subcategories: subs.some(s => s.name !== 'None') ? subs : [],
      };
    })
    .filter(c => c.amount > 0)
    .sort((a, b) => b.amount - a.amount);
}

// ac: web-insights-parity - every figure the Insights page shows for one book, built the same way as
// Android's GetDashboardDataUseCase.
// - code is the book's currency, or null for Combined (every currency in the home currency).
// - wallet, when set, is its own zone, so every transfer into or out of it counts.
// - Otherwise the zone is the currency, so a transfer inside one currency is neither money in nor
//   out. Investments are a zone of their own. Combined treats all currencies as one zone.
// rows, prevRows and cycleRows are all wallets' rows for the cycle, the previous cycle and the last
// six cycles (oldest first, ending with the selected one). rates is the rate table.
export function buildSummary({ rows, prevRows, cycleRows, cycles, code, wallet, rates, homeCode }) {
  const combined = code == null;
  const convert = list => (combined ? convertToHome(list, rates) : list);
  let zone, zoneOf, keep;
  if (wallet) {
    zone = WALLET_ZONE;
    zoneOf = w => (w === wallet ? WALLET_ZONE : 'OTHER');
    keep = r => r.wallet === wallet;
  } else if (combined) {
    zone = COMBINED_ZONE;
    zoneOf = w => (isInvestmentWallet(w) ? 'INV' : COMBINED_ZONE);
    keep = () => true;
  } else {
    zone = code;
    zoneOf = w => (isInvestmentWallet(w) ? 'INV' : walletCurrency(w));
    keep = r => walletCurrency(r.wallet) === code;
  }
  const current = convert(rows);
  const flow = moneyFlow(current, zone, zoneOf);
  const expenses = current.filter(r => r.tx_type === 'expense' && keep(r));
  const prevExpenses = convert(prevRows).filter(r => r.tx_type === 'expense' && keep(r));

  const byWallet = new Map();
  for (const r of expenses) {
    if (!r.wallet || amountOf(r) <= 0) continue;
    byWallet.set(r.wallet, (byWallet.get(r.wallet) || 0) + amountOf(r));
  }
  const categorySpend = sum(expenses);
  const dailySpend = {};
  for (const r of expenses) {
    const day = String(r.date).slice(0, 10);
    dailySpend[day] = (dailySpend[day] || 0) + amountOf(r);
  }
  const todayIso = new Date().toISOString().slice(0, 10);
  return {
    currency: code ?? homeCode,
    moneyIn: flow.moneyIn,
    moneyOut: flow.moneyOut,
    net: flow.net,
    categorySpend,
    categories: categoryBreakdown(expenses, prevExpenses),
    topExpenses: expenses.filter(r => amountOf(r) > 0).sort((a, b) => amountOf(b) - amountOf(a)).slice(0, TOP_EXPENSES),
    walletBreakdown: [...byWallet.entries()]
      .map(([id, amount]) => ({ wallet: id, amount, share: categorySpend > 0 ? amount / categorySpend : 0 }))
      .sort((a, b) => b.amount - a.amount),
    dailySpend,
    trend: cycles.map((c, i) => {
      const f = moneyFlow(convert(cycleRows[i] || []), zone, zoneOf);
      return { from: c.from, to: c.to, moneyIn: f.moneyIn, moneyOut: f.moneyOut, inProgress: todayIso >= c.from && todayIso <= c.to };
    }),
  };
}

// The summary of each currency in codes, and the combined one. The caller picks the one of the
// selected book.
export function buildInsights({ codes, ...input }) {
  const byCurrency = {};
  for (const code of codes) byCurrency[code] = buildSummary({ ...input, code });
  return { combined: buildSummary({ ...input, code: null }), byCurrency };
}

export function isEmptyInsights(s) {
  return s.moneyIn === 0 && s.moneyOut === 0 && s.categorySpend === 0;
}
