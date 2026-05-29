import { vi } from 'vitest';

vi.mock('pdfjs-dist', () => ({
  default: { GlobalWorkerOptions: {} },
  GlobalWorkerOptions: {},
  getDocument: vi.fn(),
}));

import { detectFormat, parseBcaCc, parseBcaSavings, parseMandiriCc, parseMandiriSavings } from './pdfParser';

// ── detectFormat ─────────────────────────────────────────────
it('detects BCA CC from text', () => {
  expect(detectFormat('BANK CENTRAL ASIA JUMLAH (RP) 06-APR 06-APR NAGA SWALAYAN 472.996')).toBe('BCA_CC');
});

it('detects BCA Savings from DB marker', () => {
  expect(detectFormat('BANK CENTRAL ASIA MUTASI SALDO 01/04 SALDO AWAL 51,500.00 DB')).toBe('BCA_SAVINGS');
});

it('detects Mandiri CC from date format', () => {
  expect(detectFormat('Bank Mandiri 09-Apr-26 12-Apr-26 Grab 63,940.00')).toBe('MANDIRI_CC');
});

it('detects Mandiri Savings from DD/MM + D marker', () => {
  expect(detectFormat('09/04 09/04 MCM Transfer Saldo 450,000.00 D')).toBe('MANDIRI_SAVINGS');
});

it('returns UNKNOWN for unrecognised text', () => {
  expect(detectFormat('random unrelated text')).toBe('UNKNOWN');
});

// ── parseBcaCc ───────────────────────────────────────────────
it('parses BCA CC expense row', () => {
  const rows = parseBcaCc('06-APR 06-APR NAGA SWALAYAN,PEKAYON BEKASI ID 472.996', 2026);
  expect(rows[0]).toMatchObject({ date: '2026-04-06', merchant: 'NAGA SWALAYAN,PEKAYON BEKASI ID', amount: 472996, tx_type: 'expense' });
});

it('parses BCA CC credit (CR) row as transfer', () => {
  const rows = parseBcaCc('26-APR 26-APR PEMBAYARAN - MYBCA 320.096 CR', 2026);
  expect(rows[0]).toMatchObject({ amount: 320096, tx_type: 'transfer' });
});

it('handles Indonesian month MEI', () => {
  const rows = parseBcaCc('03-MEI 03-MEI ALFAMRT C735 BEKASI 82.500', 2026);
  expect(rows[0].date).toBe('2026-05-03');
});

// ── parseBcaSavings ──────────────────────────────────────────
it('parses BCA Savings debit (DB) as expense', () => {
  const rows = parseBcaSavings('01/04 01/04 TRSF E-BANKING DB XL-XCFlex 51,500.00 DB', 2026);
  expect(rows[0]).toMatchObject({ date: '2026-04-01', amount: 51500, tx_type: 'expense' });
});

it('parses BCA Savings credit (no DB) as income', () => {
  const rows = parseBcaSavings('26/04 26/04 BI-FAST CR FIDRIYANTO RIZKILL 6,100,000.00', 2026);
  expect(rows[0]).toMatchObject({ amount: 6100000, tx_type: 'income' });
});

it('skips SALDO AWAL line in BCA Savings', () => {
  const rows = parseBcaSavings('01/04 SALDO AWAL 449,819.18', 2026);
  expect(rows).toHaveLength(0);
});

// ── parseMandiriCc ───────────────────────────────────────────
it('parses Mandiri CC expense row', () => {
  const rows = parseMandiriCc('09-Apr-26 12-Apr-26 Grab* A-96S83W8WXWQ7AV South Jakarta ID 63,940.00');
  expect(rows[0]).toMatchObject({ date: '2026-04-09', merchant: 'Grab* A-96S83W8WXWQ7AV South Jakarta ID', amount: 63940, tx_type: 'expense' });
});

it('parses Mandiri CC payment (CR) as transfer', () => {
  const rows = parseMandiriCc('26-Apr-26 26-Apr-26 PAYMENT THANK YOU - Livin 6,084,535.00 CR');
  expect(rows[0]).toMatchObject({ amount: 6084535, tx_type: 'transfer' });
});

// ── parseMandiriSavings ──────────────────────────────────────
it('parses Mandiri Savings debit (D) as expense', () => {
  const rows = parseMandiriSavings('11/04 11/04 -20260411BMRIIDJA 450,000.00 D', 2026);
  expect(rows[0]).toMatchObject({ date: '2026-04-11', amount: 450000, tx_type: 'expense' });
});

it('parses Mandiri Savings credit (no D) as income', () => {
  const rows = parseMandiriSavings('09/04 09/04 MCM InhouseTrf-1132458961 300,000.00', 2026);
  expect(rows[0]).toMatchObject({ amount: 300000, tx_type: 'income' });
});

it('skips Saldo Awal/Akhir lines', () => {
  const rows = parseMandiriSavings('01/04 Saldo Awal 2,912,904.28\n30/04 Saldo Akhir 368,956.28', 2026);
  expect(rows).toHaveLength(0);
});
