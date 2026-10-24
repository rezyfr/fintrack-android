import {
  WALLETS, EXPENSE_CATEGORIES, ALL_SUBCATEGORIES, DEFAULT_CATEGORIES,
  isInvestmentWallet, walletName, areAccountsLoaded,
  applyCatalog, subcategoriesFor, categoriesFor, getCatalogVersion, subscribeCatalog,
} from './transaction';

afterEach(() => applyCatalog({}));

// ac: web-manage-accounts — the account list is built from the accounts table rows
it('builds the account list from the accounts table', () => {
  applyCatalog({
    accounts: [
      { id: 'KBANK', name: 'Kasikorn', currency: 'THB', is_default: false },
      { id: 'VISA_CC', name: 'Visa', currency: 'USD', is_default: false },
    ],
  });
  expect(WALLETS.map(w => w.id)).toEqual(['KBANK', 'VISA_CC']);
  expect(WALLETS.find(w => w.id === 'VISA_CC')).toMatchObject({ currency: 'USD', type: 'credit', isDefault: false });
});

// ac: web-first-run-setup - an empty accounts table gives an empty list. An unreadable table keeps the current list.
it('empties the list for an empty table and keeps it for an unreadable one', () => {
  expect(WALLETS.length).toBeGreaterThan(0);
  applyCatalog({ accounts: null });
  expect(WALLETS.length).toBeGreaterThan(0);
  applyCatalog({ accounts: [] });
  expect(WALLETS).toEqual([]);
  expect(areAccountsLoaded()).toBe(true);
  expect(isInvestmentWallet('INVESTMENT')).toBe(false);
  expect(walletName('BBL')).toBe('BBL');
});

// ac: web-first-run-setup - the account type is the type column. The id guess is only a fallback.
it('reads the account type from the type column and guesses only when it is missing', () => {
  applyCatalog({
    accounts: [
      { id: 'SAVINGS_CC', name: 'Savings', currency: 'USD', type: 'checking' },
      { id: 'BROKER', name: 'Broker', currency: 'USD', type: 'investment' },
      { id: 'OLD_CC', name: 'Old card', currency: 'USD' },
    ],
  });
  expect(WALLETS.map(w => w.type)).toEqual(['checking', 'investment', 'credit']);
  expect(isInvestmentWallet('BROKER')).toBe(true);
  expect(walletName('BROKER')).toBe('Broker');
});

// ac: web-manage-categories — the expense categories and subcategories are built from the tables
it('builds categories and subcategories from the tables, in sort order', () => {
  applyCatalog({
    categories: [{ name: 'Pets', sort_order: 0 }, { name: 'Bills', sort_order: 1 }],
    subcategories: [
      { category: 'Pets', name: 'Vet', sort_order: 1 },
      { category: 'Pets', name: 'Food', sort_order: 0 },
    ],
  });
  expect(categoriesFor('expense')).toEqual(['Pets', 'Bills']);
  expect(subcategoriesFor('Pets')).toEqual(['Food', 'Vet']);
  expect(subcategoriesFor('Bills')).toEqual([]);
  expect(ALL_SUBCATEGORIES).toEqual(['Food', 'Vet']);
});

// ac: web-manage-categories — unreadable categories tables keep the shipped defaults
it('falls back to the default categories and subcategories', () => {
  applyCatalog({ categories: [], subcategories: [] });
  expect(EXPENSE_CATEGORIES).toEqual(DEFAULT_CATEGORIES);
  expect(subcategoriesFor('Transport')).toContain('Fuel');
});

it('notifies subscribers and bumps the version when the catalog changes', () => {
  const listener = vi.fn();
  const unsubscribe = subscribeCatalog(listener);
  const before = getCatalogVersion();
  applyCatalog({});
  expect(listener).toHaveBeenCalledTimes(1);
  expect(getCatalogVersion()).toBe(before + 1);
  unsubscribe();
});
