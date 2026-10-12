# M3 — Conductores y Vehículos

Módulo del TPI Desarrollo de Software 2026. Java 21, Spring Boot, PostgreSQL,
Flyway y Thymeleaf. Dominio y casos de uso independientes de Spring.

## Ejecutar la aplicación integrada

Requiere Java 21 y PostgreSQL (`initdb`, `pg_ctl`, `psql`, `createdb`):

```bash
bash scripts/dev.sh
# En otra terminal, carga opcional de ejemplos:
bash scripts/seed-local.sh
```

Abrir **http://localhost:3003/ui/profile**. La base persiste en `.local/postgres`,
puerto `55434`; HTTP escucha sólo en loopback. Si `SPRING_DATASOURCE_URL` está
configurada, se usa esa conexión. Para detener el PostgreSQL local:
`pg_ctl -D "$PWD/.local/postgres" stop`.

La carga agrega cinco perfiles sintéticos identificados como **Ejemplo**, cinco
vehículos y quince documentos. Es repetible y no sobrescribe ni borra filas.
La identidad local `user-sub-123` corresponde al primer ejemplo, si no existía
un perfil con esa identidad. Todas las pantallas consultan y modifican la base.
Las antiguas rutas `/ui/preview/**` redirigen a las pantallas funcionales.
El subproyecto histórico `HU3-Aplicacion` se conserva; ejecutar desde la raíz
para usar su formulario integrado con PostgreSQL.

## Pantallas

Comparten menú lateral, tipografía, tarjetas y formularios adaptables al celular.
El menú móvil incluye cierre con Escape y control de foco.
Los recursos se sirven localmente y las mutaciones web requieren CSRF.

| Ruta | Función |
| --- | --- |
| `/ui/register` | Crear el perfil de la identidad actual |
| `/ui/profile` | Consultar datos personales y estado persistidos |
| `/ui/vehicles` | Registrar y consultar vehículos, color, capacidad y servicios |
| `/ui/documents` | Registrar licencias, seguros y cédulas; consultar revisiones |
| `/ui/qualification` | Evaluar conductor, vehículo, documentos y compatibilidad de licencia |
| `/ui/availability` | Solicitar disponibilidad, consultar confirmaciones y reintentar fallos |
| `/ui/customer-ratings` | Valorar clientes de viajes completados, previa validación con M6 |
| `/ui/operator/drivers` | Revisar documentos y decidir habilitación de conductores/vehículos |

Aprobar exige documentación actual, aprobada y vigente. Las decisiones conservan
motivo, actor, fecha y versión. Corregir documentación de perfiles o vehículos
observados/rechazados los devuelve a `PENDING`; no los habilita automáticamente.
Una suspensión requiere una decisión administrativa para levantarla.

Los cambios de vehículos, documentos, calificación y solicitudes incrementan la
versión del conductor. El alta de licencia y las decisiones verifican `If-Match`.
Los ETag de disponibilidad representan su propia `desiredVersion`.
`AUTO`/`MOTO` son tipos habilitados por la licencia; `STANDARD`, `PREMIUM`,
`DELIVERY` y `PETS` son categorías comerciales independientes.

## Identidad M1

`local`, `demo` y `test` admiten `X-Dev-Subject` y `X-Dev-Roles`; en local se
asignan roles conductor y operador por defecto. **`production` y `docker`
desactivan esas identidades**, incluso combinados con un perfil de desarrollo.
Usar esos perfiles al desplegar y configurar las variables de `.env.example`.

La API valida firma JWT, issuer, audiencia, expiración y subject. La web usa
Authorization Code con PKCE, sesión y logout OIDC. Los conductores acceden a sus
recursos; los operadores revisan y califican. Las integraciones requieren scopes
`m3:eligibility:read` o `m3:drivers:read`. Un token técnico no permite operaciones
de usuarios. Fuera de desarrollo, el arranque falla si falta issuer/JWKS.

## Disponibilidad M4 y valoraciones M6

El worker entrega el outbox mediante HTTP, con identidad estable de comando,
timeout, lease y hasta seis intentos con espera creciente. `APPLIED` exige un
HTTP 200 con `appliedVersion` igual a la enviada. HTTP 202 no confirma aplicación.
Una confirmación vieja no pisa una intención más nueva. Los fallos definitivos
pueden reintentarse. Desconectar sigue siendo posible con el conductor suspendido;
conectarse exige habilitación y documentos vigentes.

`APPLIED` confirma la intención en M4; **no afirma que el conductor esté libre de
viajes**. Sin M4 se conserva el estado pendiente, reintentando o fallido. Configurar
`M4_BASE_URL`; `M4_WORKER_ENABLED=false` desactiva el worker.

Para valorar un cliente, M6 debe confirmar el mismo viaje, conductor y estado
`COMPLETED`. El cliente proviene de M6. Se admite puntuación 1–5 y comentario de
hasta 500 caracteres, con unicidad por conductor/viaje e idempotencia por clave.
Una caída de M6 devuelve 503 y no guarda datos inventados. Configurar `M6_BASE_URL`
y las credenciales técnicas `OIDC_TECH_*`.

Los contratos consumidos están en `contracts/dependencies/` y se prueban con
servidores HTTP controlados. **La interoperabilidad con despliegues reales de
M1, M4 y M6 requiere configurar esos servicios y acordar los contratos.** M3 no
incluye sus implementaciones ni sus credenciales.

## REST de los flujos nuevos

- `POST /api/v1/drivers/{id}/qualification`
- `POST /api/v1/drivers/{id}/vehicles/{vehicleId}/qualification`
  - Body: `{"status":"APPROVED","reason":"Documentación verificada"}`.
  - Headers: `If-Match: "N"`, `Idempotency-Key` de 16–128 caracteres.
  - Historial: `GET` sobre la misma ruta con sufijo `/history`.
- `POST /api/v1/drivers/{id}/customer-ratings`
  - Body: `{"tripId":"viaje-id","score":5,"comment":"Buen viaje"}`.
  - Header: `Idempotency-Key`. `GET` lista las valoraciones.
- `POST /api/v1/drivers/{id}/availability/retry`
  - Reprograma la última intención fallida; responde 202.

Los historiales nuevos aceptan `offset` y `limit` (1–100). Errores REST en
`application/problem+json`; 403 por permisos, 409 por idempotencia, 412 por versión
desactualizada y 428 si falta `If-Match` donde es obligatorio.
Documentación interactiva local: `/swagger-ui/index.html`.

## Migraciones y pruebas

V4 corrige índices incompatibles con V1 heredada. V6 conserva los campos HU-03;
V7 agrega decisiones y valoraciones. Una base con otros checksums de migraciones
antiguas requiere un plan específico: no usar `flyway repair` ni borrar datos
para ocultar la incompatibilidad.

```bash
bash mvnw verify
# Migraciones y persistencia sobre una base PostgreSQL de pruebas:
M3_PG_TEST_URL=jdbc:postgresql://127.0.0.1:55433/postgres bash mvnw verify
```

Se prueban arquitectura, formularios, CSRF, decisiones con versión e idempotencia,
valoraciones, confirmaciones M4 fuera de orden, HTTP controlado y JWT firmados
(firma, audiencia, expiración, roles, scopes y rechazo de cabeceras de desarrollo).

Existe un modo `demo` con H2 persistente en `data/`; no es el utilizado por
`scripts/dev.sh` ni por la carga PostgreSQL.
