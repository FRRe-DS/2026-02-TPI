# Observabilidad de M3 · Grupo 02 · Equipo 2

Cierre provisional: **23/11/2026**, UTN FRRe. Rama `devops/observabilidad` del repositorio `FRRe-DS/2026-02-TPI`, basada en `main` verificado en `e3104f2` al comenzar. Implementación revisada para su commit local; sin push ni PR.

**Estado:** métricas y trazas recibidas y verificadas en Grafana Cloud; dashboard importado. Se observaron 2 altas exitosas acumuladas, 6 actualizaciones locales exitosas acumuladas y Gauge 0. No se validó disponibilidad efectiva ni un escenario positivo sincronizado con M4. No se ejecutó k6 ni se desplegó M3 en cloud.

## Guías para retomar

- [Operación y reinicio del entorno](OPERACION.md): Docker, configuración privada, Keycloak y operaciones sintéticas.
- [Consultas de métricas y trazas](CONSULTAS.md): PromQL del dashboard, interpretación y Tempo.
- [Evidencias y resultados](EVIDENCIAS.md): qué se comprobó, referencias locales y límites.
- [Pendientes para k6](K6-PENDIENTES.md): escenarios, controles y criterios de comparación.

## Arquitectura

```text
Navegador / cliente de prueba
          │ HTTP
          ▼
M3: Spring Boot 3.3.4 / Java 21
 ├─ Java Agent: spans HTTP, JDBC y métricas HTTP/JVM
 ├─ RequestTimingFilter: instante de recepción HTTP
 └─ DriverMetricsAspect: histogramas alrededor de casos de uso y commit
          │ OTLP HTTP/protobuf, red interna Docker, puerto 4318
          ▼
Collector Contrib
 ├─ OTLP → memory_limiter → batch → exportador de Cloud
 └─ SQL Query Receiver (60 s) → vista agregada de PostgreSQL
          │ HTTPS OTLP HTTP/protobuf
          ▼
Grafana Cloud: Mimir (métricas), Tempo (trazas), dashboard
```

M3 es dueño de PostgreSQL. El Collector se conecta con `m3_otel`, que sólo puede leer la vista `observability.available_drivers`; no puede consultar perfiles ni documentos individuales. El SQL vive fuera de Flyway y se prepara con `setup-local.py` después de las migraciones.

La instrumentación manual está en `adapter/observability`; no se cambiaron reglas de dominio ni casos de uso. Se añadieron API OpenTelemetry y AOP; el SDK de prueba sólo tiene alcance test. En ejecución el Java Agent administra el SDK y la exportación. Sin agente, las llamadas de la API son no-op.

## Componentes y configuración

| Componente | Configuración |
| --- | --- |
| Java Agent | 2.34.0, incluido en Dockerfile; SHA-256 comprobado al construir; legible por el usuario no privilegiado de M3. |
| API OpenTelemetry | 1.44.1, versiones alineadas en Maven. |
| Collector | Imagen Contrib 0.140.0 fijada; el binario se identifica como 0.140.1. SQL Query Receiver con métricas alpha: volver a validar al actualizar. |
| Transporte M3 → Collector | `http://otel-collector:4318`, HTTP/protobuf. OTLP no se publica al host. |
| Métricas Java | Exportación cada 30 s. `service.name=m3-drivers-vehicles`, namespace `utn-frre`, entorno `local`. |
| Muestreo | `parentbased_always_on`, diagnóstico local; revisar para carga. |
| Procesamiento | Límite de memoria de 128 MiB, margen 32 MiB, comprobación 1 s, batch 5 s. No equivale a un límite Docker de memoria. |
| Cloud | `collector-grafana.yaml`: TLS, gzip, timeout 15 s, reintentos hasta 300 s y cola en memoria de 1000 solicitudes. La cola no persiste al reiniciar. |
| Salida local | Prometheus en `127.0.0.1:9464/metrics`. El modo local usa debug detallado; el overlay Cloud lo reemplaza por OTLP. |
| Logs | No se exportan por OTLP; se consultan con Docker. |

