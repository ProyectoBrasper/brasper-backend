create table brasper.fixed_costs (
    id bigint generated always as identity primary key,
    code varchar(30) not null, concept varchar(500) not null, type varchar(30) not null,
    monthly_amount numeric(14,2) not null check (monthly_amount > 0),
    currency varchar(3) not null check (currency in ('PEN', 'USD')),
    employee_id bigint, active boolean not null default true,
    created_at timestamptz not null, updated_at timestamptz not null,
    constraint uk_fixed_costs_code unique (code),
    constraint ck_fixed_costs_salary check (type <> 'SALARY' or employee_id is not null),
    constraint ck_fixed_costs_type check (type in ('SALARY', 'OVERTIME', 'ACCOUNTING', 'SCTR_INSURANCE',
        'LIFE_INSURANCE', 'OFFICE_RENT', 'CLEANING', 'BANK_FEES', 'ITF', 'HOSTING', 'ADMINISTRATIVE'))
);
create table brasper.fixed_cost_expected_weeks (
    fixed_cost_id bigint not null references brasper.fixed_costs(id) on delete cascade,
    week_order int not null, expected_week int not null check (expected_week between 1 and 5),
    primary key (fixed_cost_id, week_order)
);
create table brasper.fixed_cost_payments (
    id bigint generated always as identity primary key,
    fixed_cost_id bigint not null references brasper.fixed_costs(id) on delete cascade,
    paid_on date not null, period_year int not null check (period_year > 0),
    period_month int not null check (period_month between 1 and 12),
    amount numeric(14,2) not null check (amount > 0),
    currency varchar(3) not null check (currency in ('PEN', 'USD')),
    bank_account_id bigint, notes text
);
create index ix_fixed_cost_payments_cost on brasper.fixed_cost_payments(fixed_cost_id);
