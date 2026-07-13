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
    item: 'Food delivery',
    amount: 150,
    category: 'Food & Drink',
    tab: 'EXPENSES',
    wallet: 'BBL',
    tx_type: 'expense',
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
  expect(await screen.findByText('Food delivery')).toBeInTheDocument();
  expect(screen.getAllByText('฿150.00').length).toBeGreaterThan(0);
});

it('shows wallet chip for each row', async () => {
  render(<TransactionList />);
  await screen.findByText('Food delivery');
  expect(screen.getByText('BBL')).toBeInTheDocument();
});

it('shows error message on fetch failure', async () => {
  api.getTransactions.mockRejectedValue(new Error('Network error'));
  render(<TransactionList />);
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});

it('re-fetches when tx_type filter changes', async () => {
  const user = userEvent.setup();
  render(<TransactionList />);
  await screen.findByText('Food delivery');
  await user.click(screen.getByRole('button', { name: 'Expenses' }));
  expect(api.getTransactions).toHaveBeenCalledTimes(2);
  expect(api.getTransactions).toHaveBeenLastCalledWith(
    expect.objectContaining({ txType: 'expense' })
  );
});

it('re-fetches when wallet filter changes', async () => {
  const user = userEvent.setup();
  render(<TransactionList />);
  await screen.findByText('Food delivery');
  await user.selectOptions(screen.getByRole('combobox', { name: 'Wallet' }), 'BBL');
  expect(api.getTransactions).toHaveBeenCalledTimes(2);
  expect(api.getTransactions).toHaveBeenLastCalledWith(
    expect.objectContaining({ wallet: 'BBL' })
  );
});

it('shows an edit button for each loaded row', async () => {
  render(<TransactionList />);
  await screen.findByText('Food delivery');
  expect(screen.getByRole('button', { name: /edit food delivery/i })).toBeInTheDocument();
});

// ac: batch-edit-transaction-category
it('applies a batch category update to selected rows', async () => {
  const user = userEvent.setup();
  api.updateTransactionsCategory.mockResolvedValue();
  render(<TransactionList />);
  await screen.findByText('Food delivery');

  await user.click(screen.getByRole('checkbox', { name: /select food delivery/i }));
  await user.selectOptions(screen.getByRole('combobox', { name: 'Batch category' }), 'Groceries');
  await user.click(screen.getByRole('button', { name: 'Apply' }));

  expect(api.updateTransactionsCategory).toHaveBeenCalledWith([1], 'Groceries');
  const matches = await screen.findAllByText('Groceries');
  expect(matches.some(el => el.className.includes('chip'))).toBe(true);
});
