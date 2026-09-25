-- Add an optional subcategory to transactions.
-- Nullable text; existing rows keep NULL (no subcategory). Safe, non-destructive.
alter table transactions add column if not exists subcategory text;
