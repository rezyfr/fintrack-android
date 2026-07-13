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
    to_amount   numeric
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
