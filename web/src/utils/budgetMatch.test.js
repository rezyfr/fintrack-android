import { budgetLineFor } from './budgetMatch';

// The same cases as Android's BudgetAttributionTest. BBL is THB; BCA and MANDIRI_CC are IDR.
const line = (id, currency, category, subcategory = null, extra = {}) => ({ id, name: `line${id}`, currency, category, subcategory, sort_order: id, ...extra });
const tx = (category, wallet, subcategory = null, extra = {}) => ({ tx_type: 'expense', category, wallet, subcategory, item: 'x', ...extra });
const lines = [line(1, 'THB', 'Bills', 'Rent'), line(2, 'THB', 'Bills'), line(3, 'THB', 'Groceries'), line(4, 'IDR', 'Groceries')];
const idOf = row => budgetLineFor(row, lines)?.id ?? null;

it('lets a subcategory line win over the category line', () => {
  expect(idOf(tx('Bills', 'BBL', 'Rent'))).toBe(1);
  expect(idOf(tx('Bills', 'BBL', 'Internet'))).toBe(2);
  expect(idOf(tx('Bills', 'BBL'))).toBe(2);
});

it('picks the book from the wallet currency', () => {
  expect(idOf(tx('Groceries', 'BBL'))).toBe(3);
  expect(idOf(tx('Groceries', 'MANDIRI_CC'))).toBe(4);
});

it('gives no budget to a category without a line', () => expect(idOf(tx('Other', 'BBL'))).toBeNull());

it('ignores a manual assignment when a category line exists', () => expect(idOf(tx('Groceries', 'BBL', null, { budget_line_id: 2 }))).toBe(3));

it('keeps the old rules for lines without a category', () => {
  const ruleLines = [line(9, 'THB', null, null, { match_pattern: 'MARKET' })];
  expect(budgetLineFor(tx('Other', 'BBL', null, { item: 'MARKET' }), ruleLines)?.id).toBe(9);
});
