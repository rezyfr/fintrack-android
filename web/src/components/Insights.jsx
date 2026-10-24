import { useState, useEffect, useMemo } from 'react';
import { getTransactions } from '../api/client';
import { WALLETS } from '../constants/transaction';
import { getCurrencySettings, currenciesInUse, formatMoney, formatMoneyCompact } from '../constants/currency';
import { payCycle, calendarWeeks } from '../utils/payCycle';
import { useBook, bookCurrency, BOOK } from '../utils/book';
import { buildInsights, isEmptyInsights } from '../utils/insightsSummary';
import { loadRateTable } from '../utils/rates';

// ac: web-insights-parity — the web Insights page shows what Android's Insights shows, in the same
// order: a pay-cycle stepper, wallet chips for the book, money in/out/net, a daily spending heatmap,
// the category breakdown with the change against the previous cycle and its subcategories, the
// largest expenses, the spend per wallet, and the last six cycles.

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
const MONTHS_FULL = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];
const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
const HEAT_ALPHA = [0.18, 0.35, 0.55, 0.78, 1];

// The same colour for a category everywhere on the page. Unknown categories cycle through the rest.
const CATEGORY_COLORS = {
  'Food & Drink': 'var(--red)', Groceries: 'var(--green)', Transport: 'var(--teal)', Bills: 'var(--blue)',
  Shopping: 'var(--orange)', Entertainment: 'var(--purple)', Family: 'var(--gold)', Other: 'var(--text-3)',
  Subscriptions: 'var(--pink)', 'Health & Wellbeing': 'var(--blue)', Travel: 'var(--purple)', Gifts: 'var(--pink)',
};
const FALLBACK_COLORS = ['var(--teal)', 'var(--orange)', 'var(--purple)', 'var(--pink)', 'var(--blue)', 'var(--green)'];
function categoryColor(category) {
  if (CATEGORY_COLORS[category]) return CATEGORY_COLORS[category];
  let h = 0;
  for (const ch of category) h = (h * 31 + ch.charCodeAt(0)) >>> 0;
  return FALLBACK_COLORS[h % FALLBACK_COLORS.length];
}

function parseIso(iso) {
  const [y, m, d] = String(iso).slice(0, 10).split('-').map(Number);
  return new Date(y, m - 1, d);
}
function isoOf(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}
const dayLabel = iso => { const d = parseIso(iso); return `${WEEKDAYS[d.getDay()]}, ${d.getDate()} ${MONTHS[d.getMonth()]}`; };
const shortDay = iso => { const d = parseIso(iso); return `${d.getDate()} ${MONTHS[d.getMonth()]}`; };
const monthOf = iso => MONTHS_FULL[parseIso(iso).getMonth()];
const monthShort = iso => MONTHS[parseIso(iso).getMonth()];
const money = (code, v) => (v < 0 ? '-' : '') + formatMoney(code, Math.abs(v));

