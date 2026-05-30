import { vi } from 'vitest';

vi.mock('pdfjs-dist', () => ({
  default: { GlobalWorkerOptions: {} },
  GlobalWorkerOptions: {},
  getDocument: vi.fn(),
}));

import { detectFormat, parseBcaCc, parseBcaSavings, parseMandiriCc, parseMandiriSavings } from './pdfParser';

// Real text fragments captured from actual PDFs.
// pdfjs returns each page as ONE line of text with all rows joined by spaces.

const BCA_SAVINGS_TEXT = `REKENING TAHAPAN XPRESI FIDRIYANTO RIZKILLAH TANGGAL   KETERANGAN   CBG   MUTASI   SALDO 1 / 2  01/04   SALDO AWAL   449,819.18 01/04   TRSF E-BANKING DB   0104/FTFVA/WS95271 71237/XL-XCFlex M - - 085960540717 51,500.00   DB   398,319.18 07/04   TRSF E-BANKING DB   0704/FTSCY/WS95271 350000.00 SAVIRA ARTAMEVIA 350,000.00   DB   48,319.18 17/04   BIAYA ADM   10,000.00   DB   38,319.18 26/04   BI-FAST CR   BIF TRANSFER DR 008 FIDRIYANTO RIZKILL 6,100,000.00 26/04   TRSF E-BANKING DB   2604/FTSCY/WS95271 600000.00 FIZARIANI 600,000.00   DB 26/04   BI-FAST DB   BIF TRANSFER KE 008 FIDRIYANTO RIZKILL MyBCA 1,000,000.00   DB   41,655.18`;

const BCA_CC_TEXT = `REKENING KARTU KREDIT  TANGGAL TRANSAKSI   PEMBUKUAN   KETERANGAN   JUMLAH (RP) 06-APR   06-APR   NAGA SWALAYAN,PEKAYON   BEKASI   ID   472.996 17-APR   17-APR   ALFAMRT CH13 BVH4 BOU   BEKASI   ID   108.500 03-MEI   03-MEI   ALFAMRT C735 RNH RAYA   BEKASI   ID   82.500  SUBTOTAL TRANSAKSI   1.545.226 26-APR   26-APR   PEMBAYARAN - MYBCA   320.096   CR`;

const MANDIRI_CC_TEXT = `Lembar Tagihan Kartu Kredit BAPAK FIDRIYANTO RIZKILLAH 09-Apr-26   12-Apr-26   Grab* A-96S83W8WXWQ7AV South JakartaID   63,940.00 11-Apr-26   13-Apr-26   WWW.GRAB.COM BANGKOK TH   35,155.00 (THB 65.00 x 540.84) 12-Apr-26   12-Apr-26   BUNGA CICILAN   0.00 15-Apr-26   17-Apr-26   CLAUDE.AI SUBSCRIPTION ANTHROPIC.COMUS   3,452,903.00 (USD 200.00 x 17,264.51) 26-Apr-26   26-Apr-26   PAYMENT THANK YOU - Livin   6,084,535.00   CR`;

const MANDIRI_SAVINGS_TEXT = `Rekening Koran Statement of Account MANDIRI TABUNGAN NOW 01/04   Saldo Awal   2,912,904.28 09/04   09/04   -20260409JAGBIDJA010O0200079570   630,292.00   3,543,196.28 09/04   09/04   MCM InhouseTrf-1132458961-15600160 REZY04   300,000.00   3,843,196.28 11/04   11/04   -20260411BMRIIDJA010O0223871013   450,000.00 D   3,393,196.28 30/04   30/04   Biaya Adm -   6,000.00 D   368,956.28`;

