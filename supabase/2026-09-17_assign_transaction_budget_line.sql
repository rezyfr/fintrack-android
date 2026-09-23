-- Manual budget-line override: assign-transaction-budget-line
-- Run this in the Supabase SQL editor.
--
-- Lets a transaction be pinned to a specific budget_lines row from the Transaction List,
-- overriding whatever line the automatic wallet/pattern/category rules would have matched.
-- Null keeps the existing automatic-matching behavior.

alter table transactions
    add column if not exists budget_line_id bigint;