export default function Insights({ onOpenTransactions = () => {} }) {
  const book = useBook();
  const { homeCode } = getCurrencySettings();
  const codesKey = currenciesInUse().join(',');
  const code = bookCurrency(book);
  // ac: web-insights-parity — 0 is the cycle in progress, 1 the one before it
  const [offset, setOffset] = useState(0);
  // A wallet chip belongs to one book, so a pick made in another book counts as All wallets.
  const [walletPick, setWalletPick] = useState({ book, id: '' });
  const wallet = walletPick.book === book ? walletPick.id : '';
  const setWallet = id => setWalletPick({ book, id });
  // The latest fetch, tagged with its cycle; a result for another cycle means a fetch is running.
  const [result, setResult] = useState(null);

  const { cycleStartDay } = getCurrencySettings();
  // payCycle reads the start day from the settings, so the start day is a dependency
  const cycles = useMemo(() => [5, 4, 3, 2, 1, 0].map(i => payCycle(offset + i)), [offset, cycleStartDay]);
  const cycle = cycles[cycles.length - 1];
  // payCycle reads the start day from the settings, so the start day is a dependency
  const prev = useMemo(() => payCycle(offset + 1), [offset, cycleStartDay]);

  useEffect(() => {
    let cancelled = false;
    // Each range is read on its own, so every page of a busy cycle is included.
    const ranges = [...cycles, prev];
    Promise.all([
      Promise.all(ranges.map(r => getTransactions({ dateFrom: r.from, dateTo: r.to }))),
      loadRateTable().catch(() => null),
    ])
      .then(([lists, loaded]) => {
        if (cancelled) return;
        // ac: exchange-rate-from-transfers - per currency, the median of recent transfers, else the fallback
        const rates = loaded?.table ?? { [homeCode]: 1 };
        setResult({ offset, data: { cycleRows: lists.slice(0, cycles.length), prevRows: lists[cycles.length] ?? [], rates } });
      })
      .catch(e => { if (!cancelled) setResult({ offset, error: e.message }); });
    return () => { cancelled = true; };
  }, [offset, cycles, prev, homeCode, codesKey]);

  const loading = result?.offset !== offset;
  const error = loading ? null : result.error ?? null;
  const data = loading ? null : result.data ?? null;

  const summaries = useMemo(() => data && buildInsights({
    rows: data.cycleRows[data.cycleRows.length - 1] ?? [],
    prevRows: data.prevRows,
    cycleRows: data.cycleRows,
    cycles,
    codes: code == null ? [] : [code],
    wallet: wallet || null,
    rates: data.rates,
    homeCode,
  }), [data, cycles, code, wallet, homeCode]);
  const summary = summaries && (code == null ? summaries.combined : summaries.byCurrency[code]);

  const bookWallets = WALLETS.filter(w => code != null && w.currency === code);

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Insights</h1>
      </div>

      {/* ac: web-insights-parity — one pay cycle at a time; the next arrow stops at the cycle in progress */}
      <div className="ins-stepper">
        <button className="filter-tab" onClick={() => setOffset(o => o + 1)} aria-label="Previous cycle">←</button>
        <div className="ins-stepper-label">
          <div className="ins-stepper-month">{monthOf(cycle.to)} {parseIso(cycle.to).getFullYear()}</div>
          <div className="ins-stepper-range">{shortDay(cycle.from)} – {shortDay(cycle.to)}</div>
        </div>
        <button className="filter-tab" onClick={() => setOffset(o => Math.max(0, o - 1))} disabled={offset === 0} aria-label="Next cycle">→</button>
      </div>

      {/* ac: insights-wallet-flow — the wallets of the book; Combined covers every account, so no chips */}
      {book !== BOOK.COMBINED && (
        <div className="filter-tabs ins-wallets" role="group" aria-label="Wallets">
          <button className={`filter-tab${wallet === '' ? ' active' : ''}`} onClick={() => setWallet('')}>All wallets</button>
          {bookWallets.map(w => (
            <button key={w.id} className={`filter-tab${wallet === w.id ? ' active' : ''}`} aria-pressed={wallet === w.id} onClick={() => setWallet(w.id)}>
              {w.name}
            </button>
          ))}
        </div>
      )}

      {loading && <div className="table-state"><div className="table-state-title">Loading…</div></div>}
      {error && <div className="table-state"><div className="table-state-desc error">{error}</div></div>}
      {!loading && !error && summary && isEmptyInsights(summary) && (
        <div className="table-state">
          <div className="table-state-title">No data</div>
          <div className="table-state-desc">No transactions in this cycle.</div>
        </div>
      )}
      {!loading && !error && summary && !isEmptyInsights(summary) && (
        <div className="ins-stack">
          <BalanceCard summary={summary} />
          <DailyHeatmap key={cycle.from} summary={summary} cycle={cycle} />
          <Breakdown summary={summary} cycle={cycle} prev={prev} />
          <LargestExpenses summary={summary} onOpenTransactions={onOpenTransactions} />
          <PaidWith summary={summary} />
          <Trend key={cycle.to} summary={summary} />
        </div>
      )}
    </div>
  );
}

