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

beforeEach(() => { api.getWalletBalances.mockResolvedValue(MOCK_BALANCES); });
afterEach(() => vi.resetAllMocks());

it('shows all wallet names', async () => {
  render(<WalletBalances />);
  expect(await screen.findByText('Bangkok Bank')).toBeInTheDocument();
  expect(screen.getByText('BCA Account')).toBeInTheDocument();
  expect(screen.getByText('Mandiri Credit Card')).toBeInTheDocument();
  expect(screen.getByText('Investments')).toBeInTheDocument();
});

it('shows IDR net worth (assets minus CC debt)', async () => {
  render(<WalletBalances />);
  await screen.findByText('Bangkok Bank');
  // IDR net = 2,000,000 + 3,000,000 + 10,000,000 - 500,000 = 14,500,000
  expect(screen.getByText(/14,500,000/)).toBeInTheDocument();
});

it('shows error on fetch failure', async () => {
  api.getWalletBalances.mockRejectedValue(new Error('Network error'));
  render(<WalletBalances />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});
