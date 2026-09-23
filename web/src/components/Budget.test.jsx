import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, beforeEach, afterEach } from 'vitest';
import Budget from './Budget';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

// The pay cycle runs 26th -> 25th. Pin "now" mid-cycle so "today" sits inside it and the
// actual series has somewhere to stop.
const NOW = new Date(2026, 8, 30); // 30 Sep 2026 -> cycle 26 Sep - 25 Oct

function line(over = {}) {
  return {
    id: 1, name: 'Food & drink', kind: 'flex', currency: 'THB', target: 6800,
    due_day: null, match_wallets: ['BBL'], match_pattern: 'Kebab', match_categories: ['Food & Drink'],
    rec_min: 6870, rec_median: 8536, rec_max: 12306, rec_cycles: 5, sort_order: 0, active: true,
    ...over,
  };
}

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(NOW);
  api.getRecentFxRate.mockResolvedValue(535);
  api.getTransactions.mockResolvedValue([]);
  api.getBudgetLines.mockResolvedValue([line()]);
  api.updateBudgetLineTarget.mockResolvedValue(undefined);
});

afterEach(() => {
  vi.useRealTimers();
  vi.resetAllMocks();
});

// ac: budget-cycle-target-vs-actual — opens on the pay cycle in progress with no interaction
it('opens on the current 26th-to-25th pay cycle', async () => {
  render(<Budget />);
  expect(await screen.findByText('26 Sep – 25 Oct')).toBeInTheDocument();
});

// ac: budget-cycle-target-vs-actual — the actual series is cumulative spend to date
it('sums matching transactions in the cycle into the line actual', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab LineMan', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
    { id: 2, date: '2026-09-28', item: 'Kebab LineMan', amount: 200, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
  ]);
  render(<Budget />);
  expect(await screen.findByText('฿500')).toBeInTheDocument();
});

// ac: budget-cycle-target-vs-actual — a transaction after today is not counted yet
it('ignores spend dated after today when accumulating the actual series', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab LineMan', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
    { id: 2, date: '2026-10-05', item: 'Kebab LineMan', amount: 900, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
  ]);
  render(<Budget />);
  expect(await screen.findByText('฿300')).toBeInTheDocument();
});

// ac: budget-cycle-target-vs-actual — a line denominated in THB still counts spend that posted on
// an IDR wallet, converted at the derived rate
it('converts an IDR transaction into a THB line at the derived rate', async () => {
  api.getBudgetLines.mockResolvedValue([line({ match_wallets: ['BBL', 'MANDIRI_CC'] })]);
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab Bangkok', amount: 53500, tx_type: 'expense', wallet: 'MANDIRI_CC', category: 'Other' },
  ]);
  render(<Budget />);
  expect(await screen.findByText('฿100')).toBeInTheDocument();
});

function targetYs(chart) {
  const d = chart.querySelector('.budget-line-target').getAttribute('d');
  return d.split(' ').map(seg => Number(seg.split(',')[1]));
}

// ac: budget-cycle-target-vs-actual — a fixed line's target steps up on its due day: flat before it,
// flat at the full amount after, with exactly one jump
it('steps a fixed line target on its due day rather than sloping', async () => {
  api.getBudgetLines.mockResolvedValue([
    line({ id: 2, name: 'Condo rent', kind: 'fixed', target: 11500, due_day: 10, match_pattern: 'Rent' }),
  ]);
  render(<Budget />);
  const ys = targetYs(await screen.findByRole('img', { name: /THB/ }));
  // Cycle is 26 Sep - 25 Oct, so due day 10 is index 14.
  const before = ys.slice(0, 14);
  const after = ys.slice(14);
  expect(new Set(before).size).toBe(1);
  expect(new Set(after).size).toBe(1);
  // SVG y grows downward, so the post-due value sits above (smaller y than) the pre-due baseline.
  expect(after[0]).toBeLessThan(before[0]);
});

// ac: budget-cycle-target-vs-actual — a flexible line's target slopes evenly across the cycle
it('slopes a flexible line target evenly from zero', async () => {
  render(<Budget />);
  const ys = targetYs(await screen.findByRole('img', { name: /THB/ }));
  const steps = ys.slice(1).map((y, i) => ys[i] - y);
  // Every day advances the target by the same amount, give or take the 0.1 the path
  // coordinates are rounded to.
  expect(Math.max(...steps) - Math.min(...steps)).toBeCloseTo(0, 0);
  expect(Math.min(...steps)).toBeGreaterThan(0);
});

// ac: budget-line-visibility — the swatch toggles the line's series off the chart
it('hides a line from the chart when its swatch is toggled off', async () => {
  const user = userEvent.setup();
  render(<Budget />);
  const chart = await screen.findByRole('img', { name: /THB/ });
  const before = chart.querySelectorAll('.budget-line-actual').length;
  await user.click(screen.getByRole('button', { name: /Hide Food & drink/i }));
  expect(screen.getByRole('button', { name: /Show Food & drink/i })).toBeInTheDocument();
  expect(chart.querySelectorAll('.budget-line-actual').length).toBe(before - 1);
});

// ac: budget-line-visibility — with every line hidden there is nothing left to scale to
it('shows an empty chart state once all lines are hidden', async () => {
  const user = userEvent.setup();
  render(<Budget />);
  await user.click(await screen.findByRole('button', { name: /Hide Food & drink/i }));
  // The synthetic Unbudgeted line is still on the THB chart until it too is hidden.
  await user.click(screen.getAllByRole('button', { name: /Hide Unbudgeted/i })[0]);
  expect(screen.getAllByText('No lines selected.').length).toBeGreaterThan(0);
});

