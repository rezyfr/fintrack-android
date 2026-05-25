# Web App — Design Spec
**Date:** 2026-05-25
**Owner:** MR FIDRIYANTO
**Status:** Approved

---

## Overview

A standalone browser-based interface for viewing and inputting transactions. Data lives in Supabase (Sub-project 1). This is Sub-project 2 of 3. Budget viewing/editing is deferred to future development.

---

## Goals

- View transaction history with tab and month filters
- Add new transactions with the same fields as the Android app
- No authentication — anon key, single-user personal tool
- Deployable to Netlify free tier

---

## Stack

- **Framework:** React + Vite (SPA)
- **Hosting:** Netlify (free tier, build root: `web/`)
- **API:** Direct Supabase REST via `fetch` — no Supabase JS client library
- **Routing:** None — two views toggled by top nav (no `react-router-dom`)

---

## Project Layout

Lives in `web/` subdirectory of the existing `fintrack-android` repo.

```
web/
  src/
    api/supabase.js          # fetch wrapper, reads VITE_ env vars
    components/
      NavBar.jsx
      TransactionList.jsx
      AddTransactionForm.jsx
    App.jsx
    main.jsx
  .env.example               # documents required vars, not committed
  vite.config.js
  package.json
```

---

## Environment Variables

| Variable | Description |
|---|---|
| `VITE_SUPABASE_URL` | Supabase project URL |
| `VITE_SUPABASE_ANON_KEY` | Supabase anon key |

Set in `.env` locally (not committed). Set as Netlify environment variables in the dashboard. Mirrors the Android `local.properties` pattern.

---

## API Layer

`src/api/supabase.js` exports two functions. No third-party library — plain `fetch`.

### `getTransactions({ tab, month })`

```
GET /rest/v1/transactions
  ?order=date.desc
  &tab=eq.<tab>          (optional)
  &date=gte.<YYYY-MM-01> (optional)
  &date=lte.<YYYY-MM-DD> (optional)
  &limit=200
```

Returns an array of transaction rows.

### `addTransaction(row)`

```
POST /rest/v1/transactions
Headers:
  apikey: <anon-key>
  Authorization: Bearer <anon-key>
  Content-Type: application/json
  Prefer: return=minimal
Body: { merchant, item, amount, category, date, channel, tab, note }
```

Returns success or throws on non-2xx.

Both functions attach `apikey` and `Authorization` headers on every request. No retry logic — the form surfaces errors inline and preserves form state.

---

## Transaction List View

Default view on load. Fetches newest-first, re-fetches on filter change.

**Filters (top of view):**
- Tab dropdown: All | EXPENSES | IDR_EXPENSES | INCOME | IDR_INCOME
- Month picker: defaults to current month

**Columns displayed:**
Date, Merchant, Item, Amount, Category, Channel, Tab, Note

**Row limit:** 200 (no pagination — personal monthly view stays well within this)

---

## Add Transaction Form

All eight fields matching the Android app:

| Field | Input type | Notes |
|---|---|---|
| Merchant | text | required |
| Item | text | required |
| Amount | number | required |
| Category | select | Bills, Subscriptions, Entertainment, Food & Drink, Groceries, Health & Wellbeing, Other, Shopping, Transport, Travel, Business, Gifts |
| Channel | select | BillPayment, eWallet, PromptPay, BankTransfer, Manual, Unknown |
| Tab | select | EXPENSES, IDR_EXPENSES, INCOME, IDR_INCOME |
| Date | date picker | defaults to today |
| Note | textarea | optional |

**On submit:** POST to Supabase → show inline success message → reset form.  
**On error:** show inline error message, preserve all form field values.

---

## Navigation

Single `NavBar` component with two items: **Transactions** and **Add**. Active view stored in `App.jsx` state. No URL changes.

---

## Error Handling

- Network/API errors surface as inline messages in each view
- No global error boundary needed — both views are self-contained
- Supabase 4xx/5xx responses treated as failures; message shown to user

---

## Out of Scope

- Budget viewing/editing (future sub-project)
- Authentication / login
- Row-level security
- Real-time updates / Supabase subscriptions
- Edit or delete existing transactions
- Mobile-specific optimizations (functional on mobile, not the primary target)
