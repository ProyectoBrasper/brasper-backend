-- Where the undepreciated value of a disposed fixed asset is written off.
insert into brasper.ledger_accounts (code, name, type, active, created_at, updated_at) values
    ('65', 'Otros gastos de gestión', 'EXPENSE', true, now(), now())
on conflict (code) do nothing;
