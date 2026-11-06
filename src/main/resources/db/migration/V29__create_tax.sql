create table brasper.tax_parameters (
    id bigint generated always as identity primary key,
    igv numeric(14,4) not null check (igv >= 0),
    detraction numeric(14,4) not null check (detraction >= 0),
    retention numeric(14,4) not null check (retention >= 0),
    income_tax_prepayment numeric(14,4) not null check (income_tax_prepayment >= 0),
    effective_from date not null, created_at timestamptz not null, updated_at timestamptz not null,
    constraint uk_tax_parameters_effective_from unique (effective_from)
);
create table brasper.tax_returns (
    id bigint generated always as identity primary key,
    period_year int not null check (period_year > 0),
    period_month int not null check (period_month between 1 and 12),
    output_igv numeric(14,2) not null check (output_igv >= 0),
    input_igv numeric(14,2) not null check (input_igv >= 0),
    carried_credit numeric(14,2) not null check (carried_credit >= 0),
    income_tax_prepayment numeric(14,2) not null check (income_tax_prepayment >= 0),
    currency varchar(3) not null check (currency in ('PEN', 'USD')),
    status varchar(10) not null check (status in ('DRAFT', 'FILED', 'PAID')),
    paid_on date, bank_account_id bigint, created_at timestamptz not null, updated_at timestamptz not null,
    constraint uk_tax_returns_period unique (period_year, period_month)
);
