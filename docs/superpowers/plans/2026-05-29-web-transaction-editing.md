# Web Transaction Editing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an edit modal and delete capability to every transaction row in the web app, so users can correct any field (especially `wallet` after the backfill migration).

**Architecture:** Four tasks in order. First, extract shared constants from `AddTransactionForm` into `src/constants/transaction.js` so both the form and the new modal can import them. Second, add `updateTransaction` and `deleteTransaction` to the API layer. Third, build `EditTransactionModal` with TDD. Fourth, wire the modal into `TransactionList` with a pencil button per row.

**Tech Stack:** React 18 + Vite, Vitest + Testing Library, Supabase PostgREST

---

## File Map

| File | Change |
|------|--------|
| `web/src/constants/transaction.js` | **Create** — shared WALLETS, TX_TYPES, CATEGORIES, CHANNELS, deriveTab, currencySymbol |
| `web/src/components/AddTransactionForm.jsx` | **Modify** — import from constants instead of defining inline |
| `web/src/api/supabase.js` | **Modify** — add updateTransaction, deleteTransaction |
| `web/src/components/EditTransactionModal.jsx` | **Create** — edit/delete modal |
| `web/src/components/EditTransactionModal.test.jsx` | **Create** — TDD tests |
| `web/src/components/TransactionList.jsx` | **Modify** — add editingRow state, pencil button, modal mount |
| `web/src/components/TransactionList.test.jsx` | **Modify** — add edit button test |

---

## Task 1: Extract shared constants

**Files:**
- Create: `web/src/constants/transaction.js`
- Modify: `web/src/components/AddTransactionForm.jsx`

- [ ] **Step 1: Create `web/src/constants/transaction.js`**

```javascript
export const WALLETS = [
  { id: 'BBL',        name: 'Bangkok Bank',        currency: 'THB' },
  { id: 'BCA',        name: 'BCA Account',          currency: 'IDR' },
  { id: 'MANDIRI',    name: 'Mandiri Account',       currency: 'IDR' },
  { id: 'MANDIRI_CC', name: 'Mandiri Credit Card',   currency: 'IDR' },
  { id: 'INVESTMENT', name: 'Investments',           currency: 'IDR' },
];

export const TX_TYPES = [
  { id: 'expense',    label: 'Expense'    },
  { id: 'income',     label: 'Income'     },
  { id: 'transfer',   label: 'Transfer'   },
  { id: 'investment', label: 'Investment' },
];

export const CATEGORIES = [
  'Bills', 'Subscriptions', 'Entertainment', 'Food & Drink',
  'Groceries', 'Health & Wellbeing', 'Other', 'Shopping',
  'Transport', 'Travel', 'Business', 'Gifts',
];

export const CHANNELS = ['BillPayment', 'eWallet', 'PromptPay', 'BankTransfer', 'Manual', 'Unknown'];

export function currencySymbol(walletId) {
  return WALLETS.find(w => w.id === walletId)?.currency === 'THB' ? '฿' : 'Rp';
}

export function deriveTab(walletId, txType) {
  const isThb = WALLETS.find(w => w.id === walletId)?.currency === 'THB';
  if (txType === 'income') return isThb ? 'INCOME' : 'IDR_INCOME';
  return isThb ? 'EXPENSES' : 'IDR_EXPENSES';
}
```

- [ ] **Step 2: Update the top of `web/src/components/AddTransactionForm.jsx`**

Replace the first 40 lines (the local constant/function definitions) with these two import lines. Everything below the `const EMPTY = ...` line stays untouched.

```jsx
import { useState } from 'react';
import { addTransaction } from '../api/supabase';
import { WALLETS, TX_TYPES, CATEGORIES, CHANNELS, deriveTab, currencySymbol } from '../constants/transaction';

function today() {
  return new Date().toISOString().slice(0, 10);
}

const EMPTY = {
  merchant: '', item: '', amount: '',
  category: 'Food & Drink', channel: 'eWallet',
  wallet: 'BBL', txType: 'expense', toWallet: '', date: '', note: '',
};
```

Delete the now-redundant local `WALLETS`, `TX_TYPES`, `CATEGORIES`, `CHANNELS`, `currencySymbol`, and `deriveTab` definitions that previously followed the imports. The rest of the component (from `export default function AddTransactionForm()` onward) is unchanged.

- [ ] **Step 3: Run existing AddTransactionForm tests — expect all pass**

```bash
cd web && npx vitest run src/components/AddTransactionForm.test.jsx
```

Expected: 7/7 tests pass. If any fail, the import path or extraction is wrong — fix before continuing.

