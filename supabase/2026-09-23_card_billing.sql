-- Card billing cycle: one row per credit card wallet.
-- Replaces the hardcoded CUTOFF_DAY map in web/src/components/CardDebt.jsx.
--   cutoff_day  the day of month the statement closes
--   due_day     the day of month payment is due (in the month after the cutoff)
--   min_percent percentage of the revolving balance that forms the minimum payment
--   min_full_installments  when true, installment charges are billed in full and are
--                          not discounted by min_percent (BCA's rule; Mandiri does not do this)
create table card_billing (
    wallet                text primary key,
    cutoff_day            int not null,
    due_day               int not null,
    min_percent           numeric not null default 5,
    min_full_installments boolean not null default false
);

-- Verified against statements this cycle: BCA closes on the 3rd, due the 19th, minimum is the
-- installment step in full plus 5% of the rest (Sep bill 3,825,917 -> minimum 2,846,217).
-- Mandiri closes on the 11th, due the 1st, plain 5% (bill 20,953,446 -> minimum 1,047,680).
insert into card_billing (wallet, cutoff_day, due_day, min_percent, min_full_installments)
values
    ('BCA_CC',     3, 19, 5, true),
    ('MANDIRI_CC', 11, 1, 5, false);
