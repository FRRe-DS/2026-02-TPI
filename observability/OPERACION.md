# Operación y recuperación · Grupo 02

## Requisitos y primer arranque

PowerShell en la raíz `2026-02-TPI`, Docker Desktop con motor Linux y Python 3. El script de operaciones requiere `requests`. No hace falta un JDK local para ejecutar la imagen o las pruebas Docker. Los puertos locales son 3003 (M3), 8080 (Keycloak), 5432 (PostgreSQL) y 9464 (diagnóstico del Collector). No iniciar simultáneamente otro proyecto que use los mismos puertos o nombres.

```powershell
docker compose up -d --build
python observability/setup-local.py
.\observability\Set-GrafanaCredentials.ps1
```

Esperar a que `/health/ready` responda 200 antes de ejecutar setup: la vista depende de Flyway. El script prepara rol/vista en una base existente y genera una clave aleatoria. La configuración privada está en `.env.observability.local` y `.env.grafana.local`, ignorados por Git y excluidos de Docker. No imprimir ni adjuntar esos archivos. El script PowerShell solicita la credencial de Cloud de forma oculta; `-Force` permite reemplazarla explícitamente. No se necesitan variables permanentes de Windows.

| Variable local | Consumidor |
| --- | --- |
| `OTEL_SQL_PASSWORD` | Receptor SQL del Collector. Generada por setup. |
| `GRAFANA_OTLP_ENDPOINT` | Exportador Cloud. Endpoint base del stack. |
| `GRAFANA_OTLP_AUTH` | Material privado de autenticación del exportador; generado por PowerShell. |

Los archivos de ejemplo sólo son plantillas. El script de credenciales incorpora el usuario OTLP del stack; si cambia de stack, revisar el parámetro InstanceId. No utilizar la URL de la interfaz Grafana como endpoint OTLP.

## Levantar y comprobar

Para reducir errores al repetir comandos, definir en cada nueva terminal:

```powershell
$otelCompose = @('--env-file', '.env.observability.local', '--env-file', '.env.grafana.local', '-f', 'compose.yaml', '-f', 'compose.observability.yaml', '-f', 'compose.grafana.yaml')
docker compose @otelCompose up -d --build
docker compose @otelCompose ps
docker compose @otelCompose logs --tail 60 otel-collector
curl.exe -i http://localhost:3003/health/live
curl.exe -i http://localhost:3003/health/ready
curl.exe -i http://localhost:3003/api/v1/drivers
```

Salud: 200. API sin token: 401. Para validar los archivos antes del arranque:

```powershell
docker compose @otelCompose run --rm --no-deps otel-collector validate --config=/etc/otelcol-contrib/config.yaml --config=/etc/otelcol-contrib/sql.yaml --config=/etc/otelcol-contrib/grafana.yaml
```

No ejecutar la impresión completa de la configuración interpolada al compartir evidencias. Los logs también deben revisarse antes de publicarlos. El endpoint local 9464 demuestra recepción en el Collector, no ingesta en Cloud; comprobar Explore y Tempo.

Para diagnóstico sin Cloud, usar sólo `compose.yaml` y `compose.observability.yaml`, con `--env-file .env.observability.local`. Ese modo activa salida debug detallada; evitarlo durante carga. No mezclar ambos modos sin revisar qué archivos Compose se están pasando.

## Keycloak y dependencias

Realm local `ds-2025-realm`, cliente público `m3-web-client`, PKCE, método de cliente sin secreto. El issuer público es estable en `http://localhost:8080`; token, certificados y userinfo usan `keycloak:8080` desde M3. El mapper local agrega la audiencia `account` al token de acceso.

Se corrigieron el client-id vacío de Compose, direcciones internas incorrectas, inconsistencia del issuer y ausencia de audiencia. La dependencia service_started no comprueba que Keycloak ya esté listo: esperar su arranque antes de autenticar. La robustez del validador frente a tokens sin audiencia sigue como hallazgo de Desarrollo (puede devolver 500); no se cambió su lógica.

Las URL locales de M4/M6 deben adaptarse al entorno acordado. Dentro de un contenedor, localhost apunta a ese contenedor. Validar rutas y credenciales técnicas con los equipos responsables antes del escenario positivo de disponibilidad.

## Operaciones sintéticas

```powershell
python observability/smoke-business.py --account m3-alta-prueba-2
python observability/smoke-business.py --account m3-demo --registration-only
```

Sólo admite cuentas sintéticas declaradas en el realm. El primer comando guarda tres solicitudes distintas de desconexión, repite una clave y exige rechazo 422 al intentar conectar un perfil pendiente. Puede crear un vehículo pendiente si falta. Conserva datos de prueba; no aprueba documentación ni simula M4. Las pausas de exportación no están incluidas en las duraciones de M3.

El segundo sólo intenta el alta. **Las cuentas de prueba usadas ya tienen perfil**: repetir da 409 y no crea una nueva observación exitosa. Para otra alta:

1. Crear en Keycloak local una identidad ficticia nueva (p. ej. `tpi-otel-alta-<fecha>-01`), rol DRIVER y contraseña de prueba no temporal, sin permisos administrativos.
2. En ventana privada abrir `http://localhost:3003/oauth2/authorization/keycloak`, iniciar sesión y entrar a `/ui/register`.
3. Registrar un nombre ficticio. El perfil debe quedar PENDING. Esperar 60-90 s y consultar el contador de éxito.

No borrar perfiles ni alterar identidades para aumentar el conteo. Guardar la hora y el Trace ID; el script escribe snapshots y resultados sin secretos en `.local/evidence-<fecha-hora>/`.

Para una transición positiva: preparar conductor, vehículo y documentación aprobados mediante los flujos normales y coordinar M4. En `/ui/availability` probar conectado/disponible, conectado/no disponible y desconectado/no disponible con claves nuevas. Un 202 sólo confirma registro local. Verificar estado APPLIED y versiones iguales antes de interpretar el Gauge. No enviar disponibilidad de cuentas reales ni crear viajes para esta comprobación.

## Detener y retomar

Con la misma variable `$otelCompose`:

```powershell
docker compose @otelCompose stop
docker compose @otelCompose start
```

Esto conserva los contenedores, PostgreSQL, el estado local de Keycloak y la configuración. Tras reiniciar esperar salud, realizar login y comprobar una métrica/traza nueva. Si cambian archivos o imagen, usar `up -d --build` en lugar de start. No es necesario detener nada ahora para cerrar la documentación.

Para recrear sólo el Collector después de cambiar su configuración:

```powershell
docker compose @otelCompose up -d --force-recreate otel-collector
```

La cola en memoria se pierde al reiniciarlo. No usar la eliminación de volúmenes para una parada normal. Un down sin eliminar volúmenes conserva PostgreSQL, pero elimina los contenedores; Keycloak no tiene volumen de datos y perderá cuentas creadas manualmente, reimportando sólo las del JSON. Los perfiles en PostgreSQL pueden entonces quedar sin su identidad local. Preferir stop/start y respaldar las identidades de prueba antes de recrear Keycloak.

Antes de pasar al Grupo 15, revisar sus puertos y nombres de contenedores; detener el Grupo 02 si hay conflicto. Mantener su carpeta y credenciales separadas. El pendiente con M4 no impide iniciar el diagnóstico del Grupo 15.
