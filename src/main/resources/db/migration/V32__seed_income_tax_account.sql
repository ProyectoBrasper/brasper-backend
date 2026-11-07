-- Account used when a tax return is paid: income tax prepayment (pago a cuenta).
insert into brasper.ledger_accounts (code, name, type, active, created_at, updated_at) values
    ('4017', 'Impuesto a la renta', 'LIABILITY', true, now(), now())
on conflict (code) do nothing;
