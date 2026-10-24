// ac: web-money-flow-parity — money in and money out of one zone, the same rule as Android's
// MoneyFlow.kt. Money in is income plus the received amount of a transfer that arrives from another
// zone. Money out is expense plus a transfer that leaves for another zone. A transfer inside one zone
// moves money between the user's own accounts, so it is in neither total.
// zoneOf maps a wallet id to its zone: a currency code, a separate zone such as investments, or a
// set of selected wallets.
export function moneyFlow(rows, zone, zoneOf) {
  let moneyIn = 0;
  let moneyOut = 0;
  for (const r of rows) {
    if (!r.wallet) continue;
    const amount = Number(r.amount) || 0;
    const from = zoneOf(r.wallet);
    const to = r.to_wallet ? zoneOf(r.to_wallet) : null;
    if (r.tx_type === 'income' && from === zone) moneyIn += amount;
    else if (r.tx_type === 'expense' && from === zone) moneyOut += amount;
    else if (r.tx_type === 'transfer' && r.to_wallet) {
      if (to === zone && from !== zone) moneyIn += r.to_amount != null ? Number(r.to_amount) : amount;
      else if (from === zone && to !== zone) moneyOut += amount;
    }
  }
  return { moneyIn, moneyOut, net: moneyIn - moneyOut };
}
