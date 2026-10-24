// ac: edit-transfer-target-amount — the transactions table has a nullable to_amount column for cross-currency transfers 
// ac: edit-transfer-target-amount — the wallet_balances view credits the destination wallet with to_amount when set, otherwise with amount 
import { useEffect, useState } from 'react';
import { deleteTransaction, getInstallments } from '../api/client';
import { updateWithLink } from '../utils/installmentLink';
import { WALLETS, TX_TYPES, categoriesFor, subcategoriesFor, deriveTab, currencySymbol } from '../constants/transaction';
import DateInput from './DateInput';

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
    item:     row.item      ?? '',
    amount:   String(row.amount ?? ''),
    category: row.category  ?? 'Other',
    subcategory: row.subcategory ?? '',
    date:     row.date      ?? '',
    note:     row.note      ?? '',
    wallet:   row.wallet    ?? WALLETS[0]?.id ?? '',
    txType:   row.tx_type   ?? 'expense',
    toWallet: row.to_wallet ?? '',
    toAmount: row.to_amount != null ? String(row.to_amount) : '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [errorMsg, setErrorMsg]     = useState('');
  // ac: installment-link-edit — the link shown in the modal. The user can remove it or pick another plan.
  const [linkedId, setLinkedId]     = useState(row.installment_id ?? null);
  const [plans, setPlans]           = useState([]);

  useEffect(() => {
    let alive = true;
    Promise.resolve(getInstallments()).then((p) => { if (alive && Array.isArray(p)) setPlans(p); }).catch(() => {});
    return () => { alive = false; };
  }, []);

  function handleChange(e) {
    const { name, value } = e.target;
    if (name === 'txType') {
      setForm((f) => ({ ...f, txType: value, category: categoriesFor(value)[0], subcategory: '' }));
    } else if (name === 'category') {
      setForm((f) => ({ ...f, category: value, subcategory: '' }));
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
        item:      form.item,
        amount:    Number(form.amount),
        category:  form.category,
        subcategory: subcategoriesFor(form.category).length ? (form.subcategory || null) : null,
        date:      form.date,
        note:      form.note || null,
        wallet:    form.wallet,
        tx_type:   form.txType,
        to_wallet: form.txType === 'transfer' ? form.toWallet : null,
        // ac: edit-transfer-target-amount — saving persists the entered to_amount value to the server
        // ac: edit-transfer-target-amount — to_amount is cleared on save when type is not transfer or source and destination wallets share a currency
        to_amount: crossCurrency && form.toAmount !== '' ? Number(form.toAmount) : null,
        tab:       deriveTab(form.wallet, form.txType),
      };
      const originalId = row.installment_id ?? null;
      const result = await updateWithLink(row.id, payload, {
        installmentId: linkedId != null && linkedId !== originalId ? linkedId : null,
        unlink: originalId != null && linkedId == null,
        autoLink: originalId == null && linkedId == null && form.txType === 'expense',
      });
      onSaved({ ...row, ...payload, installment_id: result.linkedId === undefined ? originalId : result.linkedId });
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
                <label className="form-label" htmlFor="edit-item">Item</label>
                <input
                  className="form-input"
                  id="edit-item" name="item"
                  value={form.item}
                  onChange={handleChange}
                  autoComplete="off"
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
                <DateInput id="edit-date" name="date" value={form.date} onChange={handleChange} required />
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

              {/* ac: installment-link-edit — card expenses show their installment link, or a picker when unlinked */}
              {form.txType === 'expense' && (() => {
                const linkedPlan = plans.find((p) => p.id === linkedId);
                if (linkedId != null) {
                  return (
                    <div className="form-group installment-link">
                      <span className="form-label">Installment</span>
                      <div className="installment-link-row">
                        <span>
                          {linkedPlan
                            ? `Linked to ${linkedPlan.merchant}, step ${linkedPlan.current_step} of ${linkedPlan.total_installments}`
                            : 'Linked to an installment'}
                        </span>
                        <button type="button" className="btn-small" onClick={() => setLinkedId(null)}>Unlink</button>
                      </div>
                    </div>
                  );
                }
                const candidates = plans.filter((p) => p.wallet === form.wallet && p.status !== 'completed');
                if (!candidates.length) return null;
                return (
                  <div className="form-group">
                    <label className="form-label" htmlFor="edit-installment">Link to installment</label>
                    <select
                      className="form-select"
                      id="edit-installment"
                      value=""
                      onChange={(e) => setLinkedId(e.target.value ? Number(e.target.value) : null)}
                    >
                      <option value="">Not linked</option>
                      {candidates.map((p) => (
                        <option key={p.id} value={p.id}>{`${p.merchant}, step ${p.current_step} of ${p.total_installments}`}</option>
                      ))}
                    </select>
                  </div>
                );
              })()}

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

              {/* ac: add-transaction-subcategory — optional subcategory in the edit modal */}
              {subcategoriesFor(form.category).length > 0 && (
                <div className="form-group">
                  <label className="form-label" htmlFor="edit-subcategory">Subcategory</label>
                  <select
                    className="form-select"
                    id="edit-subcategory" name="subcategory"
                    value={form.subcategory} onChange={handleChange}
                  >
                    <option value="">None</option>
                    {subcategoriesFor(form.category).map((s) => <option key={s}>{s}</option>)}
                  </select>
                </div>
              )}

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
