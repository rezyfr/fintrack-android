// ac: retire-pdf-import-tab — pure text parsers, no pdfjs and no browser entry point: the web app no
// longer imports statements, and scripts/parse_bank_statement.mjs does its own PDF text extraction.

const ID_MONTHS = {
  JAN:1,FEB:2,MAR:3,APR:4,MEI:5,JUN:6,JUL:7,AGT:8,SEP:9,OKT:10,NOV:11,DES:12,
  // ac: bca-cc-month-abbreviation — BCA CC statements print August as AGU, not AGT;
  // without this alias every August-dated row is silently dropped by the parsers below.
  AGU:8,
};
const EN_MONTHS = {
  Jan:1,Feb:2,Mar:3,Apr:4,May:5,Jun:6,Jul:7,Aug:8,Sep:9,Oct:10,Nov:11,Dec:12,
};
const ID_MONTHS_FULL = {
  JANUARI:1,FEBRUARI:2,MARET:3,APRIL:4,MEI:5,JUNI:6,JULI:7,
  AGUSTUS:8,SEPTEMBER:9,OKTOBER:10,NOVEMBER:11,DESEMBER:12,
};

function pad(n) { return String(n).padStart(2, '0'); }
function parseAmtComma(s) { return parseFloat(s.replace(/,/g, '')); }
function parseAmtDot(s)   { return parseFloat(s.replace(/\./g, '')); }

// Finds the statement's own printed month/year (e.g. "TANGGAL REKENING : 03 JANUARI 2026" for
// BCA CC, "PERIODE : JUNI 2026" for BCA Savings), so transaction rows can resolve their correct
// calendar year instead of assuming "now". Returns null if the marker isn't found (older/other
// statement layouts, or test fixtures), in which case callers fall back to the passed-in year.
function findStatementMonth(text, markerRegex) {
  const m = markerRegex.exec(text);
  if (!m) return null;
  const month = ID_MONTHS_FULL[m[1].toUpperCase()];
  if (!month) return null;
  return { month, year: Number(m[2]) };
}

// A transaction dated in a month LATER than the statement's own month belongs to the previous
// year — e.g. a "05-DES" (December) row inside a statement printed "03 JANUARI 2026" is really
// 2025-12-05, since the January 2026 statement's cycle starts partway through December 2025.
function resolveTxYear(txMonth, statement, fallbackYear) {
  if (!statement) return fallbackYear;
  return txMonth > statement.month ? statement.year - 1 : statement.year;
}

export function detectFormat(text) {
  // Use markers that appear in ONE PDF type only. Avoid "Rekening Koran"
  // (appears in BCA CC fine print) and "Lembar Tagihan Kartu Kredit"
  // (appears in Mandiri Savings consolidated statements).
  if (/REKENING TAHAPAN/.test(text))      return 'BCA_SAVINGS';
  if (/REKENING KARTU KREDIT/.test(text)) return 'BCA_CC';
  if (/mandirikartukredit/.test(text))    return 'MANDIRI_CC';
  if (/MANDIRI TABUNGAN/.test(text))      return 'MANDIRI_SAVINGS';
  if (/Bangkok Bank Public Company Limited|ธนาคารกรุงเทพ/.test(text)) return 'BBL';
  return 'UNKNOWN';
}

const BCA_CC_STATEMENT_DATE_RE = /TANGGAL REKENING\s*:\s*\d{2}\s+([A-Z]+)\s+(\d{4})/;

// BCA Credit Card: DD-MMM   DD-MMM   description   amount [CR]
// Amount uses dots as thousands separator, no decimals (Indonesian format).
export function parseBcaCc(text, year = new Date().getFullYear()) {
  const stmt = findStatementMonth(text, BCA_CC_STATEMENT_DATE_RE);
  const re = /(\d{2})-([A-Z]{3})\s+\d{2}-[A-Z]{3}\s+(.+?)\s+(\d{1,3}(?:\.\d{3})+)(\s+CR)?(?=\s|$)/g;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const month = ID_MONTHS[m[2]];
    if (!month) continue;
    rows.push({
      // ac: bca-cc-statement-year-rollover — a December row inside a January statement resolves to the previous year
      date: `${resolveTxYear(month, stmt, year)}-${pad(month)}-${m[1]}`,
      item: m[3].trim(),
      amount: parseAmtDot(m[4]),
      tx_type: m[5] ? 'transfer' : 'expense',
      category: 'Other',
    });
  }
  // ac: bca-cc-installment-reversal — see cancelReversals below
  return cancelReversals(rows, (item) => item.startsWith('REVERSAL CICILAN BCA'));
}

