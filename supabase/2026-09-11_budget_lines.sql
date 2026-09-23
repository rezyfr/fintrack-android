-- Budget cycle tracking: one row per budget line.
-- A line's actual spend is matched from transactions by wallet + item pattern + category;
-- rec_* are the observed min/median/max over the five pay cycles 26 Mar - 25 Aug 2026.
-- rec_cycles counts how many of those cycles produced a match, so the UI can distinguish
-- "you never spend here" from "this line has never been tagged in the data".

create table budget_lines (
    id               bigserial primary key,
    name             text not null,
    kind             text not null check (kind in ('fixed', 'flex')),
    currency         text not null check (currency in ('THB', 'IDR')),
    target           numeric not null,
    due_day          int,
    match_wallets    text[],
    match_pattern    text,
    match_categories text[],
    rec_min          numeric,
    rec_median       numeric,
    rec_max          numeric,
    rec_cycles       int not null default 0,
    sort_order       int not null default 0,
    active           boolean not null default true
);

insert into budget_lines
    (name, kind, currency, target, due_day, match_wallets, match_pattern, match_categories,
     rec_min, rec_median, rec_max, rec_cycles, sort_order)
values
    ('Condo rent', 'fixed', 'THB', 11500, 26, array['BBL'], '^Rent$|^Condo$|LEKDARA', null, 0, 11500, 11500, 4, 0),
    ('Condo internet + electricity', 'fixed', 'THB', 2500, 26, array['BBL'], 'Electricity|Internet|Clean Service', null, 0, 2423, 3305, 4, 1),
    ('Wife', 'fixed', 'IDR', 4000000, 26, array['BCA'], '^Vira$', null, 0, 0, 4000000, 1, 2),
    ('Car installment', 'fixed', 'IDR', 3650000, 26, array['BCA'], 'Cicilan Mobil', null, 0, 3647500, 3647500, 3, 3),
    ('Mom', 'fixed', 'IDR', 800000, 26, array['BCA'], '^Mamah$|^Transfer ke Mamah$', null, 0, 200000, 900000, 4, 4),
    ('Dad', 'fixed', 'IDR', 600000, 26, array['BCA'], '^Abah$|^Transfer ke Abah$', null, 0, 0, 0, 0, 5),
    ('Parents electricity', 'fixed', 'IDR', 500000, 26, array['BCA'], 'Listrik', null, 0, 0, 406000, 2, 6),
    ('Child school', 'fixed', 'IDR', 700000, 27, array['BCA'], 'Sekolah|School|SPP|Mika Sekolah', null, 0, 0, 0, 0, 7),
    ('Groceries (Indonesia)', 'fixed', 'IDR', 860753, null, array['BCA_CC','BCA'], 'NAGA SWALAYAN|Alfamart|MIDI VC66|SUPERINDO', null, 704120, 848286, 1084261, 5, 8),
    ('Telkomsel', 'fixed', 'IDR', 88800, null, array['BCA_CC'], 'TELKMSL|Telkomsel|ATPY', null, 88800, 88800, 88800, 5, 9),
    ('Fuel', 'fixed', 'IDR', 60000, null, array['BCA_CC'], 'SPBU', null, 0, 100000, 250000, 3, 10),
    ('Card fees', 'fixed', 'IDR', 22500, null, array['MANDIRI_CC','BCA_CC'], 'STAMP DUTY|E-BILLING|TRX NOTIFICATION', null, 15000, 22500, 30000, 5, 11),
    ('Groceries (Bangkok)', 'flex', 'THB', 5300, null, array['BBL','MANDIRI_CC'], 'MAKRO|7[ -]?11|GrabMart|Big C|Susu Mika', array['Groceries'], 3706, 5732, 8082, 5, 12),
    ('Food & drink (Bangkok)', 'flex', 'THB', 6800, null, array['BBL','MANDIRI_CC'], 'SHOPEEFOOD|LINEPAY \*PF|LPTH\*|Kebab|BonChon|HACHICKEN|Pad Thai|Che Noo', array['Food & Drink'], 6870, 8536, 12306, 5, 13),
    ('Transport (Bangkok)', 'flex', 'THB', 1120, null, array['BBL','MANDIRI_CC'], 'GRAB\.COM|BOLT|GRABTAXI|LINE MAN RIDE|LINEPAY\*LP|^Grab$|^Bolt$', array['Transport'], 1744, 3595, 6019, 5, 14),
    ('Telco AIS', 'flex', 'THB', 200, null, array['BBL','MANDIRI_CC'], 'AIS SERVICES|Paket AIS', null, 76, 226, 676, 5, 15),
    ('Subscriptions', 'flex', 'THB', 680, null, array['MANDIRI_CC','BBL'], 'Google One|Google YouTube|Spotify|GITHUB|STEAM|Claude|GOOGLECLOUD|Runna', array['Subscriptions'], 66, 1044, 1505, 5, 16),
    ('Shopping', 'flex', 'THB', 0, null, array['BBL','MANDIRI_CC','BCA_CC'], 'SHOPEE(?!FOOD)|FOR SHOPEE|TIKTOK SHOP|Uniqlo', array['Shopping'], 2928, 9041, 21555, 5, 17),
    ('Entertainment', 'flex', 'THB', 1200, null, array['BBL'], 'Entertai|Xscape|Marathon', array['Entertainment'], 0, 1670, 4251, 4, 18),
    ('Cash withdrawals / misc', 'flex', 'THB', 1400, null, array['BBL'], 'CASH W/D|CASH ATM|A transaction of', null, 20, 2000, 3000, 5, 19);

