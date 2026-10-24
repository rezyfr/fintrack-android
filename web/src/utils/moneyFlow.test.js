import { moneyFlow } from './moneyFlow';

// The same cases as Android's MoneyFlowTest.kt, so both apps stay in step.
const zones = { BCA: 'IDR', MANDIRI: 'IDR', BBL: 'THB', INVESTMENT: 'INV' };
const zoneOf = w => zones[w] ?? 'IDR';
const tx = (tx_type, amount, wallet, to_wallet = null, to_amount = null) => ({ tx_type, amount, wallet, to_wallet, to_amount });

it('counts income and expense in their own zone', () => {
  const rows = [tx('income', 100, 'BCA'), tx('expense', 30, 'BCA'), tx('expense', 5, 'BBL')];
  expect(moneyFlow(rows, 'IDR', zoneOf)).toEqual({ moneyIn: 100, moneyOut: 30, net: 70 });
  expect(moneyFlow(rows, 'THB', zoneOf)).toEqual({ moneyIn: 0, moneyOut: 5, net: -5 });
});

it('ignores a transfer inside one zone', () => {
  expect(moneyFlow([tx('transfer', 50, 'BCA', 'MANDIRI')], 'IDR', zoneOf)).toEqual({ moneyIn: 0, moneyOut: 0, net: 0 });
});

it('counts a cross-currency transfer out of the source and into the destination at the received amount', () => {
  const rows = [tx('transfer', 5350, 'BCA', 'BBL', 10)];
  expect(moneyFlow(rows, 'IDR', zoneOf).moneyOut).toBe(5350);
  expect(moneyFlow(rows, 'THB', zoneOf).moneyIn).toBe(10);
});

it('treats investments as their own zone', () => {
  const rows = [tx('transfer', 1000, 'BCA', 'INVESTMENT')];
  expect(moneyFlow(rows, 'IDR', zoneOf).moneyOut).toBe(1000);
});

it('counts every transfer of a wallet when the wallet is its own zone', () => {
  const walletZoneOf = w => (w === 'BCA' ? 'BCA' : 'OTHER');
  const rows = [
    tx('income', 100, 'BCA'),
    tx('expense', 30, 'BCA'),
    tx('transfer', 50, 'BCA', 'MANDIRI'),
    tx('transfer', 10, 'BBL', 'BCA', 5350),
  ];
  expect(moneyFlow(rows, 'BCA', walletZoneOf)).toEqual({ moneyIn: 5450, moneyOut: 80, net: 5370 });
});
