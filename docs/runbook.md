# Guía de Ejecución y Operación (Runbook) · Microservicio M3

## 1. Requisitos Previos

- **Java**: JDK 21 o superior (recomendado Eclipse Temurin 21).
- **Docker y Docker Compose**: Para ejecución en contenedores.
- **Git**.

---

## 2. Ejecución con Docker Compose (Arranque Oficial)

Tal como se define en la arquitectura (Sección 11.1), Docker Compose es el arranque oficial del microservicio:

```bash
docker compose up --build
```

Esto levantará automáticamente:
1. `m3_postgres`: Contenedor dedicado de PostgreSQL 16 con volumen persistente `m3_postgres_data`.
2. `m3_drivers_vehicles`: Contenedor con la aplicación Spring Boot + Web Thymeleaf en el puerto `3003`.

Para detener los servicios:
```bash
docker compose down
```

---

## 3. Ejecución Local con Maven Wrapper

Si se dispone de JDK 21 instalado localmente:

### Windows (PowerShell / CMD):
```powershell
.\mvnw.cmd spring-boot:run
```

### Linux / macOS:
```bash
./mvnw spring-boot:run
```

El perfil `local` se activa por defecto e inicializa la base de datos con datos de prueba sintéticos para demostración.

---

## 4. Accesos y Verificación

Una vez levantada la aplicación:

- **Web UI (Perfil de Conductor - HU-02)**:
  - [http://localhost:3003/ui/profile](http://localhost:3003/ui/profile)
  - [http://localhost:3003/ui](http://localhost:3003/ui)
- **API REST (HU-02)**:
  - Conductor actual: `GET http://localhost:3003/api/v1/me/driver`
  - Conductor por ID: `GET http://localhost:3003/api/v1/drivers/a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`
- **Documentación Swagger UI**:
  - [http://localhost:3003/swagger-ui/index.html](http://localhost:3003/swagger-ui/index.html)
- **Health Checks (Liveness & Readiness)**:
  - [http://localhost:3003/health/live](http://localhost:3003/health/live)
  - [http://localhost:3003/health/ready](http://localhost:3003/health/ready)

---

## 5. Cuentas de Prueba Pre-cargadas (Seeds)

| Estado | UUID | Subject M1 | Nombre | Motivo |
| :--- | :--- | :--- | :--- | :--- |
| **APPROVED** | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11` | `user-sub-123` | Lautaro González | Habilitado para operar |
| **PENDING** | `b1ffcd88-8b1a-4fe7-aa5c-5aa8ac270b22` | `driver-pending-456` | Carlos Benítez | Pendiente de revisión |
| **OBSERVED** | `c2eebe77-7a2b-4cd6-994b-4bb7bd160c33` | `driver-observed-789` | Mariana López | Licencia no legible |
| **SUSPENDED** | `d3ffbf66-6f3c-4bc5-883a-3aa6ac050d44` | `driver-suspended-000` | Esteban Duarte | Suspensión preventiva |
