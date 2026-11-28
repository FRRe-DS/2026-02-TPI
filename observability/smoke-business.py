"""Operaciones reales de prueba locales y evidencia sin tokens. Requiere requests.

Usa sólo las cuentas sintéticas del realm local. Conserva los perfiles y vehículos
creados. No aprueba documentos ni simula confirmaciones APPLIED de M4.
"""
from pathlib import Path
from datetime import datetime, timezone, timedelta
import base64
import html
import json
import re
import time
import uuid
import requests
import argparse

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--account', choices=['m3-alta-prueba', 'm3-alta-prueba-2', 'm3-demo'], default='m3-alta-prueba')
parser.add_argument('--registration-only', action='store_true', help='Sólo alta y snapshot; no registra vehículos ni disponibilidad.')
args = parser.parse_args()

ROOT = Path(__file__).resolve().parent.parent
BASE = 'http://localhost:3003'
KC = 'http://localhost:8080/realms/ds-2025-realm'
started = datetime.now(timezone(timedelta(hours=-3)))
output = ROOT / '.local' / ('evidence-' + started.strftime('%Y%m%d-%H%M%S'))
output.mkdir(parents=True, exist_ok=False)
events = []

def snapshot(name):
    response = requests.get('http://localhost:9464/metrics', timeout=10)
    response.raise_for_status()
    lines = [line for line in response.text.splitlines()
             if line.startswith(('m3_', '# HELP m3_', '# TYPE m3_'))]
    (output / name).write_text('\n'.join(lines) + '\n', encoding='utf-8')
    return '\n'.join(lines)

def operation(session, method, path, **kwargs):
    trace_id = uuid.uuid4().hex
    headers = dict(kwargs.pop('headers', {}))
    headers['traceparent'] = '00-' + trace_id + '-' + uuid.uuid4().hex[:16] + '-01'
    response = session.request(method, BASE + path, headers=headers, timeout=20, **kwargs)
    events.append({'operation': method + ' ' + re.sub(r'/[0-9a-f-]{36}', '/{id}', path),
                   'status': response.status_code, 'trace_id': trace_id,
                   'time': datetime.now(timezone.utc).isoformat()})
    return response

def csrf(page):
    for tag in re.findall(r'<input\b[^>]*>', page):
        if 'name="_csrf"' in tag:
            return html.unescape(re.search(r'value="([^"]+)', tag).group(1))
    raise RuntimeError('No se encontró el token CSRF del formulario local.')

def browser_session(user, password):
    session = requests.Session()
    response = session.get(BASE + '/oauth2/authorization/keycloak', timeout=20)
    form = re.search(r'<form[^>]+action="([^"]+)"', response.text)
    if not form:
        raise RuntimeError('No se encontró el formulario Keycloak local.')
    # requests no implementa la excepción Secure para localhost de los navegadores.
    for cookie in session.cookies:
        if cookie.domain in ('localhost', 'localhost.local'):
            cookie.secure = False
    response = session.post(html.unescape(form.group(1)),
                            data={'username': user, 'password': password, 'credentialId': ''}, timeout=20)
    if response.status_code != 200 or ':3003/ui' not in response.url:
        raise RuntimeError('El login web local no se completó.')
    return session

snapshot('metrics-before.prom')
realm = json.loads((ROOT / 'scripts/keycloak/realm-m3-local.json').read_text(encoding='utf-8-sig'))
account = next(user for user in realm['users'] if user['username'] == args.account)
password = next(c['value'] for c in account['credentials'] if c['type'] == 'password')
session = requests.Session()
response = session.post(KC + '/protocol/openid-connect/token', data={
    'grant_type': 'password', 'client_id': 'm3-web-client',
    'username': account['username'], 'password': password, 'scope': 'openid'}, timeout=20)
response.raise_for_status()
token = response.json()['access_token']
subject = json.loads(base64.urlsafe_b64decode(token.split('.')[1] + '==='))['sub']
session.headers['Authorization'] = 'Bearer ' + token
created = operation(session, 'POST', '/api/v1/drivers',
                    json={'identitySubject': subject, 'name': 'Prueba de observabilidad TPI'})
assert created.status_code in (201, 409), 'Alta inesperada: ' + str(created.status_code)
if args.registration_only:
    print('Alta HTTP:', created.status_code, '; esperando exportación...', flush=True)
    time.sleep(35)
    snapshot('metrics-after.prom')
    evidence = {'started_local': started.isoformat(), 'new_registration': created.status_code == 201,
                'operations': events, 'cloud_verification': 'Pendiente de verificar en Grafana y Tempo.'}
    (output / 'operations.json').write_text(json.dumps(evidence, indent=2) + '\n', encoding='utf-8')
    print('Evidencias:', output)
    print('Trace ID alta:', events[0]['trace_id'])
    raise SystemExit(0)
profile = operation(session, 'GET', '/api/v1/me/driver')
profile.raise_for_status()
driver = profile.json()
vehicles_path = '/api/v1/drivers/' + driver['id'] + '/vehicles'
response = operation(session, 'GET', vehicles_path)
response.raise_for_status()
vehicles = response.json()['items']
if not vehicles:
    web = browser_session(account['username'], password)
    page = web.get(BASE + '/ui/vehicles', timeout=20)
    response = web.post(BASE + '/ui/vehicles', data={
        '_csrf': csrf(page.text), 'type': 'AUTO', 'plate': 'OTEL' + uuid.uuid4().hex[:8],
        'make': 'Test', 'model': 'Observabilidad', 'year': '2026', 'version': driver['version']}, timeout=20)
    response.raise_for_status()
    response = operation(session, 'GET', vehicles_path)
    response.raise_for_status()
    vehicles = response.json()['items']
assert vehicles, 'No se pudo registrar el vehículo sintético.'
path = '/api/v1/drivers/' + driver['id'] + '/availability-intent'
body = {'vehicleId': vehicles[0]['id'], 'connected': False, 'available': False}
key = str(uuid.uuid4())
first = operation(session, 'PUT', path, json=body, headers={'Idempotency-Key': key})
assert first.status_code == 202
replay = operation(session, 'PUT', path, json=body, headers={'Idempotency-Key': key})
assert replay.status_code == 202
print('Alta:', created.status_code, '; actualización: 202; replay: 202. Esperando exportación...', flush=True)
time.sleep(35)
for _ in range(2):
    response = operation(session, 'PUT', path, json=body, headers={'Idempotency-Key': str(uuid.uuid4())})
    assert response.status_code == 202
rejected = operation(session, 'PUT', path, json={**body, 'connected': True, 'available': True},
                     headers={'Idempotency-Key': str(uuid.uuid4())})
assert rejected.status_code == 422, 'La cuenta sintética dejó de estar pendiente; revisar el escenario.'
time.sleep(35)
snapshot('metrics-after.prom')
evidence = {'started_local': started.isoformat(), 'finished_utc': datetime.now(timezone.utc).isoformat(),
            'new_registration': created.status_code == 201, 'successful_updates': 3,
            'replays': 1, 'rejected_updates': 1, 'operations': events,
            'cloud_verification': 'Pendiente de comprobar series y Trace IDs en Grafana Cloud.',
            'gauge_note': 'Puede permanecer en 0: no se habilitan conductores ni se simula APPLIED.'}
(output / 'operations.json').write_text(json.dumps(evidence, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
print('Evidencias guardadas en:', output)
print('Trace ID alta:', events[0]['trace_id'])
print('Trace ID actualización:', next(e['trace_id'] for e in events if e['operation'].startswith('PUT')))
