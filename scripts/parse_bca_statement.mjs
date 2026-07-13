#!/usr/bin/env node
// Parse one or more BCA statement PDFs (Credit Card or savings/checking) and
// (optionally) import them into Supabase. Reuses the same regex parser as the
// web Import PDF feature (web/src/utils/pdfParser.js) so behavior stays in
// sync with that UI. Auto-detects BCA_CC vs BCA_SAVINGS per file — pass a mix
// of both in one run.
//
// Usage:
//   node scripts/parse_bca_statement.mjs <pdf...>              # list only, no writes
//   node scripts/parse_bca_statement.mjs <pdf...> --insert     # list, dedup-check, then insert
//   node scripts/parse_bca_statement.mjs <pdf...> --insert --force   # skip the dedup abort
//
// Conventions learned from prior imports (see scripts/import_bca_cc.py history
// and the BCA_CC / BCA_SAVINGS cross-import done 2026-07):
//   - BCA_CC expense rows -> wallet=BCA_CC, tx_type=expense, category="Other", tab=IDR_EXPENSES.
//   - BCA_CC "PEMBAYARAN - MYBCA" (CC payment) rows -> tx_type=transfer, wallet=BCA (source),
//     to_wallet=BCA_CC (destination), category="Transfer".
//   - BCA_SAVINGS "KARTU KREDIT/PL ... BCA CARD ..." rows are SKIPPED — this is the exact
//     same CC payment already recorded from the BCA_CC statement's own PEMBAYARAN line(s)
//     (confirmed by amounts summing exactly, 2026-06-26: 1,393,394 + 90,792 = 1,484,186).
//     Importing both sides double-counts the same money movement.
//   - BCA_SAVINGS "...TRANSFER KE 008 FIDRIYANTO..." rows -> tx_type=transfer, wallet=BCA,
//     to_wallet=MANDIRI, category="Transfer" (matches the existing "Transfer KE 008 Fidriyanto"
//     rows already in the DB — "008" is the user's own Mandiri account). The paired
//     "BIAYA TXN KE 008" fee lines stay as plain expenses.
//   - Everything else on either statement stays a plain income/expense on its own wallet,
//     category="Other" (recategorize afterward in the web app).
//   - Before inserting, the script checks for existing rows on the same wallet(s) already
//     covering the parsed date range and aborts if any are found, since a prior manual
//     lump-sum entry (e.g. "Kartu Kredit Payment") can double-count against a statement's
//     own line items (this happened for the May 2026 CC payment and had to be deleted by
//     hand). Use --force to insert anyway after you've reviewed the overlap.

import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
import * as pdfjsLib from '../web/node_modules/pdfjs-dist/legacy/build/pdf.mjs';
import { detectFormat, parseBcaCc, parseBcaSavings } from '../web/src/utils/pdfParser.js';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const REPO_ROOT = path.resolve(__dirname, '..');

const CC_PDF_PASSWORD = process.env.BCA_CC_PDF_PASSWORD || '***REMOVED-PASSWORD***';
const PAYMENT_ITEM_PREFIX = 'PEMBAYARAN';
const CC_PAYMENT_ITEM_MARKER = 'KARTU KREDIT';
const OWN_TRANSFER_MARKER = 'TRANSFER KE 008';

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

function toCcRow(r) {
  const isPayment = r.item.toUpperCase().startsWith(PAYMENT_ITEM_PREFIX);
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
function toSavingsRow(r) {
  const upper = r.item.toUpperCase();
  if (upper.includes(CC_PAYMENT_ITEM_MARKER)) return null;
  if (upper.includes(OWN_TRANSFER_MARKER)) {
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

function printListing(label, rows, skipped) {
  console.log(`\n=== ${label} (${rows.length} rows${skipped ? `, ${skipped} skipped` : ''}) ===`);
  for (const r of rows) {
    const tag = r.tx_type === 'transfer' ? `transfer ${r.wallet}->${r.to_wallet}` : r.tx_type;
    console.log(`  ${r.date}  ${String(r.amount).padStart(10)}  ${tag.padEnd(24)}  ${r.item}`);
  }
}

async function checkExisting(url, key, wallets, minDate, maxDate) {
  const orClauses = wallets.map((w) => `wallet.eq.${w}`).join(',');
  const params = new URLSearchParams({
    or: `(${orClauses})`,
    date: `gte.${minDate}`,
    select: 'id,date,item,amount,wallet,to_wallet',
    order: 'date.asc',
  });
  const res = await fetch(`${url}/rest/v1/transactions?${params}&date=lte.${maxDate}`, {
    headers: { apikey: key, Authorization: `Bearer ${key}` },
  });
  if (!res.ok) throw new Error(`Supabase error checking duplicates: ${res.status}`);
  return res.json();
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
  const force = args.includes('--force');
  const files = args.filter((a) => !a.startsWith('--'));

  if (files.length === 0) {
    console.error('Usage: node scripts/parse_bca_statement.mjs <pdf...> [--insert] [--force]');
    process.exit(1);
  }

  const allRows = [];
  const walletsSeen = new Set();
  for (const file of files) {
    // BCA_SAVINGS statements have no password; BCA_CC ones do. Try unlocked first.
    let text;
    try {
      text = await extractText(file, '');
    } catch {
      text = await extractText(file, CC_PDF_PASSWORD);
    }
    const format = detectFormat(text);

    if (format === 'BCA_CC') {
      const rows = parseBcaCc(text).map(toCcRow);
      printListing(file, rows);
      allRows.push(...rows);
      walletsSeen.add('BCA_CC').add('BCA');
    } else if (format === 'BCA_SAVINGS') {
      const parsed = parseBcaSavings(text).map(toSavingsRow);
      const rows = parsed.filter(Boolean);
      printListing(file, rows, parsed.length - rows.length);
      allRows.push(...rows);
      walletsSeen.add('BCA').add('MANDIRI');
    } else {
      console.error(`${file}: detected format "${format}", expected BCA_CC or BCA_SAVINGS — skipping`);
    }
  }

  if (allRows.length === 0) {
    console.log('\nNo rows parsed. Nothing to do.');
    return;
  }

  console.log(`\nTotal parsed: ${allRows.length} rows across ${files.length} file(s).`);

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
