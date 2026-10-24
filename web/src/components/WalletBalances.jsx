import { useState, useEffect } from 'react';
import { useBook, bookHoldsWallet } from '../utils/book';
import { getWalletBalances } from '../api/client';
import { getCurrencySettings, currenciesInUse, formatMoney } from '../constants/currency';

export default function WalletBalances() {
  const [balances, setBalances]     = useState([]);
  // ac: web-currency-books — only the accounts of the current book
  const book = useBook();
  const [balLoading, setBalLoading] = useState(true);
  const [error, setError]           = useState(null);

  useEffect(() => {
    getWalletBalances()
      .then(setBalances)
      .catch(e => setError(e.message))
      .finally(() => setBalLoading(false));
  }, []);

  if (balLoading) return <div className="table-state"><div className="table-state-title">Loading…</div></div>;
  if (error)      return <div className="table-state"><div className="table-state-desc error" role="alert">{error}</div></div>;

  // ac: balances-assets-only-summary — the wallet grid lists only non-credit wallets; card debt is read on the Card Debt tab
  const wallets   = balances.filter(w => w.type !== 'credit' && bookHoldsWallet(book, w.id));
  // ac: web-configurable-currency-pair - one net tile per currency in use, the other currencies first
  // and the home currency last
  const { homeCode } = getCurrencySettings();
  // ac: balances-assets-only-summary — the home Net counts spendable cash only, so investments are out as well as credit cards
  // ac: hide-empty-currency-sections — a currency with no wallet at all is left out, instead of
  // showing a permanent net tile for a currency the user never uses
  const nets = [...currenciesInUse().filter(c => c !== homeCode), homeCode]
    .map(code => {
      const held = wallets.filter(w => w.currency === code && (code !== homeCode || w.type !== 'investment'));
      return { code, count: held.length, net: held.reduce((sum, w) => sum + w.balance, 0) };
    })
    .filter(n => n.count > 0);

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Balances</h1>
      </div>

      <div className="wallet-grid">
        {wallets.map(w => (
          <div key={w.id} className="wallet-card">
            <div className="wallet-name">{w.name}</div>
            <div className="wallet-balance">{formatMoney(w.currency, w.balance)}</div>
          </div>
        ))}
      </div>

      {nets.length > 0 && (
        <div className="net-worth-row">
          {nets.map(n => (
            <div key={n.code} className="net-worth-card">
              <div className="net-worth-label">{n.code} Net</div>
              <div className="net-worth-value">{formatMoney(n.code, n.net)}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
