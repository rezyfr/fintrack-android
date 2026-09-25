#!/usr/bin/env node
// Parse one or more bank statement PDFs (BCA Credit Card, BCA savings/checking,
// Mandiri consolidated savings, Mandiri Credit Card, or Bangkok Bank savings) and (optionally) import them into
// Supabase. Reuses the same regex parser as the web Import PDF feature
// (web/src/utils/pdfParser.js) so behavior stays in sync with that UI. Auto-detects
// the format per file — pass a mix of formats in one run.
//
// Usage:
//   node scripts/parse_bank_statement.mjs <pdf...>              # list only, no writes
//   node scripts/parse_bank_statement.mjs <pdf...> --insert     # list, dedup-check, then insert
//   node scripts/parse_bank_statement.mjs <pdf...> --insert --force   # skip the dedup abort
//   node scripts/parse_bank_statement.mjs <pdf...> --insert --skip-payments  # drop CC payment rows
//   node scripts/parse_bank_statement.mjs <pdf...> --reconcile  # diff vs DB, insert ONLY the gaps
//
// ac: bbl-statement-reconcile — --reconcile is for wallets that are ALSO populated live via the
// Android notification listener (currently: BBL). Since most of the statement's transactions are
// typically already present (captured from bank notifications with different, friendlier item
// text than the statement's generic descriptions), a blind --insert would recreate the whole
// month as duplicates. --reconcile instead matches parsed rows against existing DB rows for the
// same wallet(s) by (date, amount) and inserts only the rows with no match — confirmed 2026-07:
// income notifications (salary, incoming transfers) aren't captured by the Android listener at
// all, so --reconcile's most common find is exactly that category of gap. It also reports DB rows
// with no matching parsed row (never deleted automatically — could be a real transaction outside
// this statement, or a notification-capture duplicate; needs a human look either way).
//
// Conventions learned from prior imports (see scripts/import_bca_cc.py history,
// the BCA_CC / BCA_SAVINGS cross-import done 2026-07, and the Mandiri CC import
// done 2026-07):
//   - BCA_CC expense rows -> wallet=BCA_CC, tx_type=expense, category="Other", tab=IDR_EXPENSES.
//   - BCA_CC "PEMBAYARAN - MYBCA" (CC payment) rows -> tx_type=transfer, wallet=BCA (source),
//     to_wallet=BCA_CC (destination), category="Transfer".
//   - BCA_SAVINGS "KARTU KREDIT/PL ... BCA CARD ..." rows are SKIPPED — this is the exact
//     same CC payment already recorded from the BCA_CC statement's own PEMBAYARAN line(s)
//     (confirmed by amounts summing exactly, 2026-06-26: 1,393,394 + 90,792 = 1,484,186).
//     Importing both sides double-counts the same money movement.
//   - BCA_SAVINGS "...TRANSFER KE 008 FIDRIYANTO..." rows -> tx_type=transfer, wallet=BCA,
//     to_wallet=MANDIRI, category="Transfer" (matches the existing "Transfer KE 008 Fidriyanto"
//     rows already in the DB — "008" is the user's own Mandiri account; this is a genuine
//     BCA->MANDIRI savings transfer, a separate hop from the MANDIRI->MANDIRI_CC payment
//     below, confirmed 2026-07: the user funds Mandiri CC payments FROM the Mandiri savings
//     account, not directly from BCA, even when the BCA transfer and the CC payment share
//     the same date/amount). The paired "BIAYA TXN KE 008" fee lines stay as plain expenses.
//   - MANDIRI_CC expense rows -> wallet=MANDIRI_CC, tx_type=expense, category="Other", tab=IDR_EXPENSES
//     (includes $0 "BUNGA CICILAN" installment-interest rows — matches existing DB precedent).
//   - MANDIRI_CC "PAYMENT THANK YOU - Livin" (CC payment) rows -> tx_type=transfer,
//     wallet=MANDIRI (source), to_wallet=MANDIRI_CC (destination), category="Transfer"
//     (matches existing "Payment CC <Month> <Year>" / "PAYMENT THANK YOU - Livin" rows).
//   - MANDIRI_CC rows the parser tags tx_type=transfer (trailing "CR" marker) that are NOT a
//     "PAYMENT THANK YOU" line are merchant credits/refunds, not CC payments -> recorded as
//     tx_type=income, wallet=MANDIRI_CC, category="Other", tab=IDR_INCOME (matches existing
//     precedent: "Refund Susu Mika", "MEMBERSHIP FEE REVERSAL").
//   - Everything else on any statement stays a plain income/expense on its own wallet,
//     category="Other" (recategorize afterward in the web app).
//   - Before inserting, the script checks for existing rows on the same wallet(s) already
//     covering the parsed date range and aborts if any are found, since a prior manual
//     lump-sum entry (e.g. "Kartu Kredit Payment") can double-count against a statement's
//     own line items (this happened for the May 2026 BCA CC payment and had to be deleted
//     by hand — a similar duplicate, "PAYMENT THANK YOU - Livin" recorded as stray income
//     alongside an existing "Payment CC May 2026" transfer, was found and deleted the same
//     way in the 2026-07 Mandiri CC import). Use --force to insert anyway after you've
//     reviewed the overlap — a date-range hit is often just the previous statement's tail,
//     not a real duplicate; check item/amount before forcing.

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import * as pdfjsLib from '../web/node_modules/pdfjs-dist/legacy/build/pdf.mjs';
import { detectFormat, parseBcaCc, parseBcaSavings, parseMandiriCc, parseMandiriSavings, parseBbl } from '../web/src/utils/pdfParser.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..');

