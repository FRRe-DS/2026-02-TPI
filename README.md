# M3 — Conductores y Vehículos

Repositorio del módulo M3 del TPI Desarrollo de Software 2026.

## Stack
- Java 21
- Spring Boot 3.3.4
- PostgreSQL 16 / Flyway
- Thymeleaf / Bootstrap 5
- Docker & Docker Compose

## Vista previa de todas las pantallas web

Con la aplicación en ejecución, abrí `http://localhost:3003/ui`. La navegación incluye
inicio, perfil, vehículos, documentación, habilitación, disponibilidad,
valoraciones E2, listado de operadores y detalle de conductor. Las rutas están
implementadas con Spring MVC y Thymeleaf; CSS y JavaScript se sirven localmente.

La interfaz de estas rutas es una **vista previa con datos de ejemplo**, indicados
en cada pantalla. Los formularios sin caso de uso integrado están deshabilitados:
no simulan guardados ni decisiones reales. La pantalla existente para registrar
licencias de un conductor real continúa en `/ui/drivers/{driverId}/documents`.
El perfil de prueba puede ejecutarse con
`sh mvnw -Dspring-boot.run.profiles=test -Dspring-boot.run.arguments=--server.port=3003 spring-boot:run`;
usa H2 efímera y no requiere PostgreSQL para recorrer la vista previa.

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
