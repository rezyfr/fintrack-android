import * as pdfjsLib from 'pdfjs-dist';

pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url,
).toString();

const ID_MONTHS = {
  JAN:1,FEB:2,MAR:3,APR:4,MEI:5,JUN:6,JUL:7,AGT:8,SEP:9,OKT:10,NOV:11,DES:12,
  Jan:1,Feb:2,Mar:3,Apr:4,May:5,Jun:6,Jul:7,Aug:8,Sep:9,Oct:10,Nov:11,Dec:12,
};

function pad(n) { return String(n).padStart(2, '0'); }

export async function extractText(file, password = '') {
  const buf = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: buf, password }).promise;
  const parts = [];
  for (let i = 1; i <= pdf.numPages; i++) {
    const page = await pdf.getPage(i);
    const content = await page.getTextContent();
    parts.push(content.items.map(it => it.str).join(' '));
  }
  return parts.join('\n');
}

export function detectFormat(text) {
  const isBca = /BANK CENTRAL ASIA/i.test(text);
  if (isBca && /\d{2}-[A-Z]{3}\s+\d{2}-[A-Z]{3}/.test(text)) return 'BCA_CC';
  if (isBca && (/MUTASI/i.test(text) || /\sDB\b/.test(text)))  return 'BCA_SAVINGS';
  if (/[Mm]andiri/.test(text) && /\d{2}-[A-Za-z]{3}-\d{2}/.test(text)) return 'MANDIRI_CC';
  if (/\d{2}\/\d{2}\s+\d{2}\/\d{2}/.test(text))               return 'MANDIRI_SAVINGS';
  return 'UNKNOWN';
}

export function parseBcaCc(text, year = new Date().getFullYear()) {
  const re = /(\d{2})-([A-Z]{3})\s+\d{2}-[A-Z]{3}\s+(.+?)\s+([\d.]+)(\s+CR)?\s*$/gm;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const [, day, mon, desc, amtStr, cr] = m;
    const month = ID_MONTHS[mon];
    if (!month) continue;
    rows.push({
      date: `${year}-${pad(month)}-${day}`,
      merchant: desc.trim(),
      amount: parseFloat(amtStr.replace(/\./g, '')),
      tx_type: cr ? 'transfer' : 'expense',
      category: 'Other',
    });
  }
  return rows;
}

export function parseBcaSavings(text, year = new Date().getFullYear()) {
  const re = /(\d{2})\/(\d{2})\s+\d{2}\/\d{2}\s+(.+?)\s+([\d,]+\.\d{2})(\s+DB)?\s*$/gm;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const [, day, mon, desc, amtStr, debit] = m;
    if (/Saldo\s*(Awal|Akhir)/i.test(desc)) continue;
    rows.push({
      date: `${year}-${mon}-${day}`,
      merchant: desc.trim(),
      amount: parseFloat(amtStr.replace(/,/g, '')),
      tx_type: debit ? 'expense' : 'income',
      category: 'Other',
    });
  }
  return rows;
}

export function parseMandiriCc(text) {
  const re = /(\d{2})-([A-Za-z]{3})-(\d{2})\s+\d{2}-[A-Za-z]{3}-\d{2}\s+(.+?)\s+([\d,]+\.\d{2})(\s+CR)?\s*$/gm;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const [, day, mon, yr, desc, amtStr, cr] = m;
    const month = ID_MONTHS[mon];
    if (!month) continue;
    rows.push({
      date: `${2000 + parseInt(yr)}-${pad(month)}-${day}`,
      merchant: desc.trim(),
      amount: parseFloat(amtStr.replace(/,/g, '')),
      tx_type: cr ? 'transfer' : 'expense',
      category: 'Other',
    });
  }
  return rows;
}

export function parseMandiriSavings(text, year = new Date().getFullYear()) {
  const re = /(\d{2})\/(\d{2})\s+\d{2}\/\d{2}\s+(.+?)\s+([\d,]+\.\d{2})(\s+D)?\s*$/gm;
  const rows = [];
  let m;
  while ((m = re.exec(text)) !== null) {
    const [, day, mon, desc, amtStr, debit] = m;
    if (/Saldo\s*(Awal|Akhir)/i.test(desc)) continue;
    rows.push({
      date: `${year}-${mon}-${day}`,
      merchant: desc.trim(),
      amount: parseFloat(amtStr.replace(/,/g, '')),
      tx_type: debit ? 'expense' : 'income',
      category: 'Other',
    });
  }
  return rows;
}

export async function parseStatement(file, password = '') {
  const text = await extractText(file, password);
  const format = detectFormat(text);
  const year = new Date().getFullYear();
  const parsers = {
    BCA_CC:          () => parseBcaCc(text, year),
    BCA_SAVINGS:     () => parseBcaSavings(text, year),
    MANDIRI_CC:      () => parseMandiriCc(text),
    MANDIRI_SAVINGS: () => parseMandiriSavings(text, year),
  };
  return { format, rows: parsers[format] ? parsers[format]() : [] };
}
