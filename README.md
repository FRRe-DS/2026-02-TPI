# M3 — Conductores y Vehículos

<<<<<<< HEAD
Repositorio del módulo M3 del TPI Desarrollo de Software 2026.
=======
[![CI Build](https://github.com/usuario/m3-drivers-vehicles/actions/workflows/ci.yml/badge.svg)](https://github.com/usuario/m3-drivers-vehicles/actions)
**Microservicio M3 · TPI Desarrollo de Software 2026 (UTN FRRe)**  
*Tecnologías: Java 21, Spring Boot 3.3.4, Spring Data JPA, PostgreSQL 16, Flyway, Spring Security, Thymeleaf + Bootstrap 5, Docker & Compose.*

---

## 1. Visión General

El módulo **M3 (Conductores y Vehículos)** es el microservicio responsable de administrar el ciclo de vida de los perfiles de los conductores, sus vehículos asociados, su documentación y el proceso administrativo de habilitación dentro del sistema Clon de Uber 2026.

Este proyecto implementa la **Historia de Usuario 2 (HU-02)** en el marco de la **Entrega 1 (E1)**:
> **HU-02 — Consultar mi perfil**  
> **Como** conductor,  
> **quiero** consultar mi perfil y mi estado actual,  
> **para** verificar mis datos y saber en qué situación me encuentro.

---

## 2. Arquitectura del Proyecto

El microservicio sigue una **Arquitectura Hexagonal (Puertos y Adaptadores)** estricta, desacoplando completamente la lógica de negocio de la infraestructura y frameworks:

```text
m3-drivers-vehicles/
├── pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
├── Dockerfile                        # Multi-stage build (JDK 21 -> JRE 21)
├── compose.yaml                      # M3 + PostgreSQL dedicado con volumen persistente
├── .env.example, .gitignore, .dockerignore
├── contracts/                        # Contratos de interfaces
│   ├── openapi.yaml                  # Contrato OpenAPI 3.0.3 (HU-02)
│   ├── events.catalog.yaml           # Catálogo de eventos de dominio
│   └── dependencies/                 # Propuestas de integración M4 / M6
│       ├── m4-availability.openapi.yaml
│       └── m6-rating-context.openapi.yaml
├── src/main/java/ar/edu/utn/frre/m3/
│   ├── M3Application.java            # Punto de entrada Spring Boot
│   ├── domain/                       # Reglas de negocio puras (POJOs sin Spring/JPA)
│   │   ├── Driver.java               # Agregado raíz Conductor
│   │   ├── DriverStatus.java         # Enum de estados (APPROVED, PENDING, etc.)
│   │   ├── PersonalInfo.java         # Value Object de datos personales
│   │   └── QualificationInfo.java    # Value Object de habilitación y motivos
│   ├── application/                  # Casos de uso y puertos
│   │   ├── port/in/                  # Interfaces de entrada (GetDriverProfileUseCase, GetMyDriverProfileUseCase)
│   │   ├── port/out/                 # Interfaces de salida (DriverRepositoryPort, CurrentPrincipalPort, ClockPort)
│   │   ├── usecase/                  # Implementación de casos de uso (GetDriverProfileService)
│   │   └── exception/                # Excepciones de negocio (DriverNotFoundException, DriverAccessDeniedException)
│   ├── adapter/
│   │   ├── rest/                     # API REST: DriverQueryRestController, DTOs, ProblemDetail RFC 9457
│   │   ├── web/                      # Web MVC: DriverProfileWebController, ViewModel
│   │   ├── persistence/              # JPA: DriverJpaEntity, SpringDataDriverRepository, Mapper, Adapter
│   │   ├── security/                 # Spring Security: SecurityConfig (cadenas API y Web), PrincipalContext
│   │   ├── integration/              # Clientes de integración externa
│   │   └── messaging/                # Publicadores y consumidores asíncronos
│   └── config/                       # Inyección de dependencias, beans y semillas
├── src/main/resources/
│   ├── application.yml               # Configuración general
│   ├── application-local.yml         # Perfil local ultrarrápido
│   ├── application-test.yml          # Perfil para suites de testing
│   ├── db/migration/                 # Migraciones Flyway
│   │   └── V1__create_drivers_table.sql
│   ├── templates/                    # Plantillas Thymeleaf (profile.html, error.html)
│   └── static/css/                   # Estilos personalizados (style.css)
├── src/test/                         # Pruebas automatizadas (Unitarias, MockMvc, JPA y ArchUnit)
├── docs/                             # Documentación (delivery-1.md, runbook.md, ADRs)
└── .github/workflows/ci.yml          # Pipeline de integración continua
```

---

## 3. Funcionalidades Implementadas (HU-02)

### A. Interfaz Web (`/ui/profile`)
- **Acceso**: `http://localhost:3003/ui/profile` o `http://localhost:3003/ui`
- Desarrollada con **Thymeleaf + Bootstrap 5**, totalmente responsiva y accesible.
- Muestra en tiempo real:
  - **Estado actual de habilitación** con badges visuales distintivos (`APPROVED`, `PENDING`, `OBSERVED`, `SUSPENDED`, `REJECTED`).
  - **Orientación al conductor**: Mensaje explicativo de su situación actual.
  - **Motivo de observación o suspensión**: En caso de estar observado o suspendido, expone el motivo exacto registrado por el operador para que el conductor sepa qué subsanar.
  - **Datos personales completos**: Nombre, Apellido, DNI, Fecha de nacimiento, Correo, Teléfono y Domicilio.
  - **Datos de auditoría técnica**: UUID propio de M3, Sujeto de identidad M1, Versión ETag del registro y fechas de alta y modificación.
  - **Selector de prueba rápida**: Barra superior que permite alternar entre perfiles con diferentes estados para pruebas ágiles de demostración.

### B. API REST (`/api/v1`)
- **`GET /api/v1/me/driver`**:
  - Consulta el perfil del conductor autenticado en base a su identidad en M1 (`identitySubject`).
  - Retorna `200 OK` con JSON `DriverProfileResponse` y cabecera `ETag: "{version}"`.
- **`GET /api/v1/drivers/{driverId}`**:
  - Consulta el perfil por UUID.
  - Valida permisos sobre objetos: únicamente permitido al propietario del perfil o a usuarios con rol `OPERATOR` / `ADMIN`.
- **Errores normalizados (RFC 9457 - ProblemDetail)**:
  - `404 Not Found`: Cuando no existe conductor (`DRIVER_NOT_FOUND`).
  - `403 Forbidden`: Cuando se intenta acceder al perfil de otro conductor sin permisos (`ACCESS_DENIED`).
  - `400 Bad Request`: Parámetro UUID inválido (`INVALID_PARAMETER_FORMAT`).

---

## 4. Instrucciones de Ejecución

### Opción 1: Con Docker Compose (Recomendado y Oficial)
```bash
docker compose up --build
```
*Levanta el contenedor de PostgreSQL 16 y el contenedor de M3 Spring Boot en el puerto 3003.*

### Opción 2: Con Maven Wrapper Localmente
```bash
# En Windows (PowerShell / CMD)
.\mvnw.cmd spring-boot:run

# En Linux / macOS
./mvnw spring-boot:run
```

---

## 5. Cuentas Pre-cargadas para Pruebas (Seeds)

Al iniciar en entorno local se cargan automáticamente las siguientes semillas de prueba:

| Estado de Habilitación | UUID M3 | Subject M1 | Nombre del Conductor | Detalle |
| :--- | :--- | :--- | :--- | :--- |
| **APPROVED** | `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11` | `user-sub-123` | Lautaro González | Habilitado para operar |
| **PENDING** | `b1ffcd88-8b1a-4fe7-aa5c-5aa8ac270b22` | `driver-pending-456` | Carlos Benítez | Pendiente de revisión administrativa |
| **OBSERVED** | `c2eebe77-7a2b-4cd6-994b-4bb7bd160c33` | `driver-observed-789` | Mariana López | "Foto de licencia no legible" |
| **SUSPENDED** | `d3ffbf66-6f3c-4bc5-883a-3aa6ac050d44` | `driver-suspended-000` | Esteban Duarte | "Suspensión preventiva por reclamos" |

---

## 6. Pruebas Automatizadas

El proyecto cuenta con una batería completa de pruebas:
- **Pruebas de Dominio (`DriverTest`)**: Comprueba invariantes, estado inicial, control de versiones y verificación de propiedad.
- **Pruebas de Aplicación (`GetDriverProfileServiceTest`)**: Comprueba reglas de negocio y seguridad mediante mocks de puertos con Mockito.
- **Pruebas de API REST (`DriverQueryRestControllerTest`)**: Comprueba respuestas HTTP, cabeceras ETag y formato RFC 9457 `ProblemDetail` con MockMvc.
- **Pruebas Web (`DriverProfileWebControllerTest`)**: Comprueba la correcta renderización del modelo y vistas Thymeleaf con MockMvc.
- **Pruebas de Persistencia JPA (`DriverPersistenceAdapterTest`)**: Comprueba persistencia, consultas y unicidad contra base de datos.
- **Pruebas de Arquitectura Hexagonal (`HexagonalArchitectureTest`)**: Utiliza **ArchUnit** para verificar que el Dominio y la Aplicación no tengan dependencias prohibidas de infraestructura o adaptadores.

Para ejecutar todas las pruebas:
```bash
./mvnw test
```
>>>>>>> viejo/main
