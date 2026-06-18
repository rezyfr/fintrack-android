// ac: edit-transfer-target-amount — the transactions table has a nullable to_amount column for cross-currency transfers (see supabase/2026-05-31_to_amount.sql)
// ac: edit-transfer-target-amount — the wallet reconciliation and wallet balances credit the destination wallet with to_amount when set, otherwise with amount (see supabase/2026-05-31_to_amount.sql)
import { useState } from 'react';
import { updateTransaction, deleteTransaction } from '../api/supabase';
import { WALLETS, TX_TYPES, categoriesFor, deriveTab, currencySymbol } from '../constants/transaction';
import { useMerchantHistory } from '../hooks/useMerchantHistory';
import MerchantInput from './MerchantInput';

function walletCurrency(id) {
  return WALLETS.find((w) => w.id === id)?.currency;
}

// ac: edit-transfer-target-amount — the edit modal shows a Received Amount input only when type is transfer and source/destination currencies differ
function isCrossCurrencyTransfer(txType, wallet, toWallet) {
  if (txType !== 'transfer' || !toWallet) return false;
  const a = walletCurrency(wallet);
  const b = walletCurrency(toWallet);
  return Boolean(a && b && a !== b);
}

export default function EditTransactionModal({ row, onClose, onSaved, onDeleted }) {
  const [form, setForm] = useState({
    merchant: row.merchant  ?? '',
    item:     row.item      ?? '',
    amount:   String(row.amount ?? ''),
    category: row.category  ?? 'Other',
    date:     row.date      ?? '',
    note:     row.note      ?? '',
    wallet:   row.wallet    ?? 'BBL',
    txType:   row.tx_type   ?? 'expense',
    toWallet: row.to_wallet ?? '',
    toAmount: row.to_amount != null ? String(row.to_amount) : '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [errorMsg, setErrorMsg]     = useState('');
  const [merchantHistory, addToHistory] = useMerchantHistory();

  function handleChange(e) {
    const { name, value } = e.target;
    if (name === 'txType') {
      setForm((f) => ({ ...f, txType: value, category: categoriesFor(value)[0] }));
    } else {
      setForm((f) => ({ ...f, [name]: value }));
    }
  }

  async function handleSave(e) {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    try {
      const crossCurrency = isCrossCurrencyTransfer(form.txType, form.wallet, form.toWallet);
      const payload = {
        merchant:  form.merchant,
        item:      form.item,
        amount:    Number(form.amount),
        category:  form.category,
        date:      form.date,
        note:      form.note || null,
        wallet:    form.wallet,
        tx_type:   form.txType,
        to_wallet: form.txType === 'transfer' ? form.toWallet : null,
        // ac: edit-transfer-target-amount — saving persists the entered to_amount value to Supabase
        // ac: edit-transfer-target-amount — to_amount is cleared on save when type is not transfer or source and destination wallets share a currency
        to_amount: crossCurrency && form.toAmount !== '' ? Number(form.toAmount) : null,
        tab:       deriveTab(form.wallet, form.txType),
      };
      await updateTransaction(row.id, payload);
      addToHistory(form.merchant);
      onSaved({ ...row, ...payload });
    } catch (err) {
      setErrorMsg(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDeleteConfirm() {
    setSubmitting(true);
    setErrorMsg('');
    try {
      await deleteTransaction(row.id);
      onDeleted(row.id);
    } catch (err) {
      setErrorMsg(err.message);
      setConfirming(false);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="modal-overlay" role="dialog" aria-modal="true">
      <div className="modal">
        <div className="modal-header">
          <h2 className="modal-title">Edit Transaction</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">✕</button>
        </div>

        {errorMsg && (
          <div className="toast toast-error" role="alert">{errorMsg}</div>
        )}

        {confirming ? (
          <div className="modal-confirm">
            <p>Delete this transaction? This cannot be undone.</p>
            <div className="modal-confirm-actions">
              <button className="btn-danger" onClick={handleDeleteConfirm} disabled={submitting}>
                {submitting ? 'Deleting…' : 'Yes, delete'}
              </button>
              <button className="btn-secondary" onClick={() => setConfirming(false)} disabled={submitting}>
                Cancel
              </button>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSave}>
            <div className="form-grid">

              <div className="form-group">
                <label className="form-label" htmlFor="edit-merchant">Merchant</label>
                <MerchantInput
                  id="edit-merchant" name="merchant"
                  value={form.merchant}
                  onChange={handleChange}
                  history={merchantHistory}
                  onSelect={(m) => setForm((f) => ({ ...f, merchant: m }))}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-item">Item</label>
                <input
                  className="form-input"
                  id="edit-item" name="item"
                  value={form.item} onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-amount">Amount</label>
                <div className="amount-wrap">
                  <span className="amount-symbol">{currencySymbol(form.wallet)}</span>
                  <input
                    className="form-input amount-input"
                    id="edit-amount" name="amount" type="number" step="0.01" min="0"
                    value={form.amount} onChange={handleChange}
                    required
                  />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-date">Date</label>
                <input
                  className="form-input"
                  id="edit-date" name="date" type="date"
                  value={form.date} onChange={handleChange}
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-wallet">Wallet</label>
                <select
                  className="form-select"
                  id="edit-wallet" name="wallet"
                  value={form.wallet} onChange={handleChange}
                >
                  {WALLETS.map((w) => <option key={w.id} value={w.id}>{w.name}</option>)}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-txType">Type</label>
                <select
                  className="form-select"
                  id="edit-txType" name="txType"
                  value={form.txType} onChange={handleChange}
                >
                  {TX_TYPES.map((t) => <option key={t.id} value={t.id}>{t.label}</option>)}
                </select>
              </div>

              {form.txType === 'transfer' && (
                <div className="form-group">
                  <label className="form-label" htmlFor="edit-toWallet">To Wallet</label>
                  <select
                    className="form-select"
                    id="edit-toWallet" name="toWallet"
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

              {/* ac: edit-transfer-target-amount — Received Amount input is shown only when type is transfer and source/destination currencies differ */}
              {isCrossCurrencyTransfer(form.txType, form.wallet, form.toWallet) && (
                <div className="form-group">
                  <label className="form-label" htmlFor="edit-toAmount">Received Amount</label>
                  <div className="amount-wrap">
                    <span className="amount-symbol">{currencySymbol(form.toWallet)}</span>
                    <input
                      className="form-input amount-input"
                      id="edit-toAmount" name="toAmount" type="number" step="0.01" min="0"
                      value={form.toAmount} onChange={handleChange}
                      placeholder={`amount in ${walletCurrency(form.toWallet)}`}
                    />
                  </div>
                </div>
              )}

              <div className="form-group">
                <label className="form-label" htmlFor="edit-category">Category</label>
                <select
                  className="form-select"
                  id="edit-category" name="category"
                  value={form.category} onChange={handleChange}
                >
                  {categoriesFor(form.txType).map((c) => <option key={c}>{c}</option>)}
                </select>
              </div>

              <div className="form-group full">
                <label className="form-label" htmlFor="edit-note">Note</label>
                <textarea
                  className="form-textarea"
                  id="edit-note" name="note"
                  value={form.note} onChange={handleChange}
                />
              </div>

            </div>

            <div className="modal-footer">
              <button className="btn-danger-outline" type="button" onClick={() => setConfirming(true)} disabled={submitting}>
                Delete
              </button>
              <div className="modal-footer-right">
                <button className="btn-secondary" type="button" onClick={onClose} disabled={submitting}>
                  Cancel
                </button>
                <button className="btn-primary" type="submit" disabled={submitting}>
                  {submitting ? 'Saving…' : 'Save'}
                </button>
              </div>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
