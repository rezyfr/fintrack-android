# Multi-Wallet Financial Tracking Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `wallet` and `tx_type` dimensions to every transaction so account balances are accurately tracked and CC bill payments / transfers are never double-counted as expenses.

**Architecture:** Three layers in dependency order. Supabase first: new `wallets` table, three new columns on `transactions`, a `wallet_balances` view for running balances. Web second: replace the `tab` dropdown in the form with wallet + tx_type selectors, update the list to filter by tx_type, add a Balances page. Android third: add the three fields to `TransactionEntity` (DB v3→v4), thread them through `SheetsRow` → `SupabaseSyncerImpl`, and replace the account/type toggles in `AddScreen`. The `tab` column is preserved on all existing rows and auto-derived for new ones.

**Tech Stack:** Supabase (PostgreSQL + PostgREST), Supabase MCP (`execute_sql`), React 18 + Vite, Vitest + Testing Library, Android Room + Compose + Hilt, Kotlin

---

## Task 1: Supabase — wallets table, transaction columns, seed, backfill

**Files:** Applied via Supabase MCP `execute_sql` — no disk files change.

- [ ] **Step 1: Create the wallets table**

```sql
CREATE TABLE IF NOT EXISTS wallets (
  id               TEXT PRIMARY KEY,
  name             TEXT NOT NULL,
  currency         TEXT NOT NULL,
  type             TEXT NOT NULL,
  starting_balance NUMERIC NOT NULL DEFAULT 0,
  start_date       DATE NOT NULL,
  created_at       TIMESTAMPTZ DEFAULT now()
);
```

- [ ] **Step 2: Add three columns to transactions**

```sql
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS wallet   TEXT;
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS tx_type  TEXT NOT NULL DEFAULT 'expense';
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS to_wallet TEXT;
```

- [ ] **Step 3: Seed the five wallets**

```sql
INSERT INTO wallets (id, name, currency, type, starting_balance, start_date) VALUES
  ('BBL',        'Bangkok Bank',       'THB', 'checking',   0, '2024-01-01'),
  ('BCA',        'BCA Account',        'IDR', 'checking',   0, '2024-01-01'),
  ('MANDIRI',    'Mandiri Account',    'IDR', 'checking',   0, '2024-01-01'),
  ('MANDIRI_CC', 'Mandiri Credit Card','IDR', 'credit',     0, '2024-01-01'),
  ('INVESTMENT', 'Investments',        'IDR', 'investment', 0, '2024-01-01')
ON CONFLICT (id) DO NOTHING;
```

Update `starting_balance` and `start_date` for each wallet to match your real account balances as of the date you began tracking.

- [ ] **Step 4: Backfill existing transactions**

```sql
UPDATE transactions
SET
  wallet  = CASE tab
    WHEN 'EXPENSES'     THEN 'BBL'
    WHEN 'INCOME'       THEN 'BBL'
    WHEN 'IDR_EXPENSES' THEN 'MANDIRI'
    WHEN 'IDR_INCOME'   THEN 'MANDIRI'
    ELSE 'MANDIRI'
  END,
  tx_type = CASE tab
    WHEN 'EXPENSES'     THEN 'expense'
    WHEN 'IDR_EXPENSES' THEN 'expense'
    WHEN 'INCOME'       THEN 'income'
    WHEN 'IDR_INCOME'   THEN 'income'
    ELSE 'expense'
  END
WHERE wallet IS NULL;
```

- [ ] **Step 5: Verify**

```sql
SELECT wallet, tx_type, COUNT(*) FROM transactions GROUP BY wallet, tx_type ORDER BY wallet;
SELECT * FROM wallets;
```

Expected: all rows have a non-null wallet; 5 wallet rows exist.

- [ ] **Step 6: Commit**

```bash
git commit -m "feat(supabase): add wallets table and wallet/tx_type columns to transactions"
```

---

## Task 2: Supabase — wallet_balances view

**Files:** Applied via Supabase MCP `execute_sql`.

- [ ] **Step 1: Create the view**

