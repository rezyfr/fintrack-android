import { useEffect, useSyncExternalStore } from 'react';
import { getAccounts, getAppSettings, getCategories, getSubcategories } from '../api/client';
import { applyCatalog, subscribeCatalog, getCatalogVersion } from '../constants/transaction';
import { subscribeCurrency, getCurrencyVersion, applyAppSettings } from '../constants/currency';

// ac: web-manage-accounts - loads the accounts table. A failed read leaves the current account list
// in place.
// ac: web-first-run-setup - loads the app_settings row. A missing row or a failed read leaves the
// browser cache or the legacy defaults in place.
// ac: web-manage-categories — loads the categories and subcategories tables. The two are used
// together, so if either read fails the shipped default categories stay in place.
export async function refreshCatalog() {
  const [accounts, categories, subcategories, appSettings] = await Promise.allSettled([
    getAccounts(), getCategories(), getSubcategories(), getAppSettings(),
  ]);
  if (appSettings.status === 'fulfilled') applyAppSettings(appSettings.value);
  const categoriesOk = categories.status === 'fulfilled' && subcategories.status === 'fulfilled';
  applyCatalog({
    accounts: accounts.status === 'fulfilled' ? accounts.value : null,
    categories: categoriesOk ? categories.value : null,
    subcategories: categoriesOk ? subcategories.value : null,
  });
}

// Loads the catalog once on startup and re-renders the caller whenever it changes, so every view
// below it reads the current WALLETS, EXPENSE_CATEGORIES and SUBCATEGORIES. It also re-renders when
// the currency pair changes, so every view shows the configured labels.
export function useCatalog() {
  useSyncExternalStore(subscribeCatalog, getCatalogVersion);
  useSyncExternalStore(subscribeCurrency, getCurrencyVersion);
  useEffect(() => { refreshCatalog(); }, []);
}
