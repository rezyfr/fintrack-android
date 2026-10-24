import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import Insights from './Insights';
import * as api from '../api/client';
import { setBook, BOOK } from '../utils/book';

vi.mock('../api/client');

// 10 Sep 2026 sits inside the cycle that opened on 26 Aug.
const INSIDE_CYCLE = new Date('2026-09-10T09:00:00');

const ROWS = [
  { id: 1, date: '2026-09-01', item: 'Salary', amount: 1000, tx_type: 'income', category: 'Salary', wallet: 'BCA' },
  { id: 2, date: '2026-09-02', item: 'Card bill', amount: 300, tx_type: 'transfer', category: 'Transfer', wallet: 'BCA', to_wallet: 'BCA_CC' },
  { id: 3, date: '2026-09-03', item: 'Top up', amount: 10, tx_type: 'transfer', category: 'Transfer', wallet: 'BBL', to_wallet: 'BCA', to_amount: 5000 },
  { id: 4, date: '2026-09-04', item: 'Pad Thai', amount: 50, tx_type: 'expense', category: 'Food & Drink', subcategory: 'Snacks', wallet: 'BBL' },
  { id: 5, date: '2026-09-05', item: 'Dog food', amount: 40, tx_type: 'expense', category: 'Pets', wallet: 'BBL' },
];
const PREV = [{ id: 9, date: '2026-08-05', item: 'Noodles', amount: 20, tx_type: 'expense', category: 'Food & Drink', wallet: 'BBL' }];

beforeEach(() => {
  vi.useFakeTimers({ shouldAdvanceTime: true });
  vi.setSystemTime(INSIDE_CYCLE);
  api.getRecentFxRate.mockResolvedValue(500);
  api.getTransactions.mockImplementation(({ dateFrom }) =>
    Promise.resolve(dateFrom === '2026-08-26' ? ROWS : dateFrom === '2026-07-26' ? PREV : []));
});

afterEach(() => {
  vi.useRealTimers();
  vi.resetAllMocks();
  setBook(BOOK.COMBINED);
});

const user = () => userEvent.setup({ advanceTimers: vi.advanceTimersByTime });
const stat = label => [...document.querySelectorAll('.stat-label')].find(e => e.textContent === label).nextSibling.textContent;

// ac: web-insights-parity — the page opens on the cycle in progress and reads each cycle on its own
it('opens on the pay cycle in progress', async () => {
  render(<Insights />);
  expect(await screen.findByText('September 2026')).toBeInTheDocument();
  expect(screen.getByText('26 Aug – 25 Sep')).toBeInTheDocument();
  expect(api.getTransactions).toHaveBeenCalledWith({ dateFrom: '2026-08-26', dateTo: '2026-09-25' });
  expect(api.getTransactions).toHaveBeenCalledWith({ dateFrom: '2026-07-26', dateTo: '2026-08-25' });
  expect(screen.getByRole('button', { name: 'Next cycle' })).toBeDisabled();
});

it('steps back one cycle', async () => {
  const u = user();
  render(<Insights />);
  await screen.findByText('September 2026');
  await u.click(screen.getByRole('button', { name: 'Previous cycle' }));
  expect(await screen.findByText('August 2026')).toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Next cycle' })).toBeEnabled();
});

// ac: web-insights-parity — Combined is one zone in the home currency at the transfer rate
it('shows Combined in rupiah with transfers between the currencies left out', async () => {
  render(<Insights />);
  await screen.findByText('Daily spending');
  expect(stat('Money in')).toBe('Rp 1,000');
  expect(stat('Money out')).toBe('Rp 45,000');
});

it('shows only the book currency, and a wallet counts all its transfers', async () => {
  const u = user();
  setBook('THB');
  const { unmount } = render(<Insights />);
  await screen.findByText('Daily spending');
  expect(stat('Money out')).toBe('฿100.00');
  unmount();

  setBook('IDR');
  render(<Insights />);
  await screen.findByText('Daily spending');
  await u.click(screen.getByRole('button', { name: 'BCA Account' }));
  expect(stat('Money in')).toBe('Rp 6,000');
  expect(stat('Money out')).toBe('Rp 300');
});

it('lists categories by their own name with the change and subcategories', async () => {
  const u = user();
  setBook('THB');
  render(<Insights />);
  const food = await screen.findByRole('button', { name: /Food & Drink/ });
  expect(within(food).getByText(/▲/)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Pets/ })).toBeInTheDocument();
  await u.click(food);
  expect(screen.getByText('Snacks')).toBeInTheDocument();
});

it('lists the largest expenses with a link to Transactions', async () => {
  const u = user();
  const open = vi.fn();
  setBook('THB');
  render(<Insights onOpenTransactions={open} />);
  expect(await screen.findByText('Largest expenses')).toBeInTheDocument();
  expect(screen.getByText('Dog food')).toBeInTheDocument();
  await u.click(screen.getByRole('button', { name: /All transactions/ }));
  expect(open).toHaveBeenCalled();
});
