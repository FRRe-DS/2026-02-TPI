# Consultas reproducibles · métricas y Tempo

Fuente de métricas verificada: `grafanacloud-giantbarracuda2583-prom` (UID `grafanacloud-prom`). Fuente Tempo: `grafanacloud-giantbarracuda2583-traces` (UID `grafanacloud-traces`). Job: `utn-frre/m3-drivers-vehicles`. Los atributos incluyen service_name y deployment_environment_name=local.

Las consultas de ejemplo suponen sólo este entorno. Al agregar otro despliegue, filtrar también por entorno/instancia para no mezclar resultados. El Gauge es global por base: no sumarlo por réplica.

## Gauge y conteos acumulados

```promql
max(m3_drivers_available{job="utn-frre/m3-drivers-vehicles"})
```

```promql
sum(m3_driver_registration_duration_seconds_count{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"})
```

```promql
sum(m3_driver_operational_state_update_duration_seconds_count{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"})
```

Son conteos desde el arranque de las instancias, no solicitudes en una ventana. Pueden reiniciarse al recrear M3. Para una prueba con baseline existente usar increase sobre la ventana y contrastar con el cliente; la extrapolación de Prometheus puede producir valores fraccionarios y no inferir el primer evento de una serie recién creada.

## Duraciones de negocio (segundos)

Promedio de alta sobre ventana reciente:

```promql
sum(rate(m3_driver_registration_duration_seconds_sum{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"}[5m]))
/
sum(rate(m3_driver_registration_duration_seconds_count{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"}[5m]))
```

p95 de alta:

```promql
histogram_quantile(0.95, sum by (le) (rate(m3_driver_registration_duration_seconds_bucket{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"}[5m])))
```

Promedio de actualización local:

```promql
sum(rate(m3_driver_operational_state_update_duration_seconds_sum{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"}[5m]))
/
sum(rate(m3_driver_operational_state_update_duration_seconds_count{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"}[5m]))
```

p95 de actualización local:

```promql
histogram_quantile(0.95, sum by (le) (rate(m3_driver_operational_state_update_duration_seconds_bucket{job="utn-frre/m3-drivers-vehicles",outcome="success",entry_point="http"}[5m])))
```

El JSON del dashboard usa `$__rate_interval` en lugar de [5m]. Para p50/p90/p99 sustituir 0.95 por 0.50/0.90/0.99. No promediar percentiles de réplicas: agregar buckets conservando le. Con tráfico escaso o tasas de count y sum en cero, el promedio no tiene valor y los paneles pueden mostrar sin datos. Verificar primero count/sum acumulados; no convertir esa ausencia a 0 ms.

Promedio acumulado de diagnóstico (no representa la ventana de tiempo seleccionada):

```promql
sum(m3_driver_registration_duration_seconds_sum{job="utn-frre/m3-drivers-vehicles",outcome="success"}) / sum(m3_driver_registration_duration_seconds_count{job="utn-frre/m3-drivers-vehicles",outcome="success"})
```

Para actualización sustituir el prefijo por `m3_driver_operational_state_update_duration_seconds`. Consultar outcome=error por separado; un 409 de alta no es un alta exitosa y un replay de disponibilidad no agrega un cambio nuevo.

## Indicadores técnicos

Throughput HTTP, incluyendo salud y login:

```promql
sum(rate(http_server_request_duration_seconds_count{job="utn-frre/m3-drivers-vehicles"}[5m]))
```

Latencia HTTP p95:

```promql
histogram_quantile(0.95, sum by (le) (rate(http_server_request_duration_seconds_bucket{job="utn-frre/m3-drivers-vehicles"}[5m])))
```

Porcentaje 5xx (sin tráfico no hay porcentaje significativo):

```promql
100 * (sum(rate(http_server_request_duration_seconds_count{job="utn-frre/m3-drivers-vehicles",http_response_status_code=~"5.."}[5m])) or vector(0)) / sum(rate(http_server_request_duration_seconds_count{job="utn-frre/m3-drivers-vehicles"}[5m]))
```

Separar 4xx esperados (401, 409, 422) de fallos inesperados. Bajo k6 filtrar por http_route la operación probada para excluir probes; conservar también el total técnico.

```promql
100 * avg(jvm_cpu_recent_utilization_ratio{job="utn-frre/m3-drivers-vehicles"})
sum(jvm_memory_used_bytes{job="utn-frre/m3-drivers-vehicles"})
```

CPU es promedio de utilización de los procesos JVM; memoria es suma de memoria JVM usada. No equivalen al uso completo del contenedor ni de toda la infraestructura.

## Tempo

En Explore elegir la fuente de trazas, rango de la prueba y TraceQL:

```traceql
{ resource.service.name = "m3-drivers-vehicles" }
```

Para una operación concreta, seleccionar Trace ID y pegar el ID guardado en operations.json. Un ID generado por el cliente no demuestra recepción; la validación requiere abrirlo en Tempo. Inspeccionar estado HTTP, spans SQL/JPA, INSERT/UPDATE y commit en la misma traza. Los workers pueden tener trazas separadas: no afirmar correlación extremo a extremo con M4 sin comprobar propagación.

Trazas verificadas: alta `82476cde9886429493cc5a9c910af601` (201, 18 spans) y disponibilidad `018610a47a63426c81dfff64af3ab9e2` (202, 43 spans). Hora local del 23/11/2026: 20:51:49 y 20:53:10 respectivamente (America/Buenos_Aires). Ampliar a ese intervalo al retomarlas; pueden caducar por retención. Los screenshots locales conservan la evidencia histórica. La duración HTTP de una traza y el histograma de negocio tienen límites distintos y no deben suponerse idénticos.

[Prometheus: funciones](https://prometheus.io/docs/prometheus/latest/querying/functions/). [Tempo: editor](https://grafana.com/docs/grafana/latest/datasources/tempo/query-editor/).