// ── detectFormat ─────────────────────────────────────────────
it('detects BCA Savings from REKENING TAHAPAN header', () => {
  expect(detectFormat(BCA_SAVINGS_TEXT)).toBe('BCA_SAVINGS');
});
it('detects BCA CC from REKENING KARTU KREDIT header', () => {
  expect(detectFormat(BCA_CC_TEXT)).toBe('BCA_CC');
});
it('detects Mandiri CC from mandirikartukredit marker', () => {
  expect(detectFormat(MANDIRI_CC_TEXT + ' mandirikartukredit')).toBe('MANDIRI_CC');
});
it('detects Mandiri Savings from MANDIRI TABUNGAN header', () => {
  expect(detectFormat(MANDIRI_SAVINGS_TEXT)).toBe('MANDIRI_SAVINGS');
});
it('returns UNKNOWN for unrecognised text', () => {
  expect(detectFormat('random unrelated text')).toBe('UNKNOWN');
});

// ── parseBcaCc ───────────────────────────────────────────────
it('parses BCA CC expense row (dot-thousands amount)', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  expect(rows[0]).toMatchObject({
    date: '2026-04-06',
    amount: 472996,
    tx_type: 'expense',
  });
  expect(rows[0].merchant).toContain('NAGA SWALAYAN');
});
it('parses BCA CC payment (CR) as transfer', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  const cr = rows.find(r => r.tx_type === 'transfer');
  expect(cr).toMatchObject({ amount: 320096 });
  expect(cr.merchant).toContain('PEMBAYARAN');
});
it('handles Indonesian month MEI -> 05', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  expect(rows.find(r => r.date.startsWith('2026-05'))).toBeDefined();
});

// ── parseBcaSavings ──────────────────────────────────────────
it('parses BCA Savings real text and skips SALDO AWAL', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  expect(rows.length).toBeGreaterThan(0);
  expect(rows.find(r => /SALDO\s*AWAL/i.test(r.merchant))).toBeUndefined();
});
it('parses BCA Savings DB row as expense', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  const xl = rows.find(r => r.merchant.includes('XL-XCFlex'));
  expect(xl).toMatchObject({ date: '2026-04-01', amount: 51500, tx_type: 'expense' });
});
it('parses BCA Savings CR row (no DB) as income', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  const cr = rows.find(r => r.merchant.includes('BI-FAST CR'));
  expect(cr).toMatchObject({ date: '2026-04-26', amount: 6100000, tx_type: 'income' });
});
it('does NOT confuse non-comma reference numbers with amounts', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  const fiz = rows.find(r => r.merchant.includes('FIZARIANI'));
  expect(fiz.amount).toBe(600000);
});

// ── parseMandiriCc ───────────────────────────────────────────
it('parses Mandiri CC expense row', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  expect(rows[0]).toMatchObject({
    date: '2026-04-09',
    amount: 63940,
    tx_type: 'expense',
  });
  expect(rows[0].merchant).toContain('Grab');
});
it('parses Mandiri CC payment (CR) as transfer', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  const cr = rows.find(r => r.tx_type === 'transfer');
  expect(cr).toMatchObject({ amount: 6084535 });
});
it('skips FX info paren and only captures actual tx amount', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  const claude = rows.find(r => r.merchant.includes('CLAUDE.AI'));
  expect(claude.amount).toBe(3452903);
});
it('parses zero-amount BUNGA CICILAN rows', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  expect(rows.find(r => r.merchant === 'BUNGA CICILAN')?.amount).toBe(0);
});

// ── parseMandiriSavings ──────────────────────────────────────
it('parses Mandiri Savings D row as expense', () => {
  const rows = parseMandiriSavings(MANDIRI_SAVINGS_TEXT, 2026);
  const debit = rows.find(r => r.tx_type === 'expense' && r.merchant.includes('20260411'));
  expect(debit).toMatchObject({ date: '2026-04-11', amount: 450000 });
});
it('parses Mandiri Savings credit (no D) as income', () => {
  const rows = parseMandiriSavings(MANDIRI_SAVINGS_TEXT, 2026);
  const credit = rows.find(r => r.merchant.includes('JAGBIDJA'));
  expect(credit).toMatchObject({ amount: 630292, tx_type: 'income' });
});
it('skips single-date Saldo Awal line', () => {
  const rows = parseMandiriSavings(MANDIRI_SAVINGS_TEXT, 2026);
  expect(rows.find(r => /Saldo\s+Awal/i.test(r.merchant))).toBeUndefined();
});
