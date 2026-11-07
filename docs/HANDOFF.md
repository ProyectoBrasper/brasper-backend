# Traspaso: estado del backend de Brasper

Documento para retomar el trabajo en otra sesión (persona o agente). Última actualización: 2026-11-06, `develop` en el PR #54 más el PR de OpenAPI.
Reglas del repo: `AGENTS.md`. Diseño de dominio: `docs/uml/` (empezar por `00-context-map.puml`).

## Cómo correr y verificar
- `./mvnw -B verify` (usa Testcontainers: Docker Desktop debe estar encendido). Hoy pasan 276 tests.
- Swagger UI: `/swagger-ui.html`. Especificación: `/v3/api-docs`. Para probar: `POST /api/v1/auth/sign-in`, copiar el token y pegarlo en "Authorize".
- CI en GitHub Actions: `build` (`./mvnw -B verify`) y `pr-size` (máximo 400 líneas por PR). Los dos son obligatorios.

## Flujo de trabajo acordado
- Gitflow: ramas `feature/<contexto>-<tema>` desde `develop`, PR a `develop`, squash. Nunca commit directo a `main` ni `develop`.
- PR de 400 líneas máximo. Si no cabe, se parte: la entidad y su migración van juntas (`ddl-auto=validate`), el resto aparte.
- El agente revisa y mergea sus propios PRs a `develop` cuando CI está verde. Merges a `main` y acciones destructivas piden confirmación.
- **Commits y PRs sin `Co-Authored-By`, sin enlaces de sesión y sin firmas de herramientas.**
- Commits: Conventional Commits en inglés, `feat(<contexto>): ...`.
- Estilo de trabajo con el usuario: informes en español, primero lo que funciona ya, una sola acción siguiente. El usuario responde "sigue" para avanzar.
- Trabajo en paralelo con Codex (plugin `codex:codex-rescue`): el agente crea un worktree dentro del repo (`git worktree add .codex-wt/<tema> -b feature/<x> origin/develop`, la carpeta está excluida en `.git/info/exclude`). Codex solo escribe archivos: su sandbox no puede escribir en `.git` ni bajar dependencias de Maven. Quien lo lanzó corre `./mvnw verify` en el worktree, hace commit, push y PR, y luego borra el worktree.

