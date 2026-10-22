> “Un solo M3 en Java, con web y API, PostgreSQL propio y Docker desde la primera entrega. Dos equipos de cinco y una persona a cargo del frontend. Empezamos acordando las interfaces; después implementamos y comprobamos las integraciones.”

# M3 — Conductores y Vehículos

**Arquitectura del microservicio · TPI Desarrollo de Software 2026**

Java y Spring Boot · PostgreSQL · Web desde E1 · Docker + Docker Compose

## Checklist de esta revisión

```text
[*] Resumir explicaciones conservando estructura, gráficos y decisiones.
[*] Mantener RF/RNF, tareas de los diez integrantes e interfaces primero.
[*] Conservar los contratos y la guía de integración sin cambios.
[] Acordar las interfaces con M1/M4/M5/M6 y registrar su aprobación.
[] Implementar y probar las dos entregas con Docker.
```

| Documento                  | Referencia                                                                                              |
| -------------------------- | ------------------------------------------------------------------------------------------------------- |
| **Versión editorial**      | 0.2.4 · 3 de octubre de 2026. Resumen de la versión 0.2.3.                                              |
| **Estado**                 | Propuesta de arquitectura; no es una aplicación implementada.                                           |
| **Contrato M3**            | [M3_openapi_java.yaml](M3_openapi_java.yaml) · 0.2.0, sin cambios.                                      |
| **Guía para otros grupos** | [M3_Interfaces_Integracion.md](M3_Interfaces_Integracion.md) · interfaces, ejemplos y propuestas M4/M6. |

> **Sí usamos Docker desde E1.** Web y API comparten el contenedor M3; PostgreSQL tiene otro contenedor y volumen persistente. Conservar las carpetas del paquete para ver los gráficos y abrir los enlaces.

## Índice

