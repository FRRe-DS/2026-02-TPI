# M3 — Conductores y Vehículos

Microservicio del TPI Desarrollo de Software 2026. Administra perfiles de
conductores, vehículos, documentación, habilitación y disponibilidad. M3 es dueño
de sus datos; M1 sólo aporta identidad y acceso mediante el `sub` del JWT.

## 1. Iniciar la aplicación

Se requiere **Java 21** y PostgreSQL con `initdb`, `pg_ctl`, `psql` y `createdb`.
También se requiere Docker en ejecución: M1/Keycloak local se inicia en un
contenedor y el arranque alternativo con `docker compose` de M3 también lo usa.
Si Docker está apagado, iniciarlo una vez con:

```bash
sudo systemctl start docker
```

```bash
bash scripts/dev.sh
# En otra terminal, carga datos sintéticos persistentes:
bash scripts/seed-local.sh
```

Abrir [http://localhost:3003](http://localhost:3003). Después de iniciar sesión,
M3 lleva a un conductor a `/ui/profile` y a un operador o administrador sin rol
de conductor a `/ui/operator/drivers`. El
script inicia PostgreSQL en `.local/postgres`, puerto `55434`, y la aplicación
escucha en loopback. Si se define `SPRING_DATASOURCE_URL`, usa esa base en su
lugar. Para detener la base local:

```bash
pg_ctl -D "$PWD/.local/postgres" stop
```

La carga crea cinco perfiles, vehículos y documentos identificados como
**Ejemplo**. Es repetible: no borra ni reemplaza datos existentes. El primer
perfil corresponde al sujeto local `user-sub-123`.

Para M1/Keycloak, copiar `.env.example`, completar sólo los datos del cliente
M3 asignado por M1 y ejecutar con el perfil `production`. No guardar secretos
de clientes en el repositorio.

### Credenciales locales de prueba

El Keycloak local usado para probar M3 tiene las siguientes cuentas de
demostración. Son credenciales de desarrollo: no deben usarse fuera del entorno
local.

| Uso | Usuario | Contraseña | Rol |
| --- | --- | --- | --- |
| Consola de Keycloak | `admin` | `ds2025` | Administrador del realm |
| Perfil de conductor | `m3-demo` | `m3-demo-local` | `DRIVER` |
| Pantallas administrativas | `m3-operator` | `m3-operator-local` | `OPERATOR` |

La consola se encuentra en [http://localhost:8080](http://localhost:8080).
Para iniciar M3 como conductor u operador, abrir
[http://localhost:3003](http://localhost:3003), elegir **Ingresar con M1** e
ingresar con la cuenta correspondiente.

Para recorrer un perfil local sin autenticar contra M1, habilitar la simulación
sólo en desarrollo con `DEMO_ACCESS_ENABLED=true` y
`DEMO_ACCESS_SUBJECT=<identity_subject de un perfil existente>`. La pantalla
inicial muestra **Simular acceso al perfil** cuando ambas variables están
configuradas. Para recorrer las pantallas administrativas, agregar
`DEMO_ACCESS_ROLES=ROLE_DRIVER,ROLE_OPERATOR`. Con esa configuración,
`/demo/operator` abre directamente el listado administrativo de ejemplo.

## 2. Qué funciona

Las pantallas comparten navegación lateral, se adaptan a celular y consultan
PostgreSQL.

| Ruta | Función |
| --- | --- |
| `/ui/register` y `/ui/profile` | Alta y consulta del perfil de la identidad actual |
| `/ui/vehicles` | Registro y consulta de vehículos |
| `/ui/documents` | Licencias, seguros y cédulas con su revisión |
| `/ui/qualification` | Estado de habilitación y decisiones |
| `/ui/availability` | Solicitud de disponibilidad, confirmación y reintento hacia M4 |
| `/ui/customer-ratings` | Valoraciones de clientes verificadas con M6 |
| `/ui/operator/drivers` | Revisión administrativa de conductores y documentos |

Una aprobación exige documentación vigente y aprobada. Corregir un documento
observado o rechazado vuelve a dejar la habilitación en `PENDING`; no habilita
automáticamente. Las decisiones registran motivo, actor, fecha y versión. Los
cambios relevantes incrementan la versión del conductor y REST devuelve `ETag`.

M4 recibe disponibilidad mediante outbox transaccional, reintentos y control de
versiones. M6 valida que el viaje pertenece al conductor y está `COMPLETED`
antes de guardar una valoración. Los errores REST usan RFC 9457,
`application/problem+json`.

## 3. Módulos y arquitectura

La aplicación sigue arquitectura hexagonal. El dominio no depende de Spring,
HTTP ni JPA.

| Capa | Ubicación | Responsabilidad |
| --- | --- | --- |
| Web | `adapter/web` | Controladores Thymeleaf, formularios, CSRF y navegación |
| API REST | `adapter/rest` | Contratos HTTP, ETag y respuestas `ProblemDetail` |
| Aplicación | `application/usecase`, `application/port` | Casos de uso y puertos de entrada/salida |
| Dominio | `domain` | Reglas de conductor, vehículo, documento y disponibilidad |
| Persistencia | `adapter/persistence` | Adaptadores JPA, entidades y repositorios PostgreSQL |
| Integración y seguridad | `adapter/integration`, `adapter/security` | Clientes M1, M4, M6 y JWT/OIDC |
| Configuración | `config` | Cableado de dependencias |

Usamos **Java 21**, Spring Boot, Thymeleaf, Spring Security OAuth2/OIDC,
**PostgreSQL**, JPA/Hibernate y Flyway. Las migraciones viven en
`src/main/resources/db/migration`; M3 usa su propia base `m3_db` y no crea
claves foráneas hacia datos de otros módulos.

## 4. Integración con M1 / Keycloak

M3 quedó configurado para el realm compartido:

```text
Issuer:       http://localhost:8080/realms/ds-2025-realm
JWKS:         http://localhost:8080/realms/ds-2025-realm/protocol/openid-connect/certs
Authorization http://localhost:8080/realms/ds-2025-realm/protocol/openid-connect/auth
Token:        http://localhost:8080/realms/ds-2025-realm/protocol/openid-connect/token
```

El adaptador valida firma, issuer, audiencia, expiración y `sub`. Acepta roles
en `realm_access.roles` y en el claim raíz `roles`, formato del realm
compartido. La web usa Authorization Code con PKCE y callback:

```text
http://localhost:3003/login/oauth2/code/keycloak
```

M1 debe registrar un cliente web de M3 con ese redirect URI y asignar `DRIVER`
para el espacio conductor y `OPERATOR` o `ADMIN` para las rutas de operador.
Las integraciones técnicas necesitan los scopes `m3:eligibility:read` o
`m3:drivers:read`; los scopes de productos, stock, reservas y envíos no
autorizan esos endpoints. Si M1 emite audiencia `account`, dejar
`OIDC_AUDIENCE=account`, valor incluido en `.env.example`.

Con M1 en marcha, `/oauth2/authorization/keycloak` verifica el flujo completo.
Los perfiles `local`, `demo` y `test` usan identidad de desarrollo;
`production` y `docker` la desactivan.

## 5. API y verificación

Endpoints principales:

- `POST /api/v1/drivers/{id}/qualification`
- `POST /api/v1/drivers/{id}/vehicles/{vehicleId}/qualification`
- `POST /api/v1/drivers/{id}/customer-ratings`
- `POST /api/v1/drivers/{id}/availability/retry`

Las mutaciones de conductor requieren `If-Match`; las idempotentes,
`Idempotency-Key` de 16 a 128 caracteres. Swagger local:
`/swagger-ui/index.html`.

```bash
bash mvnw verify
# Incluye migraciones y persistencia contra PostgreSQL:
M3_PG_TEST_URL=jdbc:postgresql://127.0.0.1:55433/postgres bash mvnw verify
```

V4 sólo agrega las tablas de disponibilidad y los índices compatibles con las
tablas compartidas de V1. No usar `flyway repair` ni borrar datos para ocultar
una incompatibilidad de migraciones.
