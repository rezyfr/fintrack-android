import { useState, useEffect, useMemo } from 'react';
import { getTransactions, getCardBilling } from '../api/supabase';
import { WALLETS } from '../constants/transaction';
import {
  latestCutoff, previousCutoff, dueDateAfter, installmentPortion, minimumPayment, daysUntil,
} from '../utils/cardBilling';

const CC_WALLETS = WALLETS.filter(w => w.type === 'credit');

// Fallback cutoff day when a card has no stored billing record yet.
const FALLBACK_CUTOFF_DAY = { BCA_CC: 3, MANDIRI_CC: 11 };

function ymd(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// Most-recent-first list of this card's past statement cutoff dates.
function recentCutoffDates(day, count = 6) {
  const now = new Date();
  let d = new Date(now.getFullYear(), now.getMonth(), day);
  if (d > now) d = new Date(d.getFullYear(), d.getMonth() - 1, day);
  const dates = [];
  for (let i = 0; i < count; i++) {
    dates.push(ymd(d));
    d = new Date(d.getFullYear(), d.getMonth() - 1, day);
  }
  return dates;
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

// ac: track-cc-transactions-as-debt — a payment or refund/credit settles the oldest unpaid charges
// first (FIFO); processing events in date order means a payment can only settle charges dated on or
// before its own date, since later charges haven't been pushed onto the queue yet.
function computeDebt(rows, wallet) {
  const events = rows
    .filter(r => r.tx_type === 'expense' || r.tx_type === 'income' || (r.tx_type === 'transfer' && r.to_wallet === wallet))
    .map(r => ({ ...r, kind: r.tx_type === 'expense' ? 'charge' : 'credit' }))
    .sort((a, b) => (a.date < b.date ? -1 : a.date > b.date ? 1 : a.id - b.id));

  const charges = [];
  for (const ev of events) {
    if (ev.kind === 'charge') {
      charges.push({ ...ev, paidAmount: 0, remaining: Number(ev.amount) });
      continue;
    }
    let pool = Number(ev.amount);
    for (const charge of charges) {
      if (pool <= 0) break;
      if (charge.remaining <= 0) continue;
      const applied = Math.min(pool, charge.remaining);
      charge.paidAmount += applied;
      charge.remaining -= applied;
      pool -= applied;
    }
  }

  return charges
    .map(c => ({
      ...c,
      // ac: track-cc-transactions-as-debt — a payment that doesn't fully cover a charge marks it
      // Partially Paid with the remaining unpaid amount shown
      status: c.remaining <= 0.01 ? 'paid' : c.paidAmount > 0 ? 'partial' : 'unpaid',
    }))
    .sort((a, b) => (a.date < b.date ? 1 : a.date > b.date ? -1 : 0));
}

function DebtStatus({ status, remaining, currency }) {
  if (status === 'paid') return <span className="debt-status debt-status-paid">✓ Paid</span>;
  if (status === 'partial') {
    return (
      <span className="debt-status debt-status-partial">
        Partially Paid · {fmt(remaining, currency)} left
      </span>
    );
  }
  return <span className="debt-status debt-status-unpaid">Unpaid</span>;
}

export default function CardDebt() {
  const [walletId, setWalletId] = useState(CC_WALLETS[0]?.id ?? '');
  const [rows, setRows] = useState([]);
  const [billing, setBilling] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  // ac: track-cc-transactions-as-debt — defaults to unpaid/partial only, matching "what do I still owe"
  const [unpaidOnly, setUnpaidOnly] = useState(true);

  // ac: card-statement-due-and-minimum — billing config is loaded once, not per card switch
  useEffect(() => {
    let cancelled = false;
    getCardBilling()
      .then(data => { if (!cancelled) setBilling(data); })
      .catch(() => { if (!cancelled) setBilling([]); });
    return () => { cancelled = true; };
  }, []);

  useEffect(() => {
    if (!walletId) return;
    let cancelled = false;
    setLoading(true);
    setError(null);
    getTransactions({ wallet: walletId, limit: 2000 })
      .then(data => { if (!cancelled) setRows(data); })
      .catch(e => { if (!cancelled) setError(e.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [walletId]);

  // ac: card-statement-due-and-minimum — the selected card's stored billing record, or a fallback
  const cardBilling = useMemo(() => {
    const found = billing.find(b => b.wallet === walletId);
    if (found) return found;
    return {
      wallet: walletId,
      cutoff_day: FALLBACK_CUTOFF_DAY[walletId] ?? 1,
      due_day: 1,
      min_percent: 5,
      min_full_installments: false,
      _fallback: true,
    };
  }, [billing, walletId]);

  const charges = useMemo(() => computeDebt(rows, walletId), [rows, walletId]);
  // ac: track-cc-transactions-as-debt — total unpaid balance for the selected card
  const totalUnpaid = charges.reduce((s, c) => s + c.remaining, 0);
  const visible = unpaidOnly ? charges.filter(c => c.status !== 'paid') : charges;
  const currency = WALLETS.find(w => w.id === walletId)?.currency ?? 'IDR';

  // ac: track-cc-transactions-as-debt — unpaid balance as of each past billing statement cutoff,
  // computed from only the charges/payments dated on or before that cutoff
  const checkpoints = useMemo(() => {
    return recentCutoffDates(cardBilling.cutoff_day).map(cutoff => {
      const asOf = computeDebt(rows.filter(r => r.date <= cutoff), walletId);
      const unpaid = asOf.reduce((s, c) => s + c.remaining, 0);
      return { cutoff, unpaid };
    });
  }, [rows, walletId, cardBilling.cutoff_day]);

  // ac: card-statement-due-and-minimum — the latest closed statement: its balance, due date, minimum
  const statement = useMemo(() => {
    const cutoff = latestCutoff(cardBilling.cutoff_day);
    const prev = previousCutoff(cutoff, cardBilling.cutoff_day);
    const asOf = computeDebt(rows.filter(r => r.date <= cutoff), walletId);
    const balance = asOf.reduce((s, c) => s + c.remaining, 0);
    const installment = installmentPortion(rows, prev, cutoff);
    const minimum = minimumPayment({
      statementBalance: balance,
      installment,
      minPercent: Number(cardBilling.min_percent),
      minFullInstallments: cardBilling.min_full_installments,
    });
    const due = dueDateAfter(cutoff, cardBilling.due_day);
    return { cutoff, balance, installment, minimum, due, daysLeft: daysUntil(due) };
  }, [rows, walletId, cardBilling]);

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Card Debt</h1>
      </div>

      {/* ac: track-cc-transactions-as-debt — a wallet selector switches between Mandiri CC and BCA CC */}
      <div className="filter-tabs">
        {CC_WALLETS.map(w => (
          <button
            key={w.id}
            className={`filter-tab${walletId === w.id ? ' active' : ''}`}
            onClick={() => setWalletId(w.id)}
          >
            {w.name}
          </button>
        ))}
      </div>

      {loading && (
        <div className="table-state"><div className="table-state-title">Loading…</div></div>
      )}
      {error && (
        <div className="table-state"><div className="table-state-desc error" role="alert">{error}</div></div>
      )}

      {!loading && !error && (
        <>
          {/* ac: card-statement-due-and-minimum — current statement: balance, due date, minimum */}
          <div className={`statement-card${statement.daysLeft < 0 ? ' statement-overdue' : ''}`}>
            <div className="statement-head">
              <span>Statement closed {formatDate(statement.cutoff)}</span>
              <span className={`statement-due-pill${statement.daysLeft < 0 ? ' overdue' : statement.daysLeft <= 3 ? ' soon' : ''}`}>
                {statement.daysLeft < 0
                  ? `Overdue by ${-statement.daysLeft}d`
                  : statement.daysLeft === 0 ? 'Due today' : `Due in ${statement.daysLeft}d`}
              </span>
            </div>
            <div className="statement-grid">
              <div>
                <div className="stat-label">Statement balance</div>
                <div className="stat-value">{fmt(statement.balance, currency)}</div>
              </div>
              <div>
                <div className="stat-label">Minimum payment</div>
                <div className="stat-value expense">{fmt(statement.minimum, currency)}</div>
              </div>
              <div>
                <div className="stat-label">Payment due</div>
                <div className="stat-value">{formatDate(statement.due)}</div>
              </div>
            </div>
            {statement.installment > 0 && cardBilling.min_full_installments && (
              <p className="statement-note">
                Minimum includes {fmt(statement.installment, currency)} of installments billed in full,
                plus {Number(cardBilling.min_percent)}% of the rest.
              </p>
            )}
            {cardBilling._fallback && (
              <p className="statement-note statement-note-warn">
                No billing record stored for this card. Showing estimated defaults.
              </p>
            )}
          </div>

          <div className="stats-row">
            <div className="stat-card">
              <div className="stat-label">Total Unpaid</div>
              <div className="stat-value expense">{fmt(totalUnpaid, currency)}</div>
            </div>
          </div>

          {/* ac: track-cc-transactions-as-debt — unpaid balance as of each past billing cutoff */}
          <div className="breakdown-card">
            <div className="breakdown-header">
              <span>Unpaid Balance at Each Billing Cutoff</span>
              <span>Cutoff: {cardBilling.cutoff_day}th</span>
            </div>
            {checkpoints.map(cp => (
              <div key={cp.cutoff} className="debt-checkpoint-row">
                <span className="debt-checkpoint-date">{formatDate(cp.cutoff)}</span>
                <span className="debt-checkpoint-amount">{fmt(cp.unpaid, currency)}</span>
              </div>
            ))}
          </div>

          <label className="debt-toggle">
            <input
              type="checkbox"
              checked={unpaidOnly}
              onChange={e => setUnpaidOnly(e.target.checked)}
            />
            Show unpaid/partial only
          </label>

          <div className="table-wrap">
            <table className="tx-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Item</th>
                  <th className="align-right">Amount</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {visible.map(c => (
                  <tr key={c.id}>
                    <td>{formatDate(c.date)}</td>
                    <td>{c.item}</td>
                    <td className="align-right">{fmt(c.amount, currency)}</td>
                    <td><DebtStatus status={c.status} remaining={c.remaining} currency={currency} /></td>
                  </tr>
                ))}
                {visible.length === 0 && (
                  <tr>
                    <td colSpan={4}>
                      <div className="table-state">
                        <div className="table-state-title">Nothing to show</div>
                        <div className="table-state-desc">
                          {unpaidOnly ? 'No unpaid or partially paid charges.' : 'No charges found.'}
                        </div>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </>
      )}
    </div>
  );
}