// Password comes from the environment. Never hardcode the bank statement password here.
const CC_PDF_PASSWORD = process.env.BCA_CC_PDF_PASSWORD || '';
const BCA_PAYMENT_ITEM_PREFIX = 'PEMBAYARAN';
const BCA_CC_PAYMENT_ITEM_MARKER = 'KARTU KREDIT';
const CC_WALLETS = new Set(['BCA_CC', 'MANDIRI_CC']);
const BCA_OWN_TRANSFER_MARKER = 'TRANSFER KE 008';
const MANDIRI_CC_PAYMENT_MARKER = 'PAYMENT THANK YOU';

function loadEnv(p) {
  const env = {};
  try {
    for (const line of readFileSync(p, 'utf8').split('\n')) {
      const m = line.match(/^([A-Z0-9_]+)=(.*)$/);
      if (m) env[m[1]] = m[2].trim();
    }
  } catch { /* file may not exist */ }
  return env;
}

function supabaseCreds() {
  const env = loadEnv(path.join(REPO_ROOT, 'web/.env'));
  const url = env.VITE_SUPABASE_URL || process.env.VITE_SUPABASE_URL;
  const key = env.VITE_SUPABASE_ANON_KEY || process.env.VITE_SUPABASE_ANON_KEY;
  if (!url || !key) throw new Error('Supabase URL/key not found in web/.env or env vars');
  return { url, key };
}

async function extractText(filePath, password) {
  const buf = readFileSync(filePath);
  const pdf = await pdfjsLib.getDocument({ data: new Uint8Array(buf), password }).promise;
  const parts = [];
  for (let i = 1; i <= pdf.numPages; i++) {
    const page = await pdf.getPage(i);
    const content = await page.getTextContent();
    parts.push(content.items.map((it) => it.str).join(' '));
  }
  return parts.join('\n');
}

function toBcaCcRow(r) {
  const isPayment = r.item.toUpperCase().startsWith(BCA_PAYMENT_ITEM_PREFIX);
  return isPayment
    ? {
        item: r.item, amount: r.amount, category: 'Transfer', date: r.date,
        tab: 'IDR_EXPENSES', note: null, wallet: 'BCA', tx_type: 'transfer',
        to_wallet: 'BCA_CC', to_amount: null,
      }
    : {
        item: r.item, amount: r.amount, category: r.category || 'Other', date: r.date,
        tab: 'IDR_EXPENSES', note: null, wallet: 'BCA_CC', tx_type: r.tx_type || 'expense',
        to_wallet: null, to_amount: null,
      };
}

