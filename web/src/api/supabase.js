function authHeaders() {
  const key = import.meta.env.VITE_SUPABASE_ANON_KEY;
  return {
    apikey: key,
    Authorization: `Bearer ${key}`,
    'Content-Type': 'application/json',
  };
}

export async function getTransactions({ tab = null, wallet = null, txType = null, month = null, category = null, search = null, amountMin = null, amountMax = null, dateFrom = null, dateTo = null } = {}) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const params = new URLSearchParams();
  params.append('order', 'date.desc');
  params.append('limit', '200');
  if (tab) params.append('tab', `eq.${tab}`);
  if (wallet) {
    // ac: transfer-in-wallet-view — also fetch transfers where this wallet is the destination
    params.append('or', `(wallet.eq.${wallet},and(tx_type.eq.transfer,to_wallet.eq.${wallet}))`);
  }
  if (txType)   params.append('tx_type',  `eq.${txType}`);
  if (category) params.append('category', `eq.${category}`); // ac: filter-transactions-by-category — selecting a category shows only transactions with that category
  // ac: advanced-transaction-filters — search by item text
  if (search) params.append('item', `ilike.*${search}*`);
  // ac: advanced-transaction-filters — amount range
  if (amountMin != null) params.append('amount', `gte.${amountMin}`);
  if (amountMax != null) params.append('amount', `lte.${amountMax}`);
  // ac: advanced-transaction-filters — date range (specific dates override month)
  if (dateFrom || dateTo) {
    if (dateFrom) params.append('date', `gte.${dateFrom}`);
    if (dateTo)   params.append('date', `lte.${dateTo}`);
  } else if (month) {
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

export async function updateTransaction(id, patch) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions?id=eq.${id}`, {
    method: 'PATCH',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify(patch),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

export async function getMonthlyOverview(months) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/rpc/get_monthly_overview`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ p_months: months }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
  return res.json();
}

export async function deleteTransaction(id) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions?id=eq.${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

export async function deleteTransactions(ids) {
  if (!ids.length) return;
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions?id=in.(${ids.join(',')})`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

// ac: batch-edit-transaction-category — single PATCH scoped to id=in.(...), only the category column changes
export async function updateTransactionsCategory(ids, category) {
  if (!ids.length) return;
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions?id=in.(${ids.join(',')})`, {
    method: 'PATCH',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify({ category }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

export async function getWalletReconciliation(month) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/rpc/get_wallet_reconciliation`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ p_month: month }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
  return res.json();
}

export async function upsertStatementBalance(walletId, month, openingBalance) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/statement_balances`, {
    method: 'POST',
    headers: { ...authHeaders(), Prefer: 'resolution=merge-duplicates' },
    body: JSON.stringify({ wallet_id: walletId, month, opening_balance: openingBalance }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

// ac: installment-overview
export async function getInstallments() {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/installments?select=*&order=due_day.asc`, {
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
  return res.json();
}

// ac: installment-name-edit — PATCH merchant name for an installment row
export async function updateInstallmentMerchant(id, merchant) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/installments?id=eq.${id}`, {
    method: 'PATCH',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify({ merchant }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

// ac: ac-isc-1, ac-isc-2 — PATCH current_step and status
export async function incrementInstallmentStep(id, newStep, newStatus) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/installments?id=eq.${id}`, {
    method: 'PATCH',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify({ current_step: newStep, status: newStatus }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

// ac: ac-isc-4 — PATCH excluded flag
export async function setInstallmentExcluded(id, excluded) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/installments?id=eq.${id}`, {
    method: 'PATCH',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify({ excluded }),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}

export async function addTransactions(rows) {
  const url = import.meta.env.VITE_SUPABASE_URL;
  const res = await fetch(`${url}/rest/v1/transactions`, {
    method: 'POST',
    headers: { ...authHeaders(), Prefer: 'return=minimal' },
    body: JSON.stringify(rows),
  });
  if (!res.ok) throw new Error(`Supabase error: ${res.status}`);
}
