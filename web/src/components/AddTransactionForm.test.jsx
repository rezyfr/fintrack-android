import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import AddTransactionForm from './AddTransactionForm';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

afterEach(() => {
  vi.resetAllMocks();
});

async function fillRequiredFields(user) {
  await user.type(screen.getByLabelText(/merchant/i), 'Grab');
  await user.type(screen.getByLabelText(/item/i), 'Food delivery');
  await user.clear(screen.getByLabelText(/amount/i));
  await user.type(screen.getByLabelText(/amount/i), '150');
}

it('renders all 8 fields', () => {
  render(<AddTransactionForm />);
  expect(screen.getByLabelText(/merchant/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/item/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/amount/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/category/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/channel/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/tab/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/date/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/note/i)).toBeInTheDocument();
});

it('shows success message and resets form on successful submit', async () => {
  api.addTransaction.mockResolvedValue(undefined);
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await fillRequiredFields(user);
  await user.click(screen.getByRole('button', { name: /add transaction/i }));
  expect(await screen.findByRole('status')).toHaveTextContent('Transaction added');
  expect(screen.getByLabelText(/merchant/i)).toHaveValue('');
});

it('shows error message and preserves form on submit failure', async () => {
  api.addTransaction.mockRejectedValue(new Error('Supabase error: 500'));
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await fillRequiredFields(user);
  await user.click(screen.getByRole('button', { name: /add transaction/i }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Supabase error: 500');
  expect(screen.getByLabelText(/merchant/i)).toHaveValue('Grab');
});

it('submits with amount as a number', async () => {
  api.addTransaction.mockResolvedValue(undefined);
  const user = userEvent.setup();
  render(<AddTransactionForm />);
  await fillRequiredFields(user);
  await user.click(screen.getByRole('button', { name: /add transaction/i }));
  await screen.findByRole('status');
  const submitted = api.addTransaction.mock.calls[0][0];
  expect(typeof submitted.amount).toBe('number');
  expect(submitted.amount).toBe(150);
});
