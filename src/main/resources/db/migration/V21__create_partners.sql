create table partners (
    id bigint generated always as identity primary key,
    full_name varchar(200) not null,
    role varchar(100),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_partners_full_name unique (full_name)
);

create table partner_contributions (
    id bigint generated always as identity primary key,
    partner_id bigint not null references partners(id),
    contributed_on date not null,
    amount numeric(14,2) not null,
    currency varchar(3) not null,
    reimbursable boolean not null,
    bank_account_id bigint references bank_accounts(id),
    expense_id bigint,
    concept varchar(500),
    constraint ck_partner_contributions_amount check (amount > 0),
    constraint ck_partner_contributions_currency check (currency in ('PEN', 'USD'))
);
