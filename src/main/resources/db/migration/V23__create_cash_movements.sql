create table cash_movements (
    id bigint generated always as identity primary key,
    bank_account_id bigint not null references bank_accounts(id),
    occurred_on date not null,
    direction varchar(10) not null,
    amount numeric(14,2) not null,
    currency varchar(3) not null,
    concept varchar(500),
    source varchar(25) not null,
    source_id bigint,
    constraint ck_cash_movements_direction check (direction in ('INFLOW', 'OUTFLOW')),
    constraint ck_cash_movements_amount check (amount > 0),
    constraint ck_cash_movements_currency check (currency in ('PEN', 'USD')),
    constraint ck_cash_movements_source check (source in ('OPENING_BALANCE', 'INVOICE_PAYMENT', 'EXPENSE',
        'FIXED_COST_PAYMENT', 'PAYABLE_PAYMENT', 'LOAN_DISBURSEMENT', 'LOAN_INSTALLMENT',
        'PARTNER_CONTRIBUTION', 'TAX_PAYMENT', 'TRANSFER', 'ADJUSTMENT'))
);
create index ix_cash_movements_account_date on cash_movements(bank_account_id, occurred_on);