## Estado por contexto
Hecho (dominio, servicios, API y fachada salvo que se indique):
- **Shared:** Money, Currency, Percentage, Period, CashFlowWeek, Ruc, TaxRates, TaxBreakdown, CostCategory, PaymentStatus, `Result`, RFC 9457 (`ProblemDetail`) con mensajes en `messages.properties` y `messages_es.properties`.
- **IAM:** usuarios, roles (ADMIN, ACCOUNTING, ENGINEER, MANAGEMENT), JWT sin estado, alta, baja y cambio de rol.
- **Projects:** clientes, proyectos con estados, avance mensual, estimaciones de costo, `ProjectsContextFacade`.
- **Billing:** facturas (IGV, retención, detracción), pagos, anular, cobros programados, `BillingContextFacade`.
- **Expenses:** proveedores, gastos (registrar, corregir, anular), `ExpensesContextFacade`.
- **Treasury, dominio y persistencia:** `BankAccount` con movimientos de caja y saldo (V20, V23), `Partner` con aportes (V21), `Payable` con pagos (V22), `Loan` con cuotas (V24).
- **Treasury, servicios:** `BankAccountCommandService` (abrir cuenta, registrar movimiento, transferir), `PartnerCommandService` (alta de socio, aporte), `LoanCommandService` (registrar préstamo, programar y pagar cuotas; mueve caja: RECEIVED entra al desembolsar y sale al pagar cuota, GRANTED al revés) y `PayableCommandService` (crear, pagar con salida de caja, reprogramar semana). En una transferencia el `sourceId` de cada movimiento es el id de la cuenta contraparte. Un aporte de socio solo se registra: no mueve caja (eso llegará con los eventos).
- **Treasury, fachada** `TreasuryContextFacade` (cuenta existe, tipo de cuenta, socio existe). Del UML faltan los saldos de caja y las sumas semanales: se agregan cuando Reporting las pida. Expenses la usa para validar que la cuenta o el socio del gasto existan (`ExpenseTreasuryLookup`).
- **Treasury, API de socios** (`/api/v1/partners`): alta, listar, ver (con aportes y total) y registrar aporte (`/{id}/contributions`, con moneda). Escritura solo ADMIN y ACCOUNTING. `totalContributed` suma montos sin convertir moneda. Falta el listado de aportes por periodo del UML.
- **Accounting, plan de cuentas** (V27): `LedgerAccount` con `AccountCode` (2 a 10 dígitos, PCGE), tipo y desactivar. Sin servicios, API ni datos semilla todavía. Numeración reservada: V28 asientos, V29 impuestos, V30 activos fijos.
- **Accounting, activos fijos** (V30): `FixedAsset` con depreciación mensual lineal sobre la base sin IGV, desde el mes siguiente a la compra; el último mes absorbe el redondeo. Sin servicios ni API todavía.
- **Accounting, libro diario** (V28): `JournalEntry` con `JournalLine`, partida doble (mínimo 2 líneas, cada línea debe o haber, suma de debe igual a haber, una sola moneda), `(source, sourceId)` único para idempotencia (los manuales no llevan `sourceId`) y reversa que crea el asiento inverso con fuente `REVERSAL`. Sin servicios ni API todavía.
- **Accounting, servicio del libro mayor:** `LedgerCommandService` (crear cuenta, registrar asiento, revertir). Un asiento que viola la partida doble se informa como `INVALID_ENTRY` (un solo fallo para todas las reglas). El UML tenía `PeriodClosed`: no hay cierre de periodos todavía, así que no existe.
- **Accounting, consultas del libro:** `LedgerQueryService` (cuentas por código, asientos por periodo o id, balance de comprobación). Atajo: el balance es el movimiento del periodo (sin saldos de apertura ni acumulados), sumado en memoria, una línea por cuenta y moneda.
- **Accounting, API del libro:** `/api/v1/ledger-accounts` (crear, listar, `/trial-balance?year=&month=`) y `/api/v1/journal-entries` (registrar asiento manual, listar por periodo, ver, `POST /{id}/reversal`). Escriben ADMIN y ACCOUNTING; leen ADMIN, ACCOUNTING y MANAGEMENT. El cliente no elige origen: los asientos de la API siempre son `MANUAL`. Importes como `{amount, currency}`; cuenta desconocida en un asiento responde 404.
- **Accounting, plan de cuentas inicial** (V31): 15 cuentas PCGE (10, 12, 33, 39, 40111, 42, 45, 50, 59, 60, 62, 63, 67, 68, 70). Las usan la depreciación (68 / 39) y las futuras reglas de asientos automáticos. Es un catálogo mínimo: ACCOUNTING agrega más por la API.
- **Accounting, depreciación mensual:** `POST /api/v1/fixed-assets/depreciation` `{year, month}` (ADMIN, ACCOUNTING) registra un asiento `DEPRECIATION` por moneda (debe 68, haber 39) con el cargo del mes de cada activo. El origen del asiento es (año*100+mes)*10+moneda, así que repetir el mes responde 409. Si falta la cuenta 68 o 39 responde 409 y no deja nada a medias (rollback). Un asiento por activo no se hace: se suma por moneda.
- **Accounting, servicio de la declaración de IGV** (V32 agrega la cuenta 4017): `TaxCommandService` prepara (IGV de ventas = haber de 40111, IGV de compras = debe de 40111, crédito arrastrado del mes anterior, pago a cuenta = ventas de la cuenta 70 x tasa vigente) y paga. Decisiones sin confirmar: pagar presenta la declaración de forma implícita (no hay paso `FILED` aparte) y registra el asiento D 40111 + D 4017 / H 10 (origen `TAX_RETURN`, id de la declaración); un banco inexistente responde 400. Atajos: las reversas cuentan del lado contrario en 40111 (el neto cuadra, los brutos no); solo líneas en PEN; pagar no mueve la caja de Treasury hasta que existan los eventos.
- **Accounting, API de declaraciones:** `/api/v1/tax-returns`: `POST {year, month}` prepara, `POST /{id}/payment {paidOn, bankAccountId}` paga, `GET ?year=&month=` consulta y `GET /history` lista (más reciente primero). Escriben ADMIN y ACCOUNTING; leen también MANAGEMENT.
- **Accounting, fachada** (`AccountingContextFacade`): `fetchTaxRatesAt` (tasas vigentes o las estándar si no hay parámetros), `fetchAccountBalancesAt`, `fetchAccountMovementsBetween` (movimiento por cuenta entre dos fechas), `fetchBalancesByAccountTypeAt`, `fetchResultByPeriod` (REVENUE, EXPENSE, RESULT) y `fetchTaxPayableByPeriod` (0 si no hay declaración). Saldos: debe menos haber en activos y gastos, al revés en el resto. Atajo: solo líneas en PEN, sumadas en memoria. Billing y Expenses ya la usan para las tasas.
- **Reporting, rentabilidad por proyecto (servicio y API):** `ProjectProfitabilityQueryService` compone Projects, Billing, Expenses y Accounting solo por sus fachadas (`ProfitabilitySource` y su adaptador). Comisión de venta = presupuesto x (1 + IGV de hoy) x tasa; utilidad bruta = (presupuesto - costo real - comisión) x participación; utilidad reconocida = utilidad bruta x avance del mes. Atajo: los importes de distintas monedas se suman como números (igual que en las fachadas). API: `GET /api/v1/reports/projects/{id}/profitability?year=&month=` y `GET /api/v1/reports/projects/profitability?year=&month=` (ADMIN, MANAGEMENT, ACCOUNTING).
- **Accounting, servicios de activos fijos y parámetros de impuestos:** `FixedAssetCommandService` (registrar con las tasas vigentes en la fecha de compra, dar de baja) y `TaxCommandService` (fijar parámetros, una fila por fecha de vigencia). Sin parámetros cargados se usan las tasas estándar (atajo). Faltan preparar y pagar la declaración y la depreciación mensual: dependen del libro diario.
- **Accounting, API de activos fijos y parámetros de impuestos:** `/api/v1/fixed-assets` (registrar, dar de baja con `POST /{id}/disposal`, listar, ver; incluye depreciación acumulada y valor en libros a hoy) y `/api/v1/tax-parameters` (fijar solo ADMIN, `?date=` vigente, `/history`). Las tasas viajan como fracciones (0.18 = 18%). Leen ADMIN, ACCOUNTING y MANAGEMENT; activos fijos los escriben ADMIN y ACCOUNTING.
- **Accounting, impuestos** (V29): `TaxParameters` (tasas por fecha de vigencia) y `TaxReturn` (declaración mensual de IGV: crédito arrastrado, IGV por pagar y total con pago a cuenta; borrador, presentada, pagada). Sin servicios ni API todavía.
- **Staff, dominio de empleados** (V25): `Employee` (contratar, actualizar cargo y sueldo, dar de baja) con `Dni` de 8 dígitos único. Sin API todavía.
- **Staff, servicio de empleados:** `EmployeeCommandService` (contratar, actualizar, dar de baja; rechaza DNI repetido, empleado inexistente o ya dado de baja y fecha de baja anterior a la contratación).
- **Staff, servicio de costos fijos:** `FixedCostCommandService` (crear, actualizar, desactivar, registrar pago). Valida código único, empleado (obligatorio para SALARY) y cuenta bancaria por `StaffTreasuryLookup` (fachada de Treasury). El pago no mueve caja todavía (llegará con el evento `FixedCostPaidEvent`).
- **Staff, API de empleados** (`/api/v1/employees`): contratar, actualizar cargo y sueldo (`PUT /{id}`), dar de baja (`POST /{id}/termination`), listar (`?active=`) y ver. Los sueldos son sensibles: leen ADMIN, ACCOUNTING y MANAGEMENT; escriben ADMIN y ACCOUNTING; ENGINEER recibe 403 (decisión mía, sin confirmar).
- **Staff, API de costos fijos** (`/api/v1/fixed-costs`): crear, actualizar (`PUT /{id}`), desactivar (`POST /{id}/deactivate`), listar (`?active=`), ver, registrar pago (`POST /{id}/payments`) y pagos de un periodo (`GET /payments?year=&month=`). Mismos permisos que empleados. La moneda de actualizaciones y pagos es la del costo. El controlador es `@Transactional` para leer los pagos lazy al armar la respuesta (atajo).
- **Staff, dominio de costos fijos** (V26): `FixedCost` con pagos mensuales (`FixedCostPayment`), semanas esperadas 1..5, `paidIn`/`pendingIn` por periodo. Un costo SALARY exige empleado. Sin servicios ni API todavía. Del UML faltan `expectedWeeksIn`/`amountPerWeek` (se agregan cuando Reporting los pida) y `FixedCostPaidEvent` (con los demás eventos).
- **Treasury, API de préstamos** (`/api/v1/loans`): registrar, listar (`?type=`), ver, programar cuota (`/{id}/installments`) y pagar cuota (`/{id}/installments/{installmentId}/payments`). Escritura solo ADMIN y ACCOUNTING.
- **Treasury, API de pagos pendientes** (`/api/v1/payables`): crear, listar (`?status=`), ver, pagar (`/{id}/payments`, escribe en caja) y reprogramar (`PUT /{id}/schedule`). Escritura solo ADMIN y ACCOUNTING.
- **Treasury, API de cuentas bancarias** (`/api/v1/bank-accounts`): listar, ver, movimientos por mes (`?year=&month=`), abrir cuenta, ajustes (`/{id}/adjustments`) y transferencias (`/transfers`). El monto va en la moneda de la cuenta (la de origen en transferencias). El saldo sale de `BankAccountQueryService` (los movimientos son lazy y `open-in-view` está apagado).