function BalanceCard({ summary }) {
  const c = summary.currency;
  return (
    <div className="stats-row">
      <div className="stat-card"><div className="stat-label">Money in</div><div className="stat-value income">{money(c, summary.moneyIn)}</div></div>
      <div className="stat-card"><div className="stat-label">Money out</div><div className="stat-value expense">{money(c, summary.moneyOut)}</div></div>
      <div className="stat-card">
        <div className="stat-label">Net</div>
        <div className={`stat-value ${summary.net >= 0 ? 'income' : 'expense'}`}>{summary.net >= 0 ? '+' : '-'}{formatMoney(c, Math.abs(summary.net))}</div>
      </div>
    </div>
  );
}

// ac: insights-daily-heatmap — one cell per day of the cycle, shaded by spend in five levels
function DailyHeatmap({ summary, cycle }) {
  const todayIso = isoOf(new Date());
  const lastDay = todayIso < cycle.to ? todayIso : cycle.to;
  const [selected, setSelected] = useState(lastDay < cycle.from ? cycle.from : lastDay);
  const weeks = calendarWeeks(cycle.from, cycle.to);
  const values = Object.entries(summary.dailySpend).filter(([d]) => d >= cycle.from && d <= cycle.to);
  const max = Math.max(0, ...values.map(([, v]) => v));
  const elapsed = Math.max(1, Math.round((parseIso(lastDay) - parseIso(cycle.from)) / 86400000) + 1);
  const average = values.reduce((t, [, v]) => t + v, 0) / elapsed;
  const level = v => (!v || max <= 0 ? 0 : Math.min(5, Math.max(1, Math.ceil((v / max) * 5))));
  const c = summary.currency;
  const amount = summary.dailySpend[selected];
  return (
    <div className="ins-card">
      <div className="ins-card-title">Daily spending</div>
      <div className="ins-heat-selected">
        <strong>{dayLabel(selected)}</strong> · {amount > 0 ? formatMoney(c, amount) : 'No spending'}
      </div>
      <div className="ins-heat-grid">
        {WEEKDAYS.map(w => <div key={w} className="ins-heat-weekday">{w}</div>)}
        {weeks.flat().map(cell => {
          if (!cell.inCycle) return <div key={cell.iso} />;
          const future = cell.iso > todayIso;
          const v = summary.dailySpend[cell.iso];
          const lvl = level(v);
          const style = !future && lvl > 0 ? { background: `color-mix(in srgb, var(--gold) ${HEAT_ALPHA[lvl - 1] * 100}%, transparent)` } : undefined;
          return (
            <button
              key={cell.iso}
              className={`ins-heat-cell${future ? ' future' : ''}${lvl === 0 && !future ? ' empty' : ''}${lvl >= 4 ? ' strong' : ''}${cell.iso === selected ? ' selected' : ''}`}
              style={style}
              disabled={future}
              onClick={() => setSelected(cell.iso)}
              aria-label={`${dayLabel(cell.iso)}: ${v ? formatMoney(c, v) : 'no spending'}`}
            >
              {parseIso(cell.iso).getDate()}
            </button>
          );
        })}
      </div>
      <div className="ins-heat-legend">
        <span>less</span>
        {HEAT_ALPHA.map(a => <span key={a} className="ins-heat-swatch" style={{ background: `color-mix(in srgb, var(--gold) ${a * 100}%, transparent)` }} />)}
        <span>more</span>
        <span className="ins-heat-avg">avg {formatMoney(c, average)}/day</span>
      </div>
    </div>
  );
}

