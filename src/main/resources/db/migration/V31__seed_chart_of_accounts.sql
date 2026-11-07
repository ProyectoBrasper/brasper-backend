-- Base chart of accounts (PCGE-style codes) used by the automatic postings and depreciation.
insert into brasper.ledger_accounts (code, name, type, active, created_at, updated_at) values
    ('10',    'Efectivo y equivalentes de efectivo',        'ASSET',     true, now(), now()),
    ('12',    'Cuentas por cobrar comerciales',             'ASSET',     true, now(), now()),
    ('33',    'Inmuebles, maquinaria y equipo',             'ASSET',     true, now(), now()),
    ('39',    'Depreciación acumulada',                     'ASSET',     true, now(), now()),
    ('40111', 'IGV - Cuenta propia',                        'LIABILITY', true, now(), now()),
    ('42',    'Cuentas por pagar comerciales',              'LIABILITY', true, now(), now()),
    ('45',    'Obligaciones financieras',                   'LIABILITY', true, now(), now()),
    ('50',    'Capital',                                    'EQUITY',    true, now(), now()),
    ('59',    'Resultados acumulados',                      'EQUITY',    true, now(), now()),
    ('60',    'Compras',                                    'EXPENSE',   true, now(), now()),
    ('62',    'Gastos de personal',                         'EXPENSE',   true, now(), now()),
    ('63',    'Gastos de servicios prestados por terceros', 'EXPENSE',   true, now(), now()),
    ('67',    'Gastos financieros',                         'EXPENSE',   true, now(), now()),
    ('68',    'Depreciación del ejercicio',                 'EXPENSE',   true, now(), now()),
    ('70',    'Ventas',                                     'REVENUE',   true, now(), now())
on conflict (code) do nothing;
