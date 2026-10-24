import { useState, useEffect } from 'react';
import { useBook, getBook, bookHoldsWallet } from '../utils/book';
import { addWithLink } from '../utils/installmentLink';
import { WALLETS, TX_TYPES, categoriesFor, subcategoriesFor, deriveTab, currencySymbol, walletCurrency } from '../constants/transaction';
import DateInput from './DateInput';

function today() {
  return new Date().toISOString().slice(0, 10);
}

// ac: web-manage-accounts — the first wallet is the first account in the current list, not a fixed id
const emptyForm = () => ({
  item: '', amount: '',
  category: 'Food & Drink',
  subcategory: '',
  wallet: firstBookWallet(getBook()), txType: 'expense', toWallet: '', toAmount: '', date: '', note: '',
});

// ac: web-currency-books — the first account of the current book. Combined takes the first account.
function firstBookWallet(book) {
  return (WALLETS.find(w => bookHoldsWallet(book, w.id)) ?? WALLETS[0])?.id ?? '';
}

export default function AddTransactionForm() {
  const [form, setForm]             = useState(() => ({ ...emptyForm(), date: today() }));
  // ac: web-currency-books — the wallet list holds the book's accounts only. The To list keeps every
  // account, so a transfer can still move money between the two books.
  const book = useBook();
  // The account list can load after this form opens, or change in Manage accounts, so the wallet is
  // checked again when the list changes.
  const walletIds = WALLETS.map(w => w.id).join(',');
  useEffect(() => {
    setForm(f => (f.wallet && WALLETS.some(w => w.id === f.wallet) && bookHoldsWallet(book, f.wallet)
      ? f
      : { ...f, wallet: firstBookWallet(book), toWallet: '', toAmount: '' }));
  }, [book, walletIds]);
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus]         = useState(null);
  const [errorMsg, setErrorMsg]     = useState('');
  // ac: installment-auto-link — the installment the saved transaction was linked to, shown in the success message
  const [linked, setLinked]         = useState(null);

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
      const { match, stepSent } = await addWithLink(payload);
      setLinked(match ? { merchant: match.installment.merchant, step: match.newStep, total: match.installment.total_installments, stepSent } : null);
      setStatus('success');
      setForm({ ...emptyForm(), date: today() });
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
            {linked && ` Linked to ${linked.merchant}, step ${linked.step} of ${linked.total}.`}
            {linked && !linked.stepSent && ' The step update will be retried.'}
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
              <input
                className="form-input"
                id="item" name="item"
                value={form.item}
                onChange={handleChange}
                placeholder="e.g. Pad Thai"
                autoComplete="off"
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
              <DateInput id="date" name="date" value={form.date} onChange={handleChange} required />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="wallet">Wallet</label>
              <select
                className="form-select"
                id="wallet" name="wallet"
                value={form.wallet} onChange={handleChange}
                required
              >
                {WALLETS.filter((w) => bookHoldsWallet(book, w.id)).map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
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
