# M3 — Conductores y Vehículos · Entrega 1 (E1)

## Checklist de la Entrega 1

```text
[*] E1-00: Contratos iniciales compartidos (OpenAPI 3.0.3, eventos, dependencias M4/M6).
[*] E1-01: Base Java 21 / Spring Boot, Maven, Dockerfile multietapa y compose.yaml con PostgreSQL propio.
[*] E1-02: Puertos de aplicación, entidades JPA, migración Flyway V1 y persistencia.
[*] E1-03: Seguridad Spring Security (cadena API JWT y cadena Web con sesión/CSRF).
[*] E1-04: Implementación de la Historia de Usuario 2 (HU-02) — Consultar mi perfil y estado actual.
[*] E1-12: Interfaz web funcional con Thymeleaf y Bootstrap 5 (/ui/profile) visualizando datos y estados (APPROVED, PENDING, OBSERVED, SUSPENDED).
[*] E1-13: Pruebas automatizadas (unitarias, JPA, MockMvc y ArchUnit).
```

## Historia de Usuario 2 (HU-02)

- **Título**: Consultar mi perfil.
- **Enunciado**: *Como conductor, quiero consultar mi perfil y mi estado actual, para verificar mis datos y saber en qué situación me encuentro.*
- **Requisito Funcional asociado**: RF-3.1 (Perfil).
- **Canales implementados**:
  1. **REST API**:
     - `GET /api/v1/me/driver` (conductor autenticado).
     - `GET /api/v1/drivers/{driverId}` (propietario u operador).
     - Header `ETag` con versión de concurrencia.
     - Errores normalizados con `application/problem+json` (RFC 9457).
  2. **Web UI**:
     - `GET /ui/profile` con vista responsiva Thymeleaf + Bootstrap 5.
     - Indicador visual (badges y alertas) según estado (`APPROVED`, `PENDING`, `OBSERVED`, `REJECTED`, `SUSPENDED`).
     - Exposición del motivo del operador en caso de observaciones o suspensión.
