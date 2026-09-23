import { useState, useEffect, useMemo } from 'react';
import { getTransactions, getBudgetLines, updateBudgetLineTarget, getRecentFxRate } from '../api/supabase';
import { payCycle, cycleDays } from '../utils/payCycle';

// Used only when the user has no cross-currency transfer on record to derive a rate from.
const FALLBACK_IDR_PER_THB = 535;

const SERIES_COLORS = [
  'var(--gold)', 'var(--blue)', 'var(--green)', 'var(--orange)', 'var(--purple)',
  'var(--pink)', 'var(--teal)', 'var(--red)',
];

const CHART = { w: 720, h: 168, padL: 56, padR: 12, padT: 14, padB: 22 };

function fmt(amount, currency) {
  const isIDR = currency === 'IDR';
  return `${isIDR ? 'Rp ' : '฿'}${Math.round(Math.abs(amount)).toLocaleString('en')}`;
}

function compact(amount, currency) {
  const abs = Math.abs(amount);
  const sym = currency === 'IDR' ? 'Rp' : '฿';
  if (abs >= 1_000_000) return `${sym}${(amount / 1_000_000).toFixed(1)}M`;
  if (abs >= 1_000) return `${sym}${Math.round(amount / 1_000)}k`;
  return `${sym}${Math.round(amount)}`;
}

function dayLabel(iso) {
  const [, m, d] = iso.split('-');
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  return `${Number(d)} ${months[Number(m) - 1]}`;
}

// A due day is a day-of-month; the cycle starts on the 26th, so day 26 is index 0 and a day
// earlier in the month lands in the back half of the cycle.
function dueIndex(dueDay, days) {
  if (dueDay == null) return null;
  const i = days.findIndex(d => Number(d.split('-')[2]) === dueDay);
  return i === -1 ? null : i;
}

function compileMatcher(line) {
  let re = null;
  if (line.match_pattern) {
    try {
      re = new RegExp(line.match_pattern, 'i');
    } catch {
      re = null; // A malformed stored pattern must not take the whole chart down.
    }
  }
  const wallets = line.match_wallets || null;
  const cats = line.match_categories || null;
  return (row) => {
    if (wallets && !wallets.includes(row.wallet)) return false;
    if (re && re.test(row.item || '')) return true;
    if (cats && cats.includes(row.category)) return true;
    return false;
  };
}

// A budget that silently drops the spend it has no rule for reads as if the money was never
// spent. Everything unmatched lands on one line per currency instead.
export const UNBUDGETED_ID = { THB: '__unbudgeted_THB__', IDR: '__unbudgeted_IDR__' };

function unbudgetedLine(currency) {
  return {
    id: UNBUDGETED_ID[currency],
    name: 'Unbudgeted',
    kind: 'flex',
    currency,
    target: 0,
    due_day: null,
    rec_cycles: 0,
    synthetic: true,
  };
}

// ac: budget-cycle-target-vs-actual — one transaction counts toward at most one line; lines are
// tested in sort_order so the first match wins and nothing is double-counted across the chart.
function attributeRows(rows, lines, idrPerThb) {
  const matchers = lines.map(compileMatcher);
  const byLine = new Map(lines.map(l => [l.id, new Map()]));
  byLine.set(UNBUDGETED_ID.THB, new Map());
  byLine.set(UNBUDGETED_ID.IDR, new Map());
  for (const row of rows) {
    if (row.tx_type !== 'expense' || row.to_wallet) continue;
    const raw = Number(row.amount) || 0;
    const rowIsThb = row.wallet === 'BBL';
    // ac: assign-transaction-budget-line — a manual override wins over the automatic rules; it
    // only applies when it still names an active line, so a deactivated line falls back to auto
    const overrideHit = row.budget_line_id != null
      ? lines.findIndex(l => l.id === row.budget_line_id)
      : -1;
    const hit = overrideHit !== -1 ? overrideHit : lines.findIndex((_, i) => matchers[i](row));
    let key, amount;
    if (hit === -1) {
      // Unmatched spend stays in the currency it was actually spent in.
      key = rowIsThb ? UNBUDGETED_ID.THB : UNBUDGETED_ID.IDR;
      amount = raw;
    } else {
      const line = lines[hit];
      key = line.id;
      amount = raw;
      if (line.currency === 'THB' && !rowIsThb) amount = raw / idrPerThb;
      if (line.currency === 'IDR' && rowIsThb) amount = raw * idrPerThb;
    }
    const perDay = byLine.get(key);
    perDay.set(row.date, (perDay.get(row.date) || 0) + amount);
  }
  return byLine;
}