// Returns null for rows that should be skipped (e.g. the CC payment already recorded
// from the BCA_CC side).
function toBcaSavingsRow(r) {
  const upper = r.item.toUpperCase();
  if (upper.includes(BCA_CC_PAYMENT_ITEM_MARKER)) return null;
  if (upper.includes(BCA_OWN_TRANSFER_MARKER)) {
    return {
      item: r.item, amount: r.amount, category: 'Transfer', date: r.date,
      tab: 'IDR_EXPENSES', note: null, wallet: 'BCA', tx_type: 'transfer',
      to_wallet: 'MANDIRI', to_amount: null,
    };
  }
  return {
    item: r.item, amount: r.amount, category: 'Other', date: r.date,
    tab: r.tx_type === 'income' ? 'IDR_INCOME' : 'IDR_EXPENSES', note: null,
    wallet: 'BCA', tx_type: r.tx_type, to_wallet: null, to_amount: null,
  };
}

// ac: mandiri-savings-cc-payment — on the savings statement a card payment shows up with the bare
// 16-digit card number as its whole detail line ("-4259456201423591"); every other reference on the
// statement is longer, shorter, or mixed alphanumeric, so a detail that is nothing but 16 digits
// identifies the payment. Recorded as MANDIRI -> MANDIRI_CC, the same shape the MANDIRI_CC
// statement's "PAYMENT THANK YOU - Livin" rows get, so --reconcile matches the ones already there.
const MANDIRI_CARD_NUMBER_RE = /^-?\d{16}$/;
// ac: mandiri-savings-bca-incoming — CENAIDJA is BCA's SWIFT code, so a credit whose reference
// carries it is the receiving leg of a BCA -> MANDIRI transfer already recorded from the BCA
// statement side as wallet=BCA, to_wallet=MANDIRI (2026-08: the 3,450,000 on 26/08 is DB row 1811,
// the 1,000,000 on 02/08 is row 1681). Recording the credit again would double the money in.
const MANDIRI_BCA_INCOMING_MARKER = 'CENAIDJA';

// The consolidated statement dates its rows DD/MM only; the period line carries the year
// ("Periode / Period : 1/08/26 s/d 31/08/26").
function mandiriStatementYear(text) {
  const m = /s\/d\s+\d{1,2}\/\d{2}\/(\d{2})/.exec(text);
  return m ? 2000 + Number(m[1]) : new Date().getFullYear();
}

// Returns null for rows that should be skipped (the BCA-side leg of an own-wallet transfer).
function toMandiriSavingsRow(r) {
  if (r.tx_type === 'income' && r.item.toUpperCase().includes(MANDIRI_BCA_INCOMING_MARKER)) return null;
  if (MANDIRI_CARD_NUMBER_RE.test(r.item.trim())) {
    return {
      item: r.item, amount: r.amount, category: 'Transfer', date: r.date,
      tab: 'IDR_EXPENSES', note: null, wallet: 'MANDIRI', tx_type: 'transfer',
      to_wallet: 'MANDIRI_CC', to_amount: null,
    };
  }
  return {
    item: r.item, amount: r.amount, category: 'Other', date: r.date,
    tab: r.tx_type === 'income' ? 'IDR_INCOME' : 'IDR_EXPENSES', note: null,
    wallet: 'MANDIRI', tx_type: r.tx_type, to_wallet: null, to_amount: null,
  };
}

