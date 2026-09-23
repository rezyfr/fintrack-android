import { useState, useEffect, useRef } from 'react';
import { getTransactions, deleteTransactions, updateTransaction, updateTransactionsCategory, getBudgetLines } from '../api/supabase';
import { WALLETS, EXPENSE_CATEGORIES, INCOME_CATEGORIES, categoriesFor, walletCurrency } from '../constants/transaction';
import EditTransactionModal from './EditTransactionModal';
import MonthNav from './MonthNav';

const TX_TYPE_OPTIONS = [
  { value: '',           label: 'All'        },
  { value: 'expense',    label: 'Expenses'   },
  { value: 'income',     label: 'Income'     },
  { value: 'transfer',   label: 'Transfers'  },
  { value: 'investment', label: 'Investment' },
];

const WALLET_OPTIONS = [
  { value: '', label: 'All wallets' },
  ...WALLETS.map(w => ({ value: w.id, label: w.name })),
];

// ac: filter-transactions-by-category — category selector lists all known expense and income categories in alphabetical order
const CATEGORY_OPTIONS = [
  { value: '', label: 'All categories' },
  ...[...new Set([...EXPENSE_CATEGORIES, ...INCOME_CATEGORIES])]
    .sort()
    .map(c => ({ value: c, label: c })),
];

// ac: batch-edit-transaction-category — category picker offered in the batch action bar (no blank "All" option)
const BATCH_CATEGORY_OPTIONS = CATEGORY_OPTIONS.filter(o => o.value !== '');

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
  'Family':             'chip-family',
  'Other':              'chip-other',
};

function currentMonth() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
}

function rowCurrency(row) {
  if (row._is_transfer_in) return walletCurrency(row.to_wallet);
  if (row.wallet) return row.wallet === 'BBL' ? 'THB' : 'IDR';
  return row.tab?.includes('IDR') ? 'IDR' : 'THB';
}

function amountClass(row) {
  if (row._is_transfer_in) return 'income';
  const t = row.tx_type || (row.tab?.includes('INCOME') ? 'income' : row.tab?.includes('EXPENSE') ? 'expense' : null);
  if (t === 'income') return 'income';
  if (t === 'expense') return 'expense';
  return 'neutral';
}

