#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Exclusivo para el cluster local creado por scripts/dev.sh.
pg_ctl -D "$PWD/.local/postgres" status >/dev/null || { echo 'Primero iniciá la aplicación con scripts/dev.sh.' >&2; exit 1; }
psql -X -v ON_ERROR_STOP=1 -h 127.0.0.1 -p 55434 -U m3_user -d m3_db -f scripts/seed-local.sql
psql -X -h 127.0.0.1 -p 55434 -U m3_user -d m3_db -c "SELECT display_name, qualification_status FROM drivers ORDER BY display_name;"
