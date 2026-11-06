-- Cross-context expense, supplier and project ids deliberately have no foreign keys.
create table payables (
    id bigint generated always as identity primary key,
    concept varchar(500) not null,
    type varchar(20) not null,
    total_amount numeric(14,2) not null,
    currency varchar(3) not null,
    supplier_id bigint,
    project_id bigint,
    expense_id bigint,
    scheduled_year int not null,
    scheduled_month int not null,
    scheduled_week int not null,
    status varchar(20) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint ck_payables_amount check (total_amount > 0),
    constraint ck_payables_currency check (currency in ('PEN', 'USD')),
    constraint ck_payables_type check (type in ('SUPPLIER', 'TECHNICIAN', 'ACCOUNTING', 'SELF_DETRACTION', 'TAX', 'OTHER')),
    constraint ck_payables_year check (scheduled_year > 0),
    constraint ck_payables_month check (scheduled_month between 1 and 12),
    constraint ck_payables_week check (scheduled_week between 1 and 6),
    constraint ck_payables_status check (status in ('PENDING', 'PARTIALLY_PAID', 'PAID'))
);
create table payable_payments (
    id bigint generated always as identity primary key,
    payable_id bigint not null references payables(id),
    paid_on date not null,
    amount numeric(14,2) not null,
    bank_account_id bigint not null references bank_accounts(id),
    constraint ck_payable_payments_amount check (amount > 0)
);