| Bloque de lectura                    | Secciones                                                                                                                                                                               |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Consigna original**                | [Consigna original de cátedra — M3](#consigna-original-de-cátedra--m3)                                                                                                                  |
| **Por dónde empezamos**              | [0. Interfaces primero](#0-punto-de-partida-interfaces-primero) · [Guía para otros grupos](M3_Interfaces_Integracion.md)                                                                |
| **Qué vamos a construir**            | [1. Alcance](#1-visión-general-y-alcance) · [2. Tecnologías](#2-tecnologías-del-proyecto) · [3. Arquitectura](#3-arquitectura-y-estructura-del-proyecto)                                |
| **Cómo nos organizamos**             | [4. Equipos y responsabilidades](#4-equipos-y-responsabilidades) · [5. Interfaz web](#5-interfaz-web-de-m3)                                                                             |
| **Cómo funciona M3**                 | [6. Datos y reglas](#6-datos-y-reglas-de-negocio) · [7. Persistencia y consistencia](#7-persistencia-concurrencia-e-idempotencia) · [8. Seguridad](#8-identidad-acceso-y-seguridad)     |
| **Cómo se comunica y ejecuta**       | [9. Contratos](#9-contratos-http-e-integraciones) · [10. Sincronización y eventos](#10-sincronización-eventos-y-redis) · [11. Docker y operación](#11-docker-configuración-y-operación) |
| **Qué debemos entregar y comprobar** | [12. Entregas](#12-plan-de-las-dos-entregas) · [13. Requisitos no funcionales](#13-requisitos-no-funcionales) · [14. Pruebas y trabajo diario](#14-pruebas-y-trabajo-diario)            |
| **Qué queda por acordar**            | [15. Acuerdos pendientes](#15-acuerdos-pendientes) · [Anexo. Referencias y revisiones](#anexo-referencias-y-control-de-revisiones)                                                      |

Los requisitos del enunciado [E], las decisiones propuestas y los acuerdos pendientes se distinguen a lo largo del documento. Los checklists indican trabajo por demostrar, no cumplimiento ya obtenido.

---

## Consigna original de cátedra — M3

> **Fuente:** `README.md` original de la cátedra (Desarrollo de Software 2026).  
> Texto copiado **literalmente** para conservar la trazabilidad entre lo que pidió la cátedra y las decisiones de arquitectura de este documento. No se resume, reinterpreta, corrige ni adapta.

### Responsabilidad original de M3

| **ID** | **Módulo** | **Responsabilidad** | **Integraciones clave** |
|---|---|---|---|
| **M3** | **Conductores y Vehículos** | Perfil, habilitación, vehículos, documentación y estado operativo. | M1, M4, M5, M6. |

### Requerimientos funcionales originales de M3

| **ID** | **Requerimiento** | **Descripción funcional** |
|---|---|---|
| RF-3.1 | Perfil de conductor | El sistema deberá permitir crear y consultar los datos del conductor y su estado de habilitación. |
| RF-3.2 | Vehículos Auto y Moto | El sistema deberá permitir registrar uno o más vehículos e indicar los tipos de servicio para los que se encuentran habilitados. |
| RF-3.3 | Documentación | El sistema deberá registrar metadatos de licencia, seguro y documentación del vehículo, incluyendo vencimientos. |
| RF-3.4 | Habilitación | Un operador autorizado deberá poder aprobar, observar, suspender o rechazar una habilitación con motivo trazable. |
| RF-3.5 | Estado operativo | El sistema deberá impedir que un conductor o vehículo no habilitado participe del despacho. |
| RF-3.6 | Disponibilidad | El conductor deberá poder declararse conectado o desconectado y disponible o no disponible, coordinando ese estado con M4. |
| RF-3.7 | Calificación del cliente | El conductor deberá poder valorar al cliente después de un viaje completado, evitando registros duplicados. |

### Reglas generales originales aplicables a M3

- Cada módulo será propietario de sus datos y colaborará mediante APIs o eventos.
- La ausencia o demora de un servicio externo deberá producir un comportamiento explícito, nunca un bloqueo indefinido.

> En este repositorio se deberán definir todas las interfaces entre los diferentes módulos que contiene el enunciado. Para ello se deberá utilizar [OpenAPI Specification](https://swagger.io/specification/).

### Módulos relacionados según la consigna original

| **ID** | **Módulo** | **Responsabilidad** | **Integraciones clave** |
|---|---|---|---|
| **M1** | **Identidad y Acceso** | Credenciales, autenticación, roles, permisos y revocación. | Todos los módulos protegidos. |
| **M4** | **Ubicación y Disponibilidad** | Posición vigente, disponibilidad, proximidad, distancia y ETA (Estimated Time of Arrival / Tiempo Estimado de Llegada). | M3, M5, mapas. |
| **M5** | **Solicitud y Despacho** | Solicitudes inmediatas o activadas por reserva, candidatos, ofertas y asignación única. | M2, M3, M4, M6, M9. |
| **M6** | **Viajes y Ciclo de Vida** | Estados, arribo, inicio, finalización, cancelación y auditoría. | M5, M7, M8. |

### Requerimientos no funcionales originales

| **ID** | **Atributo** | **Condición mínima** | **Etapa** |
|---|---|---|---|
| RNF-01 | Libertad tecnológica | Lenguaje y framework a elección del grupo, con aprobación y justificación técnica. | TP1 |
| RNF-02 | Arquitectura modular | Nueve servicios con límites y responsabilidades explícitas. Dos implementaciones independientes por módulo cuando la matrícula lo permita. | TP1 |
| RNF-03 | Contratos | APIs síncronas definidas con OpenAPI y errores consistentes. Eventos documentados en un catálogo común. | TP1 |
| RNF-04 | Contenedores | Cada módulo y dependencia deberá ejecutarse mediante imágenes OCI reproducibles y versionadas. | TP1 |
| RNF-05 | Propiedad de datos | Cada servicio controlará su persistencia. Quedará prohibido consultar directamente tablas de otro módulo. | TP1-TP2 |
| RNF-06 | Configuración y secretos | URLs, puertos, credenciales y claves se configurarán externamente y no se almacenarán en el repositorio. | TP1 |
| RNF-07 | Pruebas | Pruebas unitarias y de integración desde TP1, de integración distribuida en TP2 y End-to-End en TP3. | TP1-TP3 |
| RNF-08 | Concurrencia | La solución impedirá doble asignación, doble activación, doble finalización y doble cobro ante solicitudes concurrentes. | TP1-TP3 |
| RNF-09 | Idempotencia | Operaciones críticas y consumidores de mensajes tolerarán reintentos sin duplicar efectos. | TP2 |
| RNF-10 | Mensajería | Se utilizará RabbitMQ o alternativa aprobada para flujos asíncronos acordados. | TP2 |
| RNF-11 | Caché y vencimiento | Redis o alternativa aprobada administrará estado efímero, TTL e invalidación cuando corresponda. | TP2 |
| RNF-12 | Seguridad | Autenticación, autorización, validación, HTTPS en entornos publicados y controles básicos de vulnerabilidades web. | TP2-TP3 |
| RNF-13 | Resiliencia | Timeouts, reintentos limitados y tratamiento explícito de indisponibilidad sin bloqueos indefinidos. | TP2-TP3 |
| RNF-14 | Salud y diagnóstico | Health checks, logs estructurados y correlación por identificador de solicitud, viaje o reserva. | TP2-TP3 |
| RNF-15 | Usabilidad | Cliente responsivo con estados claros de espera, confirmación, rechazo, cancelación, error y recuperación. | TP3 |
| RNF-16 | Performance | El consorcio definirá y medirá al menos una meta del recorrido crítico. | TP3 |
| RNF-17 | Automatización | El repositorio ejecutará build y pruebas automáticamente. La publicación de imágenes será recomendada mediante pipeline. | TP2-TP3 |
| RNF-18 | Portabilidad | La solución completa se ejecutará localmente y podrá publicarse en cloud sin modificar el código de negocio. | TP3 |
| RNF-19 | Privacidad y auditoría | No se expondrán datos sensibles en logs, QR o URLs. Las operaciones críticas conservarán trazabilidad. | TP3 |
| RNF-20 | Documentación | README, diagramas, contratos, decisiones y procedimientos deberán mantenerse junto con la versión correspondiente. | TP1-TP3 |

### Trazabilidad entre la consigna y esta arquitectura

> La siguiente tabla es propia de este documento. No forma parte de la consigna: sólo permite navegar desde cada requisito original hacia la sección donde se explica nuestra implementación.

| Requisito original | Implementación / sección de arquitectura |
|---|---|
| RF-3.1 | [Perfil](#12-funcionalidades-por-entrega) |
| RF-3.2 | [Vehículos](#12-funcionalidades-por-entrega) |
| RF-3.3 | [Documentación](#63-documentación-y-revisiones) |
| RF-3.4 | [Habilitación](#65-estados-y-decisiones-de-habilitación) |
| RF-3.5 | [Estado operativo / elegibilidad](#66-condiciones-de-elegibilidad) |
| RF-3.6 | [Disponibilidad](#67-disponibilidad-solicitada-y-estado-efectivo) |
| RF-3.7 | [Valoraciones](#68-valoraciones-de-clientes) |

---

## 0. Punto de partida: interfaces primero

> **E1-00 es la primera actividad:** compartir cómo se comunica M3 antes de terminar la base, la lógica o las pantallas. No es otra entrega ni otro equipo.

### 0.1 Orden de trabajo

| Paso                        | Resultado esperado                                                                  |
| --------------------------- | ----------------------------------------------------------------------------------- |
| **Proponer**                | API M3, necesidades M1/M4/M6, ejemplos y catálogo inicial compartidos.              |
| **Acordar**                 | Datos, permisos, errores, límites y versión aprobados por proveedor y consumidores. |
| **Desarrollar en paralelo** | Implementaciones contra el contrato; simulaciones claramente identificadas.         |
| **Integrar y comprobar**    | Servicios reales y evidencia de peticiones/respuestas acordadas.                    |

B5/B1 preparan Docker y PostgreSQL en paralelo. Un desacuerdo no bloquea funciones independientes, pero cada integración compartida se implementa sobre su acuerdo.

### 0.2 Paquete inicial de interfaces

| Interfaz       | Definición inicial                                                                    | Responsable M3        |
| -------------- | ------------------------------------------------------------------------------------- | --------------------- |
| **M1 ↔ M3**    | Issuer, clientes, audiencias, roles/scopes, callback y acceso desde navegador/Docker. | B4; A1 registra.      |
| **M4/M5 → M3** | Elegibilidad individual y por lote.                                                   | A4/A1; seguridad B4.  |
| **M6 → M3**    | Resumen mínimo autorizado del conductor.                                              | A1/B4.                |
| **M3 → M4**    | Intención, versión, deduplicación y confirmación.                                     | A4/B4; revisan B1/B5. |
| **M3 → M6**    | Contexto confiable para valorar al cliente.                                           | A1/B4.                |
| **Eventos E2** | Campos, consumidores, versiones y duplicados.                                         | A1/A3/A4/B3/B5.       |

**Definir ahora no adelanta toda la implementación:** valoraciones reales y mensajería siguen en E2. A5 conserva exclusivamente el frontend. El detalle está en la [guía de integración](M3_Interfaces_Integracion.md); las propuestas M4/M6 no son sus APIs completas ni acuerdos aprobados.

### 0.3 Qué debe quedar acordado antes de integrar

```text
[] Proveedor, consumidores, responsable y entrega identificados.
[] Ruta/método, datos, ejemplos, errores y límites definidos.
[] Seguridad, correlación, timeouts, reintentos e idempotencia acordados.
[] Versión/commit, aprobación bilateral y pruebas de contrato registrados.
```

Publicar primero el borrador de diseño y luego la versión implementada. No anunciar funciones E2 como disponibles en E1; coordinar cambios de nombres, tipos o significados con sus consumidores.

---

## 1. Visión general y alcance

### 1.1 Una sola implementación de M3

**Un repositorio, un proyecto Maven, un JAR, una imagen M3 y una base propia.** Los paquetes internos se llaman mediante interfaces Java, no HTTP. Docker Compose organiza los contenedores; no se crea uno por integrante o componente.

### 1.2 Funcionalidades por entrega

| Requisito [E]                       | Primera entrega — E1                                                      | Segunda entrega — E2                                    |
| ----------------------------------- | ------------------------------------------------------------------------- | ------------------------------------------------------- |
| **RF-3.1 — Perfil**                 | Crear/consultar conductor vinculado a M1, persistido y visible en la web. | Permisos y recorrido integrado.                         |
| **RF-3.2 — Auto/Moto**              | Varios vehículos y servicios, mediante web/API.                           | Casos límite e integración.                             |
| **RF-3.3 — Documentación**          | Metadatos, revisiones y vencimientos.                                     | Propagación automática de vencimientos.                 |
| **RF-3.4 — Habilitación**           | Aprobar, observar, suspender o rechazar con motivo e historial.           | Eventos y fallos distribuidos.                          |
| **RF-3.5 — Estado operativo**       | Elegibilidad local y contrato para consumidores.                          | Verificación M4/M5 y concurrencia distribuida acordada. |
| **RF-3.6 — Disponibilidad**         | Intención persistente y primer intercambio M4.                            | Recuperación, versiones y sincronización visible.       |
| **RF-3.7 — Valoración del cliente** | Contrato/lógica probados con doble M6, sin función web real.              | Formulario/historial y M6 real.                         |

### 1.3 Responsabilidades que pertenecen a otros módulos

M1 administra identidad/SSO; M4, GPS, proximidad, disponibilidad efectiva y reservas temporales; M5, despacho; M6, viajes; M7, pagos; M8, notificaciones/comprobantes; M9, reservas futuras. **M3 no consulta sus tablas ni reconstruye esas funciones.**

La sesión web local no reemplaza M1. RF-3.3 exige metadatos; subir archivos es una extensión a acordar. No se añaden edición genérica de campos ni bajas destructivas.

### 1.4 Entregas y organización académica

**E1:** interfaces primero, Docker, persistencia, API, M1, web y primer intercambio M4. **E2:** M3 completo e integración.

El enunciado distingue TP1/TP2/TP3: confirmar su correspondencia con E1/E2. Adelantar la web no elimina E2E, usabilidad global, performance y despliegue posteriores. Los dos subequipos no sustituyen las implementaciones independientes de RNF-02. M3 figura para grupos 2 y 15: **M3 no implica cliente Keycloak `grupo-03`.** [E]

---

## 2. Tecnologías del proyecto

### 2.1 Stack propuesto

| Pieza                  | Elección                                                                                                                                     |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------------- |
| **Backend**            | Java 21 + Spring Boot/Spring MVC. Base propuesta conservada: **Boot 4.0.8**, no una afirmación de última versión. [S1]                       |
| **Frontend**           | Thymeleaf, HTML y Bootstrap 5; CSS mínimo y JavaScript para mejoras pequeñas. Sin frontend separado. [S2]                                    |
| **Persistencia**       | PostgreSQL + Spring Data JPA/Hibernate; Flyway para migraciones. [S6]                                                                        |
| **Seguridad**          | Spring Security OAuth2 Client para web/salidas; Resource Server para JWT API. Keycloak/M1. [S3] [S4] [S5]                                    |
| **Validación/errores** | Jakarta Bean Validation; reglas en Dominio; `ProblemDetail` REST y vistas HTML separadas.                                                    |
| **HTTP externo**       | Spring RestClient para M4/M6, con timeouts. [S7]                                                                                             |
| **Contratos**          | OpenAPI 3.0.3 contract-first; springdoc/Swagger UI como visor. Referencia conservada: springdoc **3.0.3**, línea 3.0.x para Boot 4.0.x. [S8] |
| **Pruebas**            | JUnit, Mockito, MockMvc, Spring Security Test, Testcontainers PostgreSQL y ArchUnit. [S9]                                                    |
| **Ejecución/calidad**  | Docker + Compose desde E1; Maven Wrapper, CI, formato automático y Actuator.                                                                 |
| **Integración E2**     | Spring AMQP/RabbitMQ, Spring Data Redis y tareas acotadas `@Scheduled`.                                                                      |

### 2.2 Preparación de la base Java

Crear con Spring Initializr las dependencias de web, Thymeleaf, seguridad OAuth2, JPA, PostgreSQL, Validation, Flyway y Actuator. Añadir soporte Flyway PostgreSQL, pruebas, springdoc y ArchUnit. Usar el BOM de Boot y fijar versiones restantes; comprobar compatibilidad sin mezclar configuraciones antiguas de seguridad.

### 2.3 Herramientas que no incorporamos

Sin Kubernetes, Eureka, Spring Cloud Gateway/Config, CQRS, event sourcing, módulos Maven múltiples, repositorio universal ni base por componente. Tampoco Spring Data REST para exponer repositorios, ni Feign/WebFlux sólo para dos clientes. **Docker sí forma parte de E1; RabbitMQ/Redis se incorporan en E2.**

---

## 3. Arquitectura y estructura del proyecto

### 3.1 Vista general

#### A. M3 dentro del sistema

![Vista de contexto de M3: navegador, identidad M1, interfaces M4/M5/M6 y dependencias Docker.](docs/diagrams/m3-contexto.png)

El navegador usa la web; otros módulos, la API. M1 aporta identidad y M3 conserva su base. Las flechas representan solicitudes; las respuestas vuelven por el mismo canal.

#### B. Dentro del contenedor de M3

![Vista interna: web y API reutilizan Aplicación y Dominio; Persistencia es el único acceso a datos.](docs/diagrams/m3-interno.png)

**Web y API comparten Aplicación y Dominio.** Sólo Persistencia usa PostgreSQL/Redis, también para los workers. M4 decide disponibilidad efectiva; M5 asigna; M6 controla viajes. Los servicios compartidos se conectan por URLs configurables.

Fuentes conservadas: [Mermaid](docs/diagrams/m3-general.mmd), [contexto SVG](docs/diagrams/m3-contexto.svg) e [interior SVG](docs/diagrams/m3-interno.svg).

### 3.2 Componentes y límites

| Componente                | Responsabilidad y límite                                                                                                                 |
| ------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| **Dominio**               | Reglas del negocio; Java sin Spring, JPA, HTTP ni RabbitMQ.                                                                              |
| **Aplicación**            | Casos de uso, permisos sobre objetos y puertos/transacciones; sin SQL ni respuestas externas crudas.                                     |
| **API REST**              | `@RestController`, DTO, validación y errores; nunca expone entidades JPA.                                                                |
| **Web/frontend**          | `@Controller`, formularios, modelos de vista y Thymeleaf. Llama Aplicación, no repositorios ni su propia API por HTTP.                   |
| **Persistencia**          | Entidades, repositorios, consultas, migraciones, transacciones, auditoría, idempotencia, outbox/inbox y Redis. **Único acceso a datos.** |
| **Seguridad**             | JWT/OIDC, autoridades y contexto confiable; sin identidades/contraseñas propias.                                                         |
| **Integraciones**         | RestClient M4/M6, traducción a tipos internos y tratamiento de fallos.                                                                   |
| **Plataforma/mensajería** | Configuración, composición, diagnóstico, broker y tareas que invocan puertos/casos de uso.                                               |

### 3.3 Dependencias y acceso a datos

Aplicación depende de Dominio y define puertos; los adaptadores los implementan. Inyección por constructor y ensamblado `@Configuration/@Bean` fuera del núcleo. JPA, JDBC, EntityManager y Redis quedan en Persistencia, salvo configuración general; ArchUnit controla límites/ciclos.

```text
Caso de uso → DriverRepository → JpaDriverRepository
            → repositorio Spring Data interno → PostgreSQL
```

`UnitOfWork.run(...)` usa `TransactionTemplate`: repositorios de una operación comparten transacción y EntityManager. DTO/formularios se transforman en comandos/modelos; nunca se enlazan directamente a entidades JPA. [S6]

### 3.4 Árbol de carpetas

```text
m3-drivers-vehicles/
├── pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
├── README.md, AGENTS.md, .env.example, .gitignore, .dockerignore
├── Dockerfile                        # M3: API + web desde E1
├── compose.yaml                      # M3 y dependencias
├── contracts/
│   ├── openapi.yaml                  # Contrato de la release
│   ├── events.catalog.yaml
│   └── dependencies/                 # M4/M6: propuestas y luego acuerdos
│       ├── m4-availability.openapi.yaml
│       └── m6-rating-context.openapi.yaml
├── src/main/java/ar/edu/utn/frre/m3/
│   ├── M3Application.java
│   ├── domain/                       # Reglas por función
│   ├── application/
│   │   ├── usecase/                  # Incluye eligibility
│   │   └── port/                     # Repositorios, UnitOfWork, M4/M6, reloj
│   ├── adapter/
│   │   ├── rest/                     # Controladores JSON, DTO, errores
│   │   ├── web/                      # A5: HTML, forms, viewmodels
│   │   ├── persistence/              # JPA, consultas, transacción, Redis
│   │   ├── security/                 # Filter chains, PrincipalContext
│   │   ├── integration/              # Clientes M4/M6
│   │   └── messaging/                # Workers, outbox, consumidores
│   └── config/                       # Wiring, propiedades, logs, salud
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml, application-test.yml
│   ├── db/migration/                 # Flyway, parte de Persistencia
│   ├── templates/                    # A5: pantallas y fragmentos
│   └── static/                       # A5: css, js, Bootstrap local
├── src/test/                         # Lógica, DB, HTTP, web, arquitectura
├── docs/                             # Entregas, acuerdos, runbook, ADR
└── .github/workflows/ci.yml
```

Son paquetes del mismo proyecto, no aplicaciones ni carpetas vacías obligatorias. El build incorpora el contrato aprobado, las plantillas y Bootstrap; no mantener otra copia manual del YAML.

### 3.5 Puertos iniciales y modificabilidad

`DriverRepository` (`findById`, `findByIdentitySubject`, `save`), repositorios específicos, `UnitOfWork`, disponibilidad M4, `TripContextPort.getCompletedTripContext(...)`, `Clock` y consulta batch. Cambiar el ORM concentra cambios; otro motor también exige revisar consultas, restricciones y migraciones.

---

## 4. Equipos y responsabilidades

**Dos equipos de cinco en el mismo repositorio. A5 es el único responsable del frontend.** A1 coordina OpenAPI y valoraciones; cada autor documenta su operación y prueba su parte.

### 4.1 Equipo A · Funciones, API e interfaz

| Rol                                      | Primera entrega                                                                         | Segunda entrega                                  | Colabora con |
| ---------------------------------------- | --------------------------------------------------------------------------------------- | ------------------------------------------------ | ------------ |
| **A1 — Perfil, contrato y valoraciones** | E1-00: publicar contratos/ejemplos; perfil/API; RF-3.7 con doble M6, sin pantalla real. | Valoraciones M6, permisos y contrato.            | B2/B3/B4     |
| **A2 — Vehículos**                       | Casos de uso/API Auto/Moto, servicios y pruebas.                                        | Casos límite y selección operativa.              | B2           |
| **A3 — Documentación y habilitación**    | Metadatos, revisión, decisiones e historial.                                            | Vencimientos, suspensión y concurrencia.         | B3/B1        |
| **A4 — Elegibilidad y disponibilidad**   | E1-00: acordar elegibilidad/intención M4/M5; aptitud y consulta para UI.                | Integración real, recuperación y orden.          | B2/B4        |
| **A5 — Frontend**                        | Thymeleaf, Bootstrap, controladores web, formularios, navegación y errores.             | Valoraciones, recuperación visible y usabilidad. | A1–A4/B4     |

### 4.2 Equipo B · Persistencia, seguridad e integración

| Rol                                   | Primera entrega                                              | Segunda entrega                                   | Colabora con |
| ------------------------------------- | ------------------------------------------------------------ | ------------------------------------------------- | ------------ |
| **B1 — Modelo y transacciones**       | Esquema, Flyway, restricciones, UnitOfWork y seeds.          | Idempotencia/inbox, recuperación y concurrencia.  | A3/B3        |
| **B2 — Perfiles y vehículos**         | Entidades, mappers, repositorios y dobles.                   | Batch e índices medidos.                          | A1/A2/A4     |
| **B3 — Documentos y auditoría**       | Documentos, decisiones, auditoría y salidas pendientes.      | Valoraciones, outbox/inbox y limpieza.            | A3/A1        |
| **B4 — Seguridad e integración HTTP** | E1-00: M1 y contratos M4/M6; seguridad y clientes HTTP.      | Integración real, revocación y fallos.            | A4/A1/A5     |
| **B5 — Ejecución y confiabilidad**    | Dockerfile/Compose, Maven, CI, Actuator, logs y worker HTTP. | RabbitMQ/Redis, reintentos/DLQ, imágenes y carga. | B1/A4        |

### 4.3 Colaboración con el frontend

A1–A4 entregan casos de uso, DTO y errores; B4, login/contexto; B5, arranque. A5 conecta pantallas, **no implementa el backend de los otros nueve**. Puede comenzar con dobles identificados, reemplazados antes de la demo. Todos hacen pruebas; no hay tester exclusivo.

### 4.4 Coordinación entre ambos equipos

A1 publica interfaces, B4 acuerda seguridad/dependencias, A4 define elegibilidad/intención, A2/A3 revisan datos/estados, B1–B3 revisan IDs/versiones/persistencia y B5 prepara validación/entorno. A5 acuerda modelos de vista.

B1 ordena migraciones y B5 mantiene Docker/CI. Integrar funciones completas desde el comienzo, sin esperar toda la base ni crear ramas permanentes o repositorios por equipo.

---

## 5. Interfaz web de M3

### 5.1 Alcance de la primera entrega

Acceso propuesto: `http://localhost:3003/ui`. Spring Boot sirve HTML con menú, tablas, formularios y mensajes reutilizables; sin panel estadístico ni aplicación frontend separada. Keycloak conserva su puerto propio. [S2]

### 5.2 Pantallas y accesos

| Pantalla                   | Función                                                  | Acceso / ruta orientativa                    |
| -------------------------- | -------------------------------------------------------- | -------------------------------------------- |
| **Inicio/perfil**          | Crear perfil propio, ver estado/motivos.                 | Conductor: `/ui`, `/ui/profile`.             |
| **Vehículos**              | Registrar/listar Auto/Moto, servicios y estado.          | Propietario: `/ui/vehicles`.                 |
| **Documentación**          | Metadatos, revisión, vencimiento e historial.            | Propietario: `/ui/documents`.                |
| **Habilitación**           | Consultar estado/decisiones; sin autoaprobarse.          | Propietario: `/ui/qualification`.            |
| **Disponibilidad**         | Vehículo, conexión, intención, sincronización y motivos. | Propietario: `/ui/availability`.             |
| **Listado administrativo** | Lista paginada de conductores.                           | Operador: `/ui/operator/drivers`.            |
| **Detalle administrativo** | Revisar documentos y decidir habilitación con motivo.    | Operador: `/ui/operator/drivers/{driverId}`. |
| **Valoraciones — E2**      | Registrar/consultar valoración validada con M6.          | Conductor: `/ui/customer-ratings`.           |

En E1 se ocultan valoraciones reales. Sin mapas, viajes, pagos, chat o administración de M1; el login redirige a Keycloak, sin almacenar contraseñas en M3.

### 5.3 Flujo de consulta y envío de formularios

**GET:** consulta → modelo de vista → Thymeleaf. **POST:** validación/seguridad → mismo caso de uso que REST → persistencia → redirección GET con mensaje (Post/Redirect/Get). GET no modifica datos; acciones administrativas HTML no requieren nuevas rutas JSON.

### 5.4 Versiones y reenvíos

Formularios mutables llevan CSRF, `idempotencyKey` y, cuando corresponde, `expectedVersion`, equivalentes a `Idempotency-Key`/`If-Match`. Reintentos conservan clave; una operación nueva usa otra. Campos ocultos no autorizan: validar identidad/propietario en servidor. Una versión vieja pide recargar y revisar, sin sobrescribir.

### 5.5 Presentación, mensajes y recuperación

Etiquetas accesibles, teclado, móvil, errores por campo y estados vacío/guardando/guardado/rechazado/fallo con recuperación. Usar texto además de color. Mostrar `PENDING`, `RETRYING`, `FAILED` o versión aplicada; **aplicada no significa conductor libre**. Actualización manual basta en E1. Conservar campos no sensibles tras error; no guardar tokens en HTML/localStorage.

### 5.6 Aptitud mostrada en pantalla

Consultar aptitud mediante un caso de uso autorizado para propietario/operador y la misma política de Dominio. No saltar permisos usando credenciales técnicas de `/integration/**`. Separar aptitud administrativa de elegibilidad operativa: no exigir disponibilidad previa para conectarse.

---

## 6. Datos y reglas de negocio

### 6.1 Propiedad de la base de datos

`m3_db` pertenece a M3 y la base Keycloak, a M1, con usuarios separados. Referencias M1/M2/M6 son IDs externos, **no claves foráneas entre servicios**.

### 6.2 Modelo lógico

| Tabla                     | Datos y restricciones esenciales                                                                                                     |
| ------------------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| `drivers`                 | UUID, `identity_subject`, nombre visible, habilitación, versión/fechas. Identidad única; con varios issuers, `(issuer, subject)`.    |
| `vehicles`                | Conductor, Auto/Moto, país, patente normalizada, marca/modelo/año y habilitación. `UNIQUE(country_code, normalized_plate)`.          |
| `vehicle_services`        | Vehículo/servicio: par único, no listas separadas por comas.                                                                         |
| `documents`               | Conductor, vehículo opcional, tipo, revisión, metadatos, revisión administrativa, vigencia e `is_current`. Historial no destructivo. |
| `document_service_types`  | Licencia y servicios habilitados.                                                                                                    |
| `qualification_decisions` | Objeto, estados anterior/nuevo, motivo, actor, fecha y versión.                                                                      |
| `availability_intents`    | Una por conductor: conexión/disponibilidad solicitadas, vehículo, versiones deseada/aplicada y sincronización.                       |
| `customer_ratings`        | Viaje, conductor, cliente, puntuación 1–5, comentario opcional y fecha. `UNIQUE(trip_id, driver_id)`. Real en E2.                    |
| `audit_log`               | Entidad, operación, actor, correlación, fecha y cambios no sensibles.                                                                |
| `idempotency_records`     | Principal/método/ruta/clave únicos, hash de petición, resultado mínimo y caducidad.                                                  |
| `outbox`                  | ID, destino/tipo, agregado/versión, payload mínimo, estado, intentos/próximo intento. HTTP E1; también eventos E2.                   |
| `inbox`                   | Consumidor y `event_id` únicos, fecha. Sólo con consumidor real en E2.                                                               |

IDs propios UUID; externos, cadenas opacas acotadas; instantes `timestamptz`/ISO 8601 `date-time`; versiones y puntuaciones enteras. B1 prepara migraciones/índices probados. Cada vehículo pertenece a un conductor, que puede tener varios. Transferencias, flotas y vehículos compartidos quedan fuera del alcance base.

### 6.3 Documentación y revisiones

Licencia sin `vehicle_id`; seguro/documentación vehicular con vehículo propio obligatorio. Reforzar coherencia con FK compuesta/restricciones. Índices únicos parciales aseguran una revisión actual por conductor/tipo o vehículo/tipo.

Renovar crea revisión, sin borrar la anterior. **Política propuesta, pendiente de aprobación:** la nueva revisión queda actual y pendiente, pudiendo interrumpir elegibilidad. Renovar sin interrupción requeriría distinguir revisión presentada y efectiva.

### 6.4 Vencimientos

`validUntil` es exclusivo: al llegar ese instante ya no habilita. Acordar zona/fin de día para fechas sin hora. La elegibilidad consulta el reloj aunque falle el job de propagación. Esta revisión académica de metadatos no certifica validez legal real.

### 6.5 Estados y decisiones de habilitación

```text
PENDING   → APPROVED / OBSERVED / REJECTED
APPROVED  → SUSPENDED / OBSERVED
OBSERVED  → PENDING, después de corregir
REJECTED  → PENDING, si se admite nueva presentación
SUSPENDED → APPROVED / OBSERVED / REJECTED, por operador
```

El operador puede suspender preventivamente cualquier objeto no suspendido, con motivo. Aprobar exige documentos actuales, aprobados y vigentes; subir documentos no rehabilita automáticamente. Guardar actor, objeto, estados, motivo, versión/fecha; no hay `PATCH status` libre. **Aprobación histórica no garantiza aptitud actual**, por ejemplo ante vencimiento.

### 6.6 Condiciones de elegibilidad

```text
Conductor aprobado
+ vehículo propio, aprobado y seleccionado
+ licencia actual, aprobada, vigente y compatible con el servicio
+ seguro/documentación vehicular actuales, aprobados y vigentes
+ servicio permitido
+ intención de conexión y aceptación de solicitudes activas
```

### 6.7 Disponibilidad solicitada y estado efectivo

Activar disponibilidad requiere aptitud administrativa/documental y vehículo propio, **no disponibilidad previa**. `connected=false` implica `available=false`.

M3 devuelve `eligible`, `reasons`, `checkedAt`, `driverVersion`, `validUntil`; candidato conocido no apto: `200`/`eligible:false`. M4 controla ubicación, ocupación/reservas y disponibilidad efectiva; M5 asignación única. **La respuesta M3 no reserva ni asigna.** `APPLIED` reconoce una versión, no disponibilidad actual; desconectarse no borra un viaje/reserva de M4.

### 6.8 Valoraciones de clientes

El conductor envía `tripId` y puntuación, no un cliente elegido. Antes de la transacción, M3 verifica en M6 conductor asignado, cliente y viaje completado; acordar estabilidad/rectificaciones. Unicidad viaje/conductor impide duplicados incluso con otra clave idempotente. M3 no modifica el viaje.

---

## 7. Persistencia, concurrencia e idempotencia

### 7.1 Repositorios y lecturas coherentes

Un módulo de datos no es una clase gigante: usar repositorios específicos y consulta batch, evitando lecturas individuales repetidas. Elegibilidad lee documentos, aprobación, intención y versión desde una consulta/snapshot consistente —por ejemplo `REPEATABLE READ`— y un solo instante de evaluación.

### 7.2 Transacciones y control de versiones

Bloqueo breve `PESSIMISTIC_WRITE` del conductor raíz, comprobar `expectedVersion` e incrementar `version` desde 1. Cambios relevantes de vehículos, documentos, habilitación e intención actualizan esa raíz. No mezclar este incremento manual con `@Version`. [S6]

**Una transacción confirma juntos** cambio, versión, historial, auditoría, salida pendiente y resultado idempotente, cuando correspondan. Repositorios comparten transacción; no esperar M4/M6/RabbitMQ dentro.

Obtener ETag con `GET /api/v1/drivers/{driverId}` y enviar `If-Match`, por ejemplo `"7"`: versión vieja `412`, faltante `428`. Valoraciones usan su unicidad/idempotencia, sin cambiar esta versión.

### 7.3 Reintentos sin efectos duplicados

Operaciones mutables usan `Idempotency-Key`, identificada por principal, método y ruta normalizada, con hash del cuerpo canónico. Guardar clave/efecto juntos: misma clave/contenido devuelve resultado original; contenido distinto, `409`. Solicitudes simultáneas esperan acotadamente o reciben conflicto reintentable.

Autenticar/autorizar antes del replay y resolverlo antes de rechazar un `If-Match` envejecido por el primer éxito. Retención propuesta: **7 días**, resultado mínimo sin tokens. Caducar claves no elimina unicidad permanente de identidad, patente o valoración.

### 7.4 Límite de la garantía entre servicios

Entre consulta y asignación puede ocurrir una suspensión. La propuesta revalida antes de confirmar, excluye suspendidos/vencidos en M4, invalida por eventos y cancela ofertas pendientes: **reduce la ventana, no demuestra atomicidad global**.

Si se exige eliminarla, acordar admisión idempotente ordenada con suspensión bajo el mismo bloqueo M3, ID conservado por M5 y tratamiento de admisiones previas. **Ese protocolo no está en el YAML actual.** No cerrar RF-3.5 estricto sin acuerdo/pruebas M3/M4/M5. Asignación, activación, finalización y cobro también requieren pruebas de sus respectivos servicios.

---

## 8. Identidad, acceso y seguridad

### 8.1 M1 y Keycloak

Conservar OAuth2/OIDC, JWT, discovery/JWKS y el enfoque Spring Resource Server del ejemplo 2025, no su realm/secretos. La web añade OAuth2 Login: `client_credentials` identifica servicios, no personas. [S3] [S5]

### 8.2 Seguridad por entrada y salida

| Entrada/salida           | Configuración                                                                                                                                                                     |
| ------------------------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **API `/api/**`\*\*      | Primera `SecurityFilterChain`: JWT, `STATELESS`, sin cookies/login web. Falta token: `401` JSON. CSRF desactivable sólo aquí.                                                     |
| **Web y resto de rutas** | Segunda cadena: `oauth2Login`, sesión y CSRF. Autorizar login/callback/recursos explícitamente; denegar lo no previsto. Cookies `HttpOnly`, `SameSite=Lax`, `Secure` al publicar. |
| **M3 → M4/M6**           | Token técnico `client_credentials`, audiencia destino y scopes mínimos; no reenviar indiscriminadamente token web.                                                                |
| **Salud/documentación**  | Salud pública mínima; Actuator detallado, Swagger y contrato según entorno/rol acordado.                                                                                          |

Cubrir todas las rutas, también `/login/**`; comprobar CSRF Thymeleaf sin desactivarlo globalmente. [S3-chains] [S4]

### 8.3 Inicio de sesión web

Navegador → M3 → Keycloak → callback → sesión → `/ui`. Cliente web confidencial separado del técnico: Authorization Code, PKCE S256 explícito, `state`/`nonce` gestionados por Spring. M1 confirma clientes y callback exacto, por ejemplo `http://localhost:3003/login/oauth2/code/keycloak`, más retorno de logout.

### 8.4 Permisos e identidad confiable

Mapear roles/scopes a `PrincipalContext` desde JWT u OIDC confiable; no asumir mapeo automático de roles Keycloak. Claims de tokens/UserInfo configurados, nunca formularios. Aplicación comprueba acción/propietario: ocultar botones no autoriza y un servicio no suplanta a un conductor.

### 8.5 Validación de tokens de la API

Verificar firma/algoritmo, `iss`, audiencia M3, `exp`, `nbf` cuando exista y `sub`. No usar ID token como Bearer. Scopes propuestos: `m3:eligibility:read` y `m3:drivers:read`; sin rol operador para servicios por defecto. [S5]

### 8.6 Sesión, cierre y revocación

Una réplica y sesión en memoria inicialmente: reiniciar exige login, sin perder PostgreSQL. M3 completo no es stateless; futuras réplicas requieren acordar sesión compartida detrás de Persistencia.

Logout POST con CSRF invalida sesión y coordina OIDC. Inactividad propuesta: **30 minutos**. Sesión/JWT local no garantizan revocación instantánea: acordar revalidación/introspección para acciones privilegiadas; si falla la validación requerida, no modificar y mostrar fallo/`503`.

### 8.7 Protección de datos, web y servicios

Autenticación, permisos y propiedad en negocio; excepciones explícitas para salud mínima/OIDC/recursos. Rechazar campos no permitidos —por ejemplo autoaprobación en el alta— y validar tipos, tamaños, IDs/reglas. Consultas parametrizadas; no descargar URLs documentales arbitrarias.

Límites iniciales ajustables: **JSON 64 KiB, páginas de 20/máximo 100, batch máximo 100**; limitar también formularios. Sin CORS abierto, tokens expuestos, `th:utext` del usuario ni formularios enlazados a JPA. Usar escape, CSRF, headers/CSP, recursos locales, escaneo de dependencias/secretos y HTTPS publicado. No exponer DB, broker, Redis ni consolas a Internet; CORS no sustituye autenticación.

---

## 9. Contratos HTTP e integraciones

### 9.1 Alcance y fuente de verdad

**YAML describe; HTTP/JSON comunica; JWT identifica.** Desde E1-00 compartir el contrato versionado de M3, no las APIs completas de nueve servicios. El YAML 0.2.0 conserva operaciones/modelos previos; Java/web no cambian el contrato JSON.

Rutas HTML/login quedan en este documento; contratos M4/M6 en `contracts/dependencies/` y eventos en `contracts/events.catalog.yaml`. La sesión web no es alternativa al Bearer.

### 9.2 Publicación del contrato y Swagger UI

`contracts/openapi.yaml` es fuente de verdad. El build copia la release y `springdoc.swagger-ui.url` la muestra, sin reemplazarla por generación automática. Swagger es documentación, no la web del conductor; requiere access token API, no aprovecha automáticamente la sesión `/ui`. [S8]

Compartir diseño **E1+E2 desde el inicio**, con etapas. La documentación ejecutable E1 anuncia sólo lo implementado: versiones del mismo contrato, no dos APIs manuales.

### 9.3 Operaciones de M3

**17 rutas, 25 operaciones y 27 esquemas**, detallados en el YAML. Prefijo funcional: `/api/v1`.

**Conductores, vehículos y documentación**

| Ruta relativa                                                      | Métodos y acceso                                | Entrega |
| ------------------------------------------------------------------ | ----------------------------------------------- | ------- |
| `/drivers`                                                         | POST propio; GET operador, paginado.            | E1      |
| `/me/driver`                                                       | GET conductor propio.                           | E1      |
| `/drivers/{driverId}`                                              | GET propietario/operador.                       | E1      |
| `/drivers/{driverId}/vehicles`                                     | POST propietario; GET propietario/operador.     | E1      |
| `/drivers/{driverId}/vehicles/{vehicleId}`                         | GET propietario/operador.                       | E1      |
| `/drivers/{driverId}/documents`                                    | POST licencia propia; GET historial autorizado. | E1      |
| `/drivers/{driverId}/vehicles/{vehicleId}/documents`               | POST propio; GET historial autorizado.          | E1      |
| `/drivers/{driverId}/documents/{documentId}/reviews`               | POST revisión por operador.                     | E1      |
| `/drivers/{driverId}/qualification-decisions`                      | POST operador; GET propietario/operador.        | E1      |
| `/drivers/{driverId}/vehicles/{vehicleId}/qualification-decisions` | POST operador; GET historial autorizado.        | E1      |

**Disponibilidad, integraciones y valoraciones**

| Ruta relativa                                 | Métodos y acceso                                              | Entrega |
| --------------------------------------------- | ------------------------------------------------------------- | ------- |
| `/drivers/{driverId}/availability`            | PUT propietario; GET propietario/operador.                    | E1/E2   |
| `/integration/drivers/{driverId}/eligibility` | GET servicio con scope específico.                            | E1      |
| `/integration/eligibility/batch`              | POST consulta, servicio autorizado, hasta 100.                | E1      |
| `/integration/drivers/{driverId}/summary`     | GET servicio autorizado, datos mínimos.                       | E1      |
| `/drivers/{driverId}/customer-ratings`        | POST conductor del viaje; GET propietario/operador, paginado. | E2      |

**Salud**

| Ruta sin prefijo | Acceso                           | Entrega |
| ---------------- | -------------------------------- | ------- |
| `/health/live`   | GET mínimo público: proceso.     | E1      |
| `/health/ready`  | GET mínimo público: preparación. | E1      |

Operador incluye Administrador autorizado. No agregar bajas destructivas ni edición libre de campos.

### 9.4 Formato de errores

REST: `application/problem+json`/RFC 9457 mediante `ProblemDetail`, separado de errores HTML. Requeridos: `type`, `title`, `status`, `code`, `correlationId`; opcionales `detail`, `instance` y errores por campo, sin datos sensibles. [P]

`400` formato; `401` token; `403` permiso; `404` inexistente/no visible; `409` conflicto; `412` versión; `422` negocio; `428` precondición; `429` límite; `503` dependencia indispensable. **Servicio caído no equivale a conductor no habilitado.**

### 9.5 Interfaces para compartir desde E1-00

Compartir IDs, nombre visible cuando corresponda, vehículo/servicio, elegibilidad, motivos codificados, fecha/versión. No licencia, póliza, domicilio, identidad interna Keycloak ni motivos privados: `INSURANCE_EXPIRED`, no número de seguro.

| ID         | Quién llama → atiende        | Interfaz                                                        | Implementación        |
| ---------- | ---------------------------- | --------------------------------------------------------------- | --------------------- |
| **INT-01** | M3/usuarios ↔ M1             | OIDC/JWT, clientes, claves, audiencias/permisos.                | E1                    |
| **INT-02** | M4/M5 → M3                   | GET elegibilidad individual, `vehicleId`/`serviceType`.         | E1                    |
| **INT-03** | M4/M5 → M3                   | POST elegibilidad batch, 1–100 candidatos.                      | E1                    |
| **INT-04** | M6 → M3                      | GET resumen mínimo de conductor.                                | Proveedor M3 en E1    |
| **INT-05** | M3 → M4                      | `PUT /api/v1/drivers/{driverId}/availability-intent`.           | Primer intercambio E1 |
| **INT-06** | M3 → M6                      | `GET /api/v1/trips/{tripId}/rating-context`, filtro `driverId`. | Real en E2            |
| **INT-07** | M3 → RabbitMQ → consumidores | Catálogo del apartado 10.2.                                     | E2                    |

**Todas se definen al inicio.** Rutas completas INT-02/03/04: apartado 9.3 más prefijo `/api/v1`. M6 puede consultar elegibilidad con autorización, sin asumir despacho; otros consumidores requieren acuerdo.

Material: [guía](M3_Interfaces_Integracion.md), [propuesta M4](contracts/dependencies/m4-availability.openapi.yaml), [propuesta M6](contracts/dependencies/m6-rating-context.openapi.yaml), [eventos](contracts/events.catalog.yaml). M4/M6 deben aprobar sus borradores; M1 aporta endpoints reales. No añadir llamada M2 si M6 ya aporta el cliente: resumen M2 por evento, si se acuerda. Suspensión/asignación sigue pendiente según 7.4.

### 9.6 Validación y evolución del contrato

Registrar estado, proveedor/consumidores, versión/commit y aprobación. CI revisa sintaxis, referencias, ejemplos y respuestas reales. Configurar Jackson/Bean Validation para campos obligatorios, `additionalProperties:false`, nulabilidad, enums/límites y errores acordados. Tipos generados y extensiones `x-...` no reemplazan validación/seguridad.

Coordinar campos opcionales; cambios de significado, eliminación o enums incompatibles requieren revisar versión con consumidores. Un mock no acredita integración.

---

## 10. Sincronización, eventos y Redis

### 10.1 Disponibilidad: persistir primero, comunicar después

```text
Validar → transacción: intención + auditoría + outbox + idempotencia
        → 202 → worker HTTP a M4 → confirmación commandId/versión
        → guardar versión aplicada
```

Sin commit PostgreSQL no hay `202`. M4 caído: `PENDING`/`RETRYING`, luego `FAILED` recuperable. `APPLIED` sólo para versión reconocida. M4 compara `desiredVersion`, deduplica `commandId` y revalida habilitación, ubicación/ocupación: un comando viejo no reconecta al desconectado.

Este comando usa HTTP, **no también RabbitMQ**. Cambiar canal requiere acuerdo/confirmación. Worker `@Scheduled` con RestClient y UnitOfWork en el mismo proceso; con varios ejecutores, lease/bloqueo durable porque el scheduler no garantiza unicidad.

### 10.2 Eventos propuestos para E2

Catálogo desde E1-00; implementación E2. [events.catalog.yaml](contracts/events.catalog.yaml) es borrador, no API HTTP ni AsyncAPI.

| Evento                          | Consumidor/propuesta                                                    |
| ------------------------------- | ----------------------------------------------------------------------- |
| `m3.qualification.changed.v1`   | M4/M5: invalidar y revisar ofertas.                                     |
| `m3.document.expired.v1`        | M4/M5: excluir/revalidar; no sustituye revisar reloj.                   |
| `m3.customer-rating.created.v1` | M2, sólo si acuerda recibir resumen.                                    |
| Viaje completado M6             | Consumo M3 opcional para precargar, sin eliminar verificación acordada. |

Sobre: `eventId`, `eventType`, `schemaVersion`, `occurredAt`, `producer`, `aggregateId`, `aggregateVersion`, `correlationId`, `payload` mínimo. Sin documentos/motivos privados. Una aprobación por evento exige revalidar; no activa disponibilidad sola.

### 10.3 Publicación y consumo con RabbitMQ

Spring AMQP/RabbitTemplate; evento y cambio juntos en PostgreSQL. Mensajes persistentes, topología durable, publisher confirms y control de no enrutados. Confirmación del broker no acredita consumo: entrega **al menos una vez**, efectos idempotentes.

Consumidor real guarda inbox/efecto en una transacción y luego ACK; deduplica `(consumer,event_id)` y compara versión en proyecciones. Invalidaciones provocan reconsulta; futuros deltas requieren detectar huecos/reconciliar. No crear consumidores sin necesidad.

### 10.4 Reintentos y recuperación

Propuesta: **1, 5, 30 segundos; 2 y 5 minutos**, con variación aleatoria; luego DLQ/estado fallido. Corregir causa antes de reprocesar, sin reencolado infinito. Vigilar cantidad/antigüedad de outbox y limitar operaciones según umbral acordado.

### 10.5 Redis: uso concreto y caída controlada

E2: limitador por principal/clase, incremento/TTL atómicos y claves derivadas sin datos personales. `RateLimitStorePort` se implementa en Persistencia; Seguridad no abre conexiones.

Redis no guarda verdad del negocio ni aprobación cacheada para despacho. Caída: límite local conservador para lecturas y degradación visible; operaciones privilegiadas pueden devolver `503` si requieren el control. Perder contadores reinicia ventanas. Acordar/probar política y aceptación de este uso para RNF-11.

---

## 11. Docker, configuración y operación

### 11.1 Entorno de ejecución por entrega

**Docker Compose es el arranque oficial de E1/E2, no una alternativa.** Entregar sólo el JAR o ejecutar Spring por separado no basta.

| Elemento           | Ejecución                                                        | Entrega                            |
| ------------------ | ---------------------------------------------------------------- | ---------------------------------- |
| **M3**             | Un contenedor: Spring Boot + API + Thymeleaf/recursos.           | E1/E2                              |
| **PostgreSQL**     | Contenedor separado y volumen persistente de M3.                 | E1/E2                              |
| **M1/Keycloak**    | Contenedor independiente local o compartido, sin tablas comunes. | Acceso real E1                     |
| **RabbitMQ/Redis** | Contenedores separados, locales o compartidos.                   | E2                                 |
| **M4/M5/M6**       | Servicios de otros equipos, URLs configurables.                  | M4 real E1; los tres en pruebas E2 |

`compose.yaml` define M3, PostgreSQL, configuración y persistencia; se amplía en E2. No duplicar servicios compartidos en cada notebook ni crear contenedor frontend. Dobles sólo para desarrollo/pruebas, no evidencia distribuida.

### 11.2 Imagen Docker y empaquetado

Dockerfile multietapa: Maven/JDK compila y JRE ejecuta `java -jar`; usuario no root, `.dockerignore`, imágenes/versiones fijadas, sin `latest`/SNAPSHOT de entrega. Incluir plantillas/Bootstrap local.

Flyway migra controladamente antes de servir; sin migraciones simultáneas. `spring.jpa.hibernate.ddl-auto=validate` y `spring.jpa.open-in-view=false`: consultas/mapeo terminan en servicio/transacción, no en plantillas. Seeds sintéticos/idempotentes por entorno; probar volumen, reinicios y copia/restauración. [S6]

### 11.3 Workers y configuración por entorno

Workers en M3, tareas acotadas y cierre ordenado; varios ejecutores requieren lease/bloqueo. URLs, puertos, credenciales, TTL y timeouts externos: cambiar entorno no modifica negocio.

```dotenv
SERVER_PORT=3003
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/m3_db
SPRING_DATASOURCE_USERNAME=<usuario-m3>
SPRING_DATASOURCE_PASSWORD=<secreto-inyectado>
OIDC_ISSUER=<issuer-exacto-del-realm>
OIDC_JWKS_URI=<jwks-del-mismo-realm-si-se-configura-separado>
OIDC_AUDIENCE=<audiencia-api-m3>
OIDC_WEB_CLIENT_ID=<cliente-web-aprobado>
OIDC_WEB_CLIENT_SECRET=<secreto-web-inyectado>
OIDC_SERVICE_CLIENT_ID=<cliente-tecnico-del-grupo>
OIDC_SERVICE_CLIENT_SECRET=<secreto-tecnico-inyectado>
M4_BASE_URL=<m4>
M6_BASE_URL=<m6>
RABBITMQ_URL=<broker-acordado>
REDIS_URL=<redis-m3>
INTEGRATION_MODE=real
```

Enlazar propiedades propias en `application.yml`; usar `spring.security.oauth2.resourceserver.jwt.issuer-uri`/`audiences`, `spring.security.oauth2.client.provider.keycloak.issuer-uri` y registros web/técnico separados. Discovery aporta endpoints; sobrescribir sólo con acuerdo M1.

Hostname M1 alcanzable desde navegador/contenedor, **sin desactivar validación del issuer**. Aceptar URLs/callbacks de proxy confiable, no `Forwarded` arbitrario. No copiar secretos/scopes ajenos. Borrar volúmenes o `docker system prune` no es arranque/migración normal.

### 11.4 Arranque oficial con Docker y comandos de apoyo

Comandos **previstos para el proyecto implementado**: este paquete no contiene Dockerfile/Compose ejecutables ni acredita su prueba.

```bash
docker compose up --build            # Arranque oficial E1/E2
```

Con configuración y dependencias accesibles, abrir `http://localhost:3003/ui`. M1/M4 reales deben estar preparados.

```bash
./mvnw verify                        # Pruebas, contrato y arquitectura
./mvnw package                       # JAR con backend y web
```

Maven apoya desarrollo/build/CI, no reemplaza Docker en la entrega. DevTools y caché de plantillas desactivada sólo en perfil local.

### 11.5 Límites de espera y comportamiento ante fallos

| Dependencia      | Propuesta inicial, ajustable                                                                  |
| ---------------- | --------------------------------------------------------------------------------------------- |
| **HTTP negocio** | Presupuesto 5 s por operación.                                                                |
| **M6**           | 2 s/intento y un reintento seguro dentro de 5 s. Sin contexto: `503`, sin valoración.         |
| **M1**           | Token técnico 2 s; reutilizar válido/renovar antes de vencer. No omitir seguridad ante fallo. |
| **M4**           | 2 s/intento, reintentos programados fuera de la petición. Conservar pendiente.                |
| **PostgreSQL**   | Transacción inicialmente 3 s, bloqueo acotado. Caída: `503`/readiness no disponible.          |
| **RabbitMQ**     | Conservar outbox; continuidad local según backlog/política.                                   |
| **Redis**        | Degradación del apartado 10.5, sin pérdida persistente.                                       |

Cancelar realmente HTTP/consultas; dejar de esperar un Future no basta. Timeout no prueba que la escritura remota no ocurrió: repetir con la misma identidad. Reintentar sólo fallos transitorios, no `403`/`422` automáticamente.

### 11.6 Logs, auditoría y salud

**Logs:** fecha, nivel, servicio/versión, correlación, ruta parametrizada, estado, duración/código. Validar/generar correlación y propagarla; sin tokens, cuerpos completos, números documentales o queries sensibles. IDs de negocio sólo necesarios/autorizados.

**Auditoría:** actor, objeto, acción, motivo, transición, versión/fecha persistidos; stdout no la reemplaza. Cuenta de aplicación sin borrado ordinario del historial y rol separado para mantenimiento/migraciones.

Adaptar Actuator a `/health/live` y `/health/ready` con JSON contractual: proceso frente a preparación, especialmente PostgreSQL/configuración. Diagnóstico detallado protegido; M6/Redis caídos no justifican reiniciar todo M3. Medir tiempos/errores, autenticación, dependencias, outbox, reintentos/DLQ sin etiquetas ilimitadas por ID.

---

## 12. Plan de las dos entregas

### 12.1 E1 · Persistencia, seguridad, interfaz y primer intercambio

**Primero E1-00; Docker/PostgreSQL en paralelo.** Luego completar login M1 → alta de perfil → PostgreSQL → consulta web, y el mismo caso por API. Ampliar funciones/pantallas sin dejar frontend para el final. Borradores no aprobados sólo como simulaciones identificadas; reservar el cierre para pruebas/demo, sin fechas inventadas.

| ID                  | Entregable                                                                                             | Responsables                          |
| ------------------- | ------------------------------------------------------------------------------------------------------ | ------------------------------------- |
| **E1-00 — Primero** | API M3, propuestas M4/M6, parámetros M1, eventos E2, ejemplos, revisión bilateral y casos de contrato. | **A1/B4/A4**; todos revisan su parte. |
| E1-01               | Java/Maven, paquetes, Dockerfile/Compose, volumen y configuración externa.                             | B5/B1                                 |
| E1-02               | Puertos, entidades/migraciones, restricciones y transacciones.                                         | B1/B2/B3; revisión A                  |
| E1-03               | Login/sesión M1, JWT, roles/propiedad y CSRF.                                                          | B4/A5                                 |
| E1-04               | Crear/consultar conductor persistente.                                                                 | A1/B2                                 |
| E1-05               | Auto/Moto, varios vehículos, servicios y patente única.                                                | A2/B2                                 |
| E1-06               | Documentos versionados, vencimientos y revisión.                                                       | A3/B3                                 |
| E1-07               | Decisiones administrativas, historial y versión.                                                       | A3/B1                                 |
| E1-08               | Elegibilidad individual/batch y aptitud web.                                                           | A4/B2                                 |
| E1-09               | Intención durable, worker y M4 real.                                                                   | A4/B4/B5                              |
| E1-10               | Verificar implementación contra E1-00; mantener contrato/Swagger y acuerdos.                           | A1/B4 y autores                       |
| E1-11               | RF-3.7 con doble M6, no integrado.                                                                     | A1/B3/B4                              |
| **E1-12**           | **Web: perfil, vehículos, documentos, habilitación, disponibilidad y operador.**                       | **A5**, con casos de A1–A4            |
| E1-13               | Pruebas backend/web, errores, logs, salud y CI.                                                        | Todos; B5 coordina                    |

Auditoría, idempotencia, versiones y restricciones desde E1, también en formularios. Outbox HTTP no requiere RabbitMQ todavía.

**Demo:** arrancar Compose/comprobar M1/M4 → conductor crea perfil/vehículo/documentos → operador revisa/aprueba → conductor pide disponibilidad y ve confirmación M4 → reiniciar/autenticarse y recuperar datos → suspender/vencer documentación y comprobar bloqueo. Separar sesiones de conductor/operador.

Además: rechazar token inválido, acceso ajeno, patente repetida, POST sin CSRF y versión vieja; M4 caído se muestra pendiente/fallo, no disponibilidad confirmada.

**Cierre de E1**

```text
[] E1-00 compartido al inicio; acuerdos/versiones registrados y diseño E2 separado de release E1.
[] Checkout limpio: Compose arranca M3/PostgreSQL; imagen/configuración/volumen y reinicios probados.
[] Web real, M1, roles/logout/CSRF y funciones persistentes; API sigue usando Bearer.
[] M4 real demostrado, o evidencia parcial declarada; sin promesa global RF-3.5 no acordada.
[] Pruebas/contrato pasan; README, release y migraciones reflejan lo implementado.
```

### 12.2 E2 · M3 completo e integración

| ID    | Entregable                                                              | Responsables         |
| ----- | ----------------------------------------------------------------------- | -------------------- |
| E2-01 | M4/M5 consumen elegibilidad y respetan suspensión/vencimiento.          | A4/B4                |
| E2-02 | Política suspensión/asignación acordada y probada.                      | A4/B1 + M4/M5        |
| E2-03 | Valoración con M6 real, sin duplicados.                                 | A1/B3/B4             |
| E2-04 | RabbitMQ: publicación, confirmaciones y reintentos.                     | B5/B3                |
| E2-05 | Deduplicación/orden en consumidores reales.                             | B1/B5 + consumidores |
| E2-06 | Job de vencimientos y propagación.                                      | A3/A4/B5             |
| E2-07 | Redis/TTL y caída controlada; datos detrás de Persistencia.             | B5/B4                |
| E2-08 | Web de valoraciones y recuperación visible.                             | **A5**; casos A1/A4  |
| E2-09 | Pruebas distribuidas, simultaneidad, duplicados, reinicios y navegador. | Todos                |
| E2-10 | Docker/Compose, CI, RabbitMQ/Redis, restauración y medición local.      | B5/B1/B2/A4          |
| E2-11 | Privacidad, auditoría, permisos y usabilidad M3.                        | A1/A3/B4/A5          |
| E2-12 | Contratos, evidencias y demo conjunta.                                  | A1 y ambos equipos   |

**Demo conjunta:** M1 autentica → M3 conductor apto → M4 intención/ubicación → M5 revalida/asigna → M6 completa → M3 verifica y guarda una valoración. No reconstruir funciones/pantallas ajenas.

**Cierre de E2**

```text
[] RF-3.1–RF-3.7 con web e integraciones reales M4/M5/M6; garantía RF-3.5 acordada.
[] Caídas, duplicados, versiones antiguas y reinicios tratados correctamente.
[] Docker/Compose E2 reproducible; RabbitMQ/Redis, permisos, auditoría y restauración probados.
[] Usabilidad/performance M3 medidas; obligaciones globales/TP3 registradas aparte.
```

### 12.3 Meta local de rendimiento a acordar y medir

Propuesta, no resultado: elegibilidad individual **p95 ≤ 300 ms**, error inesperado **< 1 %**, **20 solicitudes/s durante 60 s**, **15 s** de calentamiento y **1.000** conductores sintéticos.

Registrar CPU/RAM, JVM/heap, versiones, red, datos, concurrencia/herramienta y token precargado. Batch de 100 y renderizado web se miden aparte; no contar 401/422 esperados como errores inesperados. No reemplaza la meta conjunta RNF-16.

---

## 13. Requisitos no funcionales

Etapas originales [E]. **Implementación y evidencia pendientes**, no cumplimiento por instalar bibliotecas.

### 13.1 Base técnica y arquitectura · RNF-01 a RNF-06

| Requisito                             | Etapa fuente | Aplicación y evidencia                                                                    |
| ------------------------------------- | ------------ | ----------------------------------------------------------------------------------------- |
| **RNF-01 — Libertad tecnológica**     | TP1          | Justificación Java/Spring, versiones, arranque y aprobación.                              |
| **RNF-02 — Arquitectura modular**     | TP1          | Límites/diagramas/imports; subequipos no reemplazan implementaciones independientes.      |
| **RNF-03 — Contratos**                | TP1          | E1-00 primero: OpenAPI, errores, dependencias, eventos, acuerdos y pruebas.               |
| **RNF-04 — Contenedores**             | TP1          | Docker/Compose desde E1; imágenes versionadas, checkout limpio y persistencia comprobada. |
| **RNF-05 — Propiedad de datos**       | TP1–TP2      | Base propia y Persistencia única; permisos/imports sin tablas ajenas.                     |
| **RNF-06 — Configuración y secretos** | TP1          | Entorno validado, ejemplos sin secretos, escaneo y cambio de configuración.               |

### 13.2 Pruebas, consistencia e integración · RNF-07 a RNF-11

| Requisito                        | Etapa fuente | Aplicación y evidencia                                                          |
| -------------------------------- | ------------ | ------------------------------------------------------------------------------- |
| **RNF-07 — Pruebas**             | TP1–TP3      | Locales/web E1; distribuidas E2; E2E de plataforma. Reportes CI.                |
| **RNF-08 — Concurrencia**        | TP1–TP3      | Restricciones/versiones/transacciones; simultaneidad local y acuerdos globales. |
| **RNF-09 — Idempotencia**        | TP2          | Base E1: claves, IDs, outbox/inbox; replays/reinicios sin duplicación.          |
| **RNF-10 — Mensajería**          | TP2          | RabbitMQ E2 en flujos acordados; confirmaciones y recuperación.                 |
| **RNF-11 — Caché y vencimiento** | TP2          | Redis/limitador propuesto: aprobar uso, probar TTL, atomicidad y caída.         |

### 13.3 Seguridad, operación y calidad · RNF-12 a RNF-16

| Requisito                        | Etapa fuente | Aplicación y evidencia                                                                         |
| -------------------------------- | ------------ | ---------------------------------------------------------------------------------------------- |
| **RNF-12 — Seguridad**           | TP2–TP3      | Desde E1: JWT, OIDC/sesión/CSRF, propiedad y validación. Casos negativos/HTTPS publicado.      |
| **RNF-13 — Resiliencia**         | TP2–TP3      | Timeouts, reintentos limitados y estados pendientes; probar indisponibilidad.                  |
| **RNF-14 — Salud y diagnóstico** | TP2–TP3      | Health, logs, correlación/backlog; base E1, cierre E2.                                         |
| **RNF-15 — Usabilidad**          | TP3          | Web M3 adelantada a E1: formularios, errores, teclado/móvil. No certifica plataforma completa. |
| **RNF-16 — Performance**         | TP3          | Medición M3 y recorrido crítico conjunto; distinguir meta de resultado.                        |

### 13.4 Entrega y mantenimiento · RNF-17 a RNF-20

| Requisito                           | Etapa fuente | Aplicación y evidencia                                                                                   |
| ----------------------------------- | ------------ | -------------------------------------------------------------------------------------------------------- |
| **RNF-17 — Automatización**         | TP2–TP3      | Build/pruebas desde E1; publicación de imágenes recomendada E2.                                          |
| **RNF-18 — Portabilidad**           | TP3          | Configuración/imagen OCI; comprobar local y segundo entorno acordado.                                    |
| **RNF-19 — Privacidad y auditoría** | TP3          | Base E1: mínimos datos, historial/permisos; inspeccionar logs/URLs y decisiones.                         |
| **RNF-20 — Documentación**          | TP1–TP3      | README, diagramas, contratos, decisiones/procedimientos versionados; demo reproducible por otra persona. |

---

## 14. Pruebas y trabajo diario

### 14.1 Pruebas mínimas

| Nivel                  | Casos esenciales                                                                                                                       |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------------------------- |
| **Dominio/aplicación** | Suspensión, vencimiento límite, licencia incompatible, transiciones, desconexión, permisos, M6 con otro conductor, idempotencia/reloj. |
| **PostgreSQL real**    | Unicidad, coherencia documento/vehículo/conductor, revisión actual, rollback auditoría/outbox, versiones y escrituras simultáneas.     |
| **HTTP/contrato**      | Token/issuer/audiencia, persona frente a servicio, acceso ajeno, campos/tamaños, errores, batch/paginación y release.                  |
| **Web/navegador**      | Login/logout, roles, CSRF, versión vieja, doble envío, recuperación, móvil y plantillas sin SQL lazy; API rechaza sesión web.          |
| **Distribuidas**       | M4/M6/broker caídos, recuperación, duplicados/orden, caída tras commit o antes de ACK, suspensión durante despacho.                    |
| **Carga/operación**    | Latencia, errores, N+1, pool DB, backlog, restauración y arranque.                                                                     |

JUnit/Mockito aíslan lógica; MockMvc/Security Test prueban HTTP/formularios; Testcontainers usa PostgreSQL real. SQLite no prueba sus restricciones/bloqueos; MockMvc no reemplaza navegador/login real y los dobles no acreditan integración. [S9]

### 14.2 Ramas y revisión de cambios

Ramas pequeñas por función y nombres técnicos en inglés: `feature/m3-driver-profile`, `feature/m3-vehicle-registration`, `feature/m3-qualification-workflow`, `feature/m3-keycloak-security`, `feature/m3-availability-sync`, `feature/m3-customer-ratings`, `feature/m3-web-ui`.

PR con objetivo, impacto contractual/migraciones, pruebas/riesgos; revisión cruzada al cambiar puertos. `main` demostrable, sin builds rotos ni migraciones sin probar.

### 14.3 Cuándo se considera terminada una tarea

Requisito identificado, contrato coherente, caso de uso/persistencia y pantalla cuando corresponda; permisos/validación, auditoría/versiones/idempotencia aplicables, pruebas CI, documentación y merge sin secretos/bypass. `docs/delivery-1.md` y `delivery-2.md` llevan checklist `[]`/`[*]` arriba y evidencias debajo.

### 14.4 Evidencias de cada entrega

Guardar commit, imagen, contrato, migración, datos sintéticos y reporte. Video no reemplaza instrucciones reproducibles; instalar una biblioteca no demuestra cumplir un requisito.

---

## 15. Acuerdos pendientes

| Tema                      | Quién confirma / pendiente                                                                        |
| ------------------------- | ------------------------------------------------------------------------------------------------- |
| **Entregas/organización** | Cátedra: E1/E2 frente a TP1–TP3 e implementaciones independientes.                                |
| **Stack**                 | Equipo/cátedra: Java/Spring, versiones, compatibilidad JPA/Flyway.                                |
| **M1**                    | M1/integrador: realm, clientes, audiencias, roles/scopes, usuarios, callback/logout y revocación. |
| **Documentos/estados**    | Cátedra/analistas: requisitos por servicio, renovación, zona horaria y transiciones.              |
| **Disponibilidad**        | M3/M4/M5: intención/estado efectivo, versiones, confirmación y fallos.                            |
| **Suspensión/asignación** | M3/M4/M5/cátedra: garantía base o admisión coordinada estricta.                                   |
| **Valoraciones**          | M3/M6: contexto, estabilidad/rectificaciones del viaje completado.                                |
| **Eventos**               | Integrador/consumidores: nombres, schemas, topología, permisos, propietarios/reintentos.          |
| **Redis**                 | Integrador/cátedra: uso efímero propuesto y degradación.                                          |
| **UI/TP3**                | A5: web M3 E1; consorcio/cátedra: cliente global, E2E, performance/publicación.                   |

**Se trabajan desde E1-00**, sin bloquear Docker, dominio, puertos, persistencia o pruebas independientes. B1 controla migraciones; A1 coherencia API/YAML; A5 web acotada; B4 identidad/red. Outbox/versiones controlan pérdidas/desorden y las evidencias distinguen garantías locales/globales.

---

## Anexo. Referencias y control de revisiones

### A.1 Origen del contenido

**[E] Enunciado:** `Texto pegado(1).txt`, Desarrollo de Software 2026: interfaces (61), límites (75–94), entregas (97), RF M3 (114–122), RNF (187–209) y grupos (210–225).

**[P] Base:** arquitectura/contrato resumidos 0.1.1. **[J] Cambio Java/web 0.2.0:** stack, seguridad diferenciada y reparto A1/A5. El ejemplo 2025 aporta formato OpenAPI/M1, no requisitos funcionales de 2026.

Esta versión resume **`M3_Arquitectura_TPI_Java_Web.md` 0.2.3**. No actualiza tecnologías ni agrega requisitos; conserva decisiones y acuerdos pendientes. Las fuentes siguientes vienen del documento base, sin nueva verificación externa en esta edición.

### A.2 Referencias técnicas conservadas

| Ref.   | Documentación                                                                                         |
| ------ | ----------------------------------------------------------------------------------------------------- |
| **S1** | [Spring Boot 4.0: requisitos][S1].                                                                    |
| **S2** | [Spring MVC y Thymeleaf][S2].                                                                         |
| **S3** | [OAuth2 Login][S3] · [Cadenas de seguridad][S3-chains].                                               |
| **S4** | [CSRF/formularios][S4].                                                                               |
| **S5** | [JWT/issuer/audiencia][S5] · [OIDC Keycloak][S5-keycloak].                                            |
| **S6** | [Bloqueo JPA][S6] · [TransactionTemplate][S6-tx] · [Flyway][S6-flyway] · [Propiedades JPA][S6-props]. |
| **S7** | [RestClient][S7].                                                                                     |
| **S8** | [springdoc/YAML externo][S8].                                                                         |
| **S9** | [Testcontainers PostgreSQL][S9].                                                                      |

### A.3 Historial de cambios

| Versión            | Cambio                                                             |
| ------------------ | ------------------------------------------------------------------ |
| **0.1.1**          | Base resumida TypeScript.                                          |
| **0.2.0**          | Java, web E1 y redistribución A1/A5.                               |
| **0.2.1**          | Índice, secciones y tablas reorganizados.                          |
| **0.2.2**          | Docker/Compose como ejecución oficial E1.                          |
| **0.2.3**          | Diagramas, interfaces primero, guía y borradores M4/M6.            |
| **0.2.4 · actual** | Resumen editorial con la misma estructura y contratos sin cambios. |

### A.4 Contrato y comprobaciones previas

M3 YAML 0.2.0 mantiene 17 rutas, 25 operaciones y 27 esquemas. Las revisiones previas registran comprobaciones estructurales; **no acreditan una aplicación funcionando ni una validación integral OpenAPI**. Siguen pendientes pruebas reales de Java, migraciones, Docker, navegador/M1, integraciones y rendimiento.

### A.5 Comprobación de la revisión editorial

Conservar secciones, RF/RNF, entregables/responsables y enlaces. Sólo A1–A5/B1–B5 identifican personas. La reducción afecta explicaciones y repeticiones, no el alcance de implementación.

### A.6 Comprobación de la aclaración sobre Docker

Se mantiene Docker/Compose obligatorio en alcance, responsabilidades B5, arranque, RNF-04 y cierre E1/E2. No se entrega infraestructura implementada.

### A.7 Revisión de diagramas e interfaces iniciales

Imágenes, fuentes de diagramas, guía, contratos y ejemplos se conservan. M4/M6 y eventos siguen como propuestas: su aprobación bilateral e implementación permanecen pendientes.

> **Resultado esperado:** compartir interfaces al comenzar; demostrar M3 persistente y protegido desde navegador/Docker en E1; completar integraciones en E2. Una lógica compartida, una entrada a datos y un responsable frontend.

[E]: #a1-origen-del-contenido
[P]: #a1-origen-del-contenido
[J]: #a1-origen-del-contenido
[S1]: https://docs.spring.io/spring-boot/4.0/system-requirements.html
[S2]: https://spring.io/guides/gs/serving-web-content/
[S3]: https://docs.spring.io/spring-security/reference/servlet/oauth2/login/core.html
[S3-chains]: https://docs.spring.io/spring-security/reference/servlet/configuration/java.html
[S4]: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
[S5]: https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html
[S5-keycloak]: https://www.keycloak.org/securing-apps/oidc-layers
[S6]: https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html
[S6-tx]: https://docs.spring.io/spring-framework/reference/data-access/transaction/programmatic.html
[S6-flyway]: https://docs.spring.io/spring-boot/how-to/data-initialization.html
[S6-props]: https://docs.spring.io/spring-boot/appendix/application-properties/index.html
[S7]: https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
[S8]: https://springdoc.org/
[S9]: https://java.testcontainers.org/modules/databases/postgres/