```sql
CREATE OR REPLACE VIEW wallet_balances AS
SELECT
  w.id,
  w.name,
  w.currency,
  w.type,
  w.starting_balance + COALESCE(
    SUM(
      CASE w.type
        WHEN 'credit' THEN
          -- CC: expenses and cash-advances increase debt; payments reduce it
          CASE
            WHEN t.tx_type IN ('expense', 'transfer') AND t.wallet = w.id THEN  t.amount
            WHEN t.tx_type = 'transfer'               AND t.to_wallet = w.id THEN -t.amount
            ELSE 0
          END
        ELSE
          -- Checking / investment: income and transfers-in are positive
          CASE
            WHEN t.tx_type = 'income'                                       AND t.wallet    = w.id THEN  t.amount
            WHEN t.tx_type IN ('expense', 'transfer', 'investment')         AND t.wallet    = w.id THEN -t.amount
            WHEN t.tx_type IN ('transfer', 'investment')                    AND t.to_wallet = w.id THEN  t.amount
            ELSE 0
          END
      END
    ), 0
  ) AS balance
FROM wallets w
LEFT JOIN transactions t ON t.wallet = w.id OR t.to_wallet = w.id
GROUP BY w.id, w.name, w.currency, w.type, w.starting_balance;
```

- [ ] **Step 2: Grant SELECT on the view to the anon role**

```sql
GRANT SELECT ON wallet_balances TO anon;
```

Without this, the PostgREST fetch from the web app will return a 401/403.

- [ ] **Step 3: Verify with a sample query**

```sql
SELECT id, name, currency, type, balance FROM wallet_balances ORDER BY id;
```

Expected: 5 rows, balances reflect starting_balance (0 until you update them).

- [ ] **Step 4: Commit**

```bash
git commit -m "feat(supabase): add wallet_balances view for running account balances"
```

---

## Task 3: Web — supabase.js API layer

**Files:**
- Modify: `web/src/api/supabase.js`

- [ ] **Step 1: Update `getTransactions` to accept wallet and txType filters; add `getWalletBalances`**

Replace the entire file with:

