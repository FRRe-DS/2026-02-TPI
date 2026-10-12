# M3 — Conductores y Vehículos

Repositorio del módulo M3 del TPI Desarrollo de Software 2026.

## Stack
- Java 21
- Spring Boot 3.3.4
- PostgreSQL 16 / Flyway
- Thymeleaf / Bootstrap 5
- Docker & Docker Compose

## Frontend integrado con persistencia

La integración parte de `main` (`b804907`, PR #9). El formulario de HU-03
incorporado desde `HU3-Aplicacion` ahora utiliza los casos de uso y PostgreSQL
de M3: guarda marca, modelo, año, patente, color, capacidad y categorías de
servicio. `AUTO`/`MOTO` siguen representando el tipo habilitado por la licencia;
`STANDARD`, `PREMIUM`, `DELIVERY` y `PETS` son categorías comerciales separadas.
La aplicación independiente en `HU3-Aplicacion` conserva su versión original;
para usar el flujo integrado, iniciar el proyecto raíz y abrir `/ui`.

- `/ui/register`: crear el perfil de la identidad de la sesión.
- `/ui/profile`: consultar el perfil guardado.
- `/ui/vehicles`: consultar vehículos y acceder al formulario nuevo de HU-03.
- `/ui/documents`: gestionar licencias, seguros, cédulas e historiales.
- `/ui/qualification`: consultar la aptitud calculada por el backend.
- `/ui/availability`: guardar solicitudes y ver su estado persistido.
- `/ui/operator/drivers`: consultar conductores y revisar documentación.

Los formularios web tienen CSRF. El alta del vehículo incrementa la versión del
conductor y rechaza versiones desactualizadas. El alta queda `PENDING`.
Las revisiones de documentos exigen un motivo y el rol de operador.
Los recursos CSS, iconos y JavaScript se sirven localmente.

### Iniciar en Linux con PostgreSQL instalado

Requiere Java 21, `initdb`, `pg_ctl`, `psql` y `createdb`:

```bash
# JAVA_HOME debe apuntar a un JDK 21.
bash scripts/dev.sh
```

Abrir `http://localhost:3003/ui`. El script crea una base independiente en
`.local/postgres`, puerto `55434`, que conserva los datos entre ejecuciones.
Si se configura `SPRING_DATASOURCE_URL`, usa esa conexión sin crear otro clúster.
La identidad local predeterminada es `user-sub-123`; si no tiene perfil, usar
**Crear mi perfil**. La autenticación real con M1 todavía requiere completar
la configuración y el endurecimiento de los adaptadores heredados.

Para detener el PostgreSQL creado por el script:
`pg_ctl -D "$PWD/.local/postgres" stop`.

### Demostración opcional sin PostgreSQL

```bash
bash mvnw -Dspring-boot.run.profiles=demo spring-boot:run
```

Usa H2 persistente en `data/` y carga cuentas sintéticas identificadas como
prueba. El perfil `local` no genera esas cuentas; la migración heredada V5
mantiene un conductor/vehículo demo independiente de la identidad local.
Las antiguas pantallas estáticas quedan en `/ui/preview`.

### Alcance pendiente

La solicitud de disponibilidad se guarda junto con auditoría y outbox, pero el
repositorio aún no incorpora el worker que la entrega a M4. La UI muestra
`PENDING`, sin afirmar disponibilidad efectiva. Las valoraciones E2 y la
aprobación administrativa de perfiles/vehículos aún no tienen un flujo completo
integrado; la revisión disponible es la de documentos.

### Migraciones

Se corrigió V4, que no podía ejecutarse sobre V1 por índices que referían a
columnas inexistentes de auditoría e idempotencia. V6 agrega los campos de HU-03.
La cadena V1–V6 está validada sobre una base nueva. Una base anterior que haya
aplicado otra versión de V1/V2 necesita un plan de migración específico:
no ejecutar `flyway repair` ni borrar sus datos para eludir los checksums.
El script local crea una base separada para evitar modificar esas bases.

## HU-05: documentación del conductor

La licencia se registra en `POST /api/v1/drivers/{driverId}/documents` o desde
`/ui/drivers/{driverId}/documents`. El conductor debe existir previamente en `drivers`.
La pantalla muestra todas las revisiones y el estado de revisión. Una renovación
marca la anterior como no actual, conserva su fila y deja la nueva pendiente.

La API recibe los metadatos `documentNumber`, `issuedAt`, `validUntil` y
`serviceTypes` (`AUTO` y/o `MOTO`). `storageReference` es opcional. Las fechas de
la API son instantes ISO 8601; `validUntil` es exclusivo. La web usa fechas de
Argentina y trata el día de vencimiento ingresado como inclusivo.

Para registrar por API, enviar `If-Match` con la versión actual del conductor
(disponible como `ETag` en el GET de documentos) y una `Idempotency-Key` única de
16 a 128 caracteres. Reenviar la misma petición con la misma clave devuelve la
revisión original. El GET devuelve `items` y `nextCursor`; acepta `cursor` y
`limit` (1 a 100). La respuesta pública omite el número de licencia.

## HU-09: consultar aptitud para trabajar (CLON-14)

Permite consultar si un conductor está habilitado para tomar viajes (elegible), evaluando las siguientes reglas de negocio del dominio:
1. **Calificación del conductor:** Debe estar en estado `APPROVED`.
2. **Vehículo habilitado:** Debe poseer al menos un vehículo en estado `APPROVED`.
3. **Licencia de conducir vigente:** Debe tener una licencia (`DRIVER_LICENSE`) en estado `APPROVED`, vigente en la fecha actual (`validUntil > now`).
4. **Coincidencia de tipos de servicio:** El tipo de servicio habilitado en el vehículo (`AUTO`, `MOTO`) debe coincidir con los tipos de servicio autorizados en su licencia.
5. **Cálculo de vigencia:** El resultado es elegible hasta la fecha de expiración de la licencia (o 24 horas si no vence antes). Si no es elegible, incluye la lista detallada de motivos (`reasons`).

### Endpoints REST (Integración con otros módulos)

- `GET /api/v1/integration/drivers/{driverId}/eligibility`
  - Consulta la aptitud de un conductor por su UUID.
  - Parámetros opcionales: `?serviceType=AUTO` (para consultar aptitud en un servicio específico).
  - Devuelve JSON:
    ```json
    {
      "driverId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
      "eligible": true,
      "reasons": [],
      "validUntil": "2027-10-11T12:00:00Z",
      "checkedAt": "2026-10-11T20:53:00Z"
    }
    ```
- `POST /api/v1/integration/eligibility/batch`
  - Consulta en lote para un conjunto de conductores (hasta 100).
  - Body:
    ```json
    {
      "driverIds": ["a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11", "b1ffcd88-8b1a-4fe7-aa5c-5aa8ac270b22"],
      "serviceType": "AUTO"
    }
    ```

### Interfaz Web (UI para el conductor)

- `GET /ui/qualification`
  - Vista Thymeleaf donde el conductor autenticado puede consultar su estado de aptitud en tiempo real.
  - Si es apto, muestra confirmación verde y enlace para publicar disponibilidad.
  - Si no es apto, muestra el listado de razones específicas a subsanar (ej. vehículo no aprobado, licencia vencida).
  - Si no tiene perfil registrado, muestra aviso para completarlo.

## Ejecución y Tests

### Ejecutar tests (en contenedor Docker con Java 21)
```bash
docker run --rm -v "$(pwd)":/app -w /app eclipse-temurin:21-jdk-alpine ./mvnw test
```

### Ejecutar localmente con Docker Compose
```bash
# Levantar base de datos PostgreSQL
docker compose up -d postgres

# Ejecutar la aplicación (Spring Boot 3.3.4)
./mvnw spring-boot:run
```
La aplicación inicia en `http://localhost:3003`.