const BCA_SAVINGS_PERIOD_RE = /PERIODE\s*:\s*([A-Z]+)\s+(\d{4})/;

// BCA Savings: DD/MM   description   amount [DB] [balance]
// Each transaction has ONE date. Amount uses commas + .dd decimals.
// Row patterns:
//   credit (income):  desc  amount [balance]
//   debit  (expense): desc  amount DB [balance]
// Balance is optional (omitted on multi-row blocks).
export function parseBcaSavings(text, year = new Date().getFullYear()) {
  const stmt = findStatementMonth(text, BCA_SAVINGS_PERIOD_RE);
  const dateRe = /(?:^|\s)(\d{2})\/(\d{2})\s+/g;
  // ac: bca-savings-sub-thousand-amount — an amount under 1,000.00 has no comma group (the
  // BUNGA interest line, e.g. "BUNGA   37.32   3,393,597.61"). Requiring a comma group skipped it
  // and took the running balance in the next column as the amount instead. The \b anchors keep
  // separator-less numbers embedded in descriptions ("QRC014 00000.00IDM", "...WS95031 3000000.00")
  // from matching, since those have no word boundary before the digits that precede the dot.
  const amtRe  = /\b\d{1,3}(?:,\d{3})*\.\d{2}\b/g;
  const dbRe   = /\sDB(?=\s|$)/;
  // ac: bca-statement-footer-bleed — the last row's segment runs to end-of-text, which for the
  // whole (multi-page) document includes the trailing SALDO/MUTASI summary footer; that footer's
  // "MUTASI DB :" text false-matches dbRe if left in, misclassifying the final CR row as an expense.
  const footerRe = /\b(?:SALDO\s+(?:AWAL|AKHIR)|MUTASI\s+(?:CR|DB))\s*:/i;
  const rows = [];
  const dateMatches = [...text.matchAll(dateRe)];
  for (let i = 0; i < dateMatches.length; i++) {
    const m = dateMatches[i];
    const start = m.index + m[0].length;
    const end   = i + 1 < dateMatches.length ? dateMatches[i+1].index : text.length;
    let segment = text.slice(start, end);
    if (/^Saldo\s+(Awal|Akhir)|^SALDO\s+(AWAL|AKHIR)|^Bersambung/i.test(segment.trim())) continue;
    const footerMatch = footerRe.exec(segment);
    if (footerMatch) segment = segment.slice(0, footerMatch.index);
    const amts = [...segment.matchAll(amtRe)];
    if (amts.length === 0) continue;
    const isDebit = dbRe.test(segment);
    const txAmt = amts[0];
    const desc  = segment.slice(0, txAmt.index).trim();
    if (!desc) continue;
    // ac: bca-cc-statement-year-rollover — same rollover applies to a savings statement's own period
    const txMonth = Number(m[2]);
    rows.push({
      date: `${resolveTxYear(txMonth, stmt, year)}-${m[2]}-${m[1]}`,
      item: desc,
      amount: parseAmtComma(txAmt[0]),
      tx_type: isDebit ? 'expense' : 'income',
      category: 'Other',
    });
  }
  return rows;
}

// Mandiri Credit Card: DD-MMM-YY   DD-MMM-YY   description   amount [CR] [(FX info)]
// Amount uses commas + .dd decimals. FX info in parens follows for foreign-currency tx.
// Statement header has same date-date shape (Statement Date / Due Date) followed by
// "Tipe Kartu... Total Tagihan... amount". Skip those by keyword.
const MANDIRI_CC_HEADER = /Tipe\s*Kartu|Card Type|Statement Date|Pembayaran\s*Minimum|Tanggal\s*Cetak|Total Tagihan/i;
export function parseMandiriCc(text) {
  const re = /(\d{2})-([A-Za-z]{3})-(\d{2})\s+\d{2}-[A-Za-z]{3}-\d{2}\s+(.+?)\s+([\d,]+\.\d{2})(\s+CR)?(?=\s|$)/g;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const month = EN_MONTHS[m[2]];
    if (!month) continue;
    if (MANDIRI_CC_HEADER.test(m[4])) continue;
    rows.push({
      date: `${2000 + parseInt(m[3], 10)}-${pad(month)}-${m[1]}`,
      item: m[4].trim(),
      amount: parseAmtComma(m[5]),
      tx_type: m[6] ? 'transfer' : 'expense',
      category: 'Other',
    });
  }
  return cancelReversals(rows, (item) => item.startsWith('Convert to IPP'));
}

