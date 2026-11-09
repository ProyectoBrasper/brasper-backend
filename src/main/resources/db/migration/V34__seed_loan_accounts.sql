-- Loans the company granted (asset) and the interest they earn; loans received use 45, already seeded.
insert into brasper.ledger_accounts (code, name, type, active, created_at, updated_at) values
    ('16', 'Préstamos por cobrar', 'ASSET',   true, now(), now()),
    ('77', 'Ingresos financieros', 'REVENUE', true, now(), now())
on conflict (code) do nothing;
