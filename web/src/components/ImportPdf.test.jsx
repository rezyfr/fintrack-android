import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import ImportPdf from './ImportPdf';
import * as parser from '../utils/pdfParser';
import * as api from '../api/supabase';

vi.mock('../utils/pdfParser');
vi.mock('../api/supabase');

const MOCK_ROWS = [
  { date: '2026-04-09', item: 'Grab', amount: 63940, tx_type: 'expense', category: 'Transport' },
  { date: '2026-04-10', item: 'Gojek', amount: 30000, tx_type: 'expense', category: 'Transport' },
];

beforeEach(() => {
  parser.parseStatement.mockResolvedValue({ format: 'MANDIRI_CC', rows: MOCK_ROWS });
  api.addTransactions.mockResolvedValue(undefined);
});
afterEach(() => vi.resetAllMocks());

it('shows wallet selector and file input initially, Extract disabled', () => {
  render(<ImportPdf />);
  expect(screen.getByLabelText('Wallet')).toBeInTheDocument();
  expect(screen.getByLabelText('PDF Statement')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Extract Transactions' })).toBeDisabled();
});

it('enables Extract button once file is chosen', async () => {
  const user = userEvent.setup();
  render(<ImportPdf />);
  await user.upload(screen.getByLabelText('PDF Statement'), new File(['%PDF'], 's.pdf', { type: 'application/pdf' }));
  expect(screen.getByRole('button', { name: 'Extract Transactions' })).not.toBeDisabled();
});

it('shows extracted rows in preview table', async () => {
  const user = userEvent.setup();
  render(<ImportPdf />);
  await user.upload(screen.getByLabelText('PDF Statement'), new File(['%PDF'], 's.pdf', { type: 'application/pdf' }));
  await user.click(screen.getByRole('button', { name: 'Extract Transactions' }));
  expect(await screen.findByDisplayValue('Grab')).toBeInTheDocument();
  expect(screen.getByDisplayValue('Gojek')).toBeInTheDocument();
});

it('calls addTransactions with wallet and tx_type on import', async () => {
  const user = userEvent.setup();
  render(<ImportPdf />);
  await user.upload(screen.getByLabelText('PDF Statement'), new File(['%PDF'], 's.pdf', { type: 'application/pdf' }));
  await user.click(screen.getByRole('button', { name: 'Extract Transactions' }));
  await screen.findByDisplayValue('Grab');
  await user.click(screen.getByRole('button', { name: /import/i }));
  await waitFor(() => {
    expect(api.addTransactions).toHaveBeenCalledWith(
      expect.arrayContaining([
        expect.objectContaining({ item: 'Grab', tx_type: 'expense' }),
      ])
    );
  });
});

it('shows error alert when extraction fails', async () => {
  parser.parseStatement.mockRejectedValue(new Error('Bad PDF'));
  const user = userEvent.setup();
  render(<ImportPdf />);
  await user.upload(screen.getByLabelText('PDF Statement'), new File(['%PDF'], 's.pdf', { type: 'application/pdf' }));
  await user.click(screen.getByRole('button', { name: 'Extract Transactions' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Bad PDF');
});

it('shows success status and resets after import', async () => {
  const user = userEvent.setup();
  render(<ImportPdf />);
  await user.upload(screen.getByLabelText('PDF Statement'), new File(['%PDF'], 's.pdf', { type: 'application/pdf' }));
  await user.click(screen.getByRole('button', { name: 'Extract Transactions' }));
  await screen.findByDisplayValue('Grab');
  await user.click(screen.getByRole('button', { name: /import/i }));
  expect(await screen.findByRole('status')).toHaveTextContent(/imported/i);
  expect(screen.queryByDisplayValue('Grab')).not.toBeInTheDocument();
});
