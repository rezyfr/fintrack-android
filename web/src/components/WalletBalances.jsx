import { useState, useEffect } from 'react';
import { getWalletBalances } from '../api/supabase';

const FMT = {
  THB: (n) => `฿${Number(n).toLocaleString('en', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`,
  IDR: (n) => `Rp ${Math.round(Number(n)).toLocaleString('en')}`,
};

export default function WalletBalances() {
  const [balances, setBalances] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [error, setError]       = useState(null);

  useEffect(() => {
    getWalletBalances()
      .then(setBalances)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="wallet-loading">Loading balances…</div>;
  if (error)   return <div className="wallet-error" role="alert">{error}</div>;

  const idrAssets = balances.filter((w) => w.currency === 'IDR' && w.type !== 'credit');
  const idrCC     = balances.filter((w) => w.currency === 'IDR' && w.type === 'credit');
  const thbNet    = balances.filter((w) => w.currency === 'THB').reduce((s, w) => s + w.balance, 0);
  const idrNet    = idrAssets.reduce((s, w) => s + w.balance, 0)
                  - idrCC.reduce((s, w) => s + w.balance, 0);

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Balances</h1>
      </div>

      <div className="wallet-grid">
        {balances.map((w) => (
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
    </div>
  );
}
