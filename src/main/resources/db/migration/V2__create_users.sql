create table users (
    id            bigint generated always as identity primary key,
    username      varchar(50)  not null,
    password_hash varchar(100) not null,
    full_name     varchar(150) not null,
    role          varchar(20)  not null,
    active        boolean      not null default true,
    created_at    timestamptz  not null,
    updated_at    timestamptz  not null,
    constraint uk_users_username unique (username),
    constraint ck_users_role check (role in ('ROLE_ADMIN', 'ROLE_MANAGEMENT', 'ROLE_ACCOUNTING', 'ROLE_ENGINEER'))
);
