// The FinTrack server API. The contract is api/openapi.yaml in the server repository.
// Every call sends the bearer token. A call that fails throws an Error that names the HTTP status.

// The server sends at most this many rows in one page.
const MAX_PAGE_ROWS = 1000;
// ac: web-complete-range-fetch — a closed date range reads every page, up to this many rows. It stops a runaway loop.
const MAX_RANGE_ROWS = 5000;
// The server changes at most this many transactions in one batch.
const MAX_BATCH_IDS = 500;

function apiUrl(path, query = {}) {
  const base = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== null && value !== undefined && value !== '') params.append(key, String(value));
  }
  const text = params.toString();
  return `${base}${path}${text ? `?${text}` : ''}`;
}

// Sends one request. A status listed in `ignore` counts as success and returns null.
async function call(method, path, { query, body, ignore = [] } = {}) {
  // The server rejects a JSON content type on a request without a body, so the header goes with a body only.
  const headers = { Authorization: `Bearer ${import.meta.env.VITE_API_TOKEN}` };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const res = await fetch(apiUrl(path, query), {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (ignore.includes(res.status)) return null;
  if (!res.ok) throw new Error(`API error: ${res.status}`);
  return res.status === 204 ? null : res.json();
}

const chunks = (list, size) => Array.from({ length: Math.ceil(list.length / size) }, (_, i) => list.slice(i * size, (i + 1) * size));

export async function getTransactions({ wallet = null, txType = null, month = null, category = null, search = null, amountMin = null, amountMax = null, dateFrom = null, dateTo = null, limit = 200 } = {}) {
  // ac: advanced-transaction-filters — a date range overrides the month
  const hasRange = Boolean(dateFrom || dateTo);
  const query = {
    // ac: transfer-in-wallet-view — also fetch transfers where this wallet is the destination
    involving_wallet: wallet,
    tx_type: txType,
    category,
    search,
    amount_min: amountMin,
    amount_max: amountMax,
    date_from: dateFrom,
    date_to: dateTo,
    month: hasRange ? null : month,
    // ac: track-cc-transactions-as-debt — callers needing full wallet history (not just the last 200) can raise `limit`
    limit: Math.min(limit, MAX_PAGE_ROWS),
  };
  // ac: web-complete-range-fetch — a range closed at both ends reads every page, the same as Android's
  // TransactionFetchDataSourceImpl, so a busy cycle is not cut at `limit` rows. An open-ended fetch
  // stops at `limit` rows.
  const bounded = Boolean((dateFrom && dateTo) || (!hasRange && month));
  const rows = [];
  let offset = 0;
  for (;;) {
    const page = await call('GET', '/v1/transactions', { query: { ...query, offset: offset || null } });
    rows.push(...page.items);
    if (page.next_offset === null) break;
    if (bounded ? rows.length >= MAX_RANGE_ROWS : rows.length >= limit) break;
    offset = page.next_offset;
  }
  return rows;
}

export async function addTransaction(row) {
  await call('POST', '/v1/transactions', { body: row });
}

// ac: remove-wallet-reconciliation — the wallet balances endpoint is the only balances call
export async function getWalletBalances() {
  const rows = await call('GET', '/v1/wallet-balances');
  return rows.sort((a, b) => (a.id < b.id ? -1 : a.id > b.id ? 1 : 0));
}

export async function updateTransaction(id, patch) {
  await call('PATCH', `/v1/transactions/${id}`, { body: patch });
}

// A transaction that is already gone counts as deleted.
export async function deleteTransaction(id) {
  await call('DELETE', `/v1/transactions/${id}`, { ignore: [404] });
}

export async function deleteTransactions(ids) {
  for (const batch of chunks(ids, MAX_BATCH_IDS)) {
    await call('DELETE', '/v1/transactions', { query: { ids: batch.join(',') } });
  }
}

export async function updateTransactionsCategory(ids, category) {
  for (const batch of chunks(ids, MAX_BATCH_IDS)) {
    await call('PATCH', '/v1/transactions', { body: { ids: batch, category } });
  }
}

export function getInstallments() {
  return call('GET', '/v1/installments');
}

export async function updateInstallmentMerchant(id, merchant) {
  await call('PATCH', `/v1/installments/${id}`, { body: { merchant } });
}

export async function incrementInstallmentStep(id, newStep, newStatus) {
  await call('PATCH', `/v1/installments/${id}`, { body: { current_step: newStep, status: newStatus } });
}

export async function setInstallmentExcluded(id, excluded) {
  await call('PATCH', `/v1/installments/${id}`, { body: { excluded } });
}

export function getBudgetLines() {
  return call('GET', '/v1/budget-lines');
}

// ac: budget-line-target-editing — change the target of one line
export async function updateBudgetLineTarget(id, target) {
  await call('PATCH', `/v1/budget-lines/${id}`, { body: { target } });
}

// ac: budget-cycle-target-vs-actual — budget lines are denominated in the foreign or the home currency
// but a line's transactions can post on a wallet in the other currency (foreign spend on a home-currency
// credit card). The rate is derived from the user's own recent cross-currency transfers rather than
// hardcoded, so it tracks what they actually got, and falls back only when no such transfer exists.
// ac: web-configurable-currency-pair - a transfer counts when it leaves an account in the source
// currency (fromWallets) for a home-currency account (toWallets), so no wallet id is hardcoded. The
// caller calls this once for each non-home currency in use. The result is home-currency units per one
// unit of the source currency.
export async function getRecentFxRate({ fromWallets, toWallets }) {
  if (!fromWallets.length || !toWallets.length) return null;
  const { rates } = await call('GET', '/v1/transfer-rates', {
    query: { from_wallets: fromWallets.join(','), to_wallets: toWallets.join(','), limit: 10 },
  });
  const usable = rates.filter(v => Number.isFinite(v) && v > 0).sort((a, b) => a - b);
  if (usable.length === 0) return null;
  return usable[Math.floor(usable.length / 2)];
}

// ac: card-statement-due-and-minimum — per-card billing config (cutoff day, due day, minimum rule).
export function getCardBilling() {
  return call('GET', '/v1/card-billing');
}

// ac: web-manage-accounts — the accounts list is shared with the Android app.
export function getAccounts() {
  return call('GET', '/v1/accounts');
}

// The server answers 404 until the first-run setup saves the settings.
export function getAppSettings() {
  return call('GET', '/v1/settings', { ignore: [404] });
}

export async function saveAppSettings(settings) {
  await call('PUT', '/v1/settings', { body: settings });
}

export async function saveAccount({ id, ...fields }) {
  await call('PUT', `/v1/accounts/${encodeURIComponent(id)}`, { body: fields });
}

export async function deleteAccount(id) {
  await call('DELETE', `/v1/accounts/${encodeURIComponent(id)}`, { ignore: [404] });
}

export function getCategories() {
  return call('GET', '/v1/categories');
}

export async function saveCategory(category) {
  await call('PUT', '/v1/categories', { body: category });
}

export async function deleteCategory(name) {
  await call('DELETE', '/v1/categories', { query: { name }, ignore: [404] });
}

export function getSubcategories() {
  return call('GET', '/v1/subcategories');
}

export async function saveSubcategory(subcategory) {
  await call('POST', '/v1/subcategories', { body: subcategory });
}

export async function deleteSubcategory(category, name) {
  await call('DELETE', '/v1/subcategories', { query: { category, name }, ignore: [404] });
}
