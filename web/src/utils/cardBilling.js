// Statement-cycle math for credit cards. Pure functions so the Card Debt view and its tests share
// one definition of when a statement closes, when it is due, and what the minimum payment is.

function iso(d) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

// ac: card-statement-due-and-minimum — the statement closing date is the most recent cutoff day.
export function latestCutoff(cutoffDay, now = new Date()) {
  let d = new Date(now.getFullYear(), now.getMonth(), cutoffDay);
  if (d > now) d = new Date(d.getFullYear(), d.getMonth() - 1, cutoffDay);
  return iso(d);
}

// The cutoff one statement before a given cutoff, so a statement window is (previousCutoff, cutoff].
export function previousCutoff(cutoffIso, cutoffDay) {
  const [y, m] = cutoffIso.split('-').map(Number);
  const d = new Date(y, m - 2, cutoffDay);
  return iso(d);
}

// ac: card-statement-due-and-minimum — the due date is the card's due day on the first month after
// the statement closes. When the due day is on or before the cutoff day it lands the next month.
export function dueDateAfter(cutoffIso, dueDay) {
  const [y, m, d] = cutoffIso.split('-').map(Number);
  const cutoff = new Date(y, m - 1, d);
  let due = new Date(y, m - 1, dueDay);
  while (due <= cutoff) due = new Date(due.getFullYear(), due.getMonth() + 1, dueDay);
  return iso(due);
}

// An installment step bills as its own line. The parser tags these with "NNN/NNN" or "CICILAN",
// so the same shapes identify them here.
const INSTALLMENT = /\b\d{2,3}\/\d{2,3}\b|cicilan/i;

// ac: card-statement-due-and-minimum — installment charges billed inside this statement window.
export function installmentPortion(rows, fromExclusiveIso, toInclusiveIso) {
  return rows
    .filter(r => r.tx_type === 'expense'
      && r.date > fromExclusiveIso && r.date <= toInclusiveIso
      && INSTALLMENT.test(r.item || ''))
    .reduce((s, r) => s + (Number(r.amount) || 0), 0);
}

// ac: card-statement-due-and-minimum — a percentage of the balance, and for a card whose
// installments bill in full, the installment charges plus that percentage of the rest.
export function minimumPayment({ statementBalance, installment, minPercent, minFullInstallments }) {
  const pct = minPercent / 100;
  if (minFullInstallments) {
    const revolving = Math.max(0, statementBalance - installment);
    return Math.min(statementBalance, installment + revolving * pct);
  }
  return statementBalance * pct;
}

export function daysUntil(dueIso, now = new Date()) {
  const [y, m, d] = dueIso.split('-').map(Number);
  const due = new Date(y, m - 1, d);
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  return Math.round((due - today) / 86400000);
}
