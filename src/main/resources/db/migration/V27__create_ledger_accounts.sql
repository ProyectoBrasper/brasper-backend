create table brasper.ledger_accounts (
    id bigint generated always as identity primary key,
    code varchar(10) not null, name varchar(200) not null,
    type varchar(20) not null, active boolean not null default true,
    created_at timestamptz not null, updated_at timestamptz not null,
    constraint uk_ledger_accounts_code unique (code),
    constraint ck_ledger_accounts_type check (type in ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE'))
);
