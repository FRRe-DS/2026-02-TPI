# Evidencias y resultados · 23/11/2026

## Validado directamente

| Evidencia | Resultado y alcance |
| --- | --- |
| Explore/Mimir | 2 observaciones acumuladas de altas exitosas y 6 de actualizaciones locales exitosas; no son totales garantizados para otro arranque. |
| Dashboard importado | Gauge 0 y datos de promedio/p95 para alta y actualización; indicadores HTTP y JVM recibidos. |
| Gauge | 0 según condiciones SQL; no se comprobó un conductor disponible efectivamente ni un valor positivo confirmado por M4. |
| Alta sintética | HTTP 201; traza con 18 spans, SELECT/INSERT en drivers y commit. |
| Disponibilidad sintética | HTTP 202; traza con 43 spans, consultas, intención, auditoría, outbox, idempotencia, UPDATE y commit. |
| Idempotencia y rechazo | En la segunda tanda: incremento de 3 éxitos, no 4, pese al replay; una conexión no habilitada produjo 422 y una observación de error. |
| Datos | Cuentas sintéticas declaradas; se conservaron perfiles/vehículos pendientes. No se cambiaron reglas de negocio ni se habilitó un conductor artificialmente. |

La verificación se realizó en la sesión del stack del Equipo 2. Los IDs enviados desde el cliente se buscaron y abrieron en Tempo; no se infirió recepción a partir del envío. Las duraciones HTTP observadas en esas trazas fueron 66.84 ms (alta) y 81.1 ms (actualización); no son el promedio/p95 del histograma ni resultados de carga.

## Enlaces históricos

Los enlaces siguientes fijan el intervalo del 23/11/2026, 20:35-21:00 de Buenos Aires, para evitar que un enlace relativo a la última hora quede vacío al retomar. Requieren sesión en el stack y están sujetos a su retención.