// ac: insights-compact-breakdown — donut plus one line per category, with the change against the
// previous cycle; a category with subcategories expands into them
function Breakdown({ summary, cycle, prev }) {
  const [open, setOpen] = useState(null);
  const c = summary.currency;
  const cats = summary.categories;
  if (cats.length === 0) return null;
  const R = 70, STROKE = 26, C = 2 * Math.PI * R;
  const toggle = cat => setOpen(o => (o === cat ? null : cat));
  const openCat = cats.find(x => x.category === open);
  const starts = cats.reduce((list, x, i) => [...list, i === 0 ? 0 : list[i - 1] + cats[i - 1].share * C], []);
  return (
    <div className="ins-card">
      <div className="ins-card-head">
        <span className="ins-card-title">Spending breakdown</span>
        <span className="ins-card-total">{formatMoney(c, summary.categorySpend)}</span>
      </div>
      <div className="ins-donut-wrap">
        <svg viewBox="0 0 200 200" className="ins-donut" role="img" aria-label="Spending by category">
          <circle cx="100" cy="100" r={R} fill="none" stroke="var(--border)" strokeWidth={STROKE} />
          {cats.map((x, i) => (
            <circle
              key={x.category}
              cx="100" cy="100" r={R} fill="none"
              stroke={categoryColor(x.category)}
              strokeWidth={open === x.category ? STROKE + 6 : STROKE}
              strokeDasharray={`${Math.max(x.share * C - 1.5, 0)} ${C}`}
              strokeDashoffset={-starts[i]}
              transform="rotate(-90 100 100)"
              className="ins-donut-seg"
              onClick={() => toggle(x.category)}
            />
          ))}
          <text x="100" y="94" textAnchor="middle" className="ins-donut-label">{openCat ? openCat.category : 'Total'}</text>
          <text x="100" y="116" textAnchor="middle" className="ins-donut-value">{formatMoneyCompact(c, openCat ? openCat.amount : summary.categorySpend)}</text>
        </svg>
      </div>
      <div className="ins-hint">Tap the chart or a row to see sub-categories</div>
      <div className="ins-list-head">
        <span>{monthOf(cycle.to)} breakdown</span>
        <span>vs {monthOf(prev.to)}</span>
      </div>
      {cats.map(x => {
        const delta = x.amount - x.previous;
        const expanded = open === x.category;
        return (
          <div key={x.category} className="ins-cat">
            <button className="ins-cat-row" onClick={() => toggle(x.category)} aria-expanded={expanded}>
              <span className="ins-swatch" style={{ background: categoryColor(x.category) }} />
              <span className={`ins-cat-name${expanded ? ' open' : ''}`}>{x.category}</span>
              <span className="ins-cat-amount">{formatMoney(c, x.amount)}</span>
              <span
                className={`ins-delta ${delta > 0 ? 'up' : delta < 0 ? 'down' : ''}`}
                title={delta === 0 ? undefined : `${delta > 0 ? 'Up' : 'Down'} ${formatMoney(c, Math.abs(delta))} vs previous cycle`}
              >
                {delta === 0 ? '' : `${delta > 0 ? '▲' : '▼'} ${formatMoneyCompact(c, Math.abs(delta))}`}
              </span>
              <span className={`ins-chevron${expanded ? ' open' : ''}`}>{x.subcategories.length > 0 ? '›' : ''}</span>
            </button>
            {expanded && x.subcategories.map(s => (
              <div key={s.name} className="ins-sub-row">
                <span>{s.name}</span>
                <span>{formatMoney(c, s.amount)} · {Math.round(s.share * 100)}%</span>
              </div>
            ))}
          </div>
        );
      })}
    </div>
  );
}

// ac: insights-largest-expenses — the cycle's five largest expenses, with a link to Transactions
function LargestExpenses({ summary, onOpenTransactions }) {
  if (summary.topExpenses.length === 0) return null;
  const c = summary.currency;
  return (
    <div className="ins-card">
      <div className="ins-card-title">Largest expenses</div>
      {summary.topExpenses.map(tx => (
        <div key={tx.id} className="ins-tx-row">
          <span className="ins-swatch round" style={{ background: categoryColor(tx.category || 'Other') }} />
          <div className="ins-tx-main">
            <div className="ins-tx-item">{tx.item}</div>
            <div className="ins-tx-meta">{tx.category} · {dayLabel(tx.date)}</div>
          </div>
          <span className="ins-tx-amount">-{formatMoney(c, Number(tx.amount))}</span>
        </div>
      ))}
      <button className="ins-link" onClick={onOpenTransactions}>All transactions →</button>
    </div>
  );
}

