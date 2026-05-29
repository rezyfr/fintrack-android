import { useState, useEffect } from 'react';
import { getTransactions } from '../api/supabase';
import EditTransactionModal from './EditTransactionModal';

const TX_TYPE_OPTIONS = [
  { value: '',           label: 'All'        },
  { value: 'expense',    label: 'Expenses'   },
  { value: 'income',     label: 'Income'     },
  { value: 'transfer',   label: 'Transfers'  },
  { value: 'investment', label: 'Investment' },
];

const WALLET_OPTIONS = [
  { value: '',           label: 'All wallets'  },
  { value: 'BBL',        label: 'Bangkok Bank' },
  { value: 'BCA',        label: 'BCA'          },
  { value: 'MANDIRI',    label: 'Mandiri'      },
  { value: 'MANDIRI_CC', label: 'Mandiri CC'   },
  { value: 'INVESTMENT', label: 'Investments'  },
];

const CATEGORY_CLASS = {
  'Food & Drink':       'chip-food',
  'Transport':          'chip-transport',
  'Bills':              'chip-bills',
  'Subscriptions':      'chip-subscriptions',
  'Groceries':          'chip-groceries',
  'Shopping':           'chip-shopping',
  'Entertainment':      'chip-entertainment',
  'Health & Wellbeing': 'chip-health',
  'Travel':             'chip-travel',
  'Business':           'chip-business',
  'Gifts':              'chip-gifts',
  'Other':              'chip-other',
};

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function rowCurrency(row) {
  if (row.wallet) return row.wallet === 'BBL' ? 'THB' : 'IDR';
  return row.tab?.includes('IDR') ? 'IDR' : 'THB';
}

function amountClass(row) {
  const t = row.tx_type || (row.tab?.includes('INCOME') ? 'income' : row.tab?.includes('EXPENSE') ? 'expense' : null);
  if (t === 'income') return 'income';
  if (t === 'expense') return 'expense';
  return 'neutral';
}

function formatAmount(row) {
  const isIDR = rowCurrency(row) === 'IDR';
  const prefix = isIDR ? 'Rp ' : '฿';
  const num = Number(row.amount);
  if (isNaN(num)) return '—';
  const decimals = isIDR ? 0 : 2;
  return `${prefix}${num.toLocaleString('en', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })}`;
}

function formatDate(iso) {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  if (!y || !m || !d) return iso;
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  return `${d} ${months[Number(m) - 1]} ${y.slice(2)}`;
}

function currencyTotal(rows, currency) {
  const subset = rows.filter(r => rowCurrency(r) === currency && (r.tx_type === 'expense' || r.tx_type === 'income' || !r.tx_type));
  if (subset.length === 0) return null;
  const total = subset.reduce((s, r) => s + Number(r.amount || 0), 0);
  const allExpense = subset.every(r => r.tx_type === 'expense' || r.tab?.includes('EXPENSE'));
  const allIncome  = subset.every(r => r.tx_type === 'income'  || r.tab?.includes('INCOME'));
  const amtClass   = allExpense ? 'expense' : allIncome ? 'income' : 'neutral';
  const prefix     = currency === 'IDR' ? 'Rp ' : '฿';
  const decimals   = currency === 'IDR' ? 0 : 2;
  const formatted  = `${prefix}${total.toLocaleString('en', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })}`;
  return { formatted, amtClass };
}

function computeStats(rows) {
  return {
    count: rows.length,
    thb: currencyTotal(rows, 'THB'),
    idr: currencyTotal(rows, 'IDR'),
  };
}

function SkeletonRows() {
  return Array.from({ length: 6 }).map((_, i) => (
    <tr key={i}>
      <td><span className="skeleton" style={{ width: 72 }} /></td>
      <td><span className="skeleton" style={{ width: 130 }} /></td>
      <td><span className="skeleton" style={{ width: 100 }} /></td>
      <td style={{ textAlign: 'right' }}><span className="skeleton" style={{ width: 70 }} /></td>
      <td><span className="skeleton" style={{ width: 80 }} /></td>
      <td><span className="skeleton" style={{ width: 74 }} /></td>
      <td><span className="skeleton" style={{ width: 60 }} /></td>
      <td><span className="skeleton" style={{ width: 90 }} /></td>
      <td />
    </tr>
  ));
}

