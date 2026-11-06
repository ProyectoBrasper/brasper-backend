create table brasper.fixed_assets (
    id bigint generated always as identity primary key,
    description varchar(300) not null check (length(trim(description)) > 0),
    acquired_on date not null,
    taxable_base numeric(14,2) not null check (taxable_base > 0),
    igv numeric(14,2) not null check (igv >= 0),
    total numeric(14,2) not null check (total = taxable_base + igv),
    currency varchar(3) not null check (currency in ('PEN', 'USD')),
    useful_life_months int not null check (useful_life_months between 1 and 600),
    expense_id bigint, disposed_on date,
    created_at timestamptz not null, updated_at timestamptz not null,
    constraint ck_fixed_assets_disposal check (disposed_on >= acquired_on)
);
