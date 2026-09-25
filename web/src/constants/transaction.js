export const WALLETS = [
  { id: 'BBL',        name: 'Bangkok Bank',        currency: 'THB', type: 'checking'   },
  { id: 'BCA',        name: 'BCA Account',          currency: 'IDR', type: 'checking'   },
  { id: 'BCA_CC',     name: 'BCA Credit Card',      currency: 'IDR', type: 'credit'     },
  { id: 'MANDIRI',    name: 'Mandiri Account',       currency: 'IDR', type: 'checking'   },
  { id: 'MANDIRI_CC', name: 'Mandiri Credit Card',   currency: 'IDR', type: 'credit'     },
  { id: 'INVESTMENT', name: 'Investments',           currency: 'IDR', type: 'investment' },
];

export const TX_TYPES = [
  { id: 'expense',    label: 'Expense'    },
  { id: 'income',     label: 'Income'     },
  { id: 'transfer',   label: 'Transfer'   },
  { id: 'investment', label: 'Investment' },
];

export const EXPENSE_CATEGORIES = [
  'Bills', 'Subscriptions', 'Entertainment', 'Food & Drink',
  'Groceries', 'Health & Wellbeing', 'Family', 'Other', 'Shopping',
  'Transport', 'Travel', 'Business', 'Gifts',
];

export const INCOME_CATEGORIES = [
  'Salary', 'Freelance', 'Business', 'Dividends', 'Rental', 'Bonus', 'Gift', 'Other',
];

export function categoriesFor(txType) {
  if (txType === 'income') return INCOME_CATEGORIES;
  if (txType === 'transfer') return ['Transfer'];
  if (txType === 'investment') return ['Investment', 'Dividends', 'Other'];
  return EXPENSE_CATEGORIES;
}

// ac: add-transaction-subcategory — fixed subcategory lists per category. Categories not listed
// here (for example Groceries) have no subcategories. Subcategory is always optional.
export const SUBCATEGORIES = {
  'Transport':          ['Ride-hailing', 'Fuel', 'Toll', 'E-money'],
  'Food & Drink':       ['Restaurant', 'Cafe / coffee', 'Food delivery', 'Snacks'],
  'Bills':              ['Rent', 'Electricity', 'Water', 'Internet', 'Mobile / telco', 'Insurance'],
  'Subscriptions':      ['Streaming', 'Music', 'Software / cloud'],
  'Family':             ['Mom', 'Dad', 'Wife', 'Kids', 'Household'],
  'Health & Wellbeing': ['Gym / fitness', 'Pharmacy', 'Doctor / medical', 'Sports gear'],
  'Shopping':           ['Clothing', 'Electronics', 'Home', 'Personal care'],
  'Travel':             ['Flights', 'Hotels', 'Local transport', 'Activities'],
  'Entertainment':      ['Movies', 'Games', 'Events', 'Hobbies'],
  'Gifts':              ['Family', 'Friends', 'Charity'],
  'Business':           ['Supplies', 'Services', 'Fees'],
};

// ac: add-transaction-subcategory — subcategories available for a category, empty when it has none
export function subcategoriesFor(category) {
  return SUBCATEGORIES[category] || [];
}

// All distinct subcategories, for the transactions filter dropdown.
export const ALL_SUBCATEGORIES = [...new Set(Object.values(SUBCATEGORIES).flat())].sort();


export function currencySymbol(walletId) {
  return WALLETS.find(w => w.id === walletId)?.currency === 'THB' ? '฿' : 'Rp';
}

export function walletCurrency(walletId) {
  return WALLETS.find(w => w.id === walletId)?.currency ?? 'IDR';
}

export function deriveTab(walletId, txType) {
  const isThb = WALLETS.find(w => w.id === walletId)?.currency === 'THB';
  if (txType === 'income') return isThb ? 'INCOME' : 'IDR_INCOME';
  return isThb ? 'EXPENSES' : 'IDR_EXPENSES';
}
