import * as pdfjsLib from 'pdfjs-dist';

let workerReady = false;
async function ensureWorker() {
  if (workerReady) return;
  const { default: PdfWorker } = await import('pdfjs-dist/build/pdf.worker.min.mjs?worker');
  pdfjsLib.GlobalWorkerOptions.workerPort = new PdfWorker();
  workerReady = true;
}

const ID_MONTHS = {
  JAN:1,FEB:2,MAR:3,APR:4,MEI:5,JUN:6,JUL:7,AGT:8,SEP:9,OKT:10,NOV:11,DES:12,
};
const EN_MONTHS = {
  Jan:1,Feb:2,Mar:3,Apr:4,May:5,Jun:6,Jul:7,Aug:8,Sep:9,Oct:10,Nov:11,Dec:12,
};

function pad(n) { return String(n).padStart(2, '0'); }
function parseAmtComma(s) { return parseFloat(s.replace(/,/g, '')); }
function parseAmtDot(s)   { return parseFloat(s.replace(/\./g, '')); }

export async function extractText(file, password = '') {
  await ensureWorker();
  const buf = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: new Uint8Array(buf), password }).promise;
  const parts = [];
  for (let i = 1; i <= pdf.numPages; i++) {
    const page = await pdf.getPage(i);
    const content = await page.getTextContent();
    parts.push(content.items.map(it => it.str).join(' '));
  }
  return parts.join('\n');
}

export function detectFormat(text) {
  // Use markers that appear in ONE PDF type only. Avoid "Rekening Koran"
  // (appears in BCA CC fine print) and "Lembar Tagihan Kartu Kredit"
  // (appears in Mandiri Savings consolidated statements).
  if (/REKENING TAHAPAN/.test(text))      return 'BCA_SAVINGS';
  if (/REKENING KARTU KREDIT/.test(text)) return 'BCA_CC';
  if (/mandirikartukredit/.test(text))    return 'MANDIRI_CC';
  if (/MANDIRI TABUNGAN/.test(text))      return 'MANDIRI_SAVINGS';
  return 'UNKNOWN';
}

// BCA Credit Card: DD-MMM   DD-MMM   description   amount [CR]
// Amount uses dots as thousands separator, no decimals (Indonesian format).
export function parseBcaCc(text, year = new Date().getFullYear()) {
  const re = /(\d{2})-([A-Z]{3})\s+\d{2}-[A-Z]{3}\s+(.+?)\s+(\d{1,3}(?:\.\d{3})+)(\s+CR)?(?=\s|$)/g;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const month = ID_MONTHS[m[2]];
    if (!month) continue;
    rows.push({
      date: `${year}-${pad(month)}-${m[1]}`,
      merchant: m[3].trim(),
      amount: parseAmtDot(m[4]),
      tx_type: m[5] ? 'transfer' : 'expense',
      category: 'Other',
    });
  }
  return rows;
}

// BCA Savings: DD/MM   description   amount [DB] [balance]
// Each transaction has ONE date. Amount uses commas + .dd decimals.
// Row patterns:
//   credit (income):  desc  amount [balance]
//   debit  (expense): desc  amount DB [balance]
// Balance is optional (omitted on multi-row blocks).
export function parseBcaSavings(text, year = new Date().getFullYear()) {
  const dateRe = /(?:^|\s)(\d{2})\/(\d{2})\s+/g;
  const amtRe  = /\b\d{1,3}(?:,\d{3})+\.\d{2}\b/g;
  const dbRe   = /\sDB(?=\s|$)/;
  const rows = [];
  const dateMatches = [...text.matchAll(dateRe)];
  for (let i = 0; i < dateMatches.length; i++) {
    const m = dateMatches[i];
    const start = m.index + m[0].length;
    const end   = i + 1 < dateMatches.length ? dateMatches[i+1].index : text.length;
    const segment = text.slice(start, end);
    if (/^Saldo\s+(Awal|Akhir)|^SALDO\s+(AWAL|AKHIR)|^Bersambung/i.test(segment.trim())) continue;
    const amts = [...segment.matchAll(amtRe)];
    if (amts.length === 0) continue;
    const isDebit = dbRe.test(segment);
    const txAmt = amts[0];
    const desc  = segment.slice(0, txAmt.index).trim();
    if (!desc) continue;
    rows.push({
      date: `${year}-${m[2]}-${m[1]}`,
      merchant: desc,
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
      merchant: m[4].trim(),
      amount: parseAmtComma(m[5]),
      tx_type: m[6] ? 'transfer' : 'expense',
      category: 'Other',
    });
  }
  return rows;
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
      merchant: m[3].trim(),
      amount: parseAmtComma(m[4]),
      tx_type: m[5] ? 'expense' : 'income',
      category: 'Other',
    });
  }
  return rows;
}

const PARSERS = {
  BCA_CC: parseBcaCc,
  BCA_SAVINGS: parseBcaSavings,
  MANDIRI_CC: parseMandiriCc,
  MANDIRI_SAVINGS: parseMandiriSavings,
};

const WALLET_TO_FORMAT = {
  BCA: 'BCA_SAVINGS',
  BCA_CC: 'BCA_CC',
  MANDIRI: 'MANDIRI_SAVINGS',
  MANDIRI_CC: 'MANDIRI_CC',
};

export async function parseStatement(file, password = '', wallet = null) {
  const text = await extractText(file, password);
  const format = WALLET_TO_FORMAT[wallet] || detectFormat(text);
  const fn = PARSERS[format];
  return { format, rows: fn ? fn(text, new Date().getFullYear()) : [] };
}
