import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { vi } from 'vitest';
import TransactionList from './TransactionList';
import * as api from '../api/client';
import BookSwitch from './BookSwitch';
import { setBook, BOOK } from '../utils/book';

vi.mock('../api/client');

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

const MOCK_BUDGET_LINES = [
  { id: 5, name: 'Mom' },
  { id: 6, name: 'Dad' },
];

beforeEach(() => {
  api.getTransactions.mockResolvedValue(MOCK_ROWS);
  api.getBudgetLines.mockResolvedValue(MOCK_BUDGET_LINES);
});

afterEach(() => {
  vi.resetAllMocks();
  setBook(BOOK.COMBINED);
});

it('shows transaction rows after load', async () => {
  render(<TransactionList />);
  expect(await screen.findByText('Food delivery')).toBeInTheDocument();
  expect(screen.getAllByText('฿150.00').length).toBeGreaterThan(0);
});

it('shows wallet chip for each row', async () => {
  render(<TransactionList />);
  await screen.findByText('Food delivery');
  expect(screen.getByText('Bangkok Bank', { selector: '.chip-wallet' })).toBeInTheDocument();
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

// ac: assign-transaction-budget-line
it('shows Auto for a row with no budget line assigned', async () => {
  render(<TransactionList />);
  expect(await screen.findByText('Auto')).toBeInTheDocument();
});

// ac: assign-transaction-budget-line
it('assigns a transaction to a budget line from the Budget cell', async () => {
  const user = userEvent.setup();
  api.updateTransaction.mockResolvedValue();
  render(<TransactionList />);

  const cell = await screen.findByText('Auto');
  const td = cell.closest('td');
  await user.dblClick(cell);
  await user.selectOptions(within(td).getByRole('combobox'), '6');

  expect(api.updateTransaction).toHaveBeenCalledWith(1, { budget_line_id: 6 });
  // scope to the budget cell; "Dad" also exists as a subcategory <option>
  expect(await screen.findByText('Dad', { selector: '.tx-budget' })).toBeInTheDocument();
});

// ac: assign-transaction-budget-line
it('clears a budget line assignment back to Auto', async () => {
  const user = userEvent.setup();
  api.getTransactions.mockResolvedValue([{ ...MOCK_ROWS[0], budget_line_id: 6 }]);
  api.updateTransaction.mockResolvedValue();
  render(<TransactionList />);

  const cell = await screen.findByText('Dad', { selector: '.tx-budget' });
  const td = cell.closest('td');
  await user.dblClick(cell);
  await user.selectOptions(within(td).getByRole('combobox'), '');

  expect(api.updateTransaction).toHaveBeenCalledWith(1, { budget_line_id: null });
  expect(await screen.findByText('Auto')).toBeInTheDocument();
});

// ac: web-currency-books — the book switch narrows the list to one currency
it('shows only the rows of the picked book', async () => {
  const user = userEvent.setup();
  api.getTransactions.mockResolvedValue([
    { id: 1, date: '2026-05-01', item: 'Pad Thai Sukhumvit', amount: 150, category: 'Food & Drink', tab: 'EXPENSES', wallet: 'BBL', tx_type: 'expense' },
    { id: 2, date: '2026-05-02', item: 'Groceries Jakarta', amount: 50000, category: 'Groceries', tab: 'IDR_EXPENSES', wallet: 'BCA', tx_type: 'expense' },
  ]);
  render(<><BookSwitch /><TransactionList /></>);
  expect((await screen.findAllByText('Groceries Jakarta')).length).toBeGreaterThan(0);
  expect(screen.getAllByText('Pad Thai Sukhumvit').length).toBeGreaterThan(0);

  await user.click(screen.getByRole('button', { name: 'Thailand' }));
  expect(screen.getAllByText('Pad Thai Sukhumvit').length).toBeGreaterThan(0);
  expect(screen.queryAllByText('Groceries Jakarta')).toHaveLength(0);

  await user.click(screen.getByRole('button', { name: 'Indonesia' }));
  expect(screen.getAllByText('Groceries Jakarta').length).toBeGreaterThan(0);
  expect(screen.queryAllByText('Pad Thai Sukhumvit')).toHaveLength(0);
});