```javascript
function authHeaders() {
  const key = import.meta.env.VITE_SUPABASE_ANON_KEY;
  return {
    apikey: key,
    Authorization: `Bearer ${key}`,
    'Content-Type': 'application/json',
  };
}

export async function getTransactions({ tab = null, wallet = null, txType = null, month = null } = {}) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const params = new URLSearchParams();
  params.append('order', 'date.desc');
  params.append('limit', '200');
  if (tab)    params.append('tab',     `eq.${tab}`);
  if (wallet) params.append('wallet',  `eq.${wallet}`);
  if (txType) params.append('tx_type', `eq.${txType}`);
  if (month) {
    const [year, mon] = month.split('-').map(Number);
    const lastDay = new Date(year, mon, 0).getDate();
    const pad = (n) => String(n).padStart(2, '0');
    params.append('date', `gte.${month}-01`);
    params.append('date', `lte.${month}-${pad(lastDay)}`);
  }
  const res = await fetch(`${url}/rest/v1/transactions?${params}`, {
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
  return res.json();
}

export async function addTransaction(row) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions`, {
    method: 'POST',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify(row),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

export async function getWalletBalances() {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/wallet_balances?order=id.asc`, {
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
  return res.json();
}
```

- [ ] **Step 2: Commit**

```bash
git add web/src/api/supabase.js
git commit -m "feat(web): update supabase API layer for wallet/tx_type filters and wallet balances"
```

---

## Task 4: Web — AddTransactionForm (TDD)

**Files:**
- Modify: `web/src/components/AddTransactionForm.jsx`
- Create: `web/src/components/AddTransactionForm.test.jsx`

- [ ] **Step 1: Write the failing tests**

Create `web/src/components/AddTransactionForm.test.jsx`:

```jsx
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import AddTransactionForm from './AddTransactionForm';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

beforeEach(() => { api.addTransaction.mockResolvedValue(undefined); });
afterEach(() => vi.resetAllMocks());

it('shows THB symbol for BBL wallet by default', () => {
  render(<AddTransactionForm />);
  expect(screen.getByText('฿')).toBeInTheDocument();
});

it('shows Rp symbol when switching to an IDR wallet', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.selectOptions(screen.getByLabelText('Wallet'), 'BCA');
  expect(screen.getByText('Rp')).toBeInTheDocument();
});

it('shows To Wallet field when tx_type is transfer', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.selectOptions(screen.getByLabelText('Type'), 'transfer');
  expect(screen.getByLabelText('To Wallet')).toBeInTheDocument();
});

it('hides To Wallet field for non-transfer types', () => {
  render(<AddTransactionForm />);
  expect(screen.queryByLabelText('To Wallet')).not.toBeInTheDocument();
});

it('submits with wallet, tx_type, and derived tab', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.type(screen.getByLabelText('Merchant'), 'Grab');
  await user.type(screen.getByLabelText('Item'), 'Food');
  await user.type(screen.getByLabelText('Amount'), '150');
  await user.click(screen.getByRole('button', { name: 'Add Transaction' }));
  await waitFor(() => {
    expect(api.addTransaction).toHaveBeenCalledWith(expect.objectContaining({
      wallet: 'BBL',
      tx_type: 'expense',
      tab: 'EXPENSES',
    }));
  });
});
```

- [ ] **Step 2: Run tests — expect failures**

```bash
cd web && npx vitest run src/components/AddTransactionForm.test.jsx
```

Expected: all 5 tests fail (component doesn't have wallet/type selectors yet).

- [ ] **Step 3: Rewrite AddTransactionForm to pass the tests**

Replace `web/src/components/AddTransactionForm.jsx`:

```jsx
import { useState } from 'react';
import { addTransaction } from '../api/supabase';

const CATEGORIES = [
  'Bills', 'Subscriptions', 'Entertainment', 'Food & Drink',
  'Groceries', 'Health & Wellbeing', 'Other', 'Shopping',
  'Transport', 'Travel', 'Business', 'Gifts',
];
const CHANNELS = ['BillPayment', 'eWallet', 'PromptPay', 'BankTransfer', 'Manual', 'Unknown'];

const WALLETS = [
  { id: 'BBL',        name: 'Bangkok Bank',        currency: 'THB' },
  { id: 'BCA',        name: 'BCA Account',          currency: 'IDR' },
  { id: 'MANDIRI',    name: 'Mandiri Account',       currency: 'IDR' },
  { id: 'MANDIRI_CC', name: 'Mandiri Credit Card',   currency: 'IDR' },
  { id: 'INVESTMENT', name: 'Investments',           currency: 'IDR' },
];

const TX_TYPES = [
  { id: 'expense',    label: 'Expense'    },
  { id: 'income',     label: 'Income'     },
  { id: 'transfer',   label: 'Transfer'   },
  { id: 'investment', label: 'Investment' },
];

function today() {
  return new Date().toISOString().slice(0, 10);
}

function currencySymbol(walletId) {
  return WALLETS.find(w => w.id === walletId)?.currency === 'THB' ? '฿' : 'Rp';
}

function deriveTab(walletId, txType) {
  const isThb = WALLETS.find(w => w.id === walletId)?.currency === 'THB';
  if (txType === 'income') return isThb ? 'INCOME'    : 'IDR_INCOME';
  return isThb ? 'EXPENSES' : 'IDR_EXPENSES';
}

const EMPTY = {
  merchant: '', item: '', amount: '',
  category: 'Food & Drink', channel: 'eWallet',
  wallet: 'BBL', txType: 'expense', toWallet: '', date: '', note: '',
};

export default function AddTransactionForm() {
  const [form, setForm]             = useState(() => ({ ...EMPTY, date: today() }));
  const [submitting, setSubmitting] = useState(false);
  const [status, setStatus]         = useState(null);
  const [errorMsg, setErrorMsg]     = useState('');

  function handleChange(e) {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSubmitting(true);
    setStatus(null);
    setErrorMsg('');
    try {
      const payload = {
        merchant:  form.merchant,
        item:      form.item,
        amount:    Number(form.amount),
        category:  form.category,
        channel:   form.channel,
        date:      form.date,
        note:      form.note || undefined,
        wallet:    form.wallet,
        tx_type:   form.txType,
        to_wallet: form.txType === 'transfer' ? form.toWallet : undefined,
        tab:       deriveTab(form.wallet, form.txType),
      };
      await addTransaction(payload);
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
              <label className="form-label" htmlFor="merchant">Merchant</label>
              <input
                className="form-input"
                id="merchant" name="merchant"
                value={form.merchant} onChange={handleChange}
                placeholder="e.g. LINE MAN"
                required
              />
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="item">Item</label>
              <input
                className="form-input"
                id="item" name="item"
                value={form.item} onChange={handleChange}
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

            <div className="form-group">
              <label className="form-label" htmlFor="category">Category</label>
              <select
                className="form-select"
                id="category" name="category"
                value={form.category} onChange={handleChange}
              >
                {CATEGORIES.map((c) => <option key={c}>{c}</option>)}
              </select>
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="channel">Channel</label>
              <select
                className="form-select"
                id="channel" name="channel"
                value={form.channel} onChange={handleChange}
              >
                {CHANNELS.map((c) => <option key={c}>{c}</option>)}
              </select>
            </div>

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
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
cd web && npx vitest run src/components/AddTransactionForm.test.jsx
```

Expected: 5 tests pass.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/AddTransactionForm.jsx web/src/components/AddTransactionForm.test.jsx
git commit -m "feat(web): replace tab picker with wallet + tx_type selectors in AddTransactionForm"
```

---

## Task 5: Web — TransactionList (TDD)

**Files:**
- Modify: `web/src/components/TransactionList.jsx`
- Modify: `web/src/components/TransactionList.test.jsx`

- [ ] **Step 1: Update the test file first**

Replace `web/src/components/TransactionList.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import TransactionList from './TransactionList';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const MOCK_ROWS = [
  {
    id: 1,
    date: '2026-05-01',
    merchant: 'Grab',
    item: 'Food delivery',
    amount: 150,
    category: 'Food & Drink',
    channel: 'eWallet',
    tab: 'EXPENSES',
    wallet: 'BBL',
    tx_type: 'expense',
    note: 'lunch',
  },
];

beforeEach(() => {
  api.getTransactions.mockResolvedValue(MOCK_ROWS);
});

afterEach(() => {
  vi.resetAllMocks();
});

it('shows transaction rows after load', async () => {
  render(<TransactionList />);
  expect(await screen.findByText('Grab')).toBeInTheDocument();
  expect(screen.getByText('Food delivery')).toBeInTheDocument();
  expect(screen.getAllByText('฿150.00').length).toBeGreaterThan(0);
});

it('shows wallet chip for each row', async () => {
  render(<TransactionList />);
  await screen.findByText('Grab');
  expect(screen.getByText('BBL')).toBeInTheDocument();
});

it('shows error message on fetch failure', async () => {
  api.getTransactions.mockRejectedValue(new Error('Network error'));
  render(<TransactionList />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});

it('re-fetches when tx_type filter changes', async () => {
  const user = userEvent.setup();
  render(<TransactionList />);
  await screen.findByText('Grab');
  await user.click(screen.getByRole('button', { name: 'Expenses' }));
  expect(api.getTransactions).toHaveBeenCalledTimes(2);
  expect(api.getTransactions).toHaveBeenLastCalledWith(
    expect.objectContaining({ txType: 'expense' })
  );
});

it('re-fetches when wallet filter changes', async () => {
  const user = userEvent.setup();
  render(<TransactionList />);
  await screen.findByText('Grab');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Wallet' }), 'BBL');
  expect(api.getTransactions).toHaveBeenCalledTimes(2);
  expect(api.getTransactions).toHaveBeenLastCalledWith(
    expect.objectContaining({ wallet: 'BBL' })
  );
});
```

- [ ] **Step 2: Run tests — expect failures**

```bash
cd web && npx vitest run src/components/TransactionList.test.jsx
```

Expected: wallet chip test, filter-by-txType test, and filter-by-wallet test fail.

- [ ] **Step 3: Rewrite TransactionList to pass all tests**

Replace `web/src/components/TransactionList.jsx`:

```jsx
import { useState, useEffect } from 'react';
import { getTransactions } from '../api/supabase';

const TX_TYPE_OPTIONS = [
  { value: '',           label: 'All'        },
  { value: 'expense',    label: 'Expenses'   },
  { value: 'income',     label: 'Income'     },
  { value: 'transfer',   label: 'Transfers'  },
  { value: 'investment', label: 'Investment' },
];

const WALLET_OPTIONS = [
  { value: '',           label: 'All wallets'     },
  { value: 'BBL',        label: 'Bangkok Bank'    },
  { value: 'BCA',        label: 'BCA'             },
  { value: 'MANDIRI',    label: 'Mandiri'         },
  { value: 'MANDIRI_CC', label: 'Mandiri CC'      },
  { value: 'INVESTMENT', label: 'Investments'     },
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
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
```

- [ ] **Step 4: Run all web tests — expect all pass**

```bash
cd web && npx vitest run
```

Expected: all tests pass including TransactionList and AddTransactionForm suites.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/TransactionList.jsx web/src/components/TransactionList.test.jsx
git commit -m "feat(web): update TransactionList to filter by wallet/tx_type and show wallet chip"
```

---

## Task 6: Web — WalletBalances component (TDD)

**Files:**
- Create: `web/src/components/WalletBalances.jsx`
- Create: `web/src/components/WalletBalances.test.jsx`

- [ ] **Step 1: Write the failing tests**

Create `web/src/components/WalletBalances.test.jsx`:

```jsx
import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import WalletBalances from './WalletBalances';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const MOCK_BALANCES = [
  { id: 'BBL',        name: 'Bangkok Bank',        currency: 'THB', type: 'checking',   balance: 50000   },
  { id: 'BCA',        name: 'BCA Account',          currency: 'IDR', type: 'checking',   balance: 2000000 },
  { id: 'MANDIRI',    name: 'Mandiri Account',       currency: 'IDR', type: 'checking',   balance: 3000000 },
  { id: 'MANDIRI_CC', name: 'Mandiri Credit Card',   currency: 'IDR', type: 'credit',     balance: 500000  },
  { id: 'INVESTMENT', name: 'Investments',           currency: 'IDR', type: 'investment', balance: 10000000},
];

beforeEach(() => { api.getWalletBalances.mockResolvedValue(MOCK_BALANCES); });
afterEach(() => vi.resetAllMocks());

it('shows all wallet names', async () => {
  render(<WalletBalances />);
  expect(await screen.findByText('Bangkok Bank')).toBeInTheDocument();
  expect(screen.getByText('BCA Account')).toBeInTheDocument();
  expect(screen.getByText('Mandiri Credit Card')).toBeInTheDocument();
  expect(screen.getByText('Investments')).toBeInTheDocument();
});

it('shows IDR net worth (assets minus CC debt)', async () => {
  render(<WalletBalances />);
  await screen.findByText('Bangkok Bank');
  // IDR net = 2,000,000 + 3,000,000 + 10,000,000 - 500,000 = 14,500,000
  expect(screen.getByText(/14,500,000/)).toBeInTheDocument();
});

it('shows error on fetch failure', async () => {
  api.getWalletBalances.mockRejectedValue(new Error('Network error'));
  render(<WalletBalances />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});
```

- [ ] **Step 2: Run tests — expect failures**

```bash
cd web && npx vitest run src/components/WalletBalances.test.jsx
```

Expected: all 3 fail (component doesn't exist yet).

- [ ] **Step 3: Create WalletBalances.jsx**

Create `web/src/components/WalletBalances.jsx`:

```jsx
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
```

- [ ] **Step 4: Run tests — expect all pass**

```bash
cd web && npx vitest run src/components/WalletBalances.test.jsx
```

Expected: 3 tests pass.

- [ ] **Step 5: Commit**

```bash
git add web/src/components/WalletBalances.jsx web/src/components/WalletBalances.test.jsx
git commit -m "feat(web): add WalletBalances component with per-account balance cards and net worth"
```

---

## Task 7: Web — Wire Balances into NavBar and App

**Files:**
- Modify: `web/src/components/NavBar.jsx`
- Modify: `web/src/App.jsx`

- [ ] **Step 1: Add Balances nav button to NavBar**

In `web/src/components/NavBar.jsx`, add a third button between Transactions and Add:

```jsx
export default function NavBar({ activeView, onNavigate }) {
  return (
    <nav className="navbar">
      <span className="navbar-brand">FinTrack</span>
      <div className="navbar-nav">
        <button
          className="nav-btn"
          onClick={() => onNavigate('list')}
          aria-current={activeView === 'list' ? 'page' : undefined}
        >
          Transactions
        </button>
        <button
          className="nav-btn"
          onClick={() => onNavigate('balances')}
          aria-current={activeView === 'balances' ? 'page' : undefined}
        >
          Balances
        </button>
        <button
          className="nav-btn"
          onClick={() => onNavigate('add')}
          aria-current={activeView === 'add' ? 'page' : undefined}
        >
          Add
        </button>
      </div>
    </nav>
  );
}
```

- [ ] **Step 2: Import WalletBalances and render it in App**

Replace `web/src/App.jsx`:

```jsx
import { useState } from 'react';
import NavBar from './components/NavBar';
import TransactionList from './components/TransactionList';
import AddTransactionForm from './components/AddTransactionForm';
import WalletBalances from './components/WalletBalances';

export default function App() {
  const [activeView, setActiveView] = useState('list');
  return (
    <>
      <NavBar activeView={activeView} onNavigate={setActiveView} />
      {activeView === 'list'     && <TransactionList />}
      {activeView === 'balances' && <WalletBalances />}
      {activeView === 'add'      && <AddTransactionForm />}
    </>
  );
}
```

- [ ] **Step 3: Run full test suite**

```bash
cd web && npx vitest run
```

Expected: all tests pass.

- [ ] **Step 4: Commit**

```bash
git add web/src/components/NavBar.jsx web/src/App.jsx
git commit -m "feat(web): add Balances nav tab wired to WalletBalances component"
```

---

## Task 8: Android — TransactionEntity + DB migration 3→4

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/db/TransactionEntity.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt`

- [ ] **Step 1: Add three fields to TransactionEntity**

Replace `TransactionEntity.kt`:

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.TransactionStatus

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val dateIso: String,
    val channel: String,
    val referenceNo: String,
    val tab: SheetTab = SheetTab.EXPENSES,
    val status: TransactionStatus = TransactionStatus.PENDING_EDIT,
    val createdAt: Long = System.currentTimeMillis(),
    val wallet: String? = null,      // 'BBL' | 'BCA' | 'MANDIRI' | 'MANDIRI_CC' | 'INVESTMENT'
    val txType: String = "expense",  // 'expense' | 'income' | 'transfer' | 'investment'
    val toWallet: String? = null     // only populated for txType = 'transfer'
)
```

- [ ] **Step 2: Add migration 3→4 to AppDatabase and bump version**

Replace `AppDatabase.kt`:

```kotlin
package com.fidriyanto.banktracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        CategoryCacheEntity::class,
        ProcessedRefEntity::class,
        MonthlyOverviewEntity::class,
        MonthlyBudgetEntity::class
    ],
    version = 4
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryCacheDao(): CategoryCacheDao
    abstract fun processedRefDao(): ProcessedRefDao
    abstract fun monthlyOverviewDao(): MonthlyOverviewDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `processed_refs`")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `processed_refs` " +
                    "(`compositeKey` TEXT NOT NULL, `processedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`compositeKey`))"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transactions ADD COLUMN wallet TEXT")
                db.execSQL("ALTER TABLE transactions ADD COLUMN txType TEXT NOT NULL DEFAULT 'expense'")
                db.execSQL("ALTER TABLE transactions ADD COLUMN toWallet TEXT")
            }
        }
    }
}
```

- [ ] **Step 3: Add MIGRATION_3_4 to the builder in `di/AppModule.kt` line 21**

In `app/src/main/java/com/fidriyanto/banktracker/di/AppModule.kt`, change:

```kotlin
.addMigrations(AppDatabase.MIGRATION_2_3)
```

to:

```kotlin
.addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
```

- [ ] **Step 4: Build to verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL, no errors.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/db/TransactionEntity.kt \
        app/src/main/java/com/fidriyanto/banktracker/data/db/AppDatabase.kt
git commit -m "feat(android): add wallet/txType/toWallet to TransactionEntity, DB migration v3->v4"
```

