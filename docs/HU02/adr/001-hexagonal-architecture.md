# ADR 001: Adopción de Arquitectura Hexagonal y Puertos/Adaptadores en M3

## Estado
Aceptado

## Contexto
El microservicio M3 (Conductores y Vehículos) forma parte de una arquitectura distribuida (TPI Desarrollo de Software 2026). Requiere desacoplamiento estricto de las reglas del negocio frente a tecnologías de persistencia (JPA/Hibernate), protocolos de transporte (HTTP/REST y Thymeleaf Web), y autenticación externa (Keycloak/OIDC). Además, debe cumplir con RNF-02 (Modularidad) y RNF-05 (Propiedad de datos).

## Decisión
Se estructura el proyecto siguiendo los principios de la Arquitectura Hexagonal (Ports & Adapters):
1. **Dominio (`domain`)**: Entidades y Value Objects puros en Java (sin anotaciones de Spring, JPA ni librerías externas).
2. **Aplicación (`application`)**:
   - `port/in`: Casos de uso como interfaces Java.
   - `port/out`: Puertos hacia el exterior (repositorios, reloj, identidad) como interfaces Java.
   - `usecase`: Implementación de los casos de uso sin acoplamiento a frameworks.
3. **Adaptadores (`adapter`)**:
   - `rest`: Controladores REST, DTOs y ProblemDetail (RFC 9457).
   - `web`: Controladores MVC y modelos de vista para Thymeleaf.
   - `persistence`: Entidades JPA, repositorios Spring Data y mappers.
   - `security`: Filtros y resolución de contexto de identidad.
4. **Validación automatizada**: Se incorpora `ArchUnit` para verificar continuamente que ninguna clase de dominio o aplicación dependa de adaptadores o tecnologías de infraestructura.

## Consecuencias
- **Positivas**: Máxima testabilidad con pruebas unitarias rápidas; independencia tecnológica; cumplimiento estricto de los requisitos no funcionales del enunciado.
- **Negativas**: Mayor cantidad de clases (mappers, interfaces de puertos y DTOs independientes).
