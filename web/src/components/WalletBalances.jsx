import { useState, useEffect, useCallback } from 'react';
import { getWalletBalances, getWalletReconciliation, upsertStatementBalance } from '../api/supabase';

const FMT = {
  THB: (n) => `฿${Number(n).toLocaleString('en', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
  IDR: (n) => `Rp ${Math.round(Number(n)).toLocaleString('en')}`,
};

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function ReconciliationStatus({ row, currency }) {
  if (row.opening_balance == null) return <span className="recon-status recon-none">—</span>;
  if (row.next_opening == null)    return <span className="recon-status recon-pending">No next month</span>;

  const diff = Number(row.difference);
  const ok = Math.abs(diff) < 0.01;
  if (ok) return <span className="recon-status recon-ok">✓ Reconciled</span>;

  const sign = diff > 0 ? '+' : '';
  const label = FMT[currency](Math.abs(diff));
  const dir   = diff > 0 ? 'overstated' : 'understated';
  return (
    <span className="recon-status recon-error" title={`Calculated closing is ${sign}${label} vs next statement. Transactions may be ${dir}.`}>
      ✗ Off by {sign}{label}
    </span>
  );
}

function ReconciliationRow({ row, onSave }) {
  const [input, setInput] = useState(
    row.opening_balance != null ? String(row.opening_balance) : ''
  );
  const [saving, setSaving] = useState(false);

  async function handleBlur() {
    const val = parseFloat(input);
    if (isNaN(val)) return;
    if (val === Number(row.opening_balance)) return;
    setSaving(true);
    await onSave(row.wallet_id, val);
    setSaving(false);
  }

  const currency = row.currency;
  const hasOpening = row.opening_balance != null;
  const net = Number(row.net_change);
  const closing = hasOpening ? Number(row.opening_balance) + net : null;

  return (
    <tr className="recon-row">
      <td className="recon-wallet">
        <span>{row.wallet_name}</span>
        <span className="recon-currency-badge">{currency}</span>
      </td>
      <td className="recon-cell">
        <div className="recon-input-wrap">
          <input
            className="recon-input"
            type="number"
            placeholder="From statement"
            value={input}
            onChange={e => setInput(e.target.value)}
            onBlur={handleBlur}
            disabled={saving}
          />
          {saving && <span className="recon-saving">…</span>}
        </div>
      </td>
      <td className={`recon-cell recon-net ${net >= 0 ? 'pos' : 'neg'}`}>
        {net >= 0 ? '+' : ''}{FMT[currency](net)}
      </td>
      <td className="recon-cell recon-closing">
        {closing != null ? FMT[currency](closing) : '—'}
      </td>
      <td className="recon-cell">
        <ReconciliationStatus row={row} currency={currency} />
      </td>
    </tr>
  );
}

export default function WalletBalances() {
  const [balances, setBalances]         = useState([]);
  const [recon, setRecon]               = useState([]);
  const [reconMonth, setReconMonth]     = useState(currentMonth());
  const [balLoading, setBalLoading]     = useState(true);
  const [reconLoading, setReconLoading] = useState(false);
  const [error, setError]               = useState(null);

  useEffect(() => {
    getWalletBalances()
      .then(setBalances)
      .catch(e => setError(e.message))
      .finally(() => setBalLoading(false));
  }, []);

  const loadRecon = useCallback((month) => {
    setReconLoading(true);
    getWalletReconciliation(month)
      .then(setRecon)
      .catch(e => setError(e.message))
      .finally(() => setReconLoading(false));
  }, []);

  useEffect(() => { loadRecon(reconMonth); }, [reconMonth, loadRecon]);

  async function handleSave(walletId, openingBalance) {
    await upsertStatementBalance(walletId, reconMonth, openingBalance);
    loadRecon(reconMonth);
  }

  if (balLoading) return <div className="table-state"><div className="table-state-title">Loading…</div></div>;
  if (error)      return <div className="table-state"><div className="table-state-desc error" role="alert">{error}</div></div>;

  const idrAssets = balances.filter(w => w.currency === 'IDR' && w.type !== 'credit');
  const idrCC     = balances.filter(w => w.currency === 'IDR' && w.type === 'credit');
  const thbNet    = balances.filter(w => w.currency === 'THB').reduce((s, w) => s + w.balance, 0);
  const idrNet    = idrAssets.reduce((s, w) => s + w.balance, 0)
                  - idrCC.reduce((s, w) => s + w.balance, 0);

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Balances</h1>
      </div>

      <div className="wallet-grid">
        {balances.map(w => (
          <div key={w.id} className={`wallet-card${w.type === 'credit' ? ' wallet-card--cc' : ''}`}>
            <div className="wallet-name">{w.name}</div>
            <div className={`wallet-balance${w.type === 'credit' ? ' liability' : ''}`}>
              {FMT[w.currency](w.balance)}
            </div>
            {w.type === 'credit' && <div className="wallet-label">owed</div>}
          </div>
        ))}
      </div>

      <div className="net-worth-row">
        <div className="net-worth-card">
          <div className="net-worth-label">THB Net</div>
          <div className="net-worth-value">{FMT.THB(thbNet)}</div>
        </div>
        <div className="net-worth-card">
          <div className="net-worth-label">IDR Net</div>
          <div className="net-worth-value">{FMT.IDR(idrNet)}</div>
        </div>
      </div>

      {/* ── Reconciliation ──────────────────────────────────── */}
      <div className="section-header" style={{ marginTop: 48 }}>
        <h2 className="section-title" style={{ fontSize: 22 }}>Reconciliation</h2>
        <input
          className="month-input"
          type="month"
          value={reconMonth}
          onChange={e => setReconMonth(e.target.value)}
          style={{ marginLeft: 'auto' }}
        />
      </div>

      <p className="recon-hint">
        Enter the opening balance from your bank statement. The app calculates the closing balance from your recorded transactions. If next month's opening is set, it checks whether they match.
      </p>

      {reconLoading ? (
        <div className="table-state"><div className="table-state-title">Loading…</div></div>
      ) : (
        <div className="table-wrap">
          <table className="tx-table recon-table">
            <thead>
              <tr>
                <th>Wallet</th>
                <th>Statement Opening</th>
                <th className="align-right">Net this month</th>
                <th className="align-right">Calculated Closing</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {recon.map(row => (
                <ReconciliationRow key={row.wallet_id} row={row} onSave={handleSave} />
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