Pendiente, en este orden sugerido:
1. **Accounting** (`08-accounting.puml`) y **Reporting** (`09`). Staff está hecho salvo `StaffContextFacade` (sumas devengadas y pagadas/programadas por semana): se agrega cuando Reporting la pida.
2. Eventos de dominio (`InvoiceIssuedEvent`, `ExpenseRegisteredEvent`, `PayablePaidEvent`, ...) y sus manejadores. Hoy ningún flujo los usa.

## Atajos y desvíos del UML (todos documentados en el código)
- Las fachadas y sus implementaciones viven en `interfaces/acl` (el UML las pone en `application/acl`) porque ArchUnit prohíbe que `application` dependa de `interfaces`.
- Puertos de salida en `application/outboundservices`, adaptadores en `infrastructure/acl`.
- Billing y Expenses leen las tasas de Accounting por la fachada (`BillingAccountingTaxRatesProvider`, `ExpenseAccountingTaxRatesProvider`). Mientras no haya parámetros fijados rigen las tasas estándar (IGV 18%).
- Las fachadas suman montos de monedas distintas como números simples.
- Filtros y paginación en memoria; pagos, avances y movimientos con carga EAGER o historial completo. Pasar a SQL si los volúmenes crecen.
- Billing: `markAsRealized` de los cobros programados nunca se llama hasta que existan los eventos.
- Billing no valida que la moneda de la factura coincida con la del proyecto ni el tipo de cuenta bancaria del depósito.
- Expenses: un gasto no mueve caja; solo comprueba que la cuenta o el socio existan. Un proyecto solo `CANCELLED` rechaza gastos nuevos (uno `COMPLETED` los acepta).
- Cada fachada y servicio nuevo sigue el patrón: puerto de dominio + interfaz Spring Data que lo extiende, y los tests usan el puerto, no la interfaz JPA.

