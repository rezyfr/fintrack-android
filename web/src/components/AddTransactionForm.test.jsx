import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import AddTransactionForm from './AddTransactionForm';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

beforeEach(() => { api.addTransaction.mockResolvedValue(undefined); });
afterEach(() => vi.resetAllMocks());

it('shows THB symbol for BBL wallet by default', () => {
  render(<AddTransactionForm />);
  expect(screen.getByText('฿')).toBeInTheDocument();
});

it('shows Rp symbol when switching to an IDR wallet', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.selectOptions(screen.getByLabelText('Wallet'), 'BCA');
  expect(screen.getByText('Rp')).toBeInTheDocument();
});

it('shows To Wallet field when tx_type is transfer', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.selectOptions(screen.getByLabelText('Type'), 'transfer');
  expect(screen.getByLabelText('To Wallet')).toBeInTheDocument();
});

it('hides To Wallet field for non-transfer types', () => {
  render(<AddTransactionForm />);
  expect(screen.queryByLabelText('To Wallet')).not.toBeInTheDocument();
});

it('submits with wallet, tx_type, and derived tab', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.type(screen.getByLabelText('Merchant'), 'Grab');
  await user.type(screen.getByLabelText('Item'), 'Food');
  await user.type(screen.getByLabelText('Amount'), '150');
  await user.click(screen.getByRole('button', { name: 'Add Transaction' }));
  await waitFor(() => {
    expect(api.addTransaction).toHaveBeenCalledWith(expect.objectContaining({
      wallet: 'BBL',
      tx_type: 'expense',
      tab: 'EXPENSES',
    }));
  });
});

it('shows success message and resets form after submit', async () => {
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.type(screen.getByLabelText('Merchant'), 'Grab');
  await user.type(screen.getByLabelText('Item'), 'Food');
  await user.type(screen.getByLabelText('Amount'), '150');
  await user.click(screen.getByRole('button', { name: 'Add Transaction' }));
  expect(await screen.findByRole('status')).toHaveTextContent('Transaction added successfully');
  expect(screen.getByLabelText('Merchant')).toHaveValue('');
});

it('shows error message on submit failure', async () => {
  api.addTransaction.mockRejectedValue(new Error('Network error'));
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await user.type(screen.getByLabelText('Merchant'), 'Grab');
  await user.type(screen.getByLabelText('Item'), 'Food');
  await user.type(screen.getByLabelText('Amount'), '150');
  await user.click(screen.getByRole('button', { name: 'Add Transaction' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Network error');
});