function toMandiriCcRow(r) {
  const upper = r.item.toUpperCase();
  if (upper.includes(MANDIRI_CC_PAYMENT_MARKER)) {
    return {
      item: r.item, amount: r.amount, category: 'Transfer', date: r.date,
      tab: 'IDR_EXPENSES', note: null, wallet: 'MANDIRI', tx_type: 'transfer',
      to_wallet: 'MANDIRI_CC', to_amount: null,
    };
  }
  if (r.tx_type === 'transfer') {
    // Trailing "CR" marker but not a CC payment -> a merchant credit/refund, not a transfer.
    return {
      item: r.item, amount: r.amount, category: 'Other', date: r.date,
      tab: 'IDR_INCOME', note: null, wallet: 'MANDIRI_CC', tx_type: 'income',
      to_wallet: null, to_amount: null,
    };
  }
  return {
    item: r.item, amount: r.amount, category: r.category || 'Other', date: r.date,
    tab: 'IDR_EXPENSES', note: null, wallet: 'MANDIRI_CC', tx_type: r.tx_type || 'expense',
    to_wallet: null, to_amount: null,
  };
}

function toBblRow(r) {
  return {
    item: r.item, amount: r.amount, category: r.category || 'Other', date: r.date,
    tab: r.tx_type === 'income' ? 'INCOME' : 'EXPENSES', note: null,
    wallet: 'BBL', tx_type: r.tx_type, to_wallet: null, to_amount: null,
  };
}

function printListing(label, rows, skipped) {
  console.log(`\n=== ${label} (${rows.length} rows${skipped ? `, ${skipped} skipped` : ''}) ===`);
  for (const r of rows) {
    const tag = r.tx_type === 'transfer' ? `transfer ${r.wallet}->${r.to_wallet}` : r.tx_type;
    console.log(`  ${r.date}  ${String(r.amount).padStart(10)}  ${tag.padEnd(24)}  ${r.item}`);
  }
}

// ac: statement-transfer-in-dedup — a wallet is also credited by transfers recorded from the OTHER
// wallet's side (wallet=BCA, to_wallet=MANDIRI), which a wallet-only filter never returns; without
// them a reconcile re-inserts those credits as fresh income rows on top of the existing transfer.
async function checkExisting(url, key, wallets, minDate, maxDate) {
  const orClauses = wallets.flatMap((w) => [`wallet.eq.${w}`, `to_wallet.eq.${w}`]).join(',');
  const params = new URLSearchParams({
    or: `(${orClauses})`,
    date: `gte.${minDate}`,
    select: 'id,date,item,amount,to_amount,wallet,to_wallet,tx_type',
    order: 'date.asc',
  });
  const res = await fetch(`${url}/rest/v1/transactions?${params}&date=lte.${maxDate}`, {
    headers: { apikey: key, Authorization: `Bearer ${key}` },
  });
  if (!res.ok) throw new Error(`Supabase error checking duplicates: ${res.status}`);
  return res.json();
}

// ac: bbl-statement-reconcile — (date, amount, direction) match key, cents precision so .40/.75 etc
// compare cleanly.
// ac: statement-transfer-in-dedup — direction is part of the key because a statement line is either
// money leaving the wallet or money entering it, and the two must never match each other: 02/08 on
// the Mandiri statement had a 1,000,000 credit from BCA and a 1,000,000 card payment out, which an
// amount-only key paired together — re-inserting the payment and dropping the credit.
function matchKey(date, amount, direction) {
  return `${date}|${Math.round(Number(amount) * 100)}|${direction}`;
}

function parsedDirection(r) {
  return r.tx_type === 'income' ? 'cr' : 'db';
}

// A transfer recorded on the other wallet's side credits this one; anything on this wallet follows
// its own tx_type.
function dbDirection(r, wallets) {
  if (!wallets.has(r.wallet)) return 'cr';
  return r.tx_type === 'income' ? 'cr' : 'db';
}

// ac: statement-transfer-in-dedup — for a transfer recorded on the other wallet's side, the figure
// that hits THIS wallet is to_amount when the transfer converts currency, else amount.
function dbMatchAmount(r, wallets) {
  if (wallets.has(r.wallet)) return r.amount;
  return r.to_amount ?? r.amount;
}