-- Added 2026-09-11 after live data showed the current cycle is Indonesia-based BCA debit spend
-- that matched no line (Rp 21.2M unbudgeted). These lines are category-matched on the Indonesian
-- wallets and sit after the exact-item commitment lines, so Wife/Mom/Dad/Car/school still win.
-- Targets are 8 days (26 Sep - 3 Oct in Indonesia) at the rate observed 26 Aug - 10 Sep.
-- rec_* stay near zero because all five reference cycles were spent in Bangkok.
insert into budget_lines
    (name, kind, currency, target, due_day, match_wallets, match_pattern, match_categories,
     rec_min, rec_median, rec_max, rec_cycles, sort_order)
values
    ('Food & drink (Indonesia)',     'flex', 'IDR', 1180000, null, array['BCA','MANDIRI'], null, array['Food & Drink'],  0, 0, 395580, 1, 20),
    ('Groceries (Indonesia, daily)', 'flex', 'IDR',  610000, null, array['BCA','MANDIRI'], null, array['Groceries'],     0, 0, 527200, 2, 21),
    ('Entertainment (Indonesia)',    'flex', 'IDR',  350000, null, array['BCA','MANDIRI'], null, array['Entertainment'], 0, 0,      0, 0, 22),
    ('Transport (Indonesia)',        'flex', 'IDR',  190000, null, array['BCA','MANDIRI'], null, array['Transport'],     0, 0,  50000, 1, 23),
    ('Shopping (Indonesia)',         'flex', 'IDR',  190000, null, array['BCA','MANDIRI'], null, array['Shopping'],      0, 0,  45000, 1, 24);

-- Child school is "Superstar Gymnastic": Rp 750,000/month, first billed 26 Aug 2026 alongside a
-- one-off Rp 1,000,000 starting fee. It had never matched under the guessed name.
update budget_lines
   set target = 750000, due_day = 26, match_pattern = 'Superstar Gymnastic'
 where name = 'Child school';

-- Added 2026-09-13 after importing the Aug-Sep Mandiri CC statement. Card interest is the single
-- largest recurring charge on that card (466,050 this cycle) and had no line; Indonesian Grab rides
-- post as "Grab* A-<code> South JakartaID" on the card, which matched nothing. Transport (Indonesia)
-- sorts after Transport (Bangkok), so "WWW.GRAB.COM BANGKOK TH" still lands on the Bangkok line.
insert into budget_lines
    (name, kind, currency, target, due_day, match_wallets, match_pattern, match_categories,
     rec_min, rec_median, rec_max, rec_cycles, sort_order)
values
    ('Card interest', 'flex', 'IDR', 466050, null, array['MANDIRI_CC','BCA_CC'], '^INTEREST$|BIAYA BUNGA', null, 406066, 466050, 496867, 3, 25);

update budget_lines
   set match_wallets = array['BCA','MANDIRI','MANDIRI_CC','BCA_CC'],
       match_pattern = 'Grab\* A-|GRABTAXI',
       target = 550000
 where name = 'Transport (Indonesia)';
