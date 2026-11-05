# AGENTS.md — brasper-backend

Reglas para cualquier persona o agente que trabaje en este repo.
Diseño de dominio: `docs/uml/` (empezar por `00-context-map.puml`).

## Stack
- Java 25, Maven, Spring Boot 4.1.x
- PostgreSQL en Supabase, esquema con Flyway (`spring.jpa.hibernate.ddl-auto=validate`)
- Tests: JUnit 5, Testcontainers (PostgreSQL), ArchUnit
- Lombok solo para `@Getter`
- Deploy: servidor local del cliente o Render (una sola imagen Docker)

## Git (gitflow)
- `main` = producción. `develop` = integración.
- Ramas de trabajo: `feature/<contexto>-<descripcion-corta>`, `release/<version>`, `hotfix/<descripcion>`.
- Nunca commit directo a `main` ni `develop`. Todo entra por PR.
- Las `feature/*` salen de `develop` y vuelven a `develop` con squash merge.
- `release/*` y `hotfix/*` salen de `main`/`develop` según gitflow y se mergean a ambas.

## Commits
- Conventional Commits: `feat|fix|refactor|test|docs|chore|ci(<contexto>): <asunto>`.
- Asunto en imperativo, máximo 72 caracteres, sin punto final.
- Un commit = una idea. Cuerpo solo si el "por qué" no es obvio.

## Pull Requests
- Máximo **400 líneas** cambiadas (sin generados ni lockfiles). Si no cabe, se parte en PRs.
- Descripción: qué cambia, por qué, cómo se probó.
- **Tests verdes antes de mergear.** La lógica nueva lleva tests en el mismo PR.
- Un PR no mezcla refactor con funcionalidad nueva.

## Arquitectura (Clean Architecture + DDD)
Paquete base: `com.brasper.platform`. Un paquete por bounded context:

```
shared/ iam/ projects/ billing/ expenses/ staff/ treasury/ accounting/ reporting/
  <contexto>/domain/{model,repositories,events}
  <contexto>/application/{commandservices,queryservices,eventhandlers,acl}
  <contexto>/infrastructure/persistence
  <contexto>/interfaces/{rest,acl}
```

Reglas:
1. Las dependencias apuntan hacia adentro: `interfaces` / `infrastructure` → `application` → `domain`.
2. `domain` no depende de Spring ni de controllers. JPA solo para mapeo, si hace falta.
3. Un contexto **nunca** usa el repositorio de otro: usa la fachada ACL (`I*ContextFacade`) o eventos de dominio.
4. Referencias entre agregados de distintos contextos solo por `Id`, nunca por relación JPA.
5. Eventos de dominio se publican después de persistir y se consumen con `@TransactionalEventListener`.
6. ArchUnit hace cumplir estas reglas en CI.

## SOLID
- Inyección por constructor. Dependencias como interfaces (puertos).
- Controllers sin lógica de negocio: solo traducen Resource ↔ Command/Query.
- Una clase, una razón para cambiar.

## Testing
- Dominio: unit tests puros, sin Spring.
- Servicios de aplicación: unit tests con mocks de puertos.
- Persistencia e integración: Testcontainers con PostgreSQL real, sin H2.
- `mvn verify` debe pasar antes de abrir y antes de mergear el PR.

## Configuración y secretos
- Ningún secreto en el repo. Todo por variables de entorno; documentar en `.env.example`.
- Variables clave: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `JWT_SECRET`.
- Supabase: usar conexión directa o session pooler (puerto 5432) con `sslmode=require`. El transaction pooler (6543) rompe Flyway.
- Perfil `dev` usa Postgres local; `prod` exige las variables.

## Idioma y nombres
- Código, commits y PRs: inglés o español, pero consistente por mensaje.
- Clases e identificadores en inglés. Lenguaje ubicuo ES↔EN según el glosario de `00-context-map.puml`.