- [Dashboard del intervalo validado](https://giantbarracuda2583.grafana.net/d/m3-grupo02-observabilidad?from=1795476900000&to=1795478400000&timezone=browser).
- [Contadores de éxito en Explore](https://giantbarracuda2583.grafana.net/explore?schemaVersion=1&panes=%7B%22x7d%22%3A%7B%22datasource%22%3A%22grafanacloud-prom%22%2C%22queries%22%3A%5B%7B%22refId%22%3A%22A%22%2C%22expr%22%3A%22%7B__name__%3D~%5C%22m3_driver_.%2A_count%5C%22%2Coutcome%3D%5C%22success%5C%22%2Cjob%3D%5C%22utn-frre%2Fm3-drivers-vehicles%5C%22%7D%22%2C%22range%22%3Atrue%2C%22instant%22%3Atrue%2C%22datasource%22%3A%7B%22type%22%3A%22prometheus%22%2C%22uid%22%3A%22grafanacloud-prom%22%7D%2C%22editorMode%22%3A%22code%22%2C%22legendFormat%22%3A%22__auto%22%7D%5D%2C%22range%22%3A%7B%22from%22%3A%221795476900000%22%2C%22to%22%3A%221795478400000%22%7D%2C%22compact%22%3Afalse%7D%7D).
- [Traza 201 · 82476cde9886429493cc5a9c910af601](https://giantbarracuda2583.grafana.net/explore?schemaVersion=1&panes=%7B%22x7d%22%3A%7B%22datasource%22%3A%22grafanacloud-traces%22%2C%22queries%22%3A%5B%7B%22refId%22%3A%22A%22%2C%22datasource%22%3A%7B%22type%22%3A%22tempo%22%2C%22uid%22%3A%22grafanacloud-traces%22%7D%2C%22queryType%22%3A%22traceId%22%2C%22serviceMapUseNativeHistograms%22%3Afalse%2C%22query%22%3A%2282476cde9886429493cc5a9c910af601%22%7D%5D%2C%22range%22%3A%7B%22from%22%3A%221795476900000%22%2C%22to%22%3A%221795478400000%22%7D%2C%22compact%22%3Afalse%7D%7D).
- [Traza 202 · 018610a47a63426c81dfff64af3ab9e2](https://giantbarracuda2583.grafana.net/explore?schemaVersion=1&panes=%7B%22x7d%22%3A%7B%22datasource%22%3A%22grafanacloud-traces%22%2C%22queries%22%3A%5B%7B%22refId%22%3A%22A%22%2C%22datasource%22%3A%7B%22type%22%3A%22tempo%22%2C%22uid%22%3A%22grafanacloud-traces%22%7D%2C%22queryType%22%3A%22traceId%22%2C%22serviceMapUseNativeHistograms%22%3Afalse%2C%22query%22%3A%22018610a47a63426c81dfff64af3ab9e2%22%7D%5D%2C%22range%22%3A%7B%22from%22%3A%221795476900000%22%2C%22to%22%3A%221795478400000%22%7D%2C%22compact%22%3Afalse%7D%7D).

## Archivos guardados localmente

Las rutas parten de la raíz de `2026-02-TPI`. `.local/` está ignorado y no se incluye en el commit. [evidence-manifest.json](evidence-manifest.json) inventaría tamaño y SHA-256 para comprobar las copias; no incorpora secretos ni los archivos privados de entorno.

| Carpeta | Contenido |
| --- | --- |
| `.local/evidence-20261123-204157/` | `metrics-before.prom`, `metrics-after.prom`, `operations.json`, `validation-summary.json`, `durations.json`: primera tanda funcional, una alta y tres actualizaciones. |
| `.local/evidence-20261123-205147/` | Snapshots antes/después, `operations.json`, `cloud-verification.json`, `tempo-alta.jpg`, `cloud-histograms.jpg`, `dashboard-cloud.jpg`: segunda alta y comprobación directa de Cloud. |
| `.local/evidence-20261123-205307/` | Snapshots, `operations.json`, `counter-validation.json`, `tempo-disponibilidad.jpg`: otra tanda de disponibilidad y traza 202. |
| `.local/closure-20261123/` | `build.log`, `tests.log`, `runtime-checks.json`, `security-audit.json`, `static-checks.json`, `git-status.txt`: controles de cierre. |

- [Captura del dashboard](../.local/evidence-20261123-205147/dashboard-cloud.jpg).
- [Captura de contadores en Cloud](../.local/evidence-20261123-205147/cloud-histograms.jpg).
- [Captura de alta en Tempo](../.local/evidence-20261123-205147/tempo-alta.jpg).
- [Captura de actualización en Tempo](../.local/evidence-20261123-205307/tempo-disponibilidad.jpg).

Estas capturas y snapshots están en esta máquina, no en un clon nuevo. Antes de la entrega final respaldar sólo las evidencias seleccionadas en la carpeta compartida del equipo, sin archivos privados de entorno ni logs sin revisar. El manifiesto no sustituye los archivos ni la retención de Cloud.

## Pendiente y límites

No se validaron: disponibilidad efectiva durante viajes; escenario positivo habilitado y APPLIED de la versión actual con M4; propagación/correlación asíncrona completa a M4; cargas k6, percentiles estadísticamente representativos, capacidad, saturación o resiliencia; despliegue cloud de M3 para la carga; exportación de logs a Loki.

Al iniciar la validación había dos intenciones FAILED; no se atribuyó la causa sólo al Gauge. Se observaron fallos DNS intermitentes del exportador durante la sesión y luego conectividad operativa e ingesta real: vigilar el canal durante k6. El validador JWT existente puede responder 500 ante audiencia ausente; se configuró el realm local para emitir la audiencia esperada y se dejó ese endurecimiento como pendiente de Desarrollo.

La integración con M4 debe coordinarse, pero no impide continuar con el Grupo 15. Los valores 2/6/0 y capturas son evidencias históricas de esta sesión, no una garantía de que el estado no cambió después.
