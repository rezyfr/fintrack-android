import { useState } from 'react';
import { addTransaction } from '../api/supabase';
import { WALLETS, TX_TYPES, categoriesFor, subcategoriesFor, deriveTab, currencySymbol, walletCurrency } from '../constants/transaction';
import { useMerchantHistory } from '../hooks/useMerchantHistory';
import MerchantInput from './MerchantInput';

function today() {
  return new Date().toISOString().slice(0, 10);
}

const EMPTY = {
  item: '', amount: '',
  category: 'Food & Drink',
  subcategory: '',
  wallet: 'BBL', txType: 'expense', toWallet: '', toAmount: '', date: '', note: '',
};

export default function AddTransactionForm() {
  const [form, setForm]             = useState(() => ({ ...EMPTY, date: today() }));
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus]         = useState(null);
  const [errorMsg, setErrorMsg]     = useState('');
  const [merchantHistory, addToHistory] = useMerchantHistory();

  function handleChange(e) {
    const { name, value } = e.target;
    if (name === 'txType') {
      setForm((f) => ({ ...f, txType: value, category: categoriesFor(value)[0], subcategory: '' }));
    } else if (name === 'category') {
      // Reset subcategory when the category changes; it may not apply anymore.
      setForm((f) => ({ ...f, category: value, subcategory: '' }));
    } else {
      setForm((f) => ({ ...f, [name]: value }));
    }
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setStatus(null);
    setErrorMsg('');
    try {
      const payload = {
        item:      form.item,
        amount:    Number(form.amount),
        category:  form.category,
        subcategory: form.subcategory || undefined,
        date:      form.date,
        note:      form.note || undefined,
        wallet:    form.wallet,
        tx_type:   form.txType,
        to_wallet: form.txType === 'transfer' ? form.toWallet : undefined,
        // ac: add-transfer-target-amount — send the received amount for cross-currency transfers
        to_amount: (form.txType === 'transfer' && form.toWallet && walletCurrency(form.wallet) !== walletCurrency(form.toWallet) && form.toAmount !== '')
          ? Number(form.toAmount) : undefined,
        tab:       deriveTab(form.wallet, form.txType),
      };
      await addTransaction(payload);
      addToHistory(form.item);
      setStatus('success');
      setForm({ ...EMPTY, date: today() });
    } catch (err) {
      setStatus('error');
      setErrorMsg(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="form-page">
      <div className="form-card">
        <h1 className="form-title">New Transaction</h1>

        {status === 'success' && (
          <div className="toast toast-success" role="status">
            ✓ Transaction added successfully.
          </div>
        )}
        {status === 'error' && (
          <div className="toast toast-error" role="alert">
            ✕ {errorMsg}
          </div>
        )}

        <form onSubmit={handleSubmit}>
          <div className="form-grid">

            <div className="form-group">
              <label className="form-label" htmlFor="item">Item</label>
              <MerchantInput
                id="item" name="item"
                value={form.item}
                onChange={handleChange}
                history={merchantHistory}
                onSelect={(m) => setForm((f) => ({ ...f, item: m }))}
                placeholder="e.g. Pad Thai"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="amount">Amount</label>
              <div className="amount-wrap">
                <span className="amount-symbol">{currencySymbol(form.wallet)}</span>
                <input
                  className="form-input amount-input"
                  id="amount" name="amount" type="number" step="0.01" min="0"
                  value={form.amount} onChange={handleChange}
                  placeholder="0.00"
                  required
                />
              </div>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="date">Date</label>
              <input
                className="form-input"
                id="date" name="date" type="date"
                value={form.date} onChange={handleChange}
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="wallet">Wallet</label>
              <select
                className="form-select"
                id="wallet" name="wallet"
                value={form.wallet} onChange={handleChange}
              >
                {WALLETS.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="txType">Type</label>
              <select
                className="form-select"
                id="txType" name="txType"
                value={form.txType} onChange={handleChange}
              >
                {TX_TYPES.map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}
              </select>
            </div>

            {form.txType === 'transfer' && (
              <div className="form-group">
                <label className="form-label" htmlFor="toWallet">To Wallet</label>
                <select
                  className="form-select"
                  id="toWallet" name="toWallet"
                  value={form.toWallet} onChange={handleChange}
                  required
                >
                  <option value="">Select destination…</option>
                  {WALLETS.filter((w) => w.id !== form.wallet).map((w) => (
                    <option key={w.id} value={w.id}>{w.name}</option>
                  ))}
                </select>
              </div>
            )}

            {/* ac: add-transfer-target-amount — received amount shown only when the transfer crosses currencies */}
            {form.txType === 'transfer' && form.toWallet && walletCurrency(form.wallet) !== walletCurrency(form.toWallet) && (
              <div className="form-group">
                <label className="form-label" htmlFor="toAmount">Received amount</label>
                <div className="amount-wrap">
                  <span className="amount-symbol">{currencySymbol(form.toWallet)}</span>
                  <input
                    className="form-input amount-input"
                    id="toAmount" name="toAmount" type="number" step="0.01" min="0"
                    value={form.toAmount} onChange={handleChange}
                    placeholder="0.00"
                    required
                  />
                </div>
              </div>
            )}

            <div className="form-group">
              <label className="form-label" htmlFor="category">Category</label>
              <select
                className="form-select"
                id="category" name="category"
                value={form.category} onChange={handleChange}
              >
                {categoriesFor(form.txType).map((c) => <option key={c}>{c}</option>)}
              </select>
            </div>

            {/* ac: add-transaction-subcategory — optional subcategory, shown only when the category has any */}
            {subcategoriesFor(form.category).length > 0 && (
              <div className="form-group">
                <label className="form-label" htmlFor="subcategory">Subcategory</label>
                <select
                  className="form-select"
                  id="subcategory" name="subcategory"
                  value={form.subcategory} onChange={handleChange}
                >
                  <option value="">None</option>
                  {subcategoriesFor(form.category).map((s) => <option key={s}>{s}</option>)}
                </select>
              </div>
            )}

            <div className="form-group full">
              <label className="form-label" htmlFor="note">Note</label>
              <textarea
                className="form-textarea"
                id="note" name="note"
                value={form.note} onChange={handleChange}
                placeholder="Optional note…"
              />
            </div>

          </div>

          <div className="form-footer">
            <button className="btn-primary" type="submit" disabled={submitting}>
              {submitting ? 'Saving…' : 'Add Transaction'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