// ac: insights-paid-with — the expense total and share per wallet
function PaidWith({ summary }) {
  if (summary.walletBreakdown.length === 0) return null;
  const c = summary.currency;
  return (
    <div className="ins-card">
      <div className="ins-card-title">Paid with</div>
      {summary.walletBreakdown.map(w => (
        <div key={w.wallet} className="ins-paid-row">
          <div className="ins-paid-head">
            <span>{WALLETS.find(x => x.id === w.wallet)?.name ?? w.wallet}</span>
            <span className="ins-paid-amount">{formatMoney(c, w.amount)}</span>
            <span className="ins-paid-share">{Math.round(w.share * 100)}%</span>
          </div>
          <div className="ins-bar"><div className="ins-bar-fill" style={{ width: `${w.share * 100}%` }} /></div>
        </div>
      ))}
    </div>
  );
}

// ac: insights-six-month-trend — money in and money out for the last six cycles; click a month
function Trend({ summary }) {
  const points = summary.trend;
  const [sel, setSel] = useState(points.length - 1);
  if (points.length < 2 || points.every(p => p.moneyIn === 0 && p.moneyOut === 0)) return null;
  const c = summary.currency;
  const W = 600, H = 190, padX = 28, top = 14, bottom = 28;
  const max = Math.max(1, ...points.map(p => Math.max(p.moneyIn, p.moneyOut)));
  const x = i => padX + ((W - padX * 2) * i) / (points.length - 1);
  const y = v => top + (H - top - bottom) * (1 - v / max);
  const path = key => points.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i)},${y(p[key])}`).join(' ');
  const p = points[sel] ?? points[points.length - 1];
  return (
    <div className="ins-card">
      <div className="ins-card-title">Last 6 months</div>
      <div className="ins-legend">
        <span><i style={{ background: 'var(--green)' }} />Money in</span>
        <span><i style={{ background: 'var(--red)' }} />Money out</span>
      </div>
      <div className="ins-trend-selected">
        <strong>{monthShort(p.to)}{p.inProgress ? ' (in progress)' : ''}</strong> · in {formatMoney(c, p.moneyIn)} · out {formatMoney(c, p.moneyOut)}
      </div>
      <svg viewBox={`0 0 ${W} ${H}`} className="ins-trend" role="img" aria-label="Money in and out over the last six cycles">
        <line x1={padX} x2={W - padX} y1={H - bottom} y2={H - bottom} stroke="var(--border)" />
        <line x1={x(sel)} x2={x(sel)} y1={top} y2={H - bottom} stroke="var(--border)" />
        <path d={path('moneyIn')} fill="none" stroke="var(--green)" strokeWidth="2.5" strokeLinecap="round" />
        <path d={path('moneyOut')} fill="none" stroke="var(--red)" strokeWidth="2.5" strokeLinecap="round" />
        {points.map((pt, i) => (
          <g key={pt.to} onClick={() => setSel(i)} className="ins-trend-point">
            <rect x={x(i) - 24} y={top} width="48" height={H - top} fill="transparent" />
            <circle cx={x(i)} cy={y(pt.moneyIn)} r={i === sel ? 6 : 4.5} fill={i === sel ? 'var(--green)' : 'var(--card)'} stroke="var(--green)" strokeWidth="2" />
            <circle cx={x(i)} cy={y(pt.moneyOut)} r={i === sel ? 6 : 4.5} fill={i === sel ? 'var(--red)' : 'var(--card)'} stroke="var(--red)" strokeWidth="2" />
            <text x={x(i)} y={H - 8} textAnchor="middle" className={`ins-trend-month${i === sel ? ' sel' : ''}`}>{monthShort(pt.to)}</text>
          </g>
        ))}
      </svg>
    </div>
  );
}
