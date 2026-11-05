-- bank_account_id belongs to the Treasury context: referenced by id, no foreign key across contexts.
create table invoice_payments (
    id              bigint generated always as identity primary key,
    invoice_id      bigint        not null,
    received_on     date          not null,
    amount          numeric(14,2) not null,
    type            varchar(25)   not null,
    bank_account_id bigint,
    reference       varchar(100),
    constraint fk_invoice_payments_invoice foreign key (invoice_id) references invoices (id),
    constraint ck_invoice_payments_amount check (amount > 0),
    constraint ck_invoice_payments_type check (type in ('NET_DEPOSIT', 'DETRACTION_DEPOSIT', 'RETENTION_CERTIFICATE'))
);

create index ix_invoice_payments_invoice on invoice_payments (invoice_id);
