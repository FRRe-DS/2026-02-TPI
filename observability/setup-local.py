"""Prepara sólo el rol y la vista de observabilidad en el PostgreSQL local existente."""
from pathlib import Path
import re
import secrets
import subprocess

ROOT = Path(__file__).resolve().parent.parent
env_file = ROOT / '.env.observability.local'
if env_file.exists():
    match = re.search(r'^OTEL_SQL_PASSWORD=([a-f0-9]{64})$', env_file.read_text(), re.M)
    if not match:
        raise SystemExit('El archivo local no contiene la clave generada esperada; revisar manualmente.')
    password = match.group(1)
else:
    password = secrets.token_hex(32)
    with env_file.open('x', encoding='utf-8') as file:
        file.write('# Generado localmente. No versionar.\nOTEL_SQL_PASSWORD=' + password + '\n')

# La contraseña generada sólo contiene hexadecimales; SQL via stdin, sin shell ni logs.
sql = """DO $$ BEGIN
IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'm3_otel') THEN
CREATE ROLE m3_otel LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
END IF; END $$;
"""
sql += "ALTER ROLE m3_otel PASSWORD '" + password + "';\n"
sql += (ROOT / 'observability/available-drivers.sql').read_text(encoding='utf-8')
result = subprocess.run(
    ['docker', 'compose', '-f', 'compose.yaml', 'exec', '-T', 'postgres',
     'psql', '-X', '-v', 'ON_ERROR_STOP=1', '-U', 'm3_user', '-d', 'm3_db'],
    cwd=ROOT, input=sql, text=True, encoding='utf-8', capture_output=True)
if result.returncode:
    raise SystemExit(result.stderr.replace(password, '[REDACTED]'))
print('Rol de lectura y vista agregada listos. Credencial guardada en .env.observability.local.')
