import { applyCatalog } from '../constants/transaction';
import { buildInsights, buildSummary, convertToHome, isEmptyInsights } from './insightsSummary';

// Default accounts: BBL is THB; BCA, BCA_CC, MANDIRI and INVESTMENT are IDR.
const row = (id, tx_type, amount, wallet, extra = {}) => ({ id, date: '2026-10-01', item: `item${id}`, tx_type, amount, wallet, category: 'Other', ...extra });
const cycles = [{ from: '2026-08-26', to: '2026-09-25' }, { from: '2026-09-26', to: '2026-10-25' }];
const rows = [
  row(1, 'income', 300000, 'BCA'),
  row(2, 'expense', 220000, 'BCA', { category: 'Groceries' }),
  row(3, 'transfer', 37000, 'BBL', { to_wallet: 'BCA', to_amount: 20000000 }),
  row(4, 'transfer', 4000000, 'BCA', { to_wallet: 'BCA_CC' }),
  row(5, 'expense', 60, 'BBL', { category: 'Pets', subcategory: 'Food' }),
];
const prevRows = [row(9, 'expense', 100000, 'BCA', { category: 'Groceries' })];
const rates = { IDR: 1, THB: 500 };
const build = (extra) => buildSummary({ rows, prevRows, cycleRows: [prevRows, rows], cycles, rates, homeCode: 'IDR', wallet: null, code: 'IDR', ...extra });

it('treats an account of type investment as its own zone, whatever its id', () => {
  applyCatalog({
    accounts: [
      { id: 'BCA', name: 'BCA', currency: 'IDR', type: 'checking' },
      { id: 'BROKER', name: 'Broker', currency: 'IDR', type: 'investment' },
    ],
  });
  const flowRows = [row(1, 'transfer', 1000, 'BCA', { to_wallet: 'BROKER' })];
  const s = buildSummary({ rows: flowRows, prevRows: [], cycleRows: [flowRows], cycles, rates: { IDR: 1 }, homeCode: 'IDR', wallet: null, code: 'IDR' });
  expect(s.moneyOut).toBe(1000);
});

it('leaves a transfer inside one currency out of money in and out', () => {
  const s = build();
  expect(s.moneyIn).toBe(300000 + 20000000);
  expect(s.moneyOut).toBe(220000);
});

it('counts every transfer of a selected wallet', () => {
  const s = build({ wallet: 'BCA' });
  expect(s.moneyIn).toBe(300000 + 20000000);
  expect(s.moneyOut).toBe(220000 + 4000000);
});

it('combines the currencies in the home currency as one zone', () => {
  const s = build({ code: null });
  expect(s.currency).toBe('IDR');
  expect(s.moneyIn).toBe(300000);
  expect(s.moneyOut).toBe(220000 + 60 * 500);
});

it('carries the previous cycle and shows subcategories when any row has one', () => {
  const idr = build();
  expect(idr.categories[0]).toMatchObject({ category: 'Groceries', amount: 220000, previous: 100000, subcategories: [] });
  const thb = build({ code: 'THB' });
  expect(thb.categories[0].subcategories.map(s => s.name)).toEqual(['Food']);
});

it('builds top expenses, paid with, daily totals and the trend', () => {
  const s = build();
  expect(s.topExpenses.map(r => r.id)).toEqual([2]);
  expect(s.walletBreakdown).toEqual([{ wallet: 'BCA', amount: 220000, share: 1 }]);
  expect(s.dailySpend).toEqual({ '2026-10-01': 220000 });
  expect(s.trend.map(t => t.moneyOut)).toEqual([100000, 220000]);
});

it('converts rows of another currency and received amounts', () => {
  const out = convertToHome([row(1, 'expense', 10, 'BBL'), row(2, 'transfer', 5350, 'BCA', { to_wallet: 'BBL', to_amount: 10 })], { IDR: 1, THB: 535 });
  expect(out.map(r => r.amount)).toEqual([5350, 5350]);
  expect(out[1].to_amount).toBe(5350);
});

// ac: web-insights-parity - with two currencies besides home, each converts with its own rate, and a
// received amount converts with the rate of the destination wallet
describe('three currencies in use', () => {
  const three = { IDR: 1, THB: 500, USD: 16000 };
  const threeRows = [
    row(1, 'expense', 10, 'BBL'),
    row(2, 'expense', 2, 'US'),
    row(3, 'expense', 3000, 'BCA'),
    row(4, 'transfer', 5000, 'BBL', { to_wallet: 'US', to_amount: 156.25 }),
    row(5, 'income', 1, 'US'),
  ];
  const input = { rows: threeRows, prevRows: [], cycleRows: [threeRows], cycles: [cycles[1]], rates: three, homeCode: 'IDR', wallet: null };
  beforeEach(() => applyCatalog({
    accounts: [
      { id: 'BCA', name: 'BCA', currency: 'IDR' },
      { id: 'BBL', name: 'BBL', currency: 'THB' },
      { id: 'US', name: 'US', currency: 'USD' },
    ],
  }));

  it('converts every row with the rate of its own currency', () => {
    const out = convertToHome(threeRows, three);
    expect(out.map(r => r.amount)).toEqual([5000, 32000, 3000, 2500000, 16000]);
    expect(out[3].to_amount).toBe(2500000);
  });

  it('combines three currencies in the home currency, and a transfer between two foreign ones counts as one zone', () => {
    const s = buildSummary({ ...input, code: null });
    expect(s.currency).toBe('IDR');
    expect(s.moneyIn).toBe(16000);
    expect(s.moneyOut).toBe(5000 + 32000 + 3000);
  });

  it('returns one summary for each currency and the combined one', () => {
    const all = buildInsights({ ...input, codes: ['IDR', 'THB', 'USD'] });
    expect(Object.keys(all.byCurrency)).toEqual(['IDR', 'THB', 'USD']);
    expect(all.byCurrency.THB).toMatchObject({ moneyOut: 10 + 5000 });
    expect(all.byCurrency.USD).toMatchObject({ moneyIn: 1 + 156.25, moneyOut: 2 });
    expect(all.byCurrency.IDR.moneyOut).toBe(3000);
    expect(all.combined.moneyOut).toBe(40000);
  });
});

it('reports an empty cycle', () => expect(isEmptyInsights(build({ rows: [] }))).toBe(true));
