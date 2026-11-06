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
- **Sin atribuciones**: ni `Co-Authored-By`, ni trailers de sesión, ni firmas de herramientas en commits ni en descripciones de PR. El autor es quien configura git.

## Pull Requests
- Máximo **400 líneas** cambiadas (sin generados ni lockfiles). Si no cabe, se parte en PRs.
- Descripción: qué cambia, por qué, cómo se probó.
- **Tests verdes antes de mergear.** La lógica nueva lleva tests en el mismo PR.
- Un PR no mezcla refactor con funcionalidad nueva.

## Flujo del agente (Claude Code)
- Al escribir código, usar el skill `ponytail`: el cambio más chico que resuelve la tarea completa, sin abstracciones ni features no pedidas.
- Al terminar una tarea, el agente abre el PR, lo **revisa** (diff, tests, tamaño, secretos) y lo **mergea por su cuenta** con squash cuando cumple todo:
  1. CI verde y `mvn verify` local verde.
  2. Máximo 400 líneas propias.
  3. Sin secretos ni archivos que no corresponden.
- Si algo falla o no cumple, no mergea: reporta qué falló.
- Esta autorización cubre PRs a `develop`. Merges a `main` (release/hotfix) y acciones destructivas siguen requiriendo confirmación.

### Continuidad entre sesiones
- `docs/HANDOFF.md` es la fuente del estado: qué contextos están hechos, atajos tomados, decisiones sin confirmar y pendientes. **Leerlo al empezar una sesión** y **actualizarlo al terminar cada contexto o cambio relevante**, en el mismo PR.
- Informes al usuario en español: primero lo que ya funciona, luego las decisiones tomadas, y una sola acción siguiente.

### Trabajo en paralelo con Codex (`codex:codex-rescue`)
1. Crear un worktree dentro del repo: `git fetch origin && git worktree add .codex-wt/<x> -b feature/<y> origin/develop` (`.codex-wt/` está en `.git/info/exclude`).
2. Lanzar Codex en segundo plano con un prompt autocontenido que diga "no ejecutes escrituras de git". Su sandbox no puede escribir en `.git` ni descargar dependencias Maven: **solo escribe archivos**.
3. El agente de Claude Code ejecuta en el worktree `git merge develop` y `./mvnw verify`, luego commit, push y PR, y sigue el flujo normal de merge.
4. Al terminar: `git worktree remove --force .codex-wt/<x>` y borrar la rama.

### Trampas conocidas
- Una entidad y su migración Flyway van en el **mismo PR** (`ddl-auto=validate` falla si falta una).
- Si un PR pasa de 400 líneas: entidad + migración juntas; servicios y API aparte; lectura y escritura de la API aparte.
- Comandos de shell largos que mezclan heredocs, comillas y python fallan con "unexpected EOF": escribir cada archivo con la herramienta de edición o usar un script aparte.
- `messages.properties` y `messages_es.properties` usan CRLF: conservarlo al editarlos. `UserCommandFailureMessagesTest` exige que cada enum de fallo tenga su clave en ambos.

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

Convenciones que salieron de aplicar esas reglas (desvían un poco de los UML):
- Los puertos de salida (hacia otro contexto) viven en `application/outboundservices`; sus adaptadores en `infrastructure/acl`.
- La implementación de cada fachada `*ContextFacade` vive en `interfaces/acl`, junto a su interfaz.
- Los nombres de adaptador son únicos entre contextos (ej. `ExpenseProjectsFacadeLookup`) para no chocar como beans.
- Repositorio = puerto plano en `domain/repositories` + interfaz Spring Data en `infrastructure/persistence/jpa` que extiende `JpaRepository` y el puerto.
- Los fallos de comando son enums mapeados a `ProblemDetail` (RFC 9457) por un assembler; los mensajes van en los dos bundles.

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
