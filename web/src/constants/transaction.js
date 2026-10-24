import { getCurrencySettings, symbolFor } from './currency';

export const ACCOUNT_TYPES = [
  { id: 'checking',   label: 'Checking'    },
  { id: 'credit',     label: 'Credit card' },
  { id: 'investment', label: 'Investment'  },
];

// The account type comes from the type column of the accounts table. A row with no type (an older
// database) falls back to a guess from the id: a *_CC id is a credit card, INVESTMENT is the
// investment pool, and anything else is a checking account.
export function accountType(a) {
  if (ACCOUNT_TYPES.some(t => t.id === a.type)) return a.type;
  if (a.id.endsWith('_CC')) return 'credit';
  if (a.id === 'INVESTMENT') return 'investment';
  return 'checking';
}

function toWallet(a) {
  return {
    id: a.id, name: a.name, currency: a.currency, type: accountType(a), isDefault: !!a.is_default,
  };
}

// ac: web-manage-accounts - the live account list. It starts empty. applyCatalog() replaces its
// contents in place, so every module that imported WALLETS sees the current list on its next render.
export const WALLETS = [];

// True when the wallet is an investment account. Reads the account type, not the id.
export function isInvestmentWallet(walletId) {
  return WALLETS.find(w => w.id === walletId)?.type === 'investment';
}

// The display name of a wallet. A wallet that is not in the account list shows its id.
export function walletName(walletId) {
  return WALLETS.find(w => w.id === walletId)?.name ?? walletId;
}

export const TX_TYPES = [
  { id: 'expense',    label: 'Expense'    },
  { id: 'income',     label: 'Income'     },
  { id: 'transfer',   label: 'Transfer'   },
  { id: 'investment', label: 'Investment' },
];

// ac: web-manage-categories — the shipped default categories and subcategories. These match the
// categories the server seeds on a new database, and they are what the app uses
// when the categories tables cannot be read or are empty.
export const DEFAULT_CATEGORIES = [
  'Bills', 'Subscriptions', 'Entertainment', 'Food & Drink',
  'Groceries', 'Health & Wellbeing', 'Family', 'Other', 'Shopping',
  'Transport', 'Travel', 'Business', 'Gifts',
];

export const DEFAULT_SUBCATEGORIES = {
  'Transport':          ['Ride-hailing', 'Fuel', 'Toll', 'E-money'],
  'Food & Drink':       ['Restaurant', 'Cafe / coffee', 'Food delivery', 'Snacks'],
  'Bills':              ['Rent', 'Electricity', 'Water', 'Internet', 'Mobile / telco', 'Insurance'],
  'Subscriptions':      ['Streaming', 'Music', 'Software / cloud'],
  'Family':             ['Household'],
  'Health & Wellbeing': ['Gym / fitness', 'Pharmacy', 'Doctor / medical', 'Sports gear'],
  'Shopping':           ['Clothing', 'Electronics', 'Home', 'Personal care'],
  'Travel':             ['Flights', 'Hotels', 'Local transport', 'Activities'],
  'Entertainment':      ['Movies', 'Games', 'Events', 'Hobbies'],
  'Gifts':              ['Family', 'Friends', 'Charity'],
  'Business':           ['Supplies', 'Services', 'Fees'],
};

// ac: web-manage-categories — the live expense category list, replaced in place by applyCatalog()
export const EXPENSE_CATEGORIES = [...DEFAULT_CATEGORIES];

export const INCOME_CATEGORIES = [
  'Salary', 'Freelance', 'Business', 'Dividends', 'Rental', 'Bonus', 'Gift', 'Other',
];

export function categoriesFor(txType) {
  if (txType === 'income') return INCOME_CATEGORIES;
  if (txType === 'transfer') return ['Transfer'];
  if (txType === 'investment') return ['Investment', 'Dividends', 'Other'];
  return EXPENSE_CATEGORIES;
}

