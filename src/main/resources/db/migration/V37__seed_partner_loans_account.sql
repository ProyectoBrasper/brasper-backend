-- Money a partner put in and will get back is a debt of the company, not capital (50).
insert into brasper.ledger_accounts (code, name, type, active, created_at, updated_at) values
    ('455', 'Cuentas por pagar a socios', 'LIABILITY', true, now(), now())
on conflict (code) do nothing;
