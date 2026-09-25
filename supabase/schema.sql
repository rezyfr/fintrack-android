-- Run this in the Supabase SQL editor to create the required tables.

create table transactions (
    id          bigserial primary key,
    item        text not null,
    amount      numeric not null,
    category    text not null,
    date        date not null,
    tab         text not null,
    note        text,
    created_at  timestamptz default now(),
    wallet      text not null,
    tx_type     text not null,
    to_wallet   text,
    to_amount   numeric,
    subcategory text,
    budget_line_id bigint
);

create table budgets (
    currency         text primary key,
    bills            numeric not null,
    subscriptions    numeric not null,
    entertainment    numeric not null,
    food_drink       numeric not null,
    groceries        numeric not null,
    health_wellbeing numeric not null,
    other            numeric not null,
    shopping         numeric not null,
    transport        numeric not null,
    travel           numeric not null,
    business         numeric not null,
    gifts            numeric not null
);

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

create table card_billing (
    wallet                text primary key,
    cutoff_day            int not null,
    due_day               int not null,
    min_percent           numeric not null default 5,
    min_full_installments boolean not null default false
);