Stack: [Grafana del Equipo 2](https://giantbarracuda2583.grafana.net).
Endpoint base OTLP: `https://otlp-gateway-prod-sa-east-1.grafana.net/otlp`.
El exportador agrega `/v1/metrics` y `/v1/traces`. La credencial sólo se configura localmente; nunca en este documento ni en la imagen.

## Las tres métricas de la bitácora

Se conservan las métricas seleccionadas con Desarrollo; los indicadores HTTP/JVM son adicionales.

| Definición de negocio | Nombre OTLP / nombre Prometheus | Tipo y unidad | Fuente y utilidad |
| --- | --- | --- | --- |
| Cantidad de conductores disponibles y habilitados | `m3.drivers.available` / `m3_drivers_available` | Gauge, `{driver}` | Consulta SQL agregada cada 60 s. Permite observar disminuciones del conjunto elegible. |
| Tiempo para dar de alta de conductor | `m3.driver.registration.duration` / `m3_driver_registration_duration_seconds` | Histograma, `s` | `CreateDriverProfileUseCase.execute`: recepción HTTP hasta retorno posterior al commit. Permite detectar demoras en registro y base de datos. |
| Tiempo de actualización del estado operativo del conductor | `m3.driver.operational_state.update.duration` / `m3_driver_operational_state_update_duration_seconds` | Histograma, `s` | `RequestAvailabilityService.execute`: recepción HTTP hasta registro local de intención, auditoría, outbox e idempotencia. Ayuda a detectar demoras locales en el registro de estados. |

### Condiciones y límites del Gauge

La vista exige simultáneamente:

- Conductor APPROVED y vehículo propio APPROVED seleccionado por la intención.
- `connected=true`, `available=true`, estado APPLIED y versiones aplicada/deseada iguales.
- Licencia, seguro y cédula actuales, aprobados y vigentes; licencia compatible con el tipo de servicio.

La aprobación histórica no basta si venció la documentación. El resultado es global por base; usar un recolector SQL por base y no sumarlo por réplica de M3. El rol tiene lectura por defecto, una conexión y timeout de consulta de 5 s.

**0 es un valor válido**, no una prueba de caída de M4. PENDING, RETRYING y FAILED quedan excluidos; al iniciar la última validación había dos intenciones FAILED. Un error de consulta no se convierte en cero: comprobar logs, frescura del dato y estados. La vista no consulta M4 ni excluye por sí sola los viajes guardados en memoria en M3: APPLIED confirma la intención sincronizada, no disponibilidad efectiva posterior durante un viaje. La validación positiva completa sigue pendiente.

### Condiciones y límites de las duraciones

El filtro captura la recepción antes de seguridad y validación. Sólo se registra el histograma si se llega a invocar el caso de uso; rechazos anteriores permanecen en métricas HTTP técnicas. AOP envuelve el interceptor transaccional del alta; las pruebas verifican que un commit fallido nunca quede como éxito. La disponibilidad usa transacciones internas que terminan antes de retornar.

Etiquetas de baja cardinalidad: `outcome=success|error`, `entry_point=http|usecase`. Las llamadas fuera de HTTP se miden desde el caso de uso. Los replays idempotentes de disponibilidad no generan otra observación; los errores quedan separados de los éxitos. No se usan nombres ni IDs de conductores como etiquetas. Los límites de buckets solicitados van de 1 ms a 10 s; comprobar los límites exportados, porque la API los expresa como una recomendación.

La actualización termina en persistencia **local**: no incluye entrega asíncrona, reintentos de M4 ni transiciones de viajes en memoria. HTTP 202 no equivale a APPLIED. Es una cobertura parcial de la sincronización operativa, documentada para coordinar con Desarrollo; no se reemplaza por otra métrica.

## Indicadores técnicos

`http.server.request.duration` aporta conteo, duración y estados HTTP. Las métricas JVM incluyen CPU, memoria, GC y threads. Son recursos del proceso/JVM, no del host, PostgreSQL, Keycloak ni del contenedor completo. Mantener esa distinción al comparar k6 con límites de infraestructura.

## Referencias

- [Consigna TPI](https://docs.google.com/document/d/1mScU5ACgixtbmfZBUc9zhQpp0omj73Q8TBXD0pNfNaA/edit).
- [Bitácora del Equipo 2](https://docs.google.com/document/d/1rGEXLKnms0JMCpG0F0XfXxd67Kti6LvWIBb2OxtX5o0/edit), sección Métricas del módulo.
- [Java Agent](https://opentelemetry.io/docs/zero-code/java/agent/).
- [SQL Query Receiver de la versión fijada](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/v0.140.0/receiver/sqlqueryreceiver).
- [OTLP de Grafana Cloud](https://grafana.com/docs/grafana-cloud/send-data/otlp/send-data-otlp/).
