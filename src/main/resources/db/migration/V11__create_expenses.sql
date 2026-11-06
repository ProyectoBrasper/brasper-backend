-- project_id, supplier_id, bank_account_id and partner_id are referenced by id only: no foreign keys.
create table expenses (
    id              bigint generated always as identity primary key,
    spent_on        date          not null,
    project_id      bigint,
    supplier_id     bigint,
    category        varchar(20)   not null,
    description     varchar(500),
    voucher_type    varchar(20)   not null,
    voucher_number  varchar(30),
    currency        varchar(3)    not null,
    taxable_base    numeric(14,2) not null,
    igv             numeric(14,2) not null,
    total           numeric(14,2) not null,
    payment_method  varchar(25)   not null,
    bank_account_id bigint,
    partner_id      bigint,
    payment_status  varchar(20)   not null,
    annulled        boolean       not null default false,
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    constraint ck_expenses_currency check (currency in ('PEN', 'USD')),
    constraint ck_expenses_category check (category in ('MATERIALS', 'LABOR', 'TRAVEL_EXPENSES', 'OTHER')),
    constraint ck_expenses_voucher_type check (voucher_type in ('INVOICE', 'SALES_RECEIPT', 'FEE_RECEIPT', 'NO_VOUCHER')),
    constraint ck_expenses_voucher_number check ((voucher_type = 'NO_VOUCHER') = (voucher_number is null)),
    constraint ck_expenses_payment_status check (payment_status in ('PENDING', 'PARTIALLY_PAID', 'PAID')),
    constraint ck_expenses_amounts check (taxable_base > 0 and igv >= 0 and total = taxable_base + igv),
    constraint ck_expenses_payment_target check (
        (payment_method in ('BANK_ACCOUNT', 'PETTY_CASH') and bank_account_id is not null and partner_id is null)
        or (payment_method = 'PARTNER_CONTRIBUTION' and partner_id is not null and bank_account_id is null))
);

create index ix_expenses_project on expenses (project_id);
create index ix_expenses_spent_on on expenses (spent_on);
