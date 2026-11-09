create table brasper.journal_entries (
    id bigint generated always as identity primary key,
    entry_date date not null, period_year int not null check (period_year > 0),
    period_month int not null check (period_month between 1 and 12),
    description varchar(300) not null check (length(trim(description)) > 0),
    source varchar(30) not null, source_id bigint, reversed boolean not null default false,
    reversal_of_id bigint, created_at timestamptz not null, updated_at timestamptz not null
);
create unique index uk_journal_entries_source on brasper.journal_entries(source, source_id) where source_id is not null;
create index ix_journal_entries_period on brasper.journal_entries(period_year, period_month);
create table brasper.journal_lines (
    id bigint generated always as identity primary key,
    journal_entry_id bigint not null references brasper.journal_entries(id) on delete cascade,
    account_code varchar(10) not null,
    debit numeric(14,2) not null default 0 check (debit >= 0),
    credit numeric(14,2) not null default 0 check (credit >= 0),
    currency varchar(3) not null check (currency in ('PEN', 'USD')), project_id bigint,
    check ((debit > 0) <> (credit > 0))
);
create index ix_journal_lines_account_code on brasper.journal_lines(account_code);