- [ ] **Step 4: Commit**

```bash
git add web/src/constants/transaction.js web/src/components/AddTransactionForm.jsx
git commit -m "refactor(web): extract shared transaction constants to src/constants/transaction.js"
```

---

## Task 2: API layer — updateTransaction and deleteTransaction

**Files:**
- Modify: `web/src/api/supabase.js`

- [ ] **Step 1: Append two functions to `web/src/api/supabase.js`**

Add at the end of the file:

```javascript
export async function updateTransaction(id, patch) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions?id=eq.${id}`, {
    method: 'PATCH',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify(patch),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

export async function deleteTransaction(id) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions?id=eq.${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}
```

- [ ] **Step 2: Run full test suite — expect all pass**

```bash
cd web && npx vitest run
```

Expected: all tests pass (no regressions from the append).

- [ ] **Step 3: Commit**

```bash
git add web/src/api/supabase.js
git commit -m "feat(web): add updateTransaction and deleteTransaction to Supabase API layer"
```

---

## Task 3: EditTransactionModal (TDD)

**Files:**
- Create: `web/src/components/EditTransactionModal.test.jsx`
- Create: `web/src/components/EditTransactionModal.jsx`

- [ ] **Step 1: Write the failing tests**

Create `web/src/components/EditTransactionModal.test.jsx`:

```jsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import EditTransactionModal from './EditTransactionModal';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const MOCK_ROW = {
  id: 42,
  merchant: 'Grab',
  item: 'Food delivery',
  amount: 150,
  category: 'Food & Drink',
  channel: 'eWallet',
  date: '2026-05-01',
  wallet: 'BBL',
  tx_type: 'expense',
  to_wallet: null,
  tab: 'EXPENSES',
  note: 'lunch',
};

beforeEach(() => {
  api.updateTransaction.mockResolvedValue(undefined);
  api.deleteTransaction.mockResolvedValue(undefined);
});
afterEach(() => vi.resetAllMocks());

it('pre-populates merchant from row', () => {
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  expect(screen.getByLabelText('Merchant')).toHaveValue('Grab');
});

it('pre-populates wallet from row', () => {
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  expect(screen.getByLabelText('Wallet')).toHaveValue('BBL');
});

it('defaults wallet to BBL when row.wallet is null', () => {
  render(<EditTransactionModal row={{ ...MOCK_ROW, wallet: null }} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  expect(screen.getByLabelText('Wallet')).toHaveValue('BBL');
});

it('saves with correct payload and calls onSaved', async () => {
  const onSaved = vi.fn();
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} />);
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() => {
    expect(api.updateTransaction).toHaveBeenCalledWith(42, expect.objectContaining({
      wallet: 'BBL',
      tx_type: 'expense',
      tab: 'EXPENSES',
    }));
    expect(onSaved).toHaveBeenCalled();
  });
});

it('shows delete confirmation when Delete is clicked', async () => {
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  expect(screen.getByText(/cannot be undone/i)).toBeInTheDocument();
  expect(api.deleteTransaction).not.toHaveBeenCalled();
});

it('calls deleteTransaction and onDeleted after confirmation', async () => {
  const onDeleted = vi.fn();
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={onDeleted} />);
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  await user.click(screen.getByRole('button', { name: 'Yes, delete' }));
  await waitFor(() => {
    expect(api.deleteTransaction).toHaveBeenCalledWith(42);
    expect(onDeleted).toHaveBeenCalledWith(42);
  });
});

it('cancelling delete confirmation returns to form', async () => {
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
  expect(screen.queryByText(/cannot be undone/i)).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
});
```

- [ ] **Step 2: Run tests — expect all 6 fail**

```bash
cd web && npx vitest run src/components/EditTransactionModal.test.jsx
```

Expected: 6 failures (component doesn't exist yet).

- [ ] **Step 3: Create `web/src/components/EditTransactionModal.jsx`**

```jsx
import { useState } from 'react';
import { updateTransaction, deleteTransaction } from '../api/supabase';
import { WALLETS, TX_TYPES, CATEGORIES, CHANNELS, deriveTab, currencySymbol } from '../constants/transaction';