function groupByMatchKey(rows, amountOf = (r) => r.amount, directionOf = parsedDirection) {
  const map = new Map();
  for (const r of rows) {
    const k = matchKey(r.date, amountOf(r), directionOf(r));
    if (!map.has(k)) map.set(k, []);
    map.get(k).push(r);
  }
  return map;
}

// ac: bbl-statement-reconcile — returns { toInsert, extraInDb }: parsed rows with no matching DB
// row are safe to insert; DB rows with no matching parsed row are only reported, never touched.
function reconcile(parsedRows, dbRows, wallets) {
  const dbByKey = groupByMatchKey(dbRows, (r) => dbMatchAmount(r, wallets), (r) => dbDirection(r, wallets));
  const parsedByKey = groupByMatchKey(parsedRows);
  const toInsert = [];
  for (const [k, group] of parsedByKey) {
    const already = (dbByKey.get(k) || []).length;
    toInsert.push(...group.slice(already));
  }
  const extraInDb = [];
  for (const [k, group] of dbByKey) {
    const matched = (parsedByKey.get(k) || []).length;
    extraInDb.push(...group.slice(matched));
  }
  return { toInsert, extraInDb };
}

async function insertRows(url, key, rows) {
  const res = await fetch(`${url}/rest/v1/transactions`, {
    method: 'POST',
    headers: { apikey: key, Authorization: `Bearer ${key}`, 'Content-Type': 'application/json', Prefer: 'return=minimal' },
    body: JSON.stringify(rows),
  });
  if (!res.ok) throw new Error(`Supabase insert failed: ${res.status} ${await res.text()}`);
}

