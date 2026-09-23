import { detectFormat, parseBcaCc, parseBcaSavings, parseMandiriCc, parseMandiriSavings, parseBbl } from './pdfParser';

// Real text fragments captured from actual PDFs.
// pdfjs returns each page as ONE line of text with all rows joined by spaces.

const BCA_SAVINGS_TEXT = `REKENING TAHAPAN XPRESI FIDRIYANTO RIZKILLAH TANGGAL   KETERANGAN   CBG   MUTASI   SALDO 1 / 2  01/04   SALDO AWAL   449,819.18 01/04   TRSF E-BANKING DB   0104/FTFVA/WS95271 71237/XL-XCFlex M - - 085960540717 51,500.00   DB   398,319.18 07/04   TRSF E-BANKING DB   0704/FTSCY/WS95271 350000.00 SAVIRA ARTAMEVIA 350,000.00   DB   48,319.18 17/04   BIAYA ADM   10,000.00   DB   38,319.18 26/04   BI-FAST CR   BIF TRANSFER DR 008 FIDRIYANTO RIZKILL 6,100,000.00 26/04   TRSF E-BANKING DB   2604/FTSCY/WS95271 600000.00 FIZARIANI 600,000.00   DB 26/04   BI-FAST DB   BIF TRANSFER KE 008 FIDRIYANTO RIZKILL MyBCA 1,000,000.00   DB   41,655.18`;

const BCA_CC_TEXT = `REKENING KARTU KREDIT  TANGGAL TRANSAKSI   PEMBUKUAN   KETERANGAN   JUMLAH (RP) 06-APR   06-APR   NAGA SWALAYAN,PEKAYON   BEKASI   ID   472.996 17-APR   17-APR   ALFAMRT CH13 BVH4 BOU   BEKASI   ID   108.500 03-MEI   03-MEI   ALFAMRT C735 RNH RAYA   BEKASI   ID   82.500  SUBTOTAL TRANSAKSI   1.545.226 26-APR   26-APR   PEMBAYARAN - MYBCA   320.096   CR`;

const MANDIRI_CC_TEXT = `Lembar Tagihan Kartu Kredit BAPAK FIDRIYANTO RIZKILLAH 09-Apr-26   12-Apr-26   Grab* A-96S83W8WXWQ7AV South JakartaID   63,940.00 11-Apr-26   13-Apr-26   WWW.GRAB.COM BANGKOK TH   35,155.00 (THB 65.00 x 540.84) 12-Apr-26   12-Apr-26   BUNGA CICILAN   0.00 15-Apr-26   17-Apr-26   CLAUDE.AI SUBSCRIPTION ANTHROPIC.COMUS   3,452,903.00 (USD 200.00 x 17,264.51) 26-Apr-26   26-Apr-26   PAYMENT THANK YOU - Livin   6,084,535.00   CR`;

const MANDIRI_SAVINGS_TEXT = `Rekening Koran Statement of Account MANDIRI TABUNGAN NOW 01/04   Saldo Awal   2,912,904.28 09/04   09/04   -20260409JAGBIDJA010O0200079570   630,292.00   3,543,196.28 09/04   09/04   MCM InhouseTrf-1132458961-15600160 REZY04   300,000.00   3,843,196.28 11/04   11/04   -20260411BMRIIDJA010O0223871013   450,000.00 D   3,393,196.28 30/04   30/04   Biaya Adm -   6,000.00 D   368,956.28`;