---

## Task 9: Android — SheetsRow, SupabaseSyncerImpl, AddViewModel, AddScreen

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/data/model/SheetsRow.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/sheets/SupabaseSyncerImpl.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/add/AddViewModel.kt`
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/add/AddScreen.kt`

- [ ] **Step 1: Add wallet/txType/toWallet to SheetsRow**

Replace `SheetsRow.kt`:

```kotlin
package com.fidriyanto.banktracker.data.model

import java.time.LocalDate

data class SheetsRow(
    val tab: SheetTab,
    val date: LocalDate,
    val merchant: String,
    val item: String,
    val amount: Double,
    val category: String,
    val channel: String,
    val note: String? = null,
    val wallet: String? = null,
    val txType: String = "expense",
    val toWallet: String? = null
)
```

- [ ] **Step 2: Update SupabaseSyncerImpl to send new fields**

Replace `SupabaseSyncerImpl.kt`:

```kotlin
package com.fidriyanto.banktracker.sheets

import android.util.Log
import com.fidriyanto.banktracker.BuildConfig
import com.fidriyanto.banktracker.data.model.SheetsRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject

class SupabaseSyncerImpl @Inject constructor(
    private val httpClient: OkHttpClient
) : SheetsSyncer {

    override suspend fun sync(row: SheetsRow): Result<Unit> = withContext(Dispatchers.IO) {
        val body = JSONObject().apply {
            put("tab",     row.tab.name)
            put("date",    row.date.toString())
            put("merchant", row.merchant)
            put("item",    row.item)
            put("amount",  row.amount)
            put("category", row.category)
            put("channel", row.channel)
            if (row.note     != null) put("note",      row.note)
            if (row.wallet   != null) put("wallet",    row.wallet)
            put("tx_type", row.txType)
            if (row.toWallet != null) put("to_wallet", row.toWallet)
        }.toString()

        val request = Request.Builder()
            .url("${BuildConfig.SUPABASE_URL}/rest/v1/transactions")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Content-Type", "application/json")
            .addHeader("Prefer", "return=minimal")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("SupabaseSyncer", "POST transactions: $body")
        return@withContext try {
            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d("SupabaseSyncer", "status=${response.code} body=$responseBody")
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("Supabase error: HTTP ${response.code} — $responseBody"))
        } catch (e: Exception) {
            Log.e("SupabaseSyncer", "sync failed", e)
            Result.failure(e)
        }
    }
}
```

