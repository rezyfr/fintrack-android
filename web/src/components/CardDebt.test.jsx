import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, beforeEach, afterEach } from 'vitest';
import CardDebt from './CardDebt';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const BILLING = [
  { wallet: 'BCA_CC', cutoff_day: 3, due_day: 19, min_percent: 5, min_full_installments: true },
  { wallet: 'MANDIRI_CC', cutoff_day: 11, due_day: 1, min_percent: 5, min_full_installments: false },
];

beforeEach(() => {
  // Every test needs the billing config loaded; individual tests still set getTransactions.
  api.getCardBilling.mockResolvedValue(BILLING);
});

afterEach(() => {
  vi.useRealTimers();
  vi.resetAllMocks();
});

// Default selected wallet is BCA Credit Card (first credit-type wallet in WALLETS order).

// ac: track-cc-transactions-as-debt — each charge shows Paid, Partially Paid, or Unpaid
it('shows Paid for a charge fully covered by a later payment', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-06-01', item: 'Groceries', amount: 100000, tx_type: 'expense', wallet: 'BCA_CC' },
    { id: 2, date: '2026-06-15', item: 'Payment', amount: 100000, tx_type: 'transfer', wallet: 'BCA', to_wallet: 'BCA_CC' },
  ]);
  const user = userEvent.setup();
  render(<CardDebt />);
  await screen.findByText('Total Unpaid');
  // Paid charges are hidden by the default unpaid-only filter — turn it off to see it.
  await user.click(screen.getByRole('checkbox', { name: /unpaid\/partial only/i }));
  await screen.findByText('Groceries');
  expect(screen.getByText('✓ Paid')).toBeInTheDocument();
});

// ac: track-cc-transactions-as-debt — a payment that doesn't fully cover a charge marks it Partially Paid
it('shows Partially Paid with the remaining amount when a payment only covers part of a charge', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-06-01', item: 'Big Purchase', amount: 100000, tx_type: 'expense', wallet: 'BCA_CC' },
    { id: 2, date: '2026-06-15', item: 'Payment', amount: 40000, tx_type: 'transfer', wallet: 'BCA', to_wallet: 'BCA_CC' },
  ]);
  render(<CardDebt />);
  const toggle = await screen.findByRole('checkbox', { name: /unpaid\/partial only/i });
  expect(toggle).toBeChecked();
  expect(await screen.findByText(/Partially Paid/)).toBeInTheDocument();
  expect(screen.getByText(/Rp 60,000 left/)).toBeInTheDocument();
});

// ac: track-cc-transactions-as-debt — a payment settles the oldest unpaid charges first (FIFO), and can
// only settle charges dated on or before its own date
it('settles the oldest charge first and cannot pay a charge dated after the payment', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-06-01', item: 'Old Charge', amount: 50000, tx_type: 'expense', wallet: 'BCA_CC' },
    { id: 2, date: '2026-06-10', item: 'Payment', amount: 50000, tx_type: 'transfer', wallet: 'BCA', to_wallet: 'BCA_CC' },
    { id: 3, date: '2026-06-20', item: 'New Charge', amount: 30000, tx_type: 'expense', wallet: 'BCA_CC' },
  ]);
  const user = userEvent.setup();
  render(<CardDebt />);
  await screen.findByText('Total Unpaid');

  // Old Charge should be paid (hidden by default unpaid-only filter); New Charge should remain unpaid.
  await user.click(screen.getByRole('checkbox', { name: /unpaid\/partial only/i }));
  expect(await screen.findByText('Old Charge')).toBeInTheDocument();
  const oldRow = screen.getByText('Old Charge').closest('tr');
  expect(oldRow).toHaveTextContent('✓ Paid');
  const newRow = screen.getByText('New Charge').closest('tr');
  expect(newRow).toHaveTextContent('Unpaid');
});

