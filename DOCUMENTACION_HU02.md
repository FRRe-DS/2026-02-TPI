# Documentación del Microservicio M3 — Conductores y Vehículos
## Implementación de la Historia de Usuario 2 (HU-02)

**Proyecto:** Trabajo Práctico Integrador (TPI) Desarrollo de Software 2026 — UTN FRRe  
**Módulo:** M3 — Conductores y Vehículos  
**Historia de Usuario:** HU-02: *Consultar mi perfil*  
**Autor:** Lautaro González  

---

## 1. ¿De qué se trata este desarrollo?

En este proyecto nos tocó desarrollar el **Módulo 3 (M3)**, cuyo objetivo es gestionar a los conductores y sus vehículos dentro de la plataforma estilo Uber/Cabify. 

La funcionalidad específica desarrollada en esta entrega es la **Historia de Usuario 2 (HU-02)**:
> *"Como conductor, quiero consultar mi perfil y mi estado actual, para verificar mis datos personales, mis calificaciones y saber en qué situación operativa me encuentro (si estoy habilitado para manejar, si estoy observado, suspendido o pendiente de aprobación)."*

El sistema permite consultar esta información tanto desde una **pantalla visual en el navegador web** (interfaz gráfica) como a través de una **API REST** (para que aplicaciones móviles u otros módulos puedan consultar los datos en formato JSON).

---

## 2. ¿Cómo está organizada la arquitectura? (Arquitectura Hexagonal)

Para cumplir con las pautas de la cátedra, implementamos una **Arquitectura Hexagonal (Ports & Adapters)**. Esta arquitectura divide el código en capas independientes para que las reglas del negocio no dependan de la tecnología ni de la base de datos:

```
[ Navegador Web (HTML) ]   [ App Móvil / REST (JSON) ]
           \                         /
            v                       v
      +-----------------------------------+
      |        ADAPTADORES DE ENTRADA     |
      |   - DriverProfileWebController    |
      |   - DriverQueryRestController     |
      +-----------------------------------+
                        |
                        v
      +-----------------------------------+
      |         CAPA DE APLICACIÓN        |
      |   - GetDriverProfileService       |
      |   - Casos de Uso (Ports In)       |
      +-----------------------------------+
                        |
                        v
      +-----------------------------------+
      |          CAPA DE DOMINIO          |
      |   (Java puro sin frameworks)      |
      |   - Driver, PersonalInfo, Status  |
      +-----------------------------------+
                        |
                        v
      +-----------------------------------+
      |        ADAPTADORES DE SALIDA      |
      |   - DriverPersistenceAdapter      |
      |   - SecurityCurrentPrincipal      |
      +-----------------------------------+
                        |
                        v
            [ Base de Datos PostgreSQL ]
```

### Detalle de cada capa:

1. **Dominio (`domain`)**:
   - Es el "corazón" del sistema. Está escrito en **Java puro** (sin anotaciones de Spring, sin Hibernate, sin dependencias de base de datos ni de web).
   - Modela al Conductor (`Driver`), sus datos personales (`PersonalInfo`), su información de calificación (`QualificationInfo`) y los posibles estados (`DriverStatus`: `PENDING`, `APPROVED`, `OBSERVED`, `SUSPENDED`, `REJECTED`).
   - Contiene la lógica propia del negocio, por ejemplo verificar si el conductor está efectivamente habilitado para prestar servicios.

2. **Aplicación (`application`)**:
   - Define las acciones que el sistema puede realizar mediante **Casos de Uso**:
     - `GetMyDriverProfileUseCase`: Obtener el perfil del conductor actualmente autenticado.
     - `GetDriverProfileUseCase`: Obtener el perfil de un conductor específico mediante su identificador único (UUID).
   - Usa **Puertos (Ports)**: Interfaces que indican qué necesita el caso de uso sin importar cómo se implemente (ej: `DriverRepositoryPort` para consultar la base de datos).
   - Maneja excepciones de negocio (ej: si el conductor no existe, lanza `DriverNotFoundException`).

3. **Adaptadores (`adapter`)**:
   - Son los "enchufes" que conectan el sistema con el exterior:
     - **Web UI (`adapter/web`)**: Controlador Spring MVC (`DriverProfileWebController`) que procesa las solicitudes web y renderiza la pantalla gráfica usando plantillas HTML de **Thymeleaf**.
     - **REST (`adapter/rest`)**: Controlador (`DriverQueryRestController`) que expone los endpoints HTTP devolviendo respuestas en JSON con encabezados `ETag` para el control de versiones (RNF-08). También incluye un manejador centralizado de errores (`RestExceptionHandler`) que devuelve los fallos en formato estándar RFC 9457 `ProblemDetail`.
     - **Persistencia (`adapter/persistence`)**: Conecta con la base de datos relacional usando Spring Data JPA y mapea las entidades de la base de datos (`DriverJpaEntity`) hacia las clases puras de Dominio.
     - **Seguridad (`adapter/security`)**: Gestiona la autenticación con Spring Security y extrae la identidad del usuario logueado (`sub` de OAuth2/OIDC).

4. **Configuración (`config`)**:
   - Conecta todas las piezas mediante clases de configuración de Spring (`BeanConfiguration`), manteniendo el dominio y la aplicación 100% libres de anotaciones intrusivas.
   - Contiene un inicializador de datos de prueba (`DataInitializer`) que precarga conductores en la base de datos para poder probar la aplicación sin tener que cargar datos a mano.

---

## 3. Base de Datos y Persistencia

