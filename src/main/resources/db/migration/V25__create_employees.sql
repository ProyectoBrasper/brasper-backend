create table brasper.employees (
    id bigint generated always as identity primary key,
    full_name varchar(200) not null, dni varchar(8) not null, position varchar(100),
    monthly_salary numeric(14,2) not null, currency varchar(3) not null,
    hired_on date not null, terminated_on date, active boolean not null,
    created_at timestamptz not null, updated_at timestamptz not null,
    constraint uk_employees_dni unique (dni),
    constraint ck_employees_salary check (monthly_salary > 0),
    constraint ck_employees_currency check (currency in ('PEN', 'USD')),
    constraint ck_employees_termination check (terminated_on >= hired_on)
);
