-- Migration: add to_amount column for cross-currency transfers.
-- Run this in the Supabase SQL editor.
--
-- Background: cross-currency transfers (e.g. MANDIRI/IDR -> BBL/THB) were stored
-- as one row with a single `amount` field. The receiving wallet was credited
-- with that same number in its own currency, inflating the destination balance.
-- `to_amount` lets the user record the actual amount that landed in the
-- destination wallet's currency.

alter table transactions
    add column if not exists to_amount numeric;

-- Wallet balances view: sum income, expense, transfer-out from `amount`;
-- sum transfer-in from coalesce(to_amount, amount) so cross-currency transfers
-- are credited with the destination-currency value when present.
create or replace view wallet_balances as
with wallets(id, name, currency, type) as (
    values
        ('BBL',        'Bangkok Bank',        'THB', 'checking'),
        ('BCA',        'BCA Account',         'IDR', 'checking'),
        ('BCA_CC',     'BCA Credit Card',     'IDR', 'credit'),
        ('MANDIRI',    'Mandiri Account',     'IDR', 'checking'),
        ('MANDIRI_CC', 'Mandiri Credit Card', 'IDR', 'credit'),
        ('INVESTMENT', 'Investments',         'IDR', 'investment')
),
flows as (
    select wallet as wallet_id,
           sum(case
                   when tx_type = 'income'   then  amount
                   when tx_type = 'expense'  then -amount
                   when tx_type = 'transfer' then -amount
                   else 0
               end) as net
      from transactions
     group by wallet
    union all
    select to_wallet as wallet_id,
           sum(coalesce(to_amount, amount)) as net
      from transactions
     where tx_type = 'transfer' and to_wallet is not null
     group by to_wallet
)
select w.id, w.name, w.currency, w.type,
       coalesce(sum(f.net), 0)::numeric as balance
  from wallets w
  left join flows f on f.wallet_id = w.id
 group by w.id, w.name, w.currency, w.type;

-- Reconciliation RPC: same coalesce(to_amount, amount) logic for transfer-in
-- so each month's net_change matches the destination wallet's statement.
create or replace function get_wallet_reconciliation(p_month text)
returns table (
    wallet_id           text,
    wallet_name         text,
    currency            text,
    wallet_type         text,
    opening_balance     numeric,
    net_change          numeric,
    calculated_closing  numeric,
    next_opening        numeric,
    difference          numeric
) language sql stable as $$
with wallets(id, name, currency, type) as (
    values
        ('BBL',        'Bangkok Bank',        'THB', 'checking'),
        ('BCA',        'BCA Account',         'IDR', 'checking'),
        ('BCA_CC',     'BCA Credit Card',     'IDR', 'credit'),
        ('MANDIRI',    'Mandiri Account',     'IDR', 'checking'),
        ('MANDIRI_CC', 'Mandiri Credit Card', 'IDR', 'credit'),
        ('INVESTMENT', 'Investments',         'IDR', 'investment')
),
month_start as (
    select to_date(p_month || '-01', 'YYYY-MM-DD') as d
),
flows as (
    select wallet as wallet_id,
           sum(case
                   when tx_type = 'income'   then  amount
                   when tx_type = 'expense'  then -amount
                   when tx_type = 'transfer' then -amount
                   else 0
               end) as net
      from transactions, month_start
     where date >= month_start.d
       and date <  (month_start.d + interval '1 month')::date
     group by wallet
    union all
    select to_wallet as wallet_id,
           sum(coalesce(to_amount, amount)) as net
      from transactions, month_start
     where tx_type = 'transfer'
       and to_wallet is not null
       and date >= month_start.d
       and date <  (month_start.d + interval '1 month')::date
     group by to_wallet
)
select
    w.id   as wallet_id,
    w.name as wallet_name,
    w.currency,
    w.type as wallet_type,
    cur.opening_balance,
    coalesce(sum(f.net), 0)::numeric as net_change,
    (cur.opening_balance + coalesce(sum(f.net), 0))::numeric as calculated_closing,
    nxt.opening_balance as next_opening,
    (nxt.opening_balance - (cur.opening_balance + coalesce(sum(f.net), 0)))::numeric as difference
  from wallets w
  left join statement_balances cur on cur.wallet_id = w.id and cur.month = p_month
  left join statement_balances nxt on nxt.wallet_id = w.id and nxt.month = to_char((to_date(p_month || '-01', 'YYYY-MM-DD') + interval '1 month')::date, 'YYYY-MM')
  left join flows f on f.wallet_id = w.id
 group by w.id, w.name, w.currency, w.type, cur.opening_balance, nxt.opening_balance;
$$;