// Page-1 fragment ends mid-statement with no following date to bound the last row's segment
// (trailing disclaimer text follows instead) — page 2 repeats the header before resuming rows.
const BBL_TEXT = `Bangkok Bank Public Company Limited 079-8-06500-9 THB 01/07/2026 - 29/07/2026 วันที่ Date รายการ Particulars เลขที่เช็ค Chq.No. ถอน Withdrawal Deposit ฝาก คงเหลือ Balance ผานทาง Via STATEMENT OF SAVING ACCOUNT 01/07/26   B/F   12,515.66 01/07/26   TRF. PROMPTPAY   150.00   12,365.66   mPhone 02/07/26   TRF FR OTH BK   469.00   12,834.66   iBank 03/07/26   PMT FOR GOODS   65.00   12,769.66   mPhone ถาไมมีการคัดคานรายการใดใน Statement นี้ ภายใน 14 วัน Page 1/2
Bangkok Bank Public Company Limited 079-8-06500-9 THB 01/07/2026 - 29/07/2026 วันที่ Date รายการ Particulars เลขที่เช็ค Chq.No. ถอน Withdrawal Deposit ฝาก คงเหลือ Balance ผานทาง Via STATEMENT OF SAVING ACCOUNT 26/07/26   SALARY   61,885.42   74,655.08   Auto`;

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
it('detects BBL from Bangkok Bank header', () => {
  expect(detectFormat(BBL_TEXT)).toBe('BBL');
});

// ── parseBcaCc ───────────────────────────────────────────────
it('parses BCA CC expense row (dot-thousands amount)', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  expect(rows[0]).toMatchObject({
    date: '2026-04-06',
    amount: 472996,
    tx_type: 'expense',
  });
  expect(rows[0].item).toContain('NAGA SWALAYAN');
});
it('parses BCA CC payment (CR) as transfer', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  const cr = rows.find(r => r.tx_type === 'transfer');
  expect(cr).toMatchObject({ amount: 320096 });
  expect(cr.item).toContain('PEMBAYARAN');
});
it('handles Indonesian month MEI -> 05', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  expect(rows.find(r => r.date.startsWith('2026-05'))).toBeDefined();
});

// ac: bca-cc-statement-year-rollover
it('resolves December rows to the previous year in a January-dated statement', () => {
  const text = `REKENING KARTU KREDIT TANGGAL REKENING   :   03 JANUARI 2026 TANGGAL TRANSAKSI   PEMBUKUAN   KETERANGAN   JUMLAH (RP) 05-DES   05-DES   NAGA SWALAYAN,PEKAYON   BEKASI   ID   398.739 03-JAN   03-JAN   SUPERINDO THB   BEKASI   ID   164.520`;
  const rows = parseBcaCc(text);
  expect(rows.find(r => r.item.includes('NAGA SWALAYAN'))).toMatchObject({ date: '2025-12-05' });
  expect(rows.find(r => r.item.includes('SUPERINDO'))).toMatchObject({ date: '2026-01-03' });
});
// ac: bca-cc-month-abbreviation
it('parses an August row printed as AGU', () => {
  const text = `REKENING KARTU KREDIT TANGGAL REKENING   :   03 SEPTEMBER 2026 TANGGAL TRANSAKSI   PEMBUKUAN   KETERANGAN   JUMLAH (RP) 04-AGU   05-AGU   SPBU 34-12805 - POT   JAKARTA SELA ID   150.000 03-SEP   03-SEP   BIAYA BUNGA   83.729`;
  const rows = parseBcaCc(text);
  expect(rows.find(r => r.item.includes('SPBU'))).toMatchObject({ date: '2026-08-04', amount: 150000 });
  expect(rows.find(r => r.item === 'BIAYA BUNGA')).toMatchObject({ date: '2026-09-03', amount: 83729 });
});