// ac: budget-line-target-editing — editing a target persists it
it('persists an edited target and shows the new value', async () => {
  const user = userEvent.setup();
  render(<Budget />);
  await user.click(await screen.findByRole('button', { name: '฿6,800' }));
  const input = screen.getByRole('spinbutton');
  await user.clear(input);
  await user.type(input, '5200');
  await user.keyboard('{Enter}');
  expect(api.updateBudgetLineTarget).toHaveBeenCalledWith(1, 5200);
  expect(await screen.findByRole('button', { name: '฿5,200' })).toBeInTheDocument();
});

// ac: budget-line-target-editing — the observed range from recent cycles is shown alongside
it('shows the recommended range from recent pay cycles', async () => {
  render(<Budget />);
  const row = (await screen.findByText('Food & drink')).closest('.budget-row');
  expect(within(row).getByText(/seen ฿7k–฿12k/)).toBeInTheDocument();
  expect(within(row).getByText(/median ฿9k/)).toBeInTheDocument();
});

// ac: budget-line-target-editing — a line nothing has matched says so rather than showing 0–0
it('says no history matched for a line with no matching cycles', async () => {
  api.getBudgetLines.mockResolvedValue([
    line({ id: 3, name: 'Dad', currency: 'IDR', target: 600000, rec_min: 0, rec_median: 0, rec_max: 0, rec_cycles: 0 }),
  ]);
  render(<Budget />);
  expect(await screen.findByText('no history matched yet')).toBeInTheDocument();
});

// A malformed stored pattern must not take the whole chart down.
it('renders even when a line has an invalid match pattern', async () => {
  api.getBudgetLines.mockResolvedValue([line({ match_pattern: '([unclosed' })]);
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
  ]);
  render(<Budget />);
  // Falls back to the category rule, so the row still totals.
  expect(await screen.findByText('฿300')).toBeInTheDocument();
});

// ac: assign-transaction-budget-line — a manual override wins over the automatic match rules
it('routes a transaction to its manually assigned line even when the automatic rules would pick a different one', async () => {
  api.getBudgetLines.mockResolvedValue([
    line({ id: 1, name: 'Food & drink' }),
    line({ id: 2, name: 'Dad', match_wallets: ['BCA'], match_pattern: 'Abah', match_categories: null }),
  ]);
  api.getTransactions.mockResolvedValue([
    // Wallet and category match "Food & drink", but the override sends it to "Dad" instead.
    { id: 1, date: '2026-09-27', item: 'Kebab LineMan', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink', budget_line_id: 2 },
  ]);
  render(<Budget />);
  const dadRow = (await screen.findByText('Dad')).closest('.budget-row');
  const foodRow = (await screen.findByText('Food & drink')).closest('.budget-row');
  expect(within(dadRow).getByText('฿300')).toBeInTheDocument();
  expect(within(foodRow).getByText('฿0')).toBeInTheDocument();
});

// ac: assign-transaction-budget-line — an override naming a line that no longer exists falls back
// to the automatic rules instead of losing the transaction
it('falls back to automatic matching when the assigned line is no longer active', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab LineMan', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink', budget_line_id: 999 },
  ]);
  render(<Budget />);
  expect(await screen.findByText('฿300')).toBeInTheDocument();
});

// Transfers between the user's own wallets are movements, not spending.
it('excludes self-transfers from a line total', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
    { id: 2, date: '2026-09-28', item: 'Kebab transfer', amount: 5000, tx_type: 'transfer', wallet: 'BBL', to_wallet: 'BCA', category: 'Food & Drink' },
  ]);
  render(<Budget />);
  expect(await screen.findByText('฿300')).toBeInTheDocument();
});

// Spend that matches no budget line must still appear, or the chart understates by omission.
it('collects spend matching no line into an Unbudgeted line in its own currency', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kebab LineMan', amount: 300, tx_type: 'expense', wallet: 'BBL', category: 'Food & Drink' },
    { id: 2, date: '2026-09-27', item: 'Kontrakan', amount: 12000000, tx_type: 'expense', wallet: 'BCA', category: 'Bills' },
    { id: 3, date: '2026-09-28', item: 'Xscape', amount: 460, tx_type: 'expense', wallet: 'BBL', category: 'Entertainment' },
  ]);
  render(<Budget />);
  const thbRow = (await screen.findAllByText('Unbudgeted'))[0].closest('.budget-row');
  const idrRow = (await screen.findAllByText('Unbudgeted'))[1].closest('.budget-row');
  // The BBL entertainment row matches no seeded line here, so it lands on THB Unbudgeted.
  expect(within(thbRow).getByText('฿460')).toBeInTheDocument();
  expect(within(idrRow).getByText('Rp 12,000,000')).toBeInTheDocument();
});

// The Unbudgeted line has no database row, so its target must not offer an edit affordance.
it('does not allow editing the Unbudgeted target', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-27', item: 'Kontrakan', amount: 500000, tx_type: 'expense', wallet: 'BCA', category: 'Bills' },
  ]);
  render(<Budget />);
  const row = (await screen.findAllByText('Unbudgeted'))[1].closest('.budget-row');
  expect(within(row).queryByRole('button', { name: /^Rp/ })).toBeNull();
  expect(within(row).getByText('spend matching no budget line')).toBeInTheDocument();
});

// The budget is set before the cycle it governs starts, so the upcoming cycle must be reachable.
it('can step forward to the upcoming pay cycle', async () => {
  const user = userEvent.setup();
  render(<Budget />);
  await screen.findByText('26 Sep – 25 Oct');
  await user.click(screen.getByRole('button', { name: /Next/ }));
  expect(await screen.findByText('26 Oct – 25 Nov')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Next/ })).toBeDisabled();
});