// ac: mandiri-cc-ipp-conversion — a purchase that Mandiri converts to an installment plan (IPP) is
// posted as a full-amount charge immediately followed, same day, by a "Convert to IPP <merchant>"
// reversal (CR) of the identical amount — the original charge nets to zero and the real debt shows
// up later as separate, smaller "NNN/NNN" installment rows. Left unfiltered, the reversed charge
// would be double-counted as debt on top of the installment rows that actually bill it.
// ac: bca-cc-installment-reversal — BCA does the same thing under a different label: the charge is
// followed, same day, by a "REVERSAL CICILAN BCA <merchant>" credit of the identical amount, and the
// debt bills as "CICILAN BCA KE nn DARI nn" rows. Verified against the statement's own SUBTOTAL
// TRANSAKSI, which counts the pair as zero.
function cancelReversals(rows, isReversal) {
  const drop = new Set();
  rows.forEach((r, i) => {
    if (drop.has(i) || r.tx_type !== 'transfer' || !isReversal(r.item)) return;
    const match = rows.findIndex((other, j) =>
      j !== i && !drop.has(j) && other.tx_type === 'expense' &&
      other.date === r.date && Math.abs(other.amount - r.amount) < 0.01
    );
    if (match !== -1) { drop.add(i); drop.add(match); }
  });
  return rows.filter((_, i) => !drop.has(i));
}

// Mandiri Savings: DD/MM   DD/MM   description   amount [D]   balance
// Both dates always present. The D marker between amount and balance = debit.
export function parseMandiriSavings(text, year = new Date().getFullYear()) {
  const re = /(\d{2})\/(\d{2})\s+\d{2}\/\d{2}\s+(.+?)\s+([\d,]+\.\d{2})(\s+D)?\s+([\d,]+\.\d{2})(?=\s|$)/g;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    rows.push({
      date: `${year}-${m[2]}-${m[1]}`,
      item: m[3].trim(),
      amount: parseAmtComma(m[4]),
      tx_type: m[5] ? 'expense' : 'income',
      category: 'Other',
    });
  }
  return rows;
}

// Bangkok Bank savings: DD/MM/YY   description   amount   balance   via-channel
// No explicit debit/credit marker (unlike BCA) — direction is inferred from the running
// balance delta between consecutive rows, seeded from the statement's own "B/F" (brought
// forward / opening balance) line. Segment-per-date (like parseBcaSavings) rather than one
// monolithic regex, so a line with no amount (e.g. B/F) can't let a non-greedy match bleed
// into the next real transaction's description.
// Not end-anchored: a page's last transaction has no following date to bound its segment, so
// trailing disclaimer/footer text ends up appended after the "via" token — match from the start
// and simply ignore whatever garbage follows.
const BBL_ROW_RE = /^(.+?)\s+([\d,]+\.\d{2})\s+([\d,]+\.\d{2})\s+([A-Za-z][\w-]*)/;
export function parseBbl(text) {
  const dateRe = /(\d{2})\/(\d{2})\/(\d{2})\s+/g;
  const dateMatches = [...text.matchAll(dateRe)];
  let runningBalance = null;
  const rows = [];
  for (let i = 0; i < dateMatches.length; i++) {
    const dm = dateMatches[i];
    const start = dm.index + dm[0].length;
    const end = i + 1 < dateMatches.length ? dateMatches[i + 1].index : text.length;
    const segment = text.slice(start, end).trim();

    if (/^B\/F\s+([\d,]+\.\d{2})$/.test(segment)) {
      runningBalance = parseAmtComma(/^B\/F\s+([\d,]+\.\d{2})$/.exec(segment)[1]);
      continue;
    }

    const m = BBL_ROW_RE.exec(segment);
    if (!m) continue; // header text, disclaimer, or summary footer — not a transaction row
    const [, particulars, amtStr, balStr] = m;
    const item = particulars.trim();
    const amount = parseAmtComma(amtStr);
    const balance = parseAmtComma(balStr);
    // ac: bbl-statement-import — direction inferred from whether the balance rose or fell
    const isIncome = runningBalance != null && balance > runningBalance;
    rows.push({
      date: `20${dm[3]}-${dm[2]}-${dm[1]}`,
      item,
      amount,
      tx_type: isIncome ? 'income' : 'expense',
      category: item.toUpperCase() === 'SALARY' ? 'Salary' : 'Other',
    });
    runningBalance = balance;
  }
  return rows;
}

// ac: retire-pdf-import-tab — detectFormat and the parseXxx functions above are the module's whole
// surface, consumed by scripts/parse_bank_statement.mjs (and its tests); the wallet-to-format map and
// parseStatement wrapper went with the Import tab.