// ac: budget-cycle-target-vs-actual — a fixed line's target steps up on its due day; a flexible
// line's slopes evenly from zero to the full amount across the cycle.
function targetSeries(line, days) {
  const target = Number(line.target) || 0;
  if (line.kind === 'fixed') {
    const at = dueIndex(line.due_day, days);
    // A fixed line with no due day is owed from the first day of the cycle.
    const step = at == null ? 0 : at;
    return days.map((_, i) => (i >= step ? target : 0));
  }
  const last = Math.max(days.length - 1, 1);
  return days.map((_, i) => (target * i) / last);
}

// ac: budget-cycle-target-vs-actual — the actual series is cumulative and stops at today rather
// than flat-lining to the end of a cycle that has not happened yet.
function actualSeries(perDay, days, todayIso) {
  const out = [];
  let run = 0;
  for (const d of days) {
    if (d > todayIso) break;
    run += perDay.get(d) || 0;
    out.push(run);
  }
  return out;
}

function pathFor(values, days, maxY) {
  if (values.length === 0) return '';
  const innerW = CHART.w - CHART.padL - CHART.padR;
  const innerH = CHART.h - CHART.padT - CHART.padB;
  const lastX = Math.max(days.length - 1, 1);
  return values
    .map((v, i) => {
      const x = CHART.padL + (innerW * i) / lastX;
      const y = CHART.padT + innerH - (innerH * v) / (maxY || 1);
      return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');
}

function niceMax(v) {
  if (v <= 0) return 1;
  const mag = 10 ** Math.floor(Math.log10(v));
  return Math.ceil(v / mag) * mag;
}

function RangeHint({ line }) {
  if (line.synthetic) {
    return <span className="budget-range budget-range-empty">spend matching no budget line</span>;
  }
  // ac: budget-line-target-editing — a line nothing has ever matched says so rather than
  // presenting a 0–0 range as if it were an observation.
  if (!line.rec_cycles) {
    return <span className="budget-range budget-range-empty">no history matched yet</span>;
  }
  return (
    <span className="budget-range">
      seen {compact(Number(line.rec_min), line.currency)}–{compact(Number(line.rec_max), line.currency)}
      {' · median '}{compact(Number(line.rec_median), line.currency)}
      {line.rec_cycles < 5 && <span className="budget-range-partial"> ({line.rec_cycles}/5 cycles)</span>}
    </span>
  );
}

function LegendRow({ line, color, visible, onToggle, actual, onSaveTarget }) {
  const [editing, setEditing] = useState(false);
  const [draft, setDraft] = useState(String(line.target));
  const [saving, setSaving] = useState(false);

  const target = Number(line.target) || 0;
  const over = actual > target && target > 0;

  async function commit() {
    const next = Number(draft);
    if (!Number.isFinite(next) || next < 0) { setDraft(String(line.target)); setEditing(false); return; }
    if (next === target) { setEditing(false); return; }
    setSaving(true);
    try {
      await onSaveTarget(line.id, next);
      setEditing(false);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className={`budget-row${visible ? '' : ' budget-row-off'}`}>
      {/* ac: budget-line-visibility — the swatch toggles both series for this line */}
      <button
        className="budget-swatch-btn"
        onClick={() => onToggle(line.id)}
        aria-pressed={visible}
        aria-label={`${visible ? 'Hide' : 'Show'} ${line.name}`}
      >
        <span className="budget-swatch" style={{ background: visible ? color : 'transparent', borderColor: color }} />
      </button>
      <span className="budget-name">
        {line.name}
        <span className={`budget-kind budget-kind-${line.kind}`}>{line.kind}</span>
      </span>
      <span className="budget-actual">
        <span className={over ? 'expense' : undefined}>{fmt(actual, line.currency)}</span>
        <span className="budget-sep"> / </span>
        {/* ac: budget-line-target-editing — inline edit, persisted on blur or Enter */}
        {line.synthetic ? (
          <span className="budget-target-static">{fmt(target, line.currency)}</span>
        ) : editing ? (
          <input
            className="budget-target-input"
            type="number"
            autoFocus
            value={draft}
            disabled={saving}
            onChange={e => setDraft(e.target.value)}
            onBlur={commit}
            onKeyDown={e => {
              if (e.key === 'Enter') commit();
              if (e.key === 'Escape') { setDraft(String(line.target)); setEditing(false); }
            }}
          />
        ) : (
          <button className="budget-target-btn" onClick={() => { setDraft(String(line.target)); setEditing(true); }}>
            {fmt(target, line.currency)}
          </button>
        )}
      </span>
      <RangeHint line={line} />
    </div>
  );
}

function Chart({ currency, lines, colors, visibleIds, days, seriesByLine, todayIndex }) {
  const shown = lines.filter(l => visibleIds.has(l.id));
  const maxY = useMemo(() => {
    // ac: budget-line-visibility — the scale follows only the lines still visible
    let m = 0;
    for (const l of shown) {
      const s = seriesByLine.get(l.id);
      if (!s) continue;
      for (const v of s.target) m = Math.max(m, v);
      for (const v of s.actual) m = Math.max(m, v);
    }
    return niceMax(m);
  }, [shown, seriesByLine]);

  if (shown.length === 0) {
    return <div className="budget-chart-empty">No lines selected.</div>;
  }

  const innerW = CHART.w - CHART.padL - CHART.padR;
  const innerH = CHART.h - CHART.padT - CHART.padB;
  const ticks = [0, 0.25, 0.5, 0.75, 1];
  const todayX = todayIndex == null
    ? null
    : CHART.padL + (innerW * todayIndex) / Math.max(days.length - 1, 1);

  return (
    <div className="budget-chart-wrap">
      <svg
        className="budget-chart"
        viewBox={`0 0 ${CHART.w} ${CHART.h}`}
        role="img"
        aria-label={`Budget target against actual for ${currency}`}
      >
        {ticks.map(t => {
          const y = CHART.padT + innerH - innerH * t;
          return (
            <g key={t}>
              <line x1={CHART.padL} y1={y} x2={CHART.w - CHART.padR} y2={y} className="budget-grid" />
              <text x={CHART.padL - 6} y={y + 3} className="budget-axis" textAnchor="end">
                {compact(maxY * t, currency)}
              </text>
            </g>
          );
        })}
        {todayX != null && (
          <g>
            <line x1={todayX} y1={CHART.padT} x2={todayX} y2={CHART.padT + innerH} className="budget-today" />
            <text x={todayX} y={CHART.padT - 2} className="budget-axis" textAnchor="middle">today</text>
          </g>
        )}
        {shown.map(l => {
          const s = seriesByLine.get(l.id);
          if (!s) return null;
          const color = colors.get(l.id);
          return (
            <g key={l.id}>
              <path d={pathFor(s.target, days, maxY)} fill="none" stroke={color} className="budget-line-target" />
              <path d={pathFor(s.actual, days, maxY)} fill="none" stroke={color} className="budget-line-actual" />
            </g>
          );
        })}
        <text x={CHART.padL} y={CHART.h - 8} className="budget-axis">{dayLabel(days[0])}</text>
        <text x={CHART.w - CHART.padR} y={CHART.h - 8} className="budget-axis" textAnchor="end">
          {dayLabel(days[days.length - 1])}
        </text>
      </svg>
      <div className="budget-chart-key">
        <span><span className="budget-key-dash budget-key-target" /> target</span>
        <span><span className="budget-key-dash budget-key-actual" /> actual</span>
      </div>
    </div>
  );
}

export default function Budget() {
  const [offset, setOffset] = useState(0);
  const [lines, setLines] = useState([]);
  const [rows, setRows] = useState([]);
  const [idrPerThb, setIdrPerThb] = useState(FALLBACK_IDR_PER_THB);
  const [derivedRate, setDerivedRate] = useState(true);
  const [hidden, setHidden] = useState(() => new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // ac: budget-cycle-target-vs-actual — opens on the pay cycle in progress with no interaction
  const cycle = useMemo(() => payCycle(offset), [offset]);
  const days = useMemo(() => cycleDays(cycle.from, cycle.to), [cycle]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      getBudgetLines(),
      getTransactions({ dateFrom: cycle.from, dateTo: cycle.to, limit: 2000 }),
      getRecentFxRate(),
    ])
      .then(([ls, rs, rate]) => {
        if (cancelled) return;
        setLines(ls);
        setRows(rs);
        if (rate) { setIdrPerThb(rate); setDerivedRate(true); } else { setDerivedRate(false); }
      })
      .catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [cycle]);

  // Unbudgeted is appended per currency so it scales and toggles like any other line.
  const allLines = useMemo(
    () => [...lines, unbudgetedLine('THB'), unbudgetedLine('IDR')],
    [lines],
  );

  const colors = useMemo(() => {
    const m = new Map();
    lines.forEach((l, i) => m.set(l.id, SERIES_COLORS[i % SERIES_COLORS.length]));
    m.set(UNBUDGETED_ID.THB, 'var(--text-2)');
    m.set(UNBUDGETED_ID.IDR, 'var(--text-2)');
    return m;
  }, [lines]);

  const todayIso = useMemo(() => {
    const d = new Date();
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
  }, []);

  const seriesByLine = useMemo(() => {
    const attributed = attributeRows(rows, lines, idrPerThb);
    const m = new Map();
    for (const l of allLines) {
      m.set(l.id, {
        target: targetSeries(l, days),
        actual: actualSeries(attributed.get(l.id) || new Map(), days, todayIso),
      });
    }
    return m;
  }, [rows, lines, allLines, days, idrPerThb, todayIso]);

  const todayIndex = useMemo(() => {
    const i = days.indexOf(todayIso);
    return i === -1 ? null : i;
  }, [days, todayIso]);

  async function saveTarget(id, target) {
    await updateBudgetLineTarget(id, target);
    setLines(ls => ls.map(l => (l.id === id ? { ...l, target } : l)));
  }

  function toggle(id) {
    setHidden(h => {
      const next = new Set(h);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  }

  const visibleIds = useMemo(
    () => new Set(allLines.filter(l => !hidden.has(l.id)).map(l => l.id)),
    [allLines, hidden],
  );

  if (loading) return <div className="page"><div className="budget-state">Loading…</div></div>;
  if (error)   return <div className="page"><div className="budget-state">{error}</div></div>;

  const byCurrency = ['THB', 'IDR'].map(c => ({ currency: c, lines: allLines.filter(l => l.currency === c) }));

  return (
    <div className="page">
      <div className="budget-head">
        <div>
          <h1 className="section-title">Budget</h1>
          <div className="budget-cycle">{dayLabel(cycle.from)} – {dayLabel(cycle.to)}</div>
        </div>
        <div className="budget-nav">
          <button className="filter-tab" onClick={() => setOffset(o => o + 1)}>← Previous</button>
          {/* One cycle ahead is reachable so the upcoming cycle can be planned before it starts. */}
          <button className="filter-tab" onClick={() => setOffset(o => Math.max(-1, o - 1))} disabled={offset === -1}>
            Next →
          </button>
        </div>
      </div>

      {!derivedRate && (
        <div className="budget-note">
          No cross-currency transfer on record — converting at {FALLBACK_IDR_PER_THB} IDR/THB.
        </div>
      )}

      {byCurrency.map(({ currency, lines: group }) => {
        if (group.length === 0) return null;
        return (
          <section key={currency} className="insight-section">
            <div className="insight-currency-label">
              {currency === 'THB' ? 'Thailand — ฿ THB' : 'Indonesia — Rp IDR'}
            </div>
            <Chart
              currency={currency}
              lines={group}
              colors={colors}
              visibleIds={visibleIds}
              days={days}
              seriesByLine={seriesByLine}
              todayIndex={todayIndex}
            />
            <div className="budget-legend">
              {group.map(l => {
                const s = seriesByLine.get(l.id);
                const actual = s && s.actual.length ? s.actual[s.actual.length - 1] : 0;
                return (
                  <LegendRow
                    key={l.id}
                    line={l}
                    color={colors.get(l.id)}
                    visible={visibleIds.has(l.id)}
                    onToggle={toggle}
                    actual={actual}
                    onSaveTarget={saveTarget}
                  />
                );
              })}
            </div>
          </section>
        );
      })}
    </div>
  );
}
