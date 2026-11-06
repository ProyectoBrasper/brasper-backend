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
- **Treasury, servicios:** `BankAccountCommandService` (abrir cuenta, registrar movimiento, transferir), `PartnerCommandService` (alta de socio, aporte), `LoanCommandService` (registrar préstamo, programar y pagar cuotas; mueve caja: RECEIVED entra al desembolsar y sale al pagar cuota, GRANTED al revés) y `PayableCommandService` (crear, pagar con salida de caja, reprogramar semana). En una transferencia el `sourceId` de cada movimiento es el id de la cuenta contraparte. Un aporte de socio solo se registra: no mueve caja (eso llegará con los eventos). Sin fachada todavía.
- **Treasury, API de cuentas bancarias** (`/api/v1/bank-accounts`): listar, ver, movimientos por mes (`?year=&month=`), abrir cuenta, ajustes (`/{id}/adjustments`) y transferencias (`/transfers`). El monto va en la moneda de la cuenta (la de origen en transferencias). El saldo sale de `BankAccountQueryService` (los movimientos son lazy y `open-in-view` está apagado).

Pendiente, en este orden sugerido:
1. **Treasury:** servicios de `Payable` y `Loan`, API de Payable, Loan y Partner, `TreasuryContextFacade`, validar cuentas y socios para Expenses.
2. **Staff** (`06-staff.puml`), **Accounting** (`08`), **Reporting** (`09`).
3. Eventos de dominio (`InvoiceIssuedEvent`, `ExpenseRegisteredEvent`, `PayablePaidEvent`, ...) y sus manejadores. Hoy ningún flujo los usa.

## Atajos y desvíos del UML (todos documentados en el código)
- Las fachadas y sus implementaciones viven en `interfaces/acl` (el UML las pone en `application/acl`) porque ArchUnit prohíbe que `application` dependa de `interfaces`.
- Puertos de salida en `application/outboundservices`, adaptadores en `infrastructure/acl`.
- `FixedTaxRatesProvider` (Billing) y `ExpenseFixedTaxRatesProvider` (Expenses) devuelven tasas fijas (IGV 18%) hasta que exista Accounting.
- Las fachadas suman montos de monedas distintas como números simples.
- Filtros y paginación en memoria; pagos, avances y movimientos con carga EAGER o historial completo. Pasar a SQL si los volúmenes crecen.
- Billing: `markAsRealized` de los cobros programados nunca se llama hasta que existan los eventos.
- Billing no valida que la moneda de la factura coincida con la del proyecto ni el tipo de cuenta bancaria del depósito.
- Expenses: cuentas bancarias y socios solo se validan como presentes. Un proyecto solo `CANCELLED` rechaza gastos nuevos (uno `COMPLETED` los acepta).
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
V1 baseline, V2 users, V3 clients, V4 projects, V5 project_progress, V6 project_cost_estimates, V7 invoices, V8 invoice_payments, V9 scheduled_collections, V10 suppliers, V11 expenses, V20 bank_accounts, V21 partners, V22 payables, V23 cash_movements, V24 loans y loan_installments. Los saltos de numeración (V12 a V19) son a propósito: eran números reservados para trabajo en paralelo. Flyway no exige que sean consecutivos.