## Decisiones confirmadas por el usuario
- Flujo de caja semanal: un mes puede tener hasta 6 semanas (lunes a domingo, recortadas al mes). Se permite "Semana 06" porque algunos meses la necesitan (ej. noviembre de 2026, donde la semana 6 es solo el día 30). El Excel original solo llegaba a 5. Ver `CashFlowWeek`.

## Decisiones propias del agente, sin confirmar por el usuario
- Matriz de permisos: lecturas para cualquier usuario autenticado; escribir en Projects, clientes, proveedores y gastos con ADMIN, ACCOUNTING y ENGINEER; escribir en Billing con ADMIN y ACCOUNTING; MANAGEMENT solo lee. Treasury (cuentas, ajustes, transferencias) se escribe solo con ADMIN y ACCOUNTING.
- Los certificados de retención no cuentan como cobrado en `BillingContextFacade`, y los aportes de socio no cuentan como pagado en `ExpensesContextFacade`.
- Swagger UI y la especificación son públicos (no exponen datos); cada llamada sigue pidiendo token.

## Pendientes de operación
- Las migraciones V2 a V23 aún no se aplicaron en Supabase: se aplican en el siguiente arranque contra esa base. Solo tiene la baseline `V1`.
- La contraseña de Supabase se expuso en el chat al inicio; el usuario decidió no rotarla.
- Avisar al compañero del frontend de los desvíos del UML y de la reescritura del historial de `main` y `develop` (`git fetch && git reset --hard origin/develop`).
- Contenedores Docker viejos: el usuario nunca dijo cuáles borrar.
- La respuesta 403 de usuarios sin permiso es un cuerpo vacío de Spring (no `ProblemDetail`).
- Dos administradores que se desactivan a la vez podrían saltarse la regla del último administrador.
- No hay endpoints de reactivar usuario ni de cambiar contraseña. El RUC no valida el dígito verificador.
- Advertencia de Mockito sobre el agente en JDK 25 (inofensiva).

## Migraciones (esquema `brasper`)
V1 baseline, V2 users, V3 clients, V4 projects, V5 project_progress, V6 project_cost_estimates, V7 invoices, V8 invoice_payments, V9 scheduled_collections, V10 suppliers, V11 expenses, V20 bank_accounts, V21 partners, V22 payables, V23 cash_movements, V24 loans y loan_installments, V25 employees, V26 fixed_costs, V27 ledger_accounts, V28 journal_entries y journal_lines, V29 tax_parameters y tax_returns, V30 fixed_assets, V31 seed_chart_of_accounts, V32 seed_income_tax_account. Los saltos de numeración (V12 a V19) son a propósito: eran números reservados para trabajo en paralelo. Flyway no exige que sean consecutivos.
