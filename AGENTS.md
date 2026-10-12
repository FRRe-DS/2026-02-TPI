# M3 — Conductores y Vehículos · Guía para Asistentes y Agentes (AGENTS.md)

Este repositorio contiene la implementación del microservicio **M3 (Conductores y Vehículos)** para el Trabajo Práctico Integrador de Desarrollo de Software 2026.

## Convenciones Arquitectónicas Críticas

1. **Arquitectura Hexagonal (Ports & Adapters)**:
   - `domain`: Java puro (sin dependencias de Spring, JPA, Hibernate ni HTTP).
   - `application`: Casos de uso (`usecase`) y puertos (`port/in`, `port/out`).
   - `adapter`: Implementaciones concretas de puertos (Web Thymeleaf, REST Controller, JPA Persistence, Security, Integration).
   - `config`: Cableado de dependencias mediante `@Configuration`.

2. **Propiedad de Datos (RNF-05)**:
   - M3 es el único dueño de su base de datos PostgreSQL (`m3_db` / tabla `drivers`).
   - M1 aporta identidad (`identity_subject`). No se cruzan claves foráneas con otros módulos.

3. **Control de Versiones y Concurrencia (RNF-08)**:
   - Cada cambio relevante en el conductor incrementa el atributo `version`.
   - Las respuestas REST retornan la versión en el header `ETag` (ej: `ETag: "1"`).

4. **Formato de Errores REST (Sección 9.4)**:
   - Todos los errores REST deben responder en formato `application/problem+json` conforme a RFC 9457 (`ProblemDetail`).
