import { useState, useEffect } from 'react';
import { getTransactions } from '../api/supabase';
import { WALLETS, walletCurrency } from '../constants/transaction';

const PRESETS = [
  { id: 'this',   label: 'This Month'    },
  { id: 'last',   label: 'Last Month'    },
  { id: 'last3',  label: 'Last 3 Months' },
  { id: 'custom', label: 'Custom'        },
];

const CATEGORIES = [
  'food_drink', 'transport', 'bills', 'subscriptions', 'entertainment',
  'groceries', 'health_wellbeing', 'family', 'shopping', 'travel',
  'business', 'gifts', 'other',
];

const CATEGORY_LABELS = {
  food_drink: 'Food & Drink', transport: 'Transport', bills: 'Bills',
  subscriptions: 'Subscriptions', entertainment: 'Entertainment',
  groceries: 'Groceries', health_wellbeing: 'Health & Wellbeing',
  family: 'Family', shopping: 'Shopping', travel: 'Travel',
  business: 'Business', gifts: 'Gifts', other: 'Other',
};

// ac: insights-filter-by-wallet — selecting a specific wallet refetches and shows only data for that wallet
const CATEGORY_TO_KEY = {
  'Food & Drink':       'food_drink',
  'Transport':          'transport',
  'Bills':              'bills',
  'Subscriptions':      'subscriptions',
  'Entertainment':      'entertainment',
  'Groceries':          'groceries',
  'Health & Wellbeing': 'health_wellbeing',
  'Family':             'family',
  'Shopping':           'shopping',
  'Travel':             'travel',
  'Business':           'business',
  'Gifts':              'gifts',
  'Other':              'other',
};

const CATEGORY_CLASS = {
  food_drink: 'chip-food', transport: 'chip-transport', bills: 'chip-bills',
  subscriptions: 'chip-subscriptions', entertainment: 'chip-entertainment',
  groceries: 'chip-groceries', health_wellbeing: 'chip-health',
  family: 'chip-family', shopping: 'chip-shopping', travel: 'chip-travel',
  business: 'chip-business', gifts: 'chip-gifts', other: 'chip-other',
};

// ac: insights-pay-cycle-periods — a period runs from pay day through the day before the next one,
// so the cycle holding a date ends on the 25th of that month, or of the next month once the 26th has
// passed. Every preset resolves to such a range; there is no calendar-month period any more.
const PAY_CYCLE_START_DAY = 26;

