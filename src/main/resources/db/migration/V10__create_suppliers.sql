create table suppliers (
    id            bigint generated always as identity primary key,
    business_name varchar(200) not null,
    ruc           varchar(11),
    type          varchar(20)  not null,
    phone         varchar(30),
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint uk_suppliers_ruc unique (ruc),
    constraint ck_suppliers_company_has_ruc check (type <> 'COMPANY' or ruc is not null)
);
