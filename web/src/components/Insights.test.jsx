import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import Insights from './Insights';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

// 10 Sep 2026 sits inside the cycle that opened on 26 Aug, so "this" resolves to 26 Aug - 25 Sep.
const INSIDE_CYCLE = new Date('2026-09-10T09:00:00');

beforeEach(() => {
  api.getTransactions.mockResolvedValue([]);
});

afterEach(() => {
  vi.useRealTimers();
  vi.resetAllMocks();
});

function setupUser() {
  return userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
}

// ac: insights-pay-cycle-periods — Insights opens on the current pay cycle with no interaction
it('opens on the pay cycle in progress', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(INSIDE_CYCLE);

  render(<Insights />);
  await screen.findByText('This Month');

  expect(api.getTransactions).toHaveBeenCalledWith(
    expect.objectContaining({ dateFrom: '2026-08-26', dateTo: '2026-09-25' })
  );
  // ac: insights-show-income-and-transfers — transfers are fetched over the same range
  expect(api.getTransactions).toHaveBeenCalledWith(
    expect.objectContaining({ txType: 'transfer', dateFrom: '2026-08-26', dateTo: '2026-09-25' })
  );
});

// ac: insights-pay-cycle-periods — the This Month preset covers the 26th through the 25th
it('starts the current cycle on pay day itself once the 26th arrives', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(new Date('2026-09-26T09:00:00'));

  render(<Insights />);
  await screen.findByText('This Month');

  expect(api.getTransactions).toHaveBeenCalledWith(
    expect.objectContaining({ dateFrom: '2026-09-26', dateTo: '2026-10-25' })
  );
});

// ac: insights-pay-cycle-periods — Last Month covers the cycle before the current one
it('resolves Last Month to the previous pay cycle', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(INSIDE_CYCLE);
  const user = setupUser();

  render(<Insights />);
  await user.click(screen.getByRole('button', { name: 'Last Month' }));

  expect(api.getTransactions).toHaveBeenCalledWith(
    expect.objectContaining({ dateFrom: '2026-07-26', dateTo: '2026-08-25' })
  );
});

// ac: insights-pay-cycle-periods — Last 3 Months covers the three most recent cycles as one range
it('resolves Last 3 Months to the three most recent cycles', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(INSIDE_CYCLE);
  const user = setupUser();

  render(<Insights />);
  await user.click(screen.getByRole('button', { name: 'Last 3 Months' }));

  expect(api.getTransactions).toHaveBeenCalledWith(
    expect.objectContaining({ dateFrom: '2026-06-26', dateTo: '2026-09-25' })
  );
});

// ac: insights-pay-cycle-periods — each preset shows the date range it resolves to
it('shows the resolved range next to the presets', async () => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(INSIDE_CYCLE);

  render(<Insights />);
  expect(await screen.findByText('26 Aug 26 – 25 Sep 26')).toBeInTheDocument();
});

// ac: insights-custom-range-and-multi-wallet — Custom preset shows day-precise From/To date inputs
it('shows day-precise date inputs when the Custom preset is selected', async () => {
  const user = userEvent.setup();
  render(<Insights />);
  await user.click(screen.getByRole('button', { name: 'Custom' }));
  expect(screen.getByLabelText('From')).toHaveAttribute('type', 'date');
  expect(screen.getByLabelText('To')).toHaveAttribute('type', 'date');
});

// ac: insights-custom-range-and-multi-wallet — the wallet filter allows selecting more than one wallet at once
it('fetches per-wallet transactions for each selected wallet when multiple wallets are chosen', async () => {
  const user = userEvent.setup();
  render(<Insights />);
  await screen.findByText('This Month');

  await user.click(screen.getByRole('button', { name: 'Bangkok Bank' }));
  await user.click(screen.getByRole('button', { name: 'Mandiri Credit Card' }));

  expect(api.getTransactions).toHaveBeenCalledWith(expect.objectContaining({ wallet: 'BBL' }));
  expect(api.getTransactions).toHaveBeenCalledWith(expect.objectContaining({ wallet: 'MANDIRI_CC' }));
});

// ac: insights-custom-range-and-multi-wallet — the custom date range and wallet selection combine with each other
it('combines the custom date range with the selected wallets', async () => {
  const user = userEvent.setup();
  render(<Insights />);
  await screen.findByText('This Month');

  await user.click(screen.getByRole('button', { name: 'Custom' }));
  await user.click(screen.getByRole('button', { name: 'Bangkok Bank' }));

  const fromInput = screen.getByLabelText('From');
  await user.clear(fromInput);
  await user.type(fromInput, '2026-06-26');

  expect(api.getTransactions).toHaveBeenCalledWith(
    expect.objectContaining({ wallet: 'BBL', dateFrom: '2026-06-26' })
  );
});

// ac: insights-custom-range-and-multi-wallet — selecting no wallets behaves the same as All wallets
it('treats deselecting back to no wallets the same as All wallets', async () => {
  const user = userEvent.setup();
  render(<Insights />);
  await screen.findByText('This Month');

  const bblButton = screen.getByRole('button', { name: 'Bangkok Bank' });
  await user.click(bblButton);
  api.getTransactions.mockClear();
  await user.click(bblButton);

  expect(api.getTransactions).toHaveBeenCalledWith(expect.objectContaining({ wallet: 'BCA' }));
  expect(api.getTransactions).toHaveBeenCalledWith(expect.objectContaining({ wallet: 'MANDIRI' }));
});

const EXPENSE_ROW = {
  id: 1, date: '2026-09-01', item: 'Lunch', amount: 500,
  tx_type: 'expense', category: 'Food & Drink', wallet: 'BBL',
};

// ac: expand-insights-category-to-transactions — clicking a category row expands it to show transactions
it('expands a category row to fetch and show its transactions', async () => {
  const user = userEvent.setup();
  api.getTransactions.mockResolvedValue([EXPENSE_ROW]);

  render(<Insights />);
  const categoryRow = await screen.findByRole('button', { name: /Food & Drink/ });
  await user.click(categoryRow);

  expect(api.getTransactions).toHaveBeenCalledWith(expect.objectContaining({ category: 'Food & Drink' }));
  expect(await screen.findByText('Lunch')).toBeInTheDocument();
});

// ac: expand-insights-category-to-transactions — clicking an expanded category row again collapses it
it('collapses an expanded category when clicked again', async () => {
  const user = userEvent.setup();
  api.getTransactions.mockResolvedValue([EXPENSE_ROW]);

  render(<Insights />);
  const categoryRow = await screen.findByRole('button', { name: /Food & Drink/ });
  await user.click(categoryRow);
  await screen.findByText('Lunch');
  await user.click(categoryRow);

  expect(screen.queryByText('Lunch')).not.toBeInTheDocument();
});
