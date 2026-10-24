import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import WalletBalances from './WalletBalances';
import * as api from '../api/client';
import { DEFAULT_CURRENCY_SETTINGS, saveCurrencySettings } from '../constants/currency';
import { applyCatalog } from '../constants/transaction';

vi.mock('../api/client');

beforeEach(() => vi.stubGlobal('localStorage', { getItem: () => null, setItem: () => {} }));
afterEach(() => {
  saveCurrencySettings(DEFAULT_CURRENCY_SETTINGS);
  vi.unstubAllGlobals();
  vi.resetAllMocks();
});

// ac: web-configurable-currency-pair - Balances shows a net tile for each currency in use, and an
// account in any currency is formatted from the shared lookup instead of crashing on an unknown code
it('shows a net tile for each currency in use and formats a custom currency', async () => {
  saveCurrencySettings({ homeCode: 'USD', homeLabel: 'United States', currencies: [{ code: 'EUR', label: 'Europe', rate: null }] });
  const accounts = [
    { id: 'US_BANK', name: 'US Bank', currency: 'USD', type: 'checking', balance: 1500 },
    { id: 'EU_BANK', name: 'EU Bank', currency: 'EUR', type: 'checking', balance: 800 },
    { id: 'CH_BANK', name: 'Swiss Bank', currency: 'CHF', type: 'checking', balance: 50 },
  ];
  applyCatalog({ accounts });
  api.getWalletBalances.mockResolvedValue(accounts);
  render(<WalletBalances />);
  expect(await screen.findByText('US Bank')).toBeInTheDocument();
  expect(screen.getByText('USD Net')).toBeInTheDocument();
  expect(screen.getByText('EUR Net')).toBeInTheDocument();
  expect(screen.getByText('CHF Net')).toBeInTheDocument();
  expect(screen.queryByText('THB Net')).not.toBeInTheDocument();
  expect(screen.getAllByText('CHF 50.00').length).toBeGreaterThan(0);
  expect(screen.getAllByText('$1,500.00').length).toBeGreaterThan(0);
});