- [ ] **Step 3: Update AddFormState and AddViewModel**

Replace `AddViewModel.kt`:

```kotlin
package com.fidriyanto.banktracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fidriyanto.banktracker.data.model.SheetTab
import com.fidriyanto.banktracker.data.model.SheetsRow
import com.fidriyanto.banktracker.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class Wallet(val id: String, val displayName: String, val currency: String) {
    BBL        ("BBL",        "Bangkok Bank",        "THB"),
    BCA        ("BCA",        "BCA Account",         "IDR"),
    MANDIRI    ("MANDIRI",    "Mandiri Account",      "IDR"),
    MANDIRI_CC ("MANDIRI_CC", "Mandiri Credit Card",  "IDR"),
    INVESTMENT ("INVESTMENT", "Investments",          "IDR"),
}

enum class TxType(val id: String, val displayName: String) {
    EXPENSE   ("expense",    "Expense"),
    INCOME    ("income",     "Income"),
    TRANSFER  ("transfer",   "Transfer"),
    INVESTMENT("investment", "Investment"),
}

data class AddFormState(
    val wallet: Wallet   = Wallet.BBL,
    val txType: TxType   = TxType.EXPENSE,
    val toWallet: Wallet? = null,
    val amount: String   = "",
    val description: String = "",
    val category: String = "Other",
    val date: LocalDate  = LocalDate.now(),
    val isLoading: Boolean  = false,
    val successMessage: String? = null,
    val errorMessage: String?   = null
)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repository: TransactionRepository
) : ViewModel() {
    private val _state = MutableStateFlow(AddFormState())
    val state = _state.asStateFlow()

    fun update(block: AddFormState.() -> AddFormState) { _state.value = _state.value.block() }

    fun submit() = viewModelScope.launch {
        val s = _state.value
        val amount = s.amount.toDoubleOrNull() ?: run {
            _state.value = s.copy(errorMessage = "Enter a valid amount"); return@launch
        }
        _state.value = s.copy(isLoading = true, errorMessage = null)

        val tab = when {
            s.wallet.currency == "THB" && s.txType == TxType.INCOME   -> SheetTab.INCOME
            s.wallet.currency == "THB"                                  -> SheetTab.EXPENSES
            s.txType == TxType.INCOME                                   -> SheetTab.IDR_INCOME
            else                                                        -> SheetTab.IDR_EXPENSES
        }
        val row = SheetsRow(
            tab      = tab,
            date     = s.date,
            merchant = s.description,
            item     = s.description,
            amount   = amount,
            category = s.category,
            channel  = "Manual",
            wallet   = s.wallet.id,
            txType   = s.txType.id,
            toWallet = if (s.txType == TxType.TRANSFER) s.toWallet?.id else null
        )
        val result = repository.insertManual(row)
        _state.value = _state.value.copy(
            isLoading      = false,
            successMessage = if (result.isSuccess) "Saved and syncing!" else null,
            errorMessage   = if (result.isFailure) "Sync failed — saved offline" else null
        )
    }
}
```

