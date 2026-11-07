-- Customer advances (anticipos de clientes): the credit side of an advance invoice.
insert into brasper.ledger_accounts (code, name, type, active, created_at, updated_at) values
    ('122', 'Anticipos de clientes', 'LIABILITY', true, now(), now())
on conflict (code) do nothing;
