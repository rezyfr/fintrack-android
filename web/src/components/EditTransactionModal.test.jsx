import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import EditTransactionModal from './EditTransactionModal';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const MOCK_ROW = {
  id: 42,
  merchant: 'Grab',
  item: 'Food delivery',
  amount: 150,
  category: 'Food & Drink',
  channel: 'eWallet',
  date: '2026-05-01',
  wallet: 'BBL',
  tx_type: 'expense',
  to_wallet: null,
  tab: 'EXPENSES',
  note: 'lunch',
};

beforeEach(() => {
  api.updateTransaction.mockResolvedValue(undefined);
  api.deleteTransaction.mockResolvedValue(undefined);
});
afterEach(() => vi.resetAllMocks());

it('pre-populates merchant from row', () => {
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  expect(screen.getByLabelText('Merchant')).toHaveValue('Grab');
});

it('pre-populates wallet from row', () => {
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  expect(screen.getByLabelText('Wallet')).toHaveValue('BBL');
});

it('defaults wallet to BBL when row.wallet is null', () => {
  render(<EditTransactionModal row={{ ...MOCK_ROW, wallet: null }} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  expect(screen.getByLabelText('Wallet')).toHaveValue('BBL');
});

it('saves with correct payload and calls onSaved', async () => {
  const onSaved = vi.fn();
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={onSaved} onDeleted={vi.fn()} />);
  await user.click(screen.getByRole('button', { name: 'Save' }));
  await waitFor(() => {
    expect(api.updateTransaction).toHaveBeenCalledWith(42, expect.objectContaining({
      wallet: 'BBL',
      tx_type: 'expense',
      tab: 'EXPENSES',
    }));
    expect(onSaved).toHaveBeenCalled();
  });
});

it('shows delete confirmation when Delete is clicked', async () => {
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  expect(screen.getByText(/cannot be undone/i)).toBeInTheDocument();
  expect(api.deleteTransaction).not.toHaveBeenCalled();
});

it('calls deleteTransaction and onDeleted after confirmation', async () => {
  const onDeleted = vi.fn();
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={onDeleted} />);
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  await user.click(screen.getByRole('button', { name: 'Yes, delete' }));
  await waitFor(() => {
    expect(api.deleteTransaction).toHaveBeenCalledWith(42);
    expect(onDeleted).toHaveBeenCalledWith(42);
  });
});

it('cancelling delete confirmation returns to form', async () => {
  const user = userEvent.setup();
  render(<EditTransactionModal row={MOCK_ROW} onClose={vi.fn()} onSaved={vi.fn()} onDeleted={vi.fn()} />);
  await user.click(screen.getByRole('button', { name: 'Delete' }));
  await user.click(screen.getByRole('button', { name: 'Cancel' }));
  expect(screen.queryByText(/cannot be undone/i)).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Save' })).toBeInTheDocument();
});