async function main() {
  const args = process.argv.slice(2);
  const doInsert = args.includes('--insert');
  const doReconcile = args.includes('--reconcile');
  const force = args.includes('--force');
  const skipPayments = args.includes('--skip-payments');
  const files = args.filter((a) => !a.startsWith('--'));

  if (files.length === 0) {
    console.error('Usage: node scripts/parse_bank_statement.mjs <pdf...> [--insert [--force] | --reconcile]');
    process.exit(1);
  }

  const allRows = [];
  const walletsSeen = new Set();
  for (const file of files) {
    // BCA_SAVINGS, MANDIRI_CC, and BBL statements have no password; BCA_CC and the Mandiri
    // consolidated statement share one. Try unlocked first, fall back to the password.
    let text;
    try {
      text = await extractText(file, '');
    } catch {
      text = await extractText(file, CC_PDF_PASSWORD);
    }
    const format = detectFormat(text);

    if (format === 'BCA_CC') {
      const rows = parseBcaCc(text).map(toBcaCcRow);
      printListing(file, rows);
      allRows.push(...rows);
      walletsSeen.add('BCA_CC').add('BCA');
    } else if (format === 'BCA_SAVINGS') {
      const parsed = parseBcaSavings(text).map(toBcaSavingsRow);
      const rows = parsed.filter(Boolean);
      printListing(file, rows, parsed.length - rows.length);
      allRows.push(...rows);
      walletsSeen.add('BCA').add('MANDIRI');
    } else if (format === 'MANDIRI_SAVINGS') {
      const parsed = parseMandiriSavings(text, mandiriStatementYear(text)).map(toMandiriSavingsRow);
      const rows = parsed.filter(Boolean);
      printListing(file, rows, parsed.length - rows.length);
      allRows.push(...rows);
      walletsSeen.add('MANDIRI');
    } else if (format === 'MANDIRI_CC') {
      const rows = parseMandiriCc(text).map(toMandiriCcRow);
      printListing(file, rows);
      allRows.push(...rows);
      walletsSeen.add('MANDIRI_CC').add('MANDIRI');
    } else if (format === 'BBL') {
      const rows = parseBbl(text).map(toBblRow);
      printListing(file, rows);
      allRows.push(...rows);
      walletsSeen.add('BBL');
    } else {
      console.error(`${file}: detected format "${format}", expected BCA_CC, BCA_SAVINGS, MANDIRI_SAVINGS, MANDIRI_CC, or BBL — skipping`);
    }
  }

  // ac: cc-payment-already-recorded — the card payment is often already in the DB as one manual
  // lump-sum row covering every card on the statement (e.g. "Payment Credit BCA" 414,427 = the
  // statement's own 409,885 + 4,542 lines). Inserting the statement's per-card PEMBAYARAN /
  // PAYMENT THANK YOU rows on top of that double-counts the same money movement, so
  // --skip-payments drops them and keeps the existing lump sum.
  if (skipPayments) {
    const before = allRows.length;
    const kept = allRows.filter((r) => r.tx_type !== 'transfer' || !CC_WALLETS.has(r.to_wallet));
    allRows.length = 0;
    allRows.push(...kept);
    console.log(`\n--skip-payments: dropped ${before - kept.length} credit-card payment transfer row(s).`);
  }

  if (allRows.length === 0) {
    console.log('\nNo rows parsed. Nothing to do.');
    return;
  }

  console.log(`\nTotal parsed: ${allRows.length} rows across ${files.length} file(s).`);

  if (doReconcile) {
    // ac: bbl-statement-reconcile — diff against the DB instead of blind-inserting everything,
    // for wallets (e.g. BBL) that are also populated live via notification capture.
    const { url, key } = supabaseCreds();
    const dates = allRows.map((r) => r.date).sort();
    const dbRows = await checkExisting(url, key, [...walletsSeen], dates[0], dates[dates.length - 1]);
    const { toInsert, extraInDb } = reconcile(allRows, dbRows, walletsSeen);

    console.log(`\n=== Reconcile: ${dbRows.length} existing DB row(s) in range vs ${allRows.length} parsed ===`);
    console.log(`\n-- Missing from DB (${toInsert.length}) — will be inserted --`);
    toInsert.forEach((r) => {
      const tag = r.tx_type === 'transfer' ? `transfer ${r.wallet}->${r.to_wallet}` : r.tx_type;
      console.log(`  ${r.date}  ${String(r.amount).padStart(10)}  ${tag.padEnd(24)}  ${r.item}`);
    });
    console.log(`\n-- In DB with no matching parsed row (${extraInDb.length}) — reported only, not touched --`);
    extraInDb.forEach((r) => console.log(`  ${r.date}  ${String(r.amount).padStart(10)}  id=${r.id}  ${r.item}`));

    if (toInsert.length === 0) {
      console.log('\nNothing to insert.');
      return;
    }
    if (!doInsert) {
      console.log('\n(Reconcile-only run — nothing written. Re-run with --reconcile --insert to submit the missing rows.)');
      return;
    }
    await insertRows(url, key, toInsert);
    console.log(`\nInserted ${toInsert.length} row(s) into Supabase.`);
    return;
  }

  if (!doInsert) {
    console.log('\n(List-only run — nothing written. Re-run with --insert to submit to Supabase.)');
    return;
  }

  const { url, key } = supabaseCreds();
  const dates = allRows.map((r) => r.date).sort();
  const existing = await checkExisting(url, key, [...walletsSeen], dates[0], dates[dates.length - 1]);
  if (existing.length > 0 && !force) {
    console.log(`\nABORTING — found ${existing.length} existing row(s) on wallet(s) [${[...walletsSeen].join(', ')}] already in this date range:`);
    console.log(JSON.stringify(existing, null, 2));
    console.log('\nReview these for overlap with the parsed rows above (a prior manual lump-sum entry can');
    console.log('double-count against a statement\'s own line items). Delete the stale row or re-run with');
    console.log('--force once you\'ve confirmed there\'s no overlap.');
    process.exit(1);
  }

  await insertRows(url, key, allRows);
  console.log(`\nInserted ${allRows.length} rows into Supabase.`);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