// ac: bca-cc-installment-reversal
it('cancels a charge and its same-day REVERSAL CICILAN BCA credit, keeping the installment rows', () => {
  const text = `REKENING KARTU KREDIT TANGGAL REKENING   :   03 AGUSTUS 2026 TANGGAL TRANSAKSI   PEMBUKUAN   KETERANGAN   JUMLAH (RP) 26-JUL   26-JUL   CICILAN BCA KE 01 DARI 03, TIKET.COM 0%3   1.081.060 26-JUL   26-JUL   TIKET.COM 0%3BU*135511765JAKARTA SLT ID   3.243.180 26-JUL   26-JUL   REVERSAL CICILAN BCA TIKET.COM 0%3BU*135   3.243.180   CR`;
  const rows = parseBcaCc(text);
  expect(rows.find(r => r.amount === 3243180)).toBeUndefined();
  expect(rows.find(r => r.item.startsWith('REVERSAL'))).toBeUndefined();
  expect(rows.find(r => r.item.includes('KE 01 DARI 03'))).toMatchObject({ amount: 1081060, tx_type: 'expense' });
});
it('keeps a PEMBAYARAN credit that shares its amount with a same-day charge', () => {
  const text = `REKENING KARTU KREDIT TANGGAL REKENING   :   03 AGUSTUS 2026 TANGGAL TRANSAKSI   PEMBUKUAN   KETERANGAN   JUMLAH (RP) 24-JUL   24-JUL   NAGA SWALAYAN,PEKAYON   BEKASI   ID   409.885 24-JUL   24-JUL   PEMBAYARAN - MYBCA   409.885   CR`;
  const rows = parseBcaCc(text);
  // Only a REVERSAL line cancels a charge; a card payment is a real, separate money movement.
  expect(rows).toHaveLength(2);
  expect(rows.find(r => r.tx_type === 'transfer')).toMatchObject({ amount: 409885 });
});
it('falls back to the passed year when no statement-date marker is present', () => {
  const rows = parseBcaCc(BCA_CC_TEXT, 2026);
  expect(rows[0].date).toBe('2026-04-06');
});

// ── parseBcaSavings ──────────────────────────────────────────
it('parses BCA Savings real text and skips SALDO AWAL', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  expect(rows.length).toBeGreaterThan(0);
  expect(rows.find(r => /SALDO\s*AWAL/i.test(r.item))).toBeUndefined();
});
it('parses BCA Savings DB row as expense', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  const xl = rows.find(r => r.item.includes('XL-XCFlex'));
  expect(xl).toMatchObject({ date: '2026-04-01', amount: 51500, tx_type: 'expense' });
});
it('parses BCA Savings CR row (no DB) as income', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  const cr = rows.find(r => r.item.includes('BI-FAST CR'));
  expect(cr).toMatchObject({ date: '2026-04-26', amount: 6100000, tx_type: 'income' });
});
// ac: bca-savings-sub-thousand-amount
it('parses a sub-1,000 amount instead of the running balance that follows it', () => {
  const text = `REKENING TAHAPAN PERIODE : AGUSTUS 2026 31/08   BUNGA   37.32   3,393,597.61`;
  const rows = parseBcaSavings(text, 2026);
  expect(rows).toHaveLength(1);
  expect(rows[0]).toMatchObject({ date: '2026-08-31', item: 'BUNGA', amount: 37.32, tx_type: 'income' });
});
it('still ignores a separator-less amount embedded in a description', () => {
  const text = `REKENING TAHAPAN PERIODE : AGUSTUS 2026 01/08   TRSF E-BANKING CR   0108/FTSCY/WS95031 3000000.00 SAVIRA ARTAMEVIA   3,000,000.00   5,542,747.29`;
  const rows = parseBcaSavings(text, 2026);
  expect(rows[0]).toMatchObject({ amount: 3000000, tx_type: 'income' });
  expect(rows[0].item).toContain('SAVIRA ARTAMEVIA');
});
it('does NOT confuse non-comma reference numbers with amounts', () => {
  const rows = parseBcaSavings(BCA_SAVINGS_TEXT, 2026);
  const fiz = rows.find(r => r.item.includes('FIZARIANI'));
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
  expect(rows[0].item).toContain('Grab');
});
it('parses Mandiri CC payment (CR) as transfer', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  const cr = rows.find(r => r.tx_type === 'transfer');
  expect(cr).toMatchObject({ amount: 6084535 });
});
it('skips FX info paren and only captures actual tx amount', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  const claude = rows.find(r => r.item.includes('CLAUDE.AI'));
  expect(claude.amount).toBe(3452903);
});
it('parses zero-amount BUNGA CICILAN rows', () => {
  const rows = parseMandiriCc(MANDIRI_CC_TEXT);
  expect(rows.find(r => r.item === 'BUNGA CICILAN')?.amount).toBe(0);
});

