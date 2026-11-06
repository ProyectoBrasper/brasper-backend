create table brasper.loans (
    id bigint generated always as identity primary key, type varchar(20) not null,
    counterparty varchar(200) not null, principal numeric(14,2) not null,
    currency varchar(3) not null, interest_rate numeric not null,
    disbursed_on date not null, bank_account_id bigint not null references brasper.bank_accounts(id),
    status varchar(20) not null, created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint ck_loans_principal check (principal > 0),
    constraint ck_loans_interest_rate check (interest_rate >= 0),
    constraint ck_loans_currency check (currency in ('PEN', 'USD')),
    constraint ck_loans_type check (type in ('RECEIVED', 'GRANTED')),
    constraint ck_loans_status check (status in ('PENDING', 'PARTIALLY_PAID', 'PAID'))
);
create table brasper.loan_installments (
    id bigint generated always as identity primary key, loan_id bigint not null references brasper.loans(id),
    due_on date not null, due_year int not null,
    due_month int not null, due_week int not null,
    principal numeric(14,2) not null, interest numeric(14,2) not null,
    paid boolean not null default false, paid_on date,
    bank_account_id bigint references brasper.bank_accounts(id),
    constraint ck_loan_installments_amounts check (principal > 0 and interest >= 0),
    constraint ck_loan_installments_year check (due_year > 0),
    constraint ck_loan_installments_month check (due_month between 1 and 12),
    constraint ck_loan_installments_week check (due_week between 1 and 6),
    constraint ck_loan_installments_payment check
        ((paid and paid_on is not null and bank_account_id is not null)
        or (not paid and paid_on is null and bank_account_id is null))
);
create index ix_loan_installments_loan on brasper.loan_installments (loan_id);
