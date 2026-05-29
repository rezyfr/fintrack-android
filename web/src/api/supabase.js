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
