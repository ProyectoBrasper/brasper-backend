create table bank_accounts (
    id             bigint generated always as identity primary key,
    bank           varchar(20) not null,
    account_number varchar(50) not null,
    type           varchar(20) not null,
    currency       varchar(3) not null,
    opening_balance numeric(14,2) not null,
    opened_on      date not null,
    created_at     timestamptz not null,
    updated_at     timestamptz not null,
    constraint uk_bank_accounts_account_number unique (account_number),
    constraint ck_bank_accounts_bank check (bank in ('BCP', 'BBVA', 'BANCO_DE_LA_NACION', 'CASH')),
    constraint ck_bank_accounts_type check (type in ('CHECKING', 'DETRACTIONS', 'PETTY_CASH')),
    constraint ck_bank_accounts_currency check (currency in ('PEN', 'USD'))
);