- **Base de datos propia (RNF-05)**: M3 cuenta con su propia base de datos **PostgreSQL 16** (`m3_db`), garantizando que ningún otro microservicio comparta tablas ni claves foráneas directas.
- **Identidad externa**: Cada conductor tiene un atributo `identity_subject` que referencia al identificador que le asigna el Módulo 1 (Autenticación), sin romper la independencia de datos.
- **Migraciones con Flyway**: Se creó el script `V1__create_drivers_table.sql` que crea automáticamente la tabla `drivers` con sus tipos de datos, restricciones e índices al iniciar la aplicación.
- **Versionado y concurrencia (RNF-08)**: Cada registro posee una columna `version` que se incrementa ante cambios para evitar colisiones y permitir cacheo condicional.

---

## 4. Pantalla Web para el Conductor (Thymeleaf + Bootstrap 5)

Se desarrolló una interfaz web limpia, moderna y adaptable a celulares y computadoras:
- **Ruta de acceso**: `http://localhost:3003/ui/profile`
- **Componentes visuales**:
  - **Barra de navegación**: Con logo del sistema, menú y perfil activo.
  - **Banner de Estado Operativo**: Muestra de forma destacada el estado del conductor con colores intuitivos (verde para *Aprobado*, amarillo para *Observado*, rojo para *Suspendido*). Si hay observaciones o sanciones, se muestra el motivo exacto en pantalla.
  - **Tarjeta de Datos Personales**: Nombre, apellido, DNI, fecha de nacimiento, correo, teléfono y domicilio declarado.
  - **Avatar dinámico**: Muestra las iniciales del conductor de forma elegante.
  - **Panel de Información Técnica y Auditoría**: Muestra el UUID de M3, el identificador de identidad de M1, la versión de registro (`v1`), y las fechas de alta y modificación.

---

## 5. Endpoints de la API REST

Para consultar los datos de forma programática (JSON), se implementaron los siguientes servicios:

| Método | Endpoint | Descripción | Formato Respuesta |
| :---: | :--- | :--- | :---: |
| **GET** | `/api/v1/me/driver` | Consulta el perfil del conductor en sesión | JSON + ETag |
| **GET** | `/api/v1/drivers/{driverId}` | Consulta el perfil de cualquier conductor por su UUID | JSON + ETag |
| **GET** | `/health/live` | Indicador de que el contenedor está vivo (Liveness) | `{"status":"UP"}` |
| **GET** | `/health/ready` | Indicador de que la base de datos está lista (Readiness) | `{"status":"UP"}` |
| **GET** | `/swagger-ui/index.html` | Documentación interactiva de la API (OpenAPI) | Swagger UI |

> **Manejo de Errores RFC 9457:** Si se consulta un conductor inexistente (ej: `/api/v1/drivers/00000000-0000-0000-0000-000000000000`), la API responde con código `404 Not Found` en formato `application/problem+json` con título, detalle y timestamp.

---

## 6. Conductores Precargados para Pruebas (Seeds)

Al iniciar el sistema en Docker, se precargan automáticamente 4 conductores con diferentes realidades para poder probar todos los casos de uso:

1. **Lautaro González (Habilitado / Aprobado)**:
   - **UUID**: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`
   - **Estado**: `APPROVED`
   - **Situación**: Listo para tomar viajes. Es el usuario por defecto que se muestra en la web.
2. **María Fernández (En Revisión)**:
   - **UUID**: `b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22`
   - **Estado**: `PENDING`
   - **Situación**: Documentación en proceso de verificación.
3. **Carlos Benítez (Con Observaciones)**:
   - **UUID**: `c2eebc99-9c0b-4ef8-bb6d-6bb9bd380a33`
   - **Estado**: `OBSERVED`
   - **Situación**: Registro de conducir próximo a vencer. Requiere actualizar documentación.
4. **Ana Martínez (Suspendida)**:
   - **UUID**: `d3eebc99-9c0b-4ef8-bb6d-6bb9bd380a44`
   - **Estado**: `SUSPENDED`
   - **Situación**: Sancionada temporalmente por reclamos reiterados de usuarios.

---

## 7. Pruebas Automatizadas (Testing)

Se implementaron **24 pruebas automatizadas** que garantizan la calidad del software:
- **Tests de Dominio**: Verifican que las reglas de negocio (estados, iniciales de avatar, aprobaciones) funcionen sin dependencias externas.
- **Tests de Integración REST (MockMvc)**: Prueban los endpoints de la API, verificando códigos HTTP 200, cabeceras `ETag`, y formato `ProblemDetail` ante errores 404.
- **Tests de Integración Web (MockMvc)**: Prueban que el controlador de Thymeleaf arme el modelo correcto y cargue la vista `profile.html`.
- **Tests de Persistencia (JPA)**: Prueban guardar y recuperar conductores en la base de datos real.
- **Tests de Arquitectura (ArchUnit)**: Valida de manera estricta que la capa de Dominio no importe paquetes de Spring ni de persistencia, respetando la Arquitectura Hexagonal.

---

## 8. Despliegue con Docker y Docker Compose

Se configuró el entorno para que cualquier persona pueda levantar el microservicio completo con un solo comando:
- **`Dockerfile`**: Compila el proyecto en Java 21 y genera una imagen liviana optimizada sobre Alpine Linux (`clonuber/m3-drivers-vehicles:0.2.0`).
- **`compose.yaml`**: Levanta de forma coordinada:
  1. `m3_postgres`: Contenedor de PostgreSQL 16 con volumen persistente en disco.
  2. `m3_drivers_vehicles`: Contenedor de la aplicación Spring Boot conectado a la base de datos y exponiendo el puerto `3003`.

### Comandos de ejecución:
- **Levantar el sistema en segundo plano**:
  ```powershell
  docker compose up -d
  ```
- **Ver el estado de los contenedores**:
  ```powershell
  docker compose ps
  ```
- **Detener el sistema**:
  ```powershell
  docker compose down
  ```
