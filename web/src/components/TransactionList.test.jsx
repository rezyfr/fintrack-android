import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import TransactionList from './TransactionList';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const MOCK_ROWS = [
  {
    id: 1,
    date: '2026-05-01',
    merchant: 'Grab',
    item: 'Food delivery',
    amount: 150,
    category: 'Food & Drink',
    channel: 'eWallet',
    tab: 'EXPENSES',
    note: 'lunch',
  },
];

beforeEach(() => {
  api.getTransactions.mockResolvedValue(MOCK_ROWS);
});

afterEach(() => {
  vi.resetAllMocks();
});

it('shows transaction rows after load', async () => {
  render(<TransactionList />);
  expect(await screen.findByText('Grab')).toBeInTheDocument();
  expect(screen.getByText('Food delivery')).toBeInTheDocument();
  expect(screen.getByText('150')).toBeInTheDocument();
});

it('shows error message on fetch failure', async () => {
  api.getTransactions.mockRejectedValue(new Error('Network error'));
  render(<TransactionList />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});

it('re-fetches when tab filter changes', async () => {
  const user = userEvent.setup();
  render(<TransactionList />);
  await screen.findByText('Grab');
  await user.selectOptions(screen.getByRole('combobox', { name: /tab/i }), 'EXPENSES');
  expect(api.getTransactions).toHaveBeenCalledTimes(2);
  expect(api.getTransactions).toHaveBeenLastCalledWith(expect.objectContaining({ tab: 'EXPENSES' }));
});
