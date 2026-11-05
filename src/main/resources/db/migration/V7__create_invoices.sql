-- project_id and client_id belong to the Projects context: referenced by id, no foreign key across contexts.
create table invoices (
    id              bigint generated always as identity primary key,
    series          varchar(4)    not null,
    correlative     int           not null,
    issued_on       date          not null,
    purchase_order  varchar(50),
    project_id      bigint        not null,
    client_id       bigint        not null,
    currency        varchar(3)    not null,
    taxable_base    numeric(14,2) not null,
    igv             numeric(14,2) not null,
    total           numeric(14,2) not null,
    retention       numeric(14,2) not null,
    detraction      numeric(14,2) not null,
    has_retention   boolean       not null,
    has_detraction  boolean       not null,
    self_detraction boolean       not null,
    advance         boolean       not null,
    status          varchar(20)   not null,
    notes           varchar(1000),
    created_at      timestamptz   not null,
    updated_at      timestamptz   not null,
    constraint uk_invoices_number unique (series, correlative),
    constraint ck_invoices_correlative check (correlative between 1 and 99999999),
    constraint ck_invoices_currency check (currency in ('PEN', 'USD')),
    constraint ck_invoices_status check (status in ('PENDING', 'PARTIALLY_COLLECTED', 'COLLECTED', 'VOIDED')),
    constraint ck_invoices_amounts check (taxable_base > 0 and igv >= 0 and total = taxable_base + igv
        and retention >= 0 and detraction >= 0 and retention + detraction <= total),
    constraint ck_invoices_self_detraction check (not self_detraction or has_detraction)
);

create index ix_invoices_project on invoices (project_id);
create index ix_invoices_issued_on on invoices (issued_on);