// ac: add-transaction-subcategory — subcategory lists per category. Categories with no entry
// (for example Groceries) have no subcategories. Subcategory is always optional.
// ac: web-manage-categories — the live map, replaced in place by applyCatalog()
export const SUBCATEGORIES = Object.fromEntries(
  Object.entries(DEFAULT_SUBCATEGORIES).map(([category, names]) => [category, [...names]]),
);

// ac: add-transaction-subcategory — subcategories available for a category, empty when it has none
export function subcategoriesFor(category) {
  return SUBCATEGORIES[category] || [];
}

function distinctSubcategories() {
  return [...new Set(Object.values(SUBCATEGORIES).flat())].sort();
}

// All distinct subcategories, for the transactions filter dropdown. Replaced in place by applyCatalog().
export const ALL_SUBCATEGORIES = distinctSubcategories();

// Subscribers are re-rendered when the catalog changes. The version number is the snapshot React
// compares, so every applyCatalog() call produces a new one.
let catalogVersion = 0;
let accountsLoaded = false;

// True once the accounts table was read successfully. The empty state banner waits for this, so it
// does not show while the catalog is still loading or when the read failed.
export function areAccountsLoaded() {
  return accountsLoaded;
}
const catalogListeners = new Set();

export function subscribeCatalog(listener) {
  catalogListeners.add(listener);
  return () => catalogListeners.delete(listener);
}

export function getCatalogVersion() {
  return catalogVersion;
}

// ac: web-manage-accounts - builds the account list from the accounts table rows. A missing result
// (the read failed) keeps the current list. An empty result clears it.
// ac: web-manage-categories — builds the category and subcategory lists from the categories tables.
// An empty or missing result keeps the shipped default categories and subcategories.
export function applyCatalog({ accounts, categories, subcategories } = {}) {
  if (accounts) {
    WALLETS.splice(0, WALLETS.length, ...accounts.map(toWallet));
    accountsLoaded = true;
  }

  const categoryNames = categories?.length ? categories.map(c => c.name) : DEFAULT_CATEGORIES;
  EXPENSE_CATEGORIES.splice(0, EXPENSE_CATEGORIES.length, ...categoryNames);

  for (const key of Object.keys(SUBCATEGORIES)) delete SUBCATEGORIES[key];
  if (categories?.length) {
    const ordered = [...(subcategories ?? [])].sort((a, b) => (a.sort_order ?? 0) - (b.sort_order ?? 0));
    for (const s of ordered) (SUBCATEGORIES[s.category] ??= []).push(s.name);
  } else {
    for (const [category, names] of Object.entries(DEFAULT_SUBCATEGORIES)) SUBCATEGORIES[category] = [...names];
  }
  const all = distinctSubcategories();
  ALL_SUBCATEGORIES.splice(0, ALL_SUBCATEGORIES.length, ...all);

  catalogVersion += 1;
  catalogListeners.forEach(l => l());
}

// ac: web-configurable-currency-pair — a wallet's currency is its own account currency. A wallet that
// is not in the account list counts as the home currency, the same fallback Android uses.
export function walletCurrency(walletId) {
  return WALLETS.find(w => w.id === walletId)?.currency ?? getCurrencySettings().homeCode;
}

// The currency symbol for a wallet, taken from the shared currency lookup
export function currencySymbol(walletId) {
  return symbolFor(walletCurrency(walletId));
}

// ac: web-configurable-currency-pair - the internal ledger tab follows the wallet currency. The tab
// names IDR_EXPENSES and IDR_INCOME stay as stored: they mean the home ledger. EXPENSES and INCOME
// mean any other currency.
export function deriveTab(walletId, txType) {
  const isHomeWallet = walletCurrency(walletId) === getCurrencySettings().homeCode;
  if (txType === 'income') return isHomeWallet ? 'IDR_INCOME' : 'INCOME';
  return isHomeWallet ? 'IDR_EXPENSES' : 'EXPENSES';
}
