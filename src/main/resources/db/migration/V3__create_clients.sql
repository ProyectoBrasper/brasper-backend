create table clients (
    id            bigint generated always as identity primary key,
    business_name varchar(200) not null,
    ruc           varchar(11)  not null,
    contact_name  varchar(150),
    phone         varchar(30),
    email         varchar(150),
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint uk_clients_ruc unique (ruc)
);