- [ ] **Step 4: Update AddScreen to use new state shape**

Replace `AddScreen.kt`:

```kotlin
package com.fidriyanto.banktracker.ui.add

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fidriyanto.banktracker.categorization.ClaudeCategorizor
import com.fidriyanto.banktracker.ui.theme.Accent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScreen(viewModel: AddViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Add Transaction", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)

        // Wallet picker
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Wallet", fontSize = 12.sp, color = Color.Gray)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Wallet.entries.forEach { w ->
                    FilterChip(
                        selected = state.wallet == w,
                        onClick  = { viewModel.update { copy(wallet = w, toWallet = null) } },
                        label    = { Text(w.id, fontSize = 11.sp) }
                    )
                }
            }
        }

        // Tx type picker
        ToggleRow("Type", TxType.entries.map { it.displayName }, state.txType.displayName) { name ->
            val picked = TxType.entries.first { it.displayName == name }
            viewModel.update { copy(txType = picked, toWallet = null) }
        }

        // To-wallet picker (only for transfers)
        if (state.txType == TxType.TRANSFER) {
            val destinations = Wallet.entries.filter { it != state.wallet }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("To Wallet", fontSize = 12.sp, color = Color.Gray)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    destinations.forEach { w ->
                        FilterChip(
                            selected = state.toWallet == w,
                            onClick  = { viewModel.update { copy(toWallet = w) } },
                            label    = { Text(w.id, fontSize = 11.sp) }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = state.amount, onValueChange = { viewModel.update { copy(amount = it) } },
            label = { Text("Amount (${state.wallet.currency})") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )

        OutlinedTextField(
            value = state.description, onValueChange = { viewModel.update { copy(description = it) } },
            label = { Text("Description / Item") }, modifier = Modifier.fillMaxWidth()
        )

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = state.category, onValueChange = {},
                readOnly = true, label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ClaudeCategorizor.CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { viewModel.update { copy(category = cat) }; expanded = false }
                    )
                }
            }
        }

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
        state.successMessage?.let { Text(it, color = Accent, fontSize = 12.sp) }

        Button(
            onClick = { viewModel.submit() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            enabled = !state.isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Accent)
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
            else Text("Sync to Sheets", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ToggleRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                FilterChip(selected = opt == selected, onClick = { onSelect(opt) }, label = { Text(opt) })
            }
        }
    }
}
```

