import { render, screen } from '@testing-library/react';
import { vi } from 'vitest';
import WalletBalances from './WalletBalances';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const MOCK_BALANCES = [
  { id: 'BBL',        name: 'Bangkok Bank',        currency: 'THB', type: 'checking',   balance: 50000   },
  { id: 'BCA',        name: 'BCA Account',          currency: 'IDR', type: 'checking',   balance: 2000000 },
  { id: 'MANDIRI',    name: 'Mandiri Account',       currency: 'IDR', type: 'checking',   balance: 3000000 },
  { id: 'MANDIRI_CC', name: 'Mandiri Credit Card',   currency: 'IDR', type: 'credit',     balance: 500000  },
  { id: 'INVESTMENT', name: 'Investments',           currency: 'IDR', type: 'investment', balance: 10000000},
];

beforeEach(() => {
  api.getWalletBalances.mockResolvedValue(MOCK_BALANCES);
  api.getWalletReconciliation.mockResolvedValue([]);
});
afterEach(() => vi.resetAllMocks());

// ac: balances-assets-only-summary — the wallet grid lists only non-credit wallets
it('shows every non-credit wallet and no credit card', async () => {
  render(<WalletBalances />);
  expect(await screen.findByText('Bangkok Bank')).toBeInTheDocument();
  expect(screen.getByText('BCA Account')).toBeInTheDocument();
  expect(screen.getByText('Mandiri Account')).toBeInTheDocument();
  expect(screen.getByText('Investments')).toBeInTheDocument();
  expect(screen.queryByText('Mandiri Credit Card')).not.toBeInTheDocument();
});

// ac: balances-assets-only-summary — IDR Net sums only IDR wallets that are neither credit nor investment
it('shows IDR net worth as spendable cash only', async () => {
  render(<WalletBalances />);
  await screen.findByText('Bangkok Bank');
  // IDR net = BCA 2,000,000 + Mandiri 3,000,000; the CC and the 10,000,000 investment are excluded.
  expect(screen.getByText(/5,000,000/)).toBeInTheDocument();
  expect(screen.queryByText(/15,000,000/)).not.toBeInTheDocument();
});

// ac: balances-assets-only-summary — the Reconciliation table lists only non-credit wallets
it('leaves credit cards out of the Reconciliation table', async () => {
  api.getWalletReconciliation.mockResolvedValue([
    { wallet_id: 'BCA', wallet_name: 'BCA Account', currency: 'IDR', wallet_type: 'checking',
      opening_balance: 1000, net_change: 500, calculated_closing: 1500, next_opening: null, difference: null },
    { wallet_id: 'MANDIRI_CC', wallet_name: 'Mandiri Credit Card', currency: 'IDR', wallet_type: 'credit',
      opening_balance: 2000, net_change: 100, calculated_closing: 2100, next_opening: null, difference: null },
  ]);
  render(<WalletBalances />);
  const reconTable = await screen.findByRole('table');
  expect(reconTable).toHaveTextContent('BCA Account');
  expect(reconTable).not.toHaveTextContent('Mandiri Credit Card');
});

it('shows error on fetch failure', async () => {
  api.getWalletBalances.mockRejectedValue(new Error('Network error'));
  render(<WalletBalances />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});
