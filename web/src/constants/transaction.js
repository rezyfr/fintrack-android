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
