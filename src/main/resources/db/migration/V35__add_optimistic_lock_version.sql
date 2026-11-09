-- Optimistic locking for the aggregates that guard a balance (invoices, payables, loans).
alter table invoices add column version bigint not null default 0;
alter table payables add column version bigint not null default 0;
alter table loans add column version bigint not null default 0;
