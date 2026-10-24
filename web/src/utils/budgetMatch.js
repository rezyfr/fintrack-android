import { walletCurrency } from '../constants/transaction';

// A line that still uses match rules: its wallets, then its pattern on the item, then its categories.
export function compileMatcher(line) {
  let re = null;
  if (line.match_pattern) {
    try {
      re = new RegExp(line.match_pattern, 'i');
    } catch {
      re = null; // A malformed stored pattern must not take the whole chart down.
    }
  }
  const wallets = line.match_wallets || null;
  const cats = line.match_categories || null;
  return (row) => {
    if (wallets && !wallets.includes(row.wallet)) return false;
    if (re && re.test(row.item || '')) return true;
    if (cats && cats.includes(row.category)) return true;
    return false;
  };
}

// ac: budget-by-category — the line an expense counts toward, or null when it has none. The same rule
// as Android's BudgetAttribution.lineFor:
// - A line set up by category takes the expenses of that category in the wallets of its currency.
//   A line for the expense's subcategory wins over the line for the whole category.
// - Lines without a category keep the old rules: a manual assignment (budget_line_id), then the first
//   matching rule in sort order.
export function budgetLineFor(row, lines) {
  if (row.tx_type !== 'expense') return null;
  const currency = walletCurrency(row.wallet);
  const byCategory = lines.filter(l => l.category && l.currency === currency && l.category === row.category);
  const sub = byCategory.find(l => l.subcategory && l.subcategory === row.subcategory);
  if (sub) return sub;
  const whole = byCategory.find(l => !l.subcategory);
  if (whole) return whole;
  const ruleLines = lines.filter(l => !l.category);
  const assigned = ruleLines.find(l => l.id === row.budget_line_id);
  if (assigned) return assigned;
  return [...ruleLines].sort((a, b) => (a.sort_order ?? 0) - (b.sort_order ?? 0)).find(l => compileMatcher(l)(row)) ?? null;
}

// True when the lines are set up by category, so a transaction is fixed by changing its category.
export function budgetsByCategory(lines) {
  return lines.some(l => l.category);
}