function isoDate(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// offset 0 is the cycle in progress, 1 the one before it, and so on.
function payCycle(offset = 0, now = new Date()) {
  const monthsBack = offset + (now.getDate() >= PAY_CYCLE_START_DAY ? 0 : 1);
  const start = new Date(now.getFullYear(), now.getMonth() - monthsBack, PAY_CYCLE_START_DAY);
  const end   = new Date(start.getFullYear(), start.getMonth() + 1, PAY_CYCLE_START_DAY - 1);
  return { from: isoDate(start), to: isoDate(end) };
}

// ac: insights-pay-cycle-periods — Last 3 Months spans the three most recent cycles as one range
function presetRange(preset, dateFrom, dateTo) {
  if (preset === 'custom') return { from: dateFrom, to: dateTo };
  if (preset === 'last')   return payCycle(1);
  if (preset === 'last3')  return { from: payCycle(2).from, to: payCycle(0).to };
  return payCycle(0);
}

const TRANSFER_IN_KEY = '__transfer_in__';
const TRANSFER_OUT_KEY = '__transfer_out__';
const TRANSFER_LABELS = { [TRANSFER_IN_KEY]: 'Transfer In', [TRANSFER_OUT_KEY]: 'Transfer Out' };

// ac: insights-show-income-and-transfers — one request per wallet; each request keeps track of which
// wallet it was queried for, since a transfer's role (in vs out) depends on which side of the wallet
// filter matched.
function buildWalletRequests(wallets, from, to, extraParams = {}) {
  return wallets.map(w => ({ wallet: w, promise: getTransactions({ wallet: w, dateFrom: from, dateTo: to, ...extraParams }) }));
}

const EMPTY_TRANSFERS = { in: 0, out: 0 };

// ac: insights-filter-by-wallet — selecting a specific wallet refetches and shows only data for that wallet
function aggregateTransactions(rows) {
  let income = 0, expenses = 0;
  const catTotals = {};
  for (const row of rows) {
    const amount = Number(row.amount) || 0;
    if (row.tx_type === 'income') {
      income += amount;
    } else if (row.tx_type === 'expense') {
      expenses += amount;
      const key = CATEGORY_TO_KEY[row.category] || 'other';
      catTotals[key] = (catTotals[key] || 0) + amount;
    }
  }
  const cats = CATEGORIES
    .map(k => ({ key: k, label: CATEGORY_LABELS[k], amount: catTotals[k] || 0 }))
    .filter(c => c.amount > 0)
    .sort((a, b) => b.amount - a.amount);
  return { income, expenses, net: income - expenses, cats };
}

function fmt(amount, currency) {
  const isIDR = currency === 'IDR';
  return `${isIDR ? 'Rp ' : '฿'}${Math.abs(amount).toLocaleString('en', {
    minimumFractionDigits: isIDR ? 0 : 2,
    maximumFractionDigits: isIDR ? 0 : 2,
  })}`;
}

function formatDate(iso) {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  if (!y || !m || !d) return iso;
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  return `${d} ${months[Number(m) - 1]} ${y.slice(2)}`;
}

function TransactionDetail({ currency, loading, error, rows }) {
  if (loading) return <div className="breakdown-detail-state">Loading…</div>;
  if (error) return <div className="breakdown-detail-state breakdown-detail-error">{error}</div>;
  if (rows.length === 0) return <div className="breakdown-detail-state">No transactions found.</div>;
  // ac: insights-subcategory-breakdown — group the category's transactions by subcategory (None when unset)
  const subTotals = rows.reduce((acc, r) => {
    const key = r.subcategory && r.subcategory.trim() ? r.subcategory : 'None';
    acc[key] = (acc[key] || 0) + Number(r.amount || 0);
    return acc;
  }, {});
  const subRows = Object.entries(subTotals).sort((a, b) => b[1] - a[1]);
  const showSubBreakdown = subRows.length > 1;
  return (
    <div className="breakdown-detail">
      {/* ac: insights-subcategory-breakdown — per-subcategory subtotals shown above the transactions */}
      {showSubBreakdown && (
        <div className="breakdown-subcats">
          {subRows.map(([label, amount]) => (
            <div key={label} className="breakdown-subcat-row">
              <span className="breakdown-subcat-label">{label}</span>
              <span className="breakdown-subcat-value">{fmt(amount, currency)}</span>
            </div>
          ))}
        </div>
      )}
      {rows.map(r => (
        <div key={r.id} className="breakdown-detail-row">
          <span className="breakdown-detail-date">{formatDate(r.date)}</span>
          <span className="breakdown-detail-item">{r.item}</span>
          <span className="breakdown-detail-value">{fmt(r.amount, currency)}</span>
        </div>
      ))}
    </div>
  );
}

// ac: insights-show-income-and-transfers — Transfer In/Out cards, clickable like a category row
function TransferStatCard({ label, amount, currency, isExpanded, onToggle, expandedRows, expandedLoading, expandedError }) {
  return (
    <div className="stat-card-wrap">
      <button className="stat-card stat-card-btn" onClick={onToggle} aria-expanded={isExpanded}>
        <div className="stat-label">
          {label}
          <span className={`breakdown-chevron${isExpanded ? ' breakdown-chevron-open' : ''}`}>▸</span>
        </div>
        <div className="stat-value">{fmt(amount, currency)}</div>
      </button>
      {isExpanded && (
        <TransactionDetail currency={currency} loading={expandedLoading} error={expandedError} rows={expandedRows} />
      )}
    </div>
  );
}

function CurrencySection({
  currency, summary, transfers,
  expandedKey, onToggleCategory, expandedRows, expandedLoading, expandedError,
}) {
  if (summary.income === 0 && summary.expenses === 0 && transfers.in === 0 && transfers.out === 0) return null;
  const label = currency === 'THB' ? 'Thailand — ฿ THB' : 'Indonesia — Rp IDR';
  return (
    <div className="insight-section">
      <div className="insight-currency-label">{label}</div>
      <div className="stats-row">
        <div className="stat-card">
          <div className="stat-label">Income</div>
          <div className="stat-value income">{fmt(summary.income, currency)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Expenses</div>
          <div className="stat-value expense">{fmt(summary.expenses, currency)}</div>
        </div>
        <div className="stat-card">
          <div className="stat-label">Net</div>
          <div className={`stat-value ${summary.net >= 0 ? 'income' : 'expense'}`}>
            {summary.net >= 0 ? '+' : '-'}{fmt(summary.net, currency)}
          </div>
        </div>
      </div>
      {/* ac: insights-show-income-and-transfers — Transfer In/Out shown separately from Income/Expenses/Net */}
      {(transfers.in > 0 || transfers.out > 0) && (
        <div className="stats-row">
          <TransferStatCard
            label={TRANSFER_LABELS[TRANSFER_IN_KEY]} amount={transfers.in} currency={currency}
            isExpanded={expandedKey === TRANSFER_IN_KEY}
            onToggle={() => onToggleCategory(TRANSFER_IN_KEY)}
            expandedRows={expandedRows} expandedLoading={expandedLoading} expandedError={expandedError}
          />
          <TransferStatCard
            label={TRANSFER_LABELS[TRANSFER_OUT_KEY]} amount={transfers.out} currency={currency}
            isExpanded={expandedKey === TRANSFER_OUT_KEY}
            onToggle={() => onToggleCategory(TRANSFER_OUT_KEY)}
            expandedRows={expandedRows} expandedLoading={expandedLoading} expandedError={expandedError}
          />
        </div>
      )}
      {summary.cats.length > 0 && (
        <div className="breakdown-card">
          <div className="breakdown-header">
            <span>Spending Breakdown</span>
            <span>{fmt(summary.expenses, currency)}</span>
          </div>
          {summary.cats.map(cat => {
            const pct = summary.expenses > 0 ? (cat.amount / summary.expenses) * 100 : 0;
            const isExpanded = expandedKey === cat.key;
            return (
              <div key={cat.key} className="breakdown-row">
                {/* ac: expand-insights-category-to-transactions — clicking a category row expands its transactions */}
                <button
                  className="breakdown-row-btn"
                  onClick={() => onToggleCategory(cat.key)}
                  aria-expanded={isExpanded}
                >
                  <div className="breakdown-meta">
                    <span className="breakdown-meta-left">
                      <span className={`chip ${CATEGORY_CLASS[cat.key]}`}>{cat.label}</span>
                      <span className={`breakdown-chevron${isExpanded ? ' breakdown-chevron-open' : ''}`}>▸</span>
                    </span>
                    <span className="breakdown-amount">
                      {fmt(cat.amount, currency)}
                      <span className="breakdown-pct"> · {Math.round(pct)}%</span>
                    </span>
                  </div>
                  <div className="breakdown-track">
                    <div className="breakdown-fill" style={{ width: `${pct}%` }} />
                  </div>
                </button>
                {isExpanded && (
                  <TransactionDetail
                    currency={currency}
                    loading={expandedLoading}
                    error={expandedError}
                    rows={expandedRows}
                  />
                )}
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function Insights() {
  const [preset,  setPreset]  = useState('this');
  // ac: insights-custom-range-and-multi-wallet — day-precise custom range state
  // ac: insights-pay-cycle-periods — Custom starts from the cycle in progress, not from today
  const [dateFrom, setDateFrom] = useState(() => payCycle(0).from);
  const [dateTo,   setDateTo]   = useState(() => payCycle(0).to);
  // ac: insights-custom-range-and-multi-wallet — empty selection means All wallets
  const [selectedWallets, setSelectedWallets] = useState([]);
  const [thb,     setThb]     = useState(null);
  const [idr,     setIdr]     = useState(null);
  const [loading, setLoading] = useState(false);
  const [error,   setError]   = useState(null);
  // ac: insights-show-income-and-transfers — Transfer In/Out totals per currency, separate from income/expenses
  const [thbTransfers, setThbTransfers] = useState(EMPTY_TRANSFERS);
  const [idrTransfers, setIdrTransfers] = useState(EMPTY_TRANSFERS);
  // ac: expand-insights-category-to-transactions — { currency, key } of the one expanded category, or null
  const [expanded,        setExpanded]        = useState(null);
  const [expandedRows,    setExpandedRows]    = useState([]);
  const [expandedLoading, setExpandedLoading] = useState(false);
  const [expandedError,   setExpandedError]   = useState(null);

  // ac: insights-pay-cycle-periods — the preset resolves to one day-precise range that drives every fetch
  const { from, to } = presetRange(preset, dateFrom, dateTo);

  // ac: insights-custom-range-and-multi-wallet — toggling a wallet adds/removes it from the selection
  function toggleWallet(id) {
    setSelectedWallets(prev => prev.includes(id) ? prev.filter(w => w !== id) : [...prev, id]);
  }

  // ac: expand-insights-category-to-transactions — clicking an expanded category again collapses it;
  // only one category's transaction list is expanded at a time
  function toggleCategory(currency, key) {
    setExpanded(prev => (prev && prev.currency === currency && prev.key === key) ? null : { currency, key });
  }

  useEffect(() => {
    if (!from || !to) return;
    let cancelled = false;
    setLoading(true);
    setError(null);

    // ac: insights-custom-range-and-multi-wallet — selecting no wallets behaves the same as All wallets
    const walletsToFetch = selectedWallets.length > 0 ? selectedWallets : WALLETS.map(w => w.id);
    // ac: insights-custom-range-and-multi-wallet — combine totals across the selected wallets, split by currency
    const requests = buildWalletRequests(walletsToFetch, from, to);

    Promise.all(requests.map(r => r.promise))
      .then(results => {
        if (cancelled) return;
        const rows    = results.flat();
        const thbRows = rows.filter(r => walletCurrency(r.wallet) === 'THB');
        const idrRows = rows.filter(r => walletCurrency(r.wallet) === 'IDR');
        setThb(aggregateTransactions(thbRows));
        setIdr(aggregateTransactions(idrRows));
      })
      .catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [from, to, selectedWallets.join(',')]);

  // ac: insights-show-income-and-transfers — Transfer In/Out totals, fetched separately from the
  // income/expense summary because a transfer counts per wallet side, not per category.
  useEffect(() => {
    if (!from || !to) return;
    let cancelled = false;
    const walletsToFetch = selectedWallets.length > 0 ? selectedWallets : WALLETS.map(w => w.id);
    const requests = buildWalletRequests(walletsToFetch, from, to, { txType: 'transfer' });

    Promise.all(requests.map(r => r.promise))
      .then(resultsList => {
        if (cancelled) return;
        const thbT = { in: 0, out: 0 };
        const idrT = { in: 0, out: 0 };
        resultsList.forEach((rows, i) => {
          const w = requests[i].wallet;
          const bucket = walletCurrency(w) === 'THB' ? thbT : idrT;
          rows.forEach(r => {
            const amt = Number(r.amount) || 0;
            if (r.wallet === w) bucket.out += amt;
            else if (r.to_wallet === w) bucket.in += amt;
          });
        });
        setThbTransfers(thbT);
        setIdrTransfers(idrT);
      })
      .catch(() => { /* transfer totals are supplementary; leave prior values on failure */ });
    return () => { cancelled = true; };
  }, [from, to, selectedWallets.join(',')]);

  // ac: expand-insights-category-to-transactions — fetches the transactions behind the expanded category,
  // scoped to the same period/date-range/wallet filters as the summary above; refetches if those change.
  useEffect(() => {
    if (!expanded) { setExpandedRows([]); return; }
    if (!from || !to) return;
    let cancelled = false;
    setExpandedLoading(true);
    setExpandedError(null);

    // ac: insights-show-income-and-transfers — Transfer In/Out expand the same way a category does
    const isTransferKey = expanded.key === TRANSFER_IN_KEY || expanded.key === TRANSFER_OUT_KEY;
    const walletsForCurrency = (selectedWallets.length > 0 ? selectedWallets : WALLETS.map(w => w.id))
      .filter(w => walletCurrency(w) === expanded.currency);
    const extraParams = isTransferKey ? { txType: 'transfer' } : { category: CATEGORY_LABELS[expanded.key] };
    const requests = buildWalletRequests(walletsForCurrency, from, to, extraParams);

    Promise.all(requests.map(r => r.promise))
      .then(resultsList => {
        if (cancelled) return;
        const rows = [];
        resultsList.forEach((batch, i) => {
          const w = requests[i].wallet;
          if (isTransferKey) {
            const wanted = expanded.key === TRANSFER_IN_KEY
              ? batch.filter(r => r.to_wallet === w)
              : batch.filter(r => r.wallet === w);
            rows.push(...wanted);
          } else {
            rows.push(...batch.filter(r => r.tx_type === 'expense'));
          }
        });
        rows.sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0));
        setExpandedRows(rows);
      })
      .catch(e => { if (!cancelled) setExpandedError(e.message); })
      .finally(() => { if (!cancelled) setExpandedLoading(false); });
    return () => { cancelled = true; };
  }, [expanded, from, to, selectedWallets.join(',')]);

  // ac: insights-show-income-and-transfers — No Data still shows when there's no income, expense, or transfer activity
  const noData = thb && idr &&
    thb.income === 0 && thb.expenses === 0 &&
    idr.income === 0 && idr.expenses === 0 &&
    thbTransfers.in === 0 && thbTransfers.out === 0 &&
    idrTransfers.in === 0 && idrTransfers.out === 0;

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Insights</h1>
      </div>

      <div className="insight-filter-bar">
        <div className="filter-tabs">
          {PRESETS.map(p => (
            <button
              key={p.id}
              className={`filter-tab${preset === p.id ? ' active' : ''}`}
              onClick={() => setPreset(p.id)}
            >
              {p.label}
            </button>
          ))}
        </div>

        {/* ac: insights-pay-cycle-periods — the resolved range is shown so a preset is never ambiguous */}
        <div className="insight-range-label">{formatDate(from)} – {formatDate(to)}</div>

        {/* ac: insights-custom-range-and-multi-wallet — the wallet filter allows selecting more than one wallet at once */}
        <div className="filter-tabs" role="group" aria-label="Wallets">
          <button
            className={`filter-tab${selectedWallets.length === 0 ? ' active' : ''}`}
            onClick={() => setSelectedWallets([])}
          >
            All wallets
          </button>
          {WALLETS.map(w => (
            <button
              key={w.id}
              className={`filter-tab${selectedWallets.includes(w.id) ? ' active' : ''}`}
              onClick={() => toggleWallet(w.id)}
              aria-pressed={selectedWallets.includes(w.id)}
            >
              {w.name}
            </button>
          ))}
        </div>

        {preset === 'custom' && (
          <div className="insight-custom-range">
            <div className="insight-custom-field">
              <label className="form-label" htmlFor="ins-from">From</label>
              <input
                id="ins-from"
                className="filter-input"
                type="date"
                value={dateFrom}
                max={dateTo}
                onChange={e => setDateFrom(e.target.value)}
              />
            </div>
            <div className="insight-custom-field">
              <label className="form-label" htmlFor="ins-to">To</label>
              <input
                id="ins-to"
                className="filter-input"
                type="date"
                value={dateTo}
                min={dateFrom}
                onChange={e => setDateTo(e.target.value)}
              />
            </div>
          </div>
        )}
      </div>

      {loading && (
        <div className="table-state">
          <div className="table-state-title">Loading…</div>
        </div>
      )}
      {error && (
        <div className="table-state">
          <div className="table-state-desc error">{error}</div>
        </div>
      )}
      {noData && (
        <div className="table-state">
          <div className="table-state-title">No data</div>
          <div className="table-state-desc">No transactions found for this period.</div>
        </div>
      )}
      {!loading && !error && thb && !noData && (
        <>
          <CurrencySection
            currency="THB"
            summary={thb}
            transfers={thbTransfers}
            expandedKey={expanded?.currency === 'THB' ? expanded.key : null}
            onToggleCategory={key => toggleCategory('THB', key)}
            expandedRows={expandedRows}
            expandedLoading={expandedLoading}
            expandedError={expandedError}
          />
          <CurrencySection
            currency="IDR"
            summary={idr}
            transfers={idrTransfers}
            expandedKey={expanded?.currency === 'IDR' ? expanded.key : null}
            onToggleCategory={key => toggleCategory('IDR', key)}
            expandedRows={expandedRows}
            expandedLoading={expandedLoading}
            expandedError={expandedError}
          />
        </>
      )}
    </div>
  );
}
