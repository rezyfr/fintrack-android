# Web Transaction Editing Design

## Context

The web app can create transactions but not edit or delete them. This became an immediate problem after the multi-wallet migration: the backfill assigned all `EXPENSES` tab rows to `BBL` (THB), but some of those were actually IDR transactions that had been filed in the wrong tab. Users need to correct `wallet`, `tx_type`, and any other field directly from the web UI.

## Approach

Edit icon per table row → modal dialog with all fields pre-populated → Save or Delete. Delete requires an in-modal confirmation step before calling Supabase.

---

## Data Layer

**File:** `web/src/api/supabase.js`

Two new exported functions:

```
updateTransaction(id, patch)
  PATCH /rest/v1/transactions?id=eq.{id}
  Headers: Prefer: return=minimal
  Body: patch object (only changed fields required)
  Throws on non-ok response.

deleteTransaction(id)
  DELETE /rest/v1/transactions?id=eq.{id}
  Throws on non-ok response.
```

---

## Shared Constants

**File:** `web/src/constants/transaction.js` (new)

Extracted from `AddTransactionForm.jsx` (which imports from here after this change):

- `WALLETS` — array of `{ id, name, currency }`
- `TX_TYPES` — array of `{ id, label }`
- `CATEGORIES` — string array
- `CHANNELS` — string array
- `deriveTab(walletId, txType)` — derives backward-compat `tab` value
- `currencySymbol(walletId)` — returns `฿` or `Rp`

`AddTransactionForm.jsx` is updated to import these instead of defining them locally. No behavior change.

---

## EditTransactionModal Component

**File:** `web/src/components/EditTransactionModal.jsx`  
**Tests:** `web/src/components/EditTransactionModal.test.jsx`

### Props

| Prop | Type | Description |
|------|------|-------------|
| `row` | object | Transaction row from Supabase (all fields) |
| `onClose` | function | Called when modal is dismissed without saving |
| `onSaved` | function(updatedRow) | Called after successful PATCH |
| `onDeleted` | function(id) | Called after successful DELETE |

### State

- Form fields initialized from `row`. Supabase returns snake_case (`tx_type`, `to_wallet`) so initialization maps these to camelCase form keys (`txType`, `toWallet`). `wallet` defaults to `'BBL'` when `row.wallet` is null (Android-parsed rows that predate the migration).
- `submitting` — boolean, disables buttons during async calls
- `confirming` — boolean, switches modal body to delete confirmation view
- `errorMsg` — string, shown inline on failure

### Behavior

**Save flow:**
1. Build payload: all form fields + `tab: deriveTab(wallet, txType)` + `to_wallet` (only if txType=transfer)
2. Call `updateTransaction(row.id, payload)`
3. On success: call `onSaved({ ...row, ...payload })`
4. On failure: set `errorMsg`, keep modal open

**Delete flow:**
1. User clicks "Delete" → `confirming = true`
2. Modal body switches to: "Delete this transaction? This cannot be undone." with "Yes, delete" and "Cancel" buttons
3. User clicks "Yes, delete" → call `deleteTransaction(row.id)`
4. On success: call `onDeleted(row.id)`
5. On failure: set `errorMsg`, return to form view (`confirming = false`)
6. User clicks "Cancel" → `confirming = false`, return to form

### Fields

Same field set as `AddTransactionForm`: wallet, txType, to_wallet (conditional on txType=transfer), merchant, item, amount, date, category, channel, note. Currency symbol derived from wallet selection.

### Tests

- Fields pre-populate from `row` prop
- Changing wallet to an IDR wallet switches currency symbol to Rp
- Save calls `updateTransaction` with correct payload including derived `tab`
- Delete button shows confirmation step (not calling deleteTransaction yet)
- Confirmed delete calls `deleteTransaction(row.id)` then `onDeleted`
- Error on save shows error message, keeps modal open

---

## TransactionList Integration

**File:** `web/src/components/TransactionList.jsx`

### State addition

```
editingRow: null | rowObject
```

### Table change

Each data row gets a 9th column with a small pencil icon button (no header). Clicking it sets `editingRow` to that row.

### Modal mounting

```jsx
{editingRow && (
  <EditTransactionModal
    row={editingRow}
    onClose={() => setEditingRow(null)}
    onSaved={(updated) => {
      setRows(rows.map(r => r.id === updated.id ? updated : r));
      setEditingRow(null);
    }}
    onDeleted={(id) => {
      setRows(rows.filter(r => r.id !== id));
      setEditingRow(null);
    }}
  />
)}
```

No refetch on save/delete — state is updated optimistically from the returned/known data.

### Tests

- Edit icon renders for each loaded row
- Clicking edit icon mounts the modal (verified by presence of modal heading or a known field)

---

## Verification

1. Load Transactions page — each row has a pencil icon
2. Click pencil on a BBL row with wrong wallet — change wallet to MANDIRI, save — row updates in the list with Mandiri chip
3. Click pencil — click Delete — confirmation text appears, `deleteTransaction` not yet called
4. Confirm delete — row disappears from list
5. Cancel delete — modal returns to edit form
6. Trigger a save with network failure (disconnect) — error message shown, modal stays open
7. Run `cd web && npx vitest run` — all tests pass
