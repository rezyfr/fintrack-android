import { useState, useEffect, useMemo } from 'react';
import { getTransactions } from '../api/supabase';
import { payCycle, calendarWeeks } from '../utils/payCycle';
import { WALLETS } from '../constants/transaction';

const IDR_WALLETS = new Set(WALLETS.filter(w => w.currency === 'IDR').map(w => w.id));
const WEEKDAYS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

function fmt(amount, currency) {
  const isIDR = currency === 'IDR';
  return `${isIDR ? 'Rp ' : '฿'}${Math.round(Math.abs(amount)).toLocaleString('en')}`;
}

function compact(amount, currency) {
  const abs = Math.abs(amount);
  const sym = currency === 'IDR' ? 'Rp' : '฿';
  if (abs >= 1_000_000) return `${sym}${(amount / 1_000_000).toFixed(abs >= 10_000_000 ? 0 : 1)}M`;
  if (abs >= 1_000) return `${sym}${Math.round(amount / 1_000)}k`;
  return `${sym}${Math.round(amount)}`;
}

function dayLabel(iso) {
  const [, m, d] = iso.split('-');
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  return `${Number(d)} ${months[Number(m) - 1]}`;
}

function todayIso() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export default function Calendar() {
  const [offset, setOffset] = useState(0);
  const [currency, setCurrency] = useState('IDR');
  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [selected, setSelected] = useState(null);

  const cycle = useMemo(() => payCycle(offset), [offset]);
  const weeks = useMemo(() => calendarWeeks(cycle.from, cycle.to), [cycle]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    getTransactions({ dateFrom: cycle.from, dateTo: cycle.to, limit: 2000 })
      .then(data => { if (!cancelled) setRows(data); })
      .catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [cycle]);

  // Changing the cycle or currency clears the selected day, so a stale day is never shown.
  const goPrev = () => { setOffset(o => o + 1); setSelected(null); };
  const goNext = () => { setOffset(o => Math.max(-1, o - 1)); setSelected(null); };
  const pickCurrency = (c) => { setCurrency(c); setSelected(null); };

  // ac: cycle-calendar-daily-totals — per-day spend and income for the chosen currency; transfers
  // between the user's own wallets are movements, not spending, so they are excluded.
  const byDay = useMemo(() => {
    const map = new Map();
    for (const r of rows) {
      if (r.to_wallet) continue;
      const isIDR = IDR_WALLETS.has(r.wallet);
      if ((currency === 'IDR') !== isIDR) continue;
      if (r.tx_type !== 'expense' && r.tx_type !== 'income') continue;
      const cell = map.get(r.date) || { expense: 0, income: 0 };
      if (r.tx_type === 'expense') cell.expense += Number(r.amount) || 0;
      else cell.income += Number(r.amount) || 0;
      map.set(r.date, cell);
    }
    return map;
  }, [rows, currency]);

  const totals = useMemo(() => {
    let expense = 0, income = 0;
    for (const [, c] of byDay) { expense += c.expense; income += c.income; }
    return { expense, income, net: income - expense };
  }, [byDay]);

  const selectedRows = useMemo(() => {
    if (!selected) return [];
    return rows
      .filter(r => r.date === selected && !r.to_wallet
        && (currency === 'IDR') === IDR_WALLETS.has(r.wallet)
        && (r.tx_type === 'expense' || r.tx_type === 'income'))
      .sort((a, b) => Number(b.amount) - Number(a.amount));
  }, [rows, selected, currency]);

  const today = todayIso();

  return (
    <div className="page">
      <div className="cal-head">
        <div>
          <h1 className="section-title">Calendar</h1>
          <div className="cal-cycle">{dayLabel(cycle.from)} – {dayLabel(cycle.to)}</div>
        </div>
        <div className="cal-controls">
          {/* ac: cycle-calendar-daily-totals — currency toggle */}
          <div className="cal-cur">
            {['IDR', 'THB'].map(c => (
              <button
                key={c}
                className={`cal-cur-btn${currency === c ? ' active' : ''}`}
                onClick={() => pickCurrency(c)}
                aria-pressed={currency === c}
              >
                {c === 'IDR' ? 'Rp' : '฿'}
              </button>
            ))}
          </div>
          <div className="cal-nav">
            <button className="filter-tab" onClick={goPrev} aria-label="Previous cycle">←</button>
            <button className="filter-tab" onClick={goNext} disabled={offset === -1} aria-label="Next cycle">→</button>
          </div>
        </div>
      </div>

      <div className="stats-row cal-stats">
        <div className="stat-card"><div className="stat-label">Spent</div><div className="stat-value expense">{fmt(totals.expense, currency)}</div></div>
        <div className="stat-card"><div className="stat-label">Received</div><div className="stat-value income">{fmt(totals.income, currency)}</div></div>
        <div className="stat-card"><div className="stat-label">Net</div><div className={`stat-value ${totals.net >= 0 ? 'income' : 'expense'}`}>{totals.net >= 0 ? '+' : '-'}{fmt(totals.net, currency)}</div></div>
      </div>

      {loading && <div className="table-state"><div className="table-state-title">Loading…</div></div>}
      {error && <div className="table-state"><div className="table-state-desc error" role="alert">{error}</div></div>}

      {!loading && !error && (
        <>
          <div className="cal-grid" role="grid" aria-label={`Spending for ${dayLabel(cycle.from)} to ${dayLabel(cycle.to)}`}>
            {WEEKDAYS.map(w => <div key={w} className="cal-weekday">{w}</div>)}
            {weeks.flat().map(cell => {
              const data = byDay.get(cell.iso);
              const isSel = selected === cell.iso;
              const dayNum = Number(cell.iso.split('-')[2]);
              return (
                <button
                  key={cell.iso}
                  className={`cal-cell${cell.inCycle ? '' : ' cal-out'}${isSel ? ' cal-sel' : ''}${cell.iso === today ? ' cal-today' : ''}`}
                  onClick={() => cell.inCycle && setSelected(isSel ? null : cell.iso)}
                  disabled={!cell.inCycle}
                  aria-pressed={isSel}
                >
                  <span className="cal-day">{dayNum}</span>
                  {cell.inCycle && data && data.expense > 0 && (
                    <span className="cal-exp">{compact(data.expense, currency)}</span>
                  )}
                  {cell.inCycle && data && data.income > 0 && (
                    <span className="cal-inc">{compact(data.income, currency)}</span>
                  )}
                </button>
              );
            })}
          </div>

          {selected && (
            <div className="cal-daylist">
              <div className="cal-daylist-head">{dayLabel(selected)}</div>
              {selectedRows.length === 0 && <div className="cal-daylist-empty">No {currency} transactions.</div>}
              {selectedRows.map(r => (
                <div key={r.id} className="cal-daylist-row">
                  <span className="cal-daylist-item">{r.item}</span>
                  <span className={`cal-daylist-amt ${r.tx_type === 'income' ? 'income' : 'expense'}`}>
                    {r.tx_type === 'income' ? '+' : '-'}{fmt(r.amount, currency)}
                  </span>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