export default function EditTransactionModal({ row, onClose, onSaved, onDeleted }) {
  const [form, setForm] = useState({
    merchant: row.merchant  ?? '',
    item:     row.item      ?? '',
    amount:   String(row.amount ?? ''),
    category: row.category  ?? 'Other',
    channel:  row.channel   ?? 'Manual',
    date:     row.date      ?? '',
    note:     row.note      ?? '',
    wallet:   row.wallet    ?? 'BBL',
    txType:   row.tx_type   ?? 'expense',
    toWallet: row.to_wallet ?? '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [errorMsg, setErrorMsg]     = useState('');

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  async function handleSave(e) {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    try {
      const payload = {
        merchant:  form.merchant,
        item:      form.item,
        amount:    Number(form.amount),
        category:  form.category,
        channel:   form.channel,
        date:      form.date,
        note:      form.note || null,
        wallet:    form.wallet,
        tx_type:   form.txType,
        to_wallet: form.txType === 'transfer' ? form.toWallet : null,
        tab:       deriveTab(form.wallet, form.txType),
      };
      await updateTransaction(row.id, payload);
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
                <input
                  className="form-input"
                  id="edit-merchant" name="merchant"
                  value={form.merchant} onChange={handleChange}
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

              <div className="form-group">
                <label className="form-label" htmlFor="edit-category">Category</label>
                <select
                  className="form-select"
                  id="edit-category" name="category"
                  value={form.category} onChange={handleChange}
                >
                  {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
                </select>
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="edit-channel">Channel</label>
                <select
                  className="form-select"
                  id="edit-channel" name="channel"
                  value={form.channel} onChange={handleChange}
                >
                  {CHANNELS.map((c) => <option key={c}>{c}</option>)}
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
```

- [ ] **Step 4: Run tests — expect all 6 pass**

```bash
cd web && npx vitest run src/components/EditTransactionModal.test.jsx
```

Expected: 6/6 pass.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/EditTransactionModal.jsx web/src/components/EditTransactionModal.test.jsx
git commit -m "feat(web): add EditTransactionModal with save and delete confirmation"
```

---

## Task 4: TransactionList integration

**Files:**
- Modify: `web/src/components/TransactionList.jsx`
- Modify: `web/src/components/TransactionList.test.jsx`

- [ ] **Step 1: Add the edit button test first**

In `web/src/components/TransactionList.test.jsx`, append after the last `it(...)` block:

```jsx
it('shows an edit button for each loaded row', async () => {
  render(<TransactionList />);
  await screen.findByText('Grab');
  expect(screen.getByRole('button', { name: /edit grab/i })).toBeInTheDocument();
});
```

- [ ] **Step 2: Run the new test — expect it to fail**

```bash
cd web && npx vitest run src/components/TransactionList.test.jsx
```

Expected: 5 pass, 1 fail (edit button not in DOM yet).

- [ ] **Step 3: Update TransactionList.jsx**

**3a.** Add the import at the top of `web/src/components/TransactionList.jsx` (after the existing `getTransactions` import):

```jsx
import EditTransactionModal from './EditTransactionModal';
```

**3b.** Add `editingRow` state alongside the existing state declarations:

```jsx
const [editingRow, setEditingRow] = useState(null);
```

**3c.** In the `<thead>` row, add a 9th `<th>` at the end (after the Note `<th>`):

```jsx
<th />
```

**3d.** In each data `<tr>` (the `rows.map` block), add a 9th `<td>` at the end (after the note `<td>`):

```jsx
<td>
  <button
    className="edit-btn"
    aria-label={`Edit ${row.merchant}`}
    onClick={() => setEditingRow(row)}
  >
    ✎
  </button>
</td>
```

**3e.** In `SkeletonRows`, add a 9th skeleton `<td>` at the end of the `<tr>`:

```jsx
<td />
```

**3f.** Mount the modal just before the closing `</div>` of the outer `<div className="page">`:

```jsx
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
```

- [ ] **Step 4: Run all web tests — expect all pass**

```bash
cd web && npx vitest run
```

Expected: all tests pass (6 in TransactionList, 6 in EditTransactionModal, 7 in AddTransactionForm, 3 in WalletBalances).

- [ ] **Step 5: Commit**

```bash
git add web/src/components/TransactionList.jsx web/src/components/TransactionList.test.jsx
git commit -m "feat(web): add edit icon per row and wire EditTransactionModal into TransactionList"
```

---

## Verification

1. Open the web app — each transaction row has a ✎ icon in the rightmost column
2. Click ✎ on a BBL row that was backfilled incorrectly — change Wallet to MANDIRI, click Save — row updates inline with Mandiri chip, no page reload
3. Click ✎ on any row — click Delete — confirmation text "cannot be undone" appears, no network call yet
4. Click "Yes, delete" — row disappears from list
5. Click ✎ — click Delete — click Cancel — confirmation gone, form returns, row still in list
6. Run `cd web && npx vitest run` — all tests pass
