# M3 — Conductores y Vehículos

Repositorio del módulo M3 del TPI Desarrollo de Software 2026.

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

La persistencia usa PostgreSQL y las migraciones Flyway `V1` y `V2`. Para
ejecutar localmente: iniciar PostgreSQL con `docker compose up -d postgres` y
luego la aplicación con `sh mvnw spring-boot:run`. El perfil local usa los datos
de conexión de `compose.yaml`.

La integración de autenticación con M1 todavía no está implementada en este
repositorio; las rutas existentes y las nuevas aún no restringen identidad o
propiedad del conductor. No publicar el servicio en una red abierta hasta
incorporar esa integración.
