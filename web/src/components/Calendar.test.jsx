import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi, beforeEach, afterEach } from 'vitest';
import Calendar from './Calendar';
import * as api from '../api/supabase';

vi.mock('../api/supabase');

const NOW = new Date(2026, 8, 30); // 30 Sep 2026 -> cycle 26 Sep - 25 Oct

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(NOW);
  api.getTransactions.mockResolvedValue([]);
});
afterEach(() => {
  vi.useRealTimers();
  vi.resetAllMocks();
});

// ac: cycle-calendar-daily-totals — opens on the current pay cycle
it('opens on the current 26th-to-25th pay cycle', async () => {
  render(<Calendar />);
  expect(await screen.findByText('26 Sep – 25 Oct')).toBeInTheDocument();
});

// ac: cycle-calendar-daily-totals — a day cell shows that day's spending total
it('sums a day\'s IDR spending onto its cell', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-28', item: 'Naga', amount: 700000, tx_type: 'expense', wallet: 'BCA' },
    { id: 2, date: '2026-09-28', item: 'Kopi', amount: 40000, tx_type: 'expense', wallet: 'BCA' },
  ]);
  render(<Calendar />);
  // 740,000 renders compact as Rp740k in the cell and the Spent total shows the full amount
  expect(await screen.findByText('Rp740k')).toBeInTheDocument();
  const spent = screen.getByText('Spent').closest('.stat-card');
  expect(within(spent).getByText('Rp 740,000')).toBeInTheDocument();
});

// ac: cycle-calendar-daily-totals — selecting a day lists that day's transactions
it('lists a day\'s transactions when its cell is tapped', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-28', item: 'Naga', amount: 700000, tx_type: 'expense', wallet: 'BCA' },
    { id: 2, date: '2026-09-29', item: 'Elsewhere', amount: 10000, tx_type: 'expense', wallet: 'BCA' },
  ]);
  const user = userEvent.setup();
  render(<Calendar />);
  await screen.findByText('Rp700k');
  await user.click(screen.getByText('Rp700k').closest('button'));
  const list = screen.getByText('28 Sep').closest('.cal-daylist');
  expect(within(list).getByText('Naga')).toBeInTheDocument();
  expect(within(list).queryByText('Elsewhere')).toBeNull();
});

// ac: cycle-calendar-daily-totals — currency toggle switches grid and totals
it('switches between IDR and THB', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-28', item: 'Naga', amount: 700000, tx_type: 'expense', wallet: 'BCA' },
    { id: 2, date: '2026-09-28', item: 'Makro', amount: 1850, tx_type: 'expense', wallet: 'BBL' },
  ]);
  const user = userEvent.setup();
  render(<Calendar />);
  expect(await screen.findByText('Rp700k')).toBeInTheDocument();
  await user.click(screen.getByRole('button', { name: '฿' }));
  // THB cell shows compact ฿2k; the Spent total shows the full ฿1,850
  expect(await screen.findByText('฿2k')).toBeInTheDocument();
  const spent = screen.getByText('Spent').closest('.stat-card');
  expect(within(spent).getByText('฿1,850')).toBeInTheDocument();
  expect(screen.queryByText('Rp700k')).toBeNull();
});

// ac: cycle-calendar-daily-totals — transfers are movements, not spending
it('excludes self-transfers from the totals', async () => {
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-09-28', item: 'Naga', amount: 700000, tx_type: 'expense', wallet: 'BCA' },
    { id: 2, date: '2026-09-28', item: 'To Mandiri', amount: 5000000, tx_type: 'transfer', wallet: 'BCA', to_wallet: 'MANDIRI' },
  ]);
  render(<Calendar />);
  const spent = (await screen.findByText('Spent')).closest('.stat-card');
  expect(within(spent).getByText('Rp 700,000')).toBeInTheDocument();
});

// ac: cycle-calendar-daily-totals — Previous/Next move one cycle and update the header
it('moves one pay cycle with the nav controls', async () => {
  const user = userEvent.setup();
  render(<Calendar />);
  await screen.findByText('26 Sep – 25 Oct');
  await user.click(screen.getByRole('button', { name: 'Previous cycle' }));
  expect(await screen.findByText('26 Aug – 25 Sep')).toBeInTheDocument();
  expect(api.getTransactions).toHaveBeenLastCalledWith(
    expect.objectContaining({ dateFrom: '2026-08-26', dateTo: '2026-09-25' }),
  );
});
