# Retomar durante k6 y la entrega final

**No se ejecutaron pruebas de carga.** Las 2 altas y 6 actualizaciones observadas son validación funcional de telemetría; no acreditan capacidad, estabilidad ni percentiles representativos.

## Antes de generar carga

- Acordar escenarios, duración, calentamiento, rampas, datos, criterios de aceptación y entorno cloud autorizado con Desarrollo. La consigna solicita 50, 100, 500 y 1000 VUs; registrar restricciones reales sin exigir un umbral de rendimiento inventado.
- Preparar identidades sintéticas únicas para altas y vehículos/documentación de prueba para disponibilidad. Repetir la misma identidad produce 409 y mide rechazo, no alta. La cuenta administrativa y los secretos no deben aparecer en scripts/reportes.
- Separar autenticación/preparación del flujo de negocio, o medirlas como escenario explícito. No usar el script smoke como generador de carga: sus pausas son para exportación y su conjunto de cuentas es pequeño.
- Para actualizaciones usar claves idempotentes distintas por operación nueva. Reservar un caso separado para replay y comprobar que no duplica el histograma. Controlar conflictos sobre el mismo conductor y su versionado/bloqueo; no interpretar esa contención como capacidad independiente por usuario.
- Revisar límites y recursos de M3, PostgreSQL, Keycloak, Collector y generador k6, red y cuotas del plan Cloud. CPU/memoria JVM no sustituyen mediciones de host/contenedor. Registrar recursos, versiones, commit y localización del generador.
- Revisar muestreo completo, volumen de spans JDBC y de workers/simulador y cola del Collector. Usar overlay Cloud sin debug detallado. Cualquier cambio de muestreo/worker/simulador debe quedar fijado y documentado para comparar escenarios.
- Validar baseline y exportación antes de cada ejecución. Guardar instante inicial/final, zona horaria y ventana Grafana. No vaciar la base ni reiniciar contadores para obtener un resultado visual conveniente.

## Qué comprobar por nivel de carga

| Dimensión | Evidencia pendiente |
| --- | --- |
| Tres métricas de negocio | Conteo/duración de altas, duración de actualizaciones e idempotencia; evolución del Gauge y frescura de la consulta SQL. |
| Latencia | Promedio y p50/p90/p95/p99 de k6 y de los histogramas; aclarar límites de cada medición y buckets. |
| Errores | HTTP 4xx esperados vs inesperados, 5xx, timeouts, fallos de commit, resultados de checks y operaciones exitosas reales. |
| Throughput | Solicitudes/s totales y por ruta, operaciones completadas/s y concurrencia efectiva. Excluir o identificar salud/login/ruido de fondo. |
| Recursos | CPU, memoria, GC, threads, conexiones a PostgreSQL y saturación de componentes. Relacionar síntomas con trazas. |
| Canal de telemetría | Exportaciones fallidas, reintentos, colas, pérdida de muestras o spans y carga inducida por la observabilidad. |

No promediar p95 de instancias ni confundir el p95 estimado por buckets con la duración exacta de una operación. Comparar ventanas equivalentes y población de éxitos. Para conteos confrontar deltas, resultados de k6 y resets del proceso; un primer evento puede no quedar reflejado en rate/increase por falta de baseline.

## Dependencia con M4

La persistencia local puede medirse con 202 aun sin confirmación remota. El Gauge excluye PENDING, RETRYING y FAILED. Para demostrar disponibilidad positiva se necesitan documentación y habilitación válidas, conectividad/credenciales técnicas y confirmación APPLIED de la versión actual por M4. La ocupación durante viajes requiere una fuente de estado efectivo acordada; la intención aplicada no alcanza por sí sola.

Coordinar una prueba habilitada, distinguir tiempo local de sincronización y verificar el comportamiento al perder/restablecer M4. No forzar APPLIED mediante SQL ni sustituir producción por una simulación sin declararlo. Si se usa un doble de prueba, etiquetar el escenario como simulado y no presentarlo como integración real.

**Este pendiente no impide continuar con el Grupo 15.** Mantenerlo en el backlog del Grupo 02 y contrastar las mismas tres definiciones con el código propio del Grupo 15; no copiar condiciones Java ni inventar disponibilidad confirmada.

## Entrega final

Conservar por escenario: commit, configuración no sensible, recursos, datasets sintéticos, scripts y resultados k6, tablas comparativas 50/100/500/1000 VUs, consultas, capturas y enlaces de Grafana, Trace IDs y explicación de límites/cuello de botella. Revisar retención de Cloud y respaldar las evidencias antes de que caduquen.

Actualizar bitácora con interpretación de resultados, restricciones de infraestructura, acuerdos con M4 y mejoras fundamentadas. Quedan pendientes el commit revisado de esta rama, posterior autorización para push/PR, integración por Desarrollo y evidencia cloud de las futuras cargas. No cambiar los workflows GHCR para cerrar esta etapa documental.
