create table projects (
    id                    bigint generated always as identity primary key,
    code                  varchar(14)   not null,
    name                  varchar(200)  not null,
    description           varchar(1000),
    client_id             bigint        not null,
    budget_amount         numeric(14,2) not null,
    currency              varchar(3)    not null,
    quoted_on             date          not null,
    status                varchar(20)   not null,
    participation         numeric(5,4)  not null,
    sales_commission_rate numeric(5,4)  not null,
    created_at            timestamptz   not null,
    updated_at            timestamptz   not null,
    constraint uk_projects_code unique (code),
    constraint fk_projects_client foreign key (client_id) references clients (id),
    constraint ck_projects_budget check (budget_amount > 0),
    constraint ck_projects_currency check (currency in ('PEN', 'USD')),
    constraint ck_projects_status check (status in ('QUOTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

create index ix_projects_client on projects (client_id);
