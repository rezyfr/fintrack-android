// ac: installment-auto-link — finds the installment a card transaction belongs to. Same rules as the
// Android app (domain/model/InstallmentMatcher.kt). A match needs the same wallet, the installment's
// merchant_pattern inside the item text (any case), and the same amount. The amount is required because
// two plans can share one pattern. Zero or several matches mean no link.
// Plans use the field names of the installments table: merchant_pattern, installment_amount,
// total_installments, current_step, status, wallet.
const AMOUNT_TOLERANCE = 1;

// The [step, total] pairs written in a statement line, for example "009/012" or "KE 03 DARI 03".
// A pair whose step is above its total is not a step, so dates such as "25/12" are skipped.
export function parseSteps(item) {
  const text = item ?? '';
  const found = [
    ...text.matchAll(/(\d{1,3})\s*\/\s*(\d{1,3})/g),
    ...text.matchAll(/KE\s+(\d{1,3})\s+DARI\s+(\d{1,3})/gi),
  ];
  return found
    .map((m) => [Number(m[1]), Number(m[2])])
    .filter(([step, total]) => step >= 1 && step <= total);
}

// ac: installment-auto-link — the step comes from the statement text when it has one, and is never lower
// than the current step, so a step fixed by hand stays. Without a number in the text, the step moves up
// by one. A step written for a different plan length gives null when strict. A plan the user picked by
// hand is not strict, so it falls back to one step up.
export function advanceInstallment(plan, item, strict = true) {
  const pairs = parseSteps(item);
  const written = pairs.find(([, total]) => total === plan.total_installments)?.[0];
  if (pairs.length > 0 && written === undefined && strict) return null;
  const step = written ?? plan.current_step + 1;
  const newStep = Math.min(Math.max(plan.current_step, step), plan.total_installments);
  return { installment: plan, newStep, newStatus: newStep >= plan.total_installments ? 'completed' : 'active' };
}

export function matchInstallment(item, wallet, amount, plans) {
  const text = (item ?? '').toLowerCase();
  const fits = plans.filter((p) =>
    p.status !== 'completed' &&
    p.wallet === wallet &&
    Boolean(p.merchant_pattern) &&
    text.includes(p.merchant_pattern.toLowerCase()) &&
    Math.abs(p.installment_amount - amount) <= AMOUNT_TOLERANCE,
  );
  return fits.length === 1 ? advanceInstallment(fits[0], item) : null;
}
