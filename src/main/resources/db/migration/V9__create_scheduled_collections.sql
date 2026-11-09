-- project_id belongs to the Projects context: referenced by id, no foreign key across contexts.
create table scheduled_collections (
    id              bigint generated always as identity primary key,
    project_id      bigint        not null,
    expected_amount numeric(14,2) not null,
    currency        varchar(3)    not null,
    expected_year   int           not null,
    expected_month  int           not null,
    expected_week   int           not null,
    status          varchar(20)   not null,
    invoice_id      bigint,
    notes           varchar(1000),
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    constraint fk_scheduled_collections_invoice foreign key (invoice_id) references invoices (id),
    constraint ck_scheduled_collections_amount check (expected_amount > 0),
    constraint ck_scheduled_collections_currency check (currency in ('PEN', 'USD')),
    constraint ck_scheduled_collections_month check (expected_month between 1 and 12),
    constraint ck_scheduled_collections_week check (expected_week between 1 and 6),
    constraint ck_scheduled_collections_status check (status in ('SCHEDULED', 'RESCHEDULED', 'REALIZED', 'CANCELLED')),
    constraint ck_scheduled_collections_realized check ((status = 'REALIZED') = (invoice_id is not null))
);

create index ix_scheduled_collections_period on scheduled_collections (expected_year, expected_month);
create index ix_scheduled_collections_project on scheduled_collections (project_id);