export default function TransactionList() {
  const [txType, setTxType] = useState('');
  const [wallet, setWallet] = useState('');
  const [month, setMonth]   = useState(currentMonth());
  const [rows, setRows]     = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]   = useState(null);
  const [editingRow, setEditingRow] = useState(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError(null);
    getTransactions({ txType: txType || null, wallet: wallet || null, month })
      .then((data) => { if (!ignore) setRows(data); })
      .catch((e)   => { if (!ignore) setError(e.message); })
      .finally(()  => { if (!ignore) setLoading(false); });
    return () => { ignore = true; };
  }, [txType, wallet, month]);

  const stats = computeStats(rows);

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Transactions</h1>
        {!loading && !error && (
          <span className="section-count">{stats.count}</span>
        )}
      </div>

      {!loading && !error && rows.length > 0 && (
        <div className="stats-row">
          <div className="stat-card">
            <div className="stat-label">Transactions</div>
            <div className="stat-value">{stats.count}</div>
          </div>
          {stats.thb && (
            <div className="stat-card">
              <div className="stat-label">THB Total</div>
              <div className={`stat-value ${stats.thb.amtClass}`}>{stats.thb.formatted}</div>
            </div>
          )}
          {stats.idr && (
            <div className="stat-card">
              <div className="stat-label">IDR Total</div>
              <div className={`stat-value ${stats.idr.amtClass}`}>{stats.idr.formatted}</div>
            </div>
          )}
        </div>
      )}

      <div className="filters">
        <div className="filter-tabs">
          {TX_TYPE_OPTIONS.map(({ value, label }) => (
            <button
              key={value}
              className={`filter-tab${txType === value ? ' active' : ''}${txType === value && value ? ` ${value}` : ''}`}
              onClick={() => setTxType(value)}
            >
              {label}
            </button>
          ))}
        </div>
        <div className="filter-row">
          <label className="sr-only" htmlFor="wallet-filter">Wallet</label>
          <select
            id="wallet-filter"
            aria-label="Wallet"
            className="filter-select"
            value={wallet}
            onChange={(e) => setWallet(e.target.value)}
          >
            {WALLET_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <input
            className="month-input"
            type="month"
            value={month}
            onChange={(e) => setMonth(e.target.value)}
          />
        </div>
      </div>

      <div className="table-wrap">
        <table className="tx-table">
          <thead>
            <tr>
              <th>Date</th>
              <th>Merchant</th>
              <th>Item</th>
              <th className="align-right">Amount</th>
              <th>Category</th>
              <th>Channel</th>
              <th>Wallet</th>
              <th>Note</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {loading && <SkeletonRows />}

            {!loading && error && (
              <tr>
                <td colSpan={8}>
                  <div className="table-state">
                    <div className="table-state-icon">⚠</div>
                    <div className="table-state-title">Failed to load</div>
                    <div className="table-state-desc error" role="alert">{error}</div>
                  </div>
                </td>
              </tr>
            )}

            {!loading && !error && rows.length === 0 && (
              <tr>
                <td colSpan={8}>
                  <div className="table-state">
                    <div className="table-state-icon">◎</div>
                    <div className="table-state-title">No transactions</div>
                    <div className="table-state-desc">Nothing recorded for this period.</div>
                  </div>
                </td>
              </tr>
            )}

            {!loading && !error && rows.map((row) => (
              <tr key={row.id}>
                <td><span className="tx-date">{formatDate(row.date)}</span></td>
                <td><span className="tx-merchant">{row.merchant}</span></td>
                <td><span className="tx-item">{row.item}</span></td>
                <td>
                  <span className={`tx-amount ${amountClass(row)}`}>
                    {formatAmount(row)}
                  </span>
                </td>
                <td>
                  {row.category && (
                    <span className={`chip ${CATEGORY_CLASS[row.category] ?? 'chip-other'}`}>
                      {row.category}
                    </span>
                  )}
                </td>
                <td>
                  {row.channel && (
                    <span className="chip chip-channel">{row.channel}</span>
                  )}
                </td>
                <td>
                  {row.wallet && (
                    <span className={`chip chip-wallet chip-wallet--${row.wallet.toLowerCase().replace('_', '-')}`}>
                      {row.wallet === 'MANDIRI_CC' ? 'CC' : row.wallet}
                    </span>
                  )}
                </td>
                <td><span className="tx-note">{row.note}</span></td>
                <td>
                  <button
                    className="edit-btn"
                    aria-label={`Edit ${row.merchant}`}
                    onClick={() => setEditingRow(row)}
                  >
                    ✎
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {editingRow && (
        <EditTransactionModal
          row={editingRow}
          onClose={() => setEditingRow(null)}
          onSaved={(updated) => {
            setRows((prev) => prev.map((r) => r.id === updated.id ? updated : r));
            setEditingRow(null);
          }}
          onDeleted={(id) => {
            setRows((prev) => prev.filter((r) => r.id !== id));
            setEditingRow(null);
          }}
        />
      )}
    </div>
  );
}
