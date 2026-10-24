import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import App from './App';
import * as api from './api/client';
import { saveCurrencySettings, DEFAULT_CURRENCY_SETTINGS } from './constants/currency';

vi.mock('./api/client');

const BANNER = 'No accounts yet. Add your accounts to start, or finish setup in the Android app.';

beforeEach(() => {
  vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {} });
  vi.stubGlobal('fetch', vi.fn(() => Promise.resolve({ ok: true })));
  api.getTransactions.mockResolvedValue([]);
  api.getBudgetLines.mockResolvedValue([]);
  api.getWalletBalances.mockResolvedValue([]);
  api.getCategories.mockResolvedValue([]);
  api.getSubcategories.mockResolvedValue([]);
  api.getAppSettings.mockResolvedValue(null);
});
afterEach(() => {
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

// ac: web-first-run-setup - an empty accounts table shows the empty state with a button to Manage accounts
it('shows the empty state banner when there are no accounts', async () => {
  api.getAccounts.mockResolvedValue([]);
  render(<App />);
  expect(await screen.findByText(BANNER)).toBeInTheDocument();
  api.saveAccount.mockResolvedValue();
  await userEvent.click(screen.getByRole('button', { name: 'Manage accounts' }));
  expect(await screen.findByRole('heading', { name: 'Accounts' })).toBeInTheDocument();
  expect(screen.queryByText(BANNER)).not.toBeInTheDocument();
});

it('shows no banner when accounts exist', async () => {
  api.getAccounts.mockResolvedValue([{ id: 'MAIN', name: 'Main', currency: 'IDR', type: 'checking' }]);
  render(<App />);
  expect(await screen.findByRole('heading', { name: 'Transactions' })).toBeInTheDocument();
  await vi.waitFor(() => expect(api.getAccounts).toHaveBeenCalled());
  expect(screen.queryByText(BANNER)).not.toBeInTheDocument();
});

// ac: web-first-run-setup - one currency in use hides the book switch
it('hides the book switch when the app has one currency', async () => {
  api.getAccounts.mockResolvedValue([{ id: 'MAIN', name: 'Main', currency: 'USD', type: 'checking' }]);
  api.getAppSettings.mockResolvedValue({ id: 1, home_code: 'USD', home_label: 'US', foreign_code: null, foreign_label: null, cycle_start_day: 1 });
  render(<App />);
  await vi.waitFor(() => expect(screen.queryByRole('group', { name: 'Book' })).not.toBeInTheDocument());
});

it('shows the book switch when the accounts hold a second currency', async () => {
  api.getAccounts.mockResolvedValue([
    { id: 'MAIN', name: 'Main', currency: 'IDR', type: 'checking' },
    { id: 'TRIP', name: 'Trip', currency: 'THB', type: 'checking' },
  ]);
  render(<App />);
  expect(await screen.findByRole('group', { name: 'Book' })).toBeInTheDocument();
});

// ac: web-currency-books - a currency that no account holds has no book
it('shows no chip for a currency that no account holds', async () => {
  api.getAccounts.mockResolvedValue([
    { id: 'MAIN', name: 'Main', currency: 'IDR', type: 'checking' },
    { id: 'TRIP', name: 'Trip', currency: 'THB', type: 'checking' },
  ]);
  render(<App />);
  const group = await screen.findByRole('group', { name: 'Book' });
  expect(within(group).getAllByRole('button').map(b => b.textContent)).toEqual(['Combined', 'Indonesia', 'Thailand']);
});