// ac: mandiri-cc-ipp-conversion
it('cancels a charge and its same-day "Convert to IPP" reversal, keeping later installment rows', () => {
  const text = `Lembar Tagihan Kartu Kredit 20-Jan-26   20-Jan-26   Google Runna Running 650-2530000 US   958,802.00 (THB 1,750.00 x 547.88) 20-Jan-26   20-Jan-26   Convert to IPP Google Runna Running   958,802.00   CR (THB 1,750.00CR x 547.88) 20-Jan-26   20-Jan-26   Google Runna Running 650-2 001/004   239,702.00`;
  const rows = parseMandiriCc(text);
  expect(rows.find(r => r.amount === 958802)).toBeUndefined();
  expect(rows.find(r => r.item.includes('Convert to IPP'))).toBeUndefined();
  expect(rows.find(r => r.item.includes('001/004'))).toMatchObject({ amount: 239702, tx_type: 'expense' });
});
it('does not cancel an unrelated charge that merely shares an amount with a different-day reversal', () => {
  const text = `Lembar Tagihan Kartu Kredit 09-Apr-26   12-Apr-26   Grab* A-96S83W8WXWQ7AV South JakartaID   63,940.00 15-Apr-26   17-Apr-26   Convert to IPP Something Else   63,940.00   CR`;
  const rows = parseMandiriCc(text);
  // Different dates -> not a matching pair, so the Grab charge must survive.
  expect(rows.find(r => r.item.includes('Grab'))).toMatchObject({ amount: 63940, tx_type: 'expense' });
});

// ── parseMandiriSavings ──────────────────────────────────────
it('parses Mandiri Savings D row as expense', () => {
  const rows = parseMandiriSavings(MANDIRI_SAVINGS_TEXT, 2026);
  const debit = rows.find(r => r.tx_type === 'expense' && r.item.includes('20260411'));
  expect(debit).toMatchObject({ date: '2026-04-11', amount: 450000 });
});
it('parses Mandiri Savings credit (no D) as income', () => {
  const rows = parseMandiriSavings(MANDIRI_SAVINGS_TEXT, 2026);
  const credit = rows.find(r => r.item.includes('JAGBIDJA'));
  expect(credit).toMatchObject({ amount: 630292, tx_type: 'income' });
});
it('skips single-date Saldo Awal line', () => {
  const rows = parseMandiriSavings(MANDIRI_SAVINGS_TEXT, 2026);
  expect(rows.find(r => /Saldo\s+Awal/i.test(r.item))).toBeUndefined();
});

// ── parseBbl ──────────────────────────────────────────────────
// ac: bbl-statement-import
it('skips the B/F (brought forward) line but uses it to seed the running balance', () => {
  const rows = parseBbl(BBL_TEXT);
  expect(rows.find(r => r.item === 'B/F')).toBeUndefined();
});
it('infers expense from a balance decrease with no explicit DB marker', () => {
  const rows = parseBbl(BBL_TEXT);
  expect(rows.find(r => r.item === 'TRF. PROMPTPAY')).toMatchObject({
    date: '2026-07-01', amount: 150, tx_type: 'expense',
  });
});
it('infers income from a balance increase', () => {
  const rows = parseBbl(BBL_TEXT);
  expect(rows.find(r => r.item === 'TRF FR OTH BK')).toMatchObject({
    date: '2026-07-02', amount: 469, tx_type: 'income',
  });
});
it('categorizes a SALARY row as Salary income', () => {
  const rows = parseBbl(BBL_TEXT);
  expect(rows.find(r => r.item === 'SALARY')).toMatchObject({
    amount: 61885.42, tx_type: 'income', category: 'Salary',
  });
});
it('does not let a page-boundary row (no following date) bleed into disclaimer text', () => {
  const rows = parseBbl(BBL_TEXT);
  const last = rows.find(r => r.item === 'PMT FOR GOODS' && r.date === '2026-07-03');
  expect(last).toMatchObject({ amount: 65, tx_type: 'expense' });
});
