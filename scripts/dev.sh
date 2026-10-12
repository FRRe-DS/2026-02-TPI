#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# An explicit datasource uses the developer's configured PostgreSQL server.
# Otherwise provision a separate cluster inside this checkout, never the system database.
if [[ -z "${SPRING_DATASOURCE_URL:-}" ]]; then
  for command_name in initdb pg_ctl psql createdb; do
    command -v "$command_name" >/dev/null || { echo "Falta $command_name. Instalá PostgreSQL o configurá SPRING_DATASOURCE_URL." >&2; exit 1; }
  done
  db_dir="$PWD/.local/postgres"
  mkdir -p .local
  if [[ ! -f "$db_dir/PG_VERSION" ]]; then
    initdb -D "$db_dir" -A trust -U m3_user > .local/postgres-init.log
  fi
  if ! pg_ctl -D "$db_dir" status >/dev/null 2>&1; then
    pg_ctl -D "$db_dir" -l "$PWD/.local/postgres.log" -o "-p 55434 -h 127.0.0.1 -k /tmp" start
  fi
  if [[ "$(psql -h 127.0.0.1 -p 55434 -U m3_user -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='m3_db'")" != "1" ]]; then
    createdb -h 127.0.0.1 -p 55434 -U m3_user m3_db
  fi
  export SPRING_DATASOURCE_URL=jdbc:postgresql://127.0.0.1:55434/m3_db
  export SPRING_DATASOURCE_USERNAME=m3_user
  export SPRING_DATASOURCE_PASSWORD=m3_password
fi
exec bash ./mvnw -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.address=127.0.0.1 "$@" spring-boot:run