function formatAmount(row) {
  const isIDR = rowCurrency(row) === 'IDR';
  const prefix = isIDR ? 'Rp ' : '฿';
  // ac: transfer-in-wallet-view — show to_amount (or amount) as positive inflow for the destination wallet
  const num = row._is_transfer_in ? Number(row.to_amount ?? row.amount) : Number(row.amount);
  if (isNaN(num)) return '—';
  const decimals = isIDR ? 0 : 2;
  const formatted = `${prefix}${num.toLocaleString('en', { minimumFractionDigits: decimals, maximumFractionDigits: decimals })}`;
  return row._is_transfer_in ? `+${formatted}` : formatted;
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

// ac: inline-cell-edit — double-clicking a free-text cell activates an inline input pre-filled with the current value
function TextCell({ initialValue, type = 'text', onCommit, onCancel }) {
  const [val, setVal] = useState(String(initialValue ?? ''));
  const ref = useRef(null);
  useEffect(() => { ref.current?.focus(); ref.current?.select(); }, []);
  return (
    <input
      ref={ref}
      className="inline-input"
      type={type}
      step={type === 'number' ? '0.01' : undefined}
      min={type === 'number' ? '0' : undefined}
      value={val}
      onChange={e => setVal(e.target.value)}
      onKeyDown={e => {
        if (e.key === 'Enter')  { e.preventDefault(); onCommit(val); } // ac: inline-cell-edit — Enter commits the change
        if (e.key === 'Escape') { e.preventDefault(); onCancel(); }    // ac: inline-cell-edit — Escape cancels and restores original
      }}
      onBlur={() => onCommit(val)} // ac: inline-cell-edit — clicking away commits the change
    />
  );
}

// ac: inline-cell-edit — double-clicking a predefined cell shows a select dropdown; choosing an option commits immediately
function SelectCell({ initialValue, options, onCommit, onCancel }) {
  const ref = useRef(null);
  useEffect(() => { ref.current?.focus(); }, []);
  return (
    <select
      ref={ref}
      className="inline-select"
      defaultValue={initialValue}
      onChange={e => onCommit(e.target.value)}
      onKeyDown={e => { if (e.key === 'Escape') { e.preventDefault(); onCancel(); } }}
      onBlur={onCancel}
    >
      {options.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
    </select>
  );
}

function SkeletonRows() {
  return Array.from({ length: 6 }).map((_, i) => (
    <tr key={i}>
      <td><span className="skeleton" style={{ width: 16, height: 16, borderRadius: 3 }} /></td>
      <td><span className="skeleton" style={{ width: 72 }} /></td>
      <td><span className="skeleton" style={{ width: 130 }} /></td>
      <td><span className="skeleton" style={{ width: 100 }} /></td>
      <td style={{ textAlign: 'right' }}><span className="skeleton" style={{ width: 70 }} /></td>
      <td><span className="skeleton" style={{ width: 80 }} /></td>
      <td><span className="skeleton" style={{ width: 70 }} /></td>
      <td><span className="skeleton" style={{ width: 60 }} /></td>
      <td><span className="skeleton" style={{ width: 90 }} /></td>
      <td />
    </tr>
  ));
}

export default function TransactionList() {
  const [txType,    setTxType]    = useState('');
  const [wallet,    setWallet]    = useState('');
  const [month,     setMonth]     = useState(currentMonth());
  // ac: filter-transactions-by-category — a category selector is shown in the transaction list filter row
  const [category,  setCategory]  = useState('');
  // ac: advanced-transaction-filters — search, amount range, and date range state
  const [search,    setSearch]    = useState('');
  const [amountMin, setAmountMin] = useState('');
  const [amountMax, setAmountMax] = useState('');
  const [dateFrom,  setDateFrom]  = useState('');
  const [dateTo,    setDateTo]    = useState('');
  const [rows, setRows]           = useState([]);
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState(null);
  const [editingRow, setEditingRow] = useState(null);
  // ac: assign-transaction-budget-line — active budget lines, fetched once for the Budget cell's dropdown
  const [budgetLines, setBudgetLines] = useState([]);

  // Batch selection state
  const [selected, setSelected]       = useState(new Set());
  const [confirming, setConfirming]   = useState(false);
  const [deleting, setDeleting]       = useState(false);
  // ac: batch-edit-transaction-category — pending category picker state
  const [batchCategory, setBatchCategory] = useState('');
  const [applyingCategory, setApplyingCategory] = useState(false);

  const [editing, setEditing] = useState(null); // { rowId, field }

  const selectAllRef = useRef(null);

  // ac: assign-transaction-budget-line — the Budget cell's dropdown lists every active line
  useEffect(() => {
    getBudgetLines().then(setBudgetLines).catch(() => {});
  }, []);

  function startEdit(row, field) {
    setEditing({ rowId: row.id, field });
  }
  function cancelEdit() { setEditing(null); }
  function isEditing(rowId, field) {
    return editing?.rowId === rowId && editing?.field === field;
  }
  async function commitEdit(value) {
    if (!editing) return;
    const { rowId, field } = editing;
    const original = rows.find(r => r.id === rowId);
    if (!original) { setEditing(null); return; }
    setEditing(null);
    // ac: assign-transaction-budget-line — the Auto option commits an empty string, which clears
    // the override back to null rather than being stored as the literal text "Auto"
    const coerced = field === 'amount' ? Number(value)
      : field === 'budget_line_id' ? (value === '' ? null : Number(value))
      : value;
    if (String(original[field] ?? '') === String(coerced ?? '')) return;
    const snapshot = rows.slice();
    // ac: inline-cell-edit — changes applied optimistically; row reverts if API call fails
    setRows(prev => prev.map(r => r.id === rowId ? { ...r, [field]: coerced } : r));
    try {
      await updateTransaction(rowId, { [field]: coerced });
    } catch (e) {
      setRows(snapshot);
      setError(`Save failed: ${e.message}`);
    }
  }

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    setError(null);
    setSelected(new Set());
    setConfirming(false);
    setBatchCategory('');
    // ac: advanced-transaction-filters — all filters combine in a single API call
    const hasDateRange = dateFrom || dateTo;
    getTransactions({
      txType: txType || null,
      wallet: wallet || null,
      month: hasDateRange ? null : month,
      category: category || null,
      search: search || null,
      amountMin: amountMin !== '' ? Number(amountMin) : null,
      amountMax: amountMax !== '' ? Number(amountMax) : null,
      dateFrom: dateFrom || null,
      dateTo: dateTo || null,
    })
      .then((data) => {
        if (ignore) return;
        // ac: transfer-in-wallet-view — mark rows that are incoming transfers for the viewed wallet
        const marked = wallet
          ? data.map(r => r.tx_type === 'transfer' && r.to_wallet === wallet && r.wallet !== wallet
              ? { ...r, _is_transfer_in: true }
              : r)
          : data;
        setRows(marked);
      })
      .catch((e)   => { if (!ignore) setError(e.message); })
      .finally(()  => { if (!ignore) setLoading(false); });
    return () => { ignore = true; };
  }, [txType, wallet, month, category, search, amountMin, amountMax, dateFrom, dateTo]);

  // Keep select-all checkbox in sync (checked / indeterminate / unchecked)
  useEffect(() => {
    if (!selectAllRef.current) return;
    const n = rows.length;
    const s = selected.size;
    selectAllRef.current.checked       = n > 0 && s === n;
    selectAllRef.current.indeterminate = s > 0 && s < n;
  }, [selected, rows]);

  function toggleAll() {
    if (selected.size === rows.length) {
      setSelected(new Set());
    } else {
      setSelected(new Set(rows.map(r => r.id)));
    }
    setConfirming(false);
  }

  function toggleRow(id) {
    setSelected(prev => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
    setConfirming(false);
  }

  async function handleBatchDelete() {
    setDeleting(true);
    try {
      await deleteTransactions([...selected]);
      setRows(prev => prev.filter(r => !selected.has(r.id)));
      setSelected(new Set());
      setConfirming(false);
    } catch (e) {
      setError(e.message);
    } finally {
      setDeleting(false);
    }
  }

  // ac: batch-edit-transaction-category — updates the category of every selected transaction
  async function handleBatchCategoryApply() {
    if (!batchCategory) return;
    const ids = [...selected];
    setApplyingCategory(true);
    try {
      await updateTransactionsCategory(ids, batchCategory);
      setRows(prev => prev.map(r => ids.includes(r.id) ? { ...r, category: batchCategory } : r));
      setSelected(new Set());
      setBatchCategory('');
    } catch (e) {
      // ac: batch-edit-transaction-category — if the batch update fails, an error is shown and rows keep their original category
      setError(e.message);
    } finally {
      setApplyingCategory(false);
    }
  }

  const stats = computeStats(rows);
  const anySelected = selected.size > 0;

  return (
    <div className="page">
      <div className="section-header">
        <h1 className="section-title">Transactions</h1>
        {!loading && !error && <span className="section-count">{stats.count}</span>}
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
          <select
            aria-label="Wallet"
            className="filter-select"
            value={wallet}
            onChange={(e) => setWallet(e.target.value)}
          >
            {WALLET_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          {/* ac: filter-transactions-by-category — a category selector is shown in the transaction list filter row */}
          <select
            aria-label="Category"
            className="filter-select"
            value={category}
            onChange={(e) => setCategory(e.target.value)}
          >
            {CATEGORY_OPTIONS.map(({ value, label }) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
          <MonthNav value={month} onChange={(v) => { setMonth(v); setDateFrom(''); setDateTo(''); }} />
        </div>
        {/* ac: advanced-transaction-filters — search, amount range, and date range inputs */}
        <div className="filter-row">
          <input
            className="filter-input"
            type="text"
            placeholder="Search item..."
            aria-label="Search item"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <input
            className="filter-input filter-input--narrow"
            type="number"
            placeholder="Min amount"
            aria-label="Minimum amount"
            min="0"
            step="0.01"
            value={amountMin}
            onChange={(e) => setAmountMin(e.target.value)}
          />
          <input
            className="filter-input filter-input--narrow"
            type="number"
            placeholder="Max amount"
            aria-label="Maximum amount"
            min="0"
            step="0.01"
            value={amountMax}
            onChange={(e) => setAmountMax(e.target.value)}
          />
          <input
            className="filter-input"
            type="date"
            aria-label="From date"
            value={dateFrom}
            onChange={(e) => setDateFrom(e.target.value)}
          />
          <input
            className="filter-input"
            type="date"
            aria-label="To date"
            value={dateTo}
            min={dateFrom}
            onChange={(e) => setDateTo(e.target.value)}
          />
          {(search || amountMin || amountMax || dateFrom || dateTo) && (
            <button
              className="filter-tab"
              onClick={() => { setSearch(''); setAmountMin(''); setAmountMax(''); setDateFrom(''); setDateTo(''); }}
            >
              Clear
            </button>
          )}
        </div>
      </div>

      {/* Batch action bar */}
      {anySelected && (
        <div className="batch-bar">
          <span className="batch-count">{selected.size} selected</span>
          {/* ac: batch-edit-transaction-category — category picker and Apply button alongside Delete */}
          {!confirming && (
            <>
              <select
                aria-label="Batch category"
                className="filter-select"
                value={batchCategory}
                onChange={(e) => setBatchCategory(e.target.value)}
                disabled={applyingCategory}
              >
                <option value="">Set category…</option>
                {BATCH_CATEGORY_OPTIONS.map(({ value, label }) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
              <button
                className="btn-secondary"
                onClick={handleBatchCategoryApply}
                disabled={!batchCategory || applyingCategory}
              >
                {applyingCategory ? 'Applying…' : 'Apply'}
              </button>
            </>
          )}
          {!confirming ? (
            <button className="btn-danger-outline" onClick={() => setConfirming(true)}>
              Delete selected
            </button>
          ) : (
            <>
              <span className="batch-confirm-text">
                Delete {selected.size} transaction{selected.size > 1 ? 's' : ''}? This cannot be undone.
              </span>
              <button className="btn-danger" onClick={handleBatchDelete} disabled={deleting}>
                {deleting ? 'Deleting…' : 'Yes, delete'}
              </button>
              <button className="btn-secondary" onClick={() => setConfirming(false)} disabled={deleting}>
                Cancel
              </button>
            </>
          )}
        </div>
      )}

      <div className="table-wrap">
        <table className="tx-table">
          <thead>
            <tr>
              <th style={{ width: 36 }}>
                <input
                  ref={selectAllRef}
                  type="checkbox"
                  className="tx-checkbox"
                  aria-label="Select all"
                  onChange={toggleAll}
                  disabled={loading || rows.length === 0}
                />
              </th>
              <th>Date</th>
              <th>Item</th>
              <th className="align-right">Amount</th>
              <th>Category</th>
              {/* ac: assign-transaction-budget-line — a Budget column sits next to Category */}
              <th>Budget</th>
              <th>Wallet</th>
              <th>Note</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {loading && <SkeletonRows />}

            {!loading && error && (
              <tr>
                <td colSpan={9}>
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
                <td colSpan={9}>
                  <div className="table-state">
                    <div className="table-state-icon">◎</div>
                    <div className="table-state-title">No transactions</div>
                    <div className="table-state-desc">Nothing recorded for this period.</div>
                  </div>
                </td>
              </tr>
            )}

            {!loading && !error && rows.map((row) => (
              <tr key={row.id} className={selected.has(row.id) ? 'row-selected' : ''}>
                <td>
                  <input
                    type="checkbox"
                    className="tx-checkbox"
                    aria-label={`Select ${row.item}`}
                    checked={selected.has(row.id)}
                    onChange={() => toggleRow(row.id)}
                  />
                </td>
                <td>
                  {isEditing(row.id, 'date')
                    ? <TextCell initialValue={row.date} type="date" onCommit={commitEdit} onCancel={cancelEdit} />
                    : <span className="tx-date editable" onDoubleClick={() => startEdit(row, 'date')}>{formatDate(row.date)}</span>
                  }
                </td>
                <td>
                  {isEditing(row.id, 'item')
                    ? <TextCell initialValue={row.item} onCommit={commitEdit} onCancel={cancelEdit} />
                    : <span className="tx-item editable" onDoubleClick={() => startEdit(row, 'item')}>{row.item}</span>
                  }
                </td>
                <td>
                  {isEditing(row.id, 'amount')
                    ? <TextCell initialValue={row.amount} type="number" onCommit={commitEdit} onCancel={cancelEdit} />
                    : <span className={`tx-amount ${amountClass(row)} editable`} onDoubleClick={() => startEdit(row, 'amount')}>{formatAmount(row)}</span>
                  }
                </td>
                <td>
                  {/* ac: transfer-in-wallet-view — label transfer-in rows distinctly in category column */}
                  {row._is_transfer_in ? (
                    <span className="chip chip-other">Transfer In</span>
                  ) : isEditing(row.id, 'category')
                    ? <SelectCell
                        initialValue={row.category}
                        options={categoriesFor(row.tx_type).map(c => ({ value: c, label: c }))}
                        onCommit={commitEdit}
                        onCancel={cancelEdit}
                      />
                    : row.category && (
                        <span className={`chip ${CATEGORY_CLASS[row.category] ?? 'chip-other'} editable`} onDoubleClick={() => startEdit(row, 'category')}>
                          {row.category}
                        </span>
                      )
                  }
                </td>
                <td>
                  {/* ac: assign-transaction-budget-line — double-click opens a dropdown of active lines plus Auto */}
                  {isEditing(row.id, 'budget_line_id')
                    ? <SelectCell
                        initialValue={row.budget_line_id ?? ''}
                        options={[{ value: '', label: 'Auto' }, ...budgetLines.map(l => ({ value: String(l.id), label: l.name }))]}
                        onCommit={commitEdit}
                        onCancel={cancelEdit}
                      />
                    : (() => {
                        const assigned = budgetLines.find(l => l.id === row.budget_line_id);
                        return (
                          <span
                            className={`tx-budget editable${assigned ? '' : ' tx-budget-auto'}`}
                            onDoubleClick={() => startEdit(row, 'budget_line_id')}
                          >
                            {assigned ? assigned.name : 'Auto'}
                          </span>
                        );
                      })()
                  }
                </td>
                <td>
                  {/* ac: transfer-in-wallet-view — show source wallet with Transfer In label for incoming transfers */}
                  {row._is_transfer_in ? (
                    <span className={`chip chip-wallet chip-wallet--${row.wallet.toLowerCase().replace('_', '-')}`} title={`Transfer from ${row.wallet}`}>
                      {row.wallet === 'MANDIRI_CC' ? 'Mandiri Credit Card' : row.wallet === 'BCA_CC' ? 'BCA Credit Card' : row.wallet} &rarr;
                    </span>
                  ) : isEditing(row.id, 'wallet')
                    ? <SelectCell
                        initialValue={row.wallet}
                        options={WALLETS.map(w => ({ value: w.id, label: w.name }))}
                        onCommit={commitEdit}
                        onCancel={cancelEdit}
                      />
                    : row.wallet && (
                        <span className={`chip chip-wallet chip-wallet--${row.wallet.toLowerCase().replace('_', '-')} editable`} onDoubleClick={() => startEdit(row, 'wallet')}>
                          {row.wallet === 'MANDIRI_CC' ? 'Mandiri Credit Card' : row.wallet === 'BCA_CC' ? 'BCA Credit Card' : row.wallet}
                        </span>
                      )
                  }
                </td>
                <td>
                  {isEditing(row.id, 'note')
                    ? <TextCell initialValue={row.note} onCommit={commitEdit} onCancel={cancelEdit} />
                    : <span className="tx-note editable" onDoubleClick={() => startEdit(row, 'note')}>{row.note}</span>
                  }
                </td>
                <td>
                  {/* ac: inline-cell-edit — edit icon opens the full modal for fields not editable inline */}
                  <button
                    className="edit-btn"
                    aria-label={`Edit ${row.item}`}
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
            setSelected((prev) => { const n = new Set(prev); n.delete(id); return n; });
            setEditingRow(null);
          }}
        />
      )}
    </div>
  );
}