- [ ] **Step 5: Build to verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL. Fix any compilation errors before committing.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/data/model/SheetsRow.kt \
        app/src/main/java/com/fidriyanto/banktracker/sheets/SupabaseSyncerImpl.kt \
        app/src/main/java/com/fidriyanto/banktracker/ui/add/AddViewModel.kt \
        app/src/main/java/com/fidriyanto/banktracker/ui/add/AddScreen.kt
git commit -m "feat(android): replace account/type toggles with wallet/txType pickers; thread through sync layer"
```

---

## Task 10: Android — TransactionCard wallet badge and currency fix

**Files:**
- Modify: `app/src/main/java/com/fidriyanto/banktracker/ui/feed/TransactionCard.kt`

Currently `TransactionCard` hardcodes `-฿` for every transaction. After this task it shows the correct currency symbol, a sign based on tx type, and a small wallet badge.

- [ ] **Step 1: Update the amount display and add wallet badge in TransactionCard**

In `TransactionCard.kt`, replace these two lines:

```kotlin
val amountStr = if (entity.amount % 1.0 == 0.0) entity.amount.toInt().toString() else entity.amount.toString()
```

with:

```kotlin
val isThb      = entity.wallet == null || entity.wallet == "BBL"
val symbol     = if (isThb) "฿" else "Rp "
val sign       = when (entity.txType) {
    "income"                -> "+"
    "transfer", "investment" -> ""
    else                    -> "-"
}
val amountStr  = if (isThb) {
    if (entity.amount % 1.0 == 0.0) entity.amount.toInt().toString() else entity.amount.toString()
} else {
    entity.amount.toLong().toString()
}
val amountDisplay = "$sign$symbol$amountStr"
```

Then in the `Column(horizontalAlignment = Alignment.End)` block, replace:

```kotlin
Text("-฿$amountStr", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmountRed)
```

with:

```kotlin
val amountColor = when (entity.txType) {
    "income" -> Accent
    "transfer", "investment" -> MutedText
    else -> AmountRed
}
Text(amountDisplay, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = amountColor)
entity.wallet?.let { w ->
    Text(
        if (w == "MANDIRI_CC") "CC" else w,
        fontSize = 10.sp,
        color = MutedText,
        modifier = Modifier.padding(top = 1.dp)
    )
}
```

- [ ] **Step 2: Build to verify compilation**

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/fidriyanto/banktracker/ui/feed/TransactionCard.kt
git commit -m "feat(android): show wallet badge and correct currency/sign in TransactionCard"
```

---

## Verification Checklist

After all tasks are complete:

1. Open the web app. Navigate to Balances — 5 wallet cards appear, balances reflect starting values (0 unless updated in Task 1).
2. Add a CC expense (Wallet: Mandiri CC, Type: Expense) — it appears in the list with a CC chip, does not change checking account balances.
3. Add a CC bill payment (Wallet: Mandiri Account, Type: Transfer, To Wallet: Mandiri Credit Card) — Mandiri CC balance decreases, Mandiri Account balance decreases; neither appears in the Expenses filter.
4. Filter by "Expenses" — transfers are absent. Filter by "Transfers" — only the bill payment appears.
5. On Android, open Add screen — wallet chips (BBL/BCA/MANDIRI/MANDIRI_CC/INVESTMENT) and type chips (Expense/Income/Transfer/Investment) are visible. Selecting Transfer reveals a "To Wallet" row.
6. Submit a transaction from Android — it appears in the web list with the correct wallet chip.