// ac: track-cc-transactions-as-debt — the page shows a total unpaid balance for the selected card
it('shows the total unpaid balance', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-06-01', item: 'Charge A', amount: 100000, tx_type: 'expense', wallet: 'BCA_CC' },
    { id: 2, date: '2026-06-05', item: 'Charge B', amount: 50000, tx_type: 'expense', wallet: 'BCA_CC' },
  ]);
  render(<CardDebt />);
  const label = await screen.findByText('Total Unpaid');
  expect(label.closest('.stat-card')).toHaveTextContent('Rp 150,000');
});

// ac: track-cc-transactions-as-debt — the page shows the total unpaid balance as of each past
// billing statement cutoff date, computed from only charges/payments dated on or before that cutoff
it('shows unpaid balance at each billing cutoff', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2020-01-01', item: 'Old Debt', amount: 75000, tx_type: 'expense', wallet: 'BCA_CC' },
  ]);
  render(<CardDebt />);
  await screen.findByText('Unpaid Balance at Each Billing Cutoff');
  // The charge long predates every recent cutoff and is never paid off, so it should
  // still show as outstanding at each checkpoint.
  expect(screen.getAllByText('Rp 75,000').length).toBeGreaterThan(0);
});

// ac: track-cc-transactions-as-debt — a wallet selector switches between Mandiri CC and BCA CC
it('refetches when switching the wallet selector', async () => {
  api.getTransactions.mockResolvedValue([]);
  const user = userEvent.setup();
  render(<CardDebt />);
  await screen.findByText('Total Unpaid');
  await user.click(screen.getByRole('button', { name: 'Mandiri Credit Card' }));
  expect(api.getTransactions).toHaveBeenLastCalledWith(expect.objectContaining({ wallet: 'MANDIRI_CC' }));
});

// ac: card-statement-due-and-minimum — statement balance, due date, and minimum shown for the card
it('shows the statement balance, due date, and minimum for BCA', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(new Date(2026, 9, 10)); // 10 Oct 2026 -> latest BCA cutoff 3 Oct, due 19 Oct
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-25', item: 'CICILAN BCA KE 03 DARI 03, TIKET.COM', amount: 2794654, tx_type: 'expense', wallet: 'BCA_CC' },
    { id: 2, date: '2026-09-20', item: 'Naga groceries', amount: 1031263, tx_type: 'expense', wallet: 'BCA_CC' },
  ]);
  render(<CardDebt />);
  const card = (await screen.findByText(/Statement closed/)).closest('.statement-card');
  // balance 3,825,917; minimum = 2,794,654 + 5% of 1,031,263 = 2,846,217
  expect(within(card).getByText('Rp 3,825,917')).toBeInTheDocument();
  expect(within(card).getByText('Rp 2,846,217')).toBeInTheDocument();
  expect(within(card).getByText('19 Oct 26')).toBeInTheDocument();
});

// ac: card-statement-due-and-minimum — Mandiri uses a plain 5% with no installment component
it('uses a plain 5% minimum for Mandiri', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(new Date(2026, 8, 20)); // 20 Sep -> latest Mandiri cutoff 11 Sep, due 1 Oct
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-05', item: 'Shopping', amount: 20000000, tx_type: 'expense', wallet: 'MANDIRI_CC' },
  ]);
  const user = userEvent.setup();
  render(<CardDebt />);
  await user.click(await screen.findByRole('button', { name: /Mandiri Credit Card/i }));
  const card = (await screen.findByText(/Statement closed/)).closest('.statement-card');
  // 5% of 20,000,000 = 1,000,000, due 1 Oct
  expect(within(card).getByText('Rp 1,000,000')).toBeInTheDocument();
  expect(within(card).getByText('01 Oct 26')).toBeInTheDocument();
});

// ac: card-statement-due-and-minimum — billing config comes from the stored record, not hardcoded
it('falls back with a warning when a card has no stored billing record', async () => {
  api.getCardBilling.mockResolvedValue([]); // nothing stored
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-01', item: 'Shopping', amount: 100000, tx_type: 'expense', wallet: 'BCA_CC' },
  ]);
  render(<CardDebt />);
  expect(await screen.findByText(/No billing record stored/i)).toBeInTheDocument();
});
