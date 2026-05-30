import { useState, useEffect } from 'react';
import { getMonthlyOverview } from '../api/supabase';

const PRESETS = [
  { id: 'this',  label: 'This Month'    },
  { id: 'last',  label: 'Last Month'    },
  { id: 'last3', label: 'Last 3 Months' },
  { id: 'custom', label: 'Custom'       },
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

const CATEGORY_CLASS = {
  food_drink: 'chip-food', transport: 'chip-transport', bills: 'chip-bills',
  subscriptions: 'chip-subscriptions', entertainment: 'chip-entertainment',
  groceries: 'chip-groceries', health_wellbeing: 'chip-health',
  family: 'chip-other', shopping: 'chip-shopping', travel: 'chip-travel',
  business: 'chip-business', gifts: 'chip-gifts', other: 'chip-other',
};

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function monthLabel(ym) {
  const [y, m] = ym.split('-').map(Number);
  const names = ['January','February','March','April','May','June',
                 'July','August','September','October','November','December'];
  return `${names[m - 1]} ${y}`;
}

function monthsBetween(fromYM, toYM) {
  const [fy, fm] = fromYM.split('-').map(Number);
  const [ty, tm] = toYM.split('-').map(Number);
  const result = [];
  let y = fy, m = fm;
  while (y < ty || (y === ty && m <= tm)) {
    result.push(monthLabel(`${y}-${String(m).padStart(2, '0')}`));
    m++; if (m > 12) { m = 1; y++; }
  }
  return result.length ? result : [monthLabel(currentMonth())];
}

function monthsForPreset(preset) {
  const now = new Date();
  const fmt = (d) => monthLabel(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
  if (preset === 'this') return [fmt(now)];
  if (preset === 'last') return [fmt(new Date(now.getFullYear(), now.getMonth() - 1, 1))];
  if (preset === 'last3') return [1, 2, 3].map(i => fmt(new Date(now.getFullYear(), now.getMonth() - i, 1)));
  return [];
}

function aggregate(rows) {
  const sum = (k) => rows.reduce((s, r) => s + (Number(r[k]) || 0), 0);
  const income   = sum('income');
  const expenses = sum('total_expenditure');
  const cats = CATEGORIES
    .map(k => ({ key: k, label: CATEGORY_LABELS[k], amount: sum(k) }))
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

function CurrencySection({ currency, summary }) {
  if (summary.income === 0 && summary.expenses === 0) return null;
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
      {summary.cats.length > 0 && (
        <div className="breakdown-card">
          <div className="breakdown-header">
            <span>Spending Breakdown</span>
            <span>{fmt(summary.expenses, currency)}</span>
          </div>
          {summary.cats.map(cat => {
            const pct = summary.expenses > 0 ? (cat.amount / summary.expenses) * 100 : 0;
            return (
              <div key={cat.key} className="breakdown-row">
                <div className="breakdown-meta">
                  <span className={`chip ${CATEGORY_CLASS[cat.key]}`}>{cat.label}</span>
                  <span className="breakdown-amount">
                    {fmt(cat.amount, currency)}
                    <span className="breakdown-pct"> · {Math.round(pct)}%</span>
                  </span>
                </div>
                <div className="breakdown-track">
                  <div className="breakdown-fill" style={{ width: `${pct}%` }} />
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

export default function Insights() {
  const cm = currentMonth();
  const [preset, setPreset]   = useState('this');
  const [fromYM, setFromYM]   = useState(cm);
  const [toYM, setToYM]       = useState(cm);
  const [thb, setThb]         = useState(null);
  const [idr, setIdr]         = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);

  function resolvedMonths() {
    return preset === 'custom' ? monthsBetween(fromYM, toYM) : monthsForPreset(preset);
  }

  useEffect(() => {
    if (preset === 'custom' && !fromYM) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    getMonthlyOverview(resolvedMonths())
      .then(rows => {
        if (cancelled) return;
        setThb(aggregate(rows.filter(r => r.currency === 'THB')));
        setIdr(aggregate(rows.filter(r => r.currency === 'IDR')));
      })
      .catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [preset, preset === 'custom' ? fromYM : null, preset === 'custom' ? toYM : null]);

  const noData = thb && idr && thb.income === 0 && thb.expenses === 0 && idr.income === 0 && idr.expenses === 0;

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Insights</h1>
      </div>

      {/* Period selector */}
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

        {preset === 'custom' && (
          <div className="insight-custom-range">
            <div className="insight-custom-field">
              <label className="form-label" htmlFor="ins-from">From</label>
              <input
                id="ins-from"
                className="month-input"
                type="month"
                value={fromYM}
                max={toYM}
                onChange={e => setFromYM(e.target.value)}
              />
            </div>
            <div className="insight-custom-field">
              <label className="form-label" htmlFor="ins-to">To</label>
              <input
                id="ins-to"
                className="month-input"
                type="month"
                value={toYM}
                min={fromYM}
                onChange={e => setToYM(e.target.value)}
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
          <CurrencySection currency="THB" summary={thb} />
          <CurrencySection currency="IDR" summary={idr} />
        </>
      )}
    </div>
  );
}
