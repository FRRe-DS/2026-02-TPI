# Guía Completa del Proyecto M3 para Principiantes
## Cómo funciona todo, qué hace cada archivo y cómo trabajar en equipo

¡Hola Lautaro! Esta guía está redactada especialmente para vos y tu equipo. Aunque no tengas experiencia previa programando ni trabajando con contenedores o microservicios, si tenés buen sentido común tecnológico vas a entender perfectamente cómo está armado este sistema, cómo funciona por dentro, y cómo podés seguir construyendo sobre él junto a tus compañeros sin romper nada.

---

## Índice Rápido
1. [Los Conceptos Clave en Palabras Simples](#1-los-conceptos-clave-en-palabras-simples)
2. [El Gran Mapa del Proyecto: Qué hay en cada carpeta y qué hace cada archivo](#2-el-gran-mapa-del-proyecto-qué-hay-en-cada-carpeta-y-qué-hace-cada-archivo)
3. [¿Cómo hicimos funcionar todo? (El Paso a Paso Detallado)](#3-cómo-hicimos-funcionar-todo-el-paso-a-paso-detallado)
4. [¿Cómo fusionar tu Historia de Usuario (HU-02) con las de tus compañeros sin romper nada?](#4-cómo-fusionar-tu-historia-de-usuario-hu-02-con-las-de-tus-compañeros-sin-romper-nada)
5. [¿Cómo usar y comunicarse con los Microservicios de otros equipos (M1, M4, M6)?](#5-cómo-usar-y-comunicarse-con-los-microservicios-de-otros-equipos-m1-m4-m6)
6. [Diccionario de Supervivencia Tecnológica](#6-diccionario-de-supervivencia-tecnológica)
7. [Comandos Rápidos para Probar Todo](#7-comandos-rápidos-para-probar-todo)

---

## 1. Los Conceptos Clave en Palabras Simples

Antes de mirar los archivos, entendamos las cuatro ideas fundamentales de este sistema:

### A. ¿Qué es un "Microservicio"?
* **La analogía del Shopping:** En una aplicación tradicional antigua (llamada *monolito*), todo el shopping es un único salón gigante: si se corta la luz en la carnicería, se apaga todo el shopping. En una arquitectura de **microservicios**, cada local es independiente:
  * El local de Identidad y Usuarios es **M1**.
  * El local de Pasajeros es **M2**.
  * Nuestro local es **M3 (Conductores y Vehículos)**.
  * El local de Viajes y Asignación es **M4**.
  * El local de Calificaciones es **M6**.
* Si el local M4 se cae o está en mantenimiento, M3 sigue funcionando porque tiene su propio espacio y sus propios datos.

### B. ¿Qué es un "Contenedor" (Docker)?
* Imaginate que para que un programa funcione necesitás instalar Java 21, configurar PostgreSQL, configurar variables de sistema, etc. Si se lo pasás a un amigo, seguro le sale el típico error: *"En mi máquina funcionaba pero en la tuya no"*.
* Un **contenedor** es como una caja precintada (como un contenedor de barco) que lleva adentro el programa y todo lo que necesita para correr de manera idéntica en Windows, Mac o Linux.
* **Docker** es el software que levanta esa caja.
* [compose.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/compose.yaml) es el director que levanta dos cajas juntas:
  1. La caja de la **Base de Datos** (PostgreSQL).
  2. La caja de **Nuestra Aplicación** (Spring Boot con Java).

### C. ¿Qué es la "Arquitectura Hexagonal"? (Puertos y Adaptadores)
* Pensá en tu computadora o en tu celular: tiene puertos USB. Vos podés enchufarle un mouse con cable, un mouse bluetooth o un teclado. El procesador de tu computadora no tiene idea de qué marca de mouse tenés enchufada; él solo sabe que por el puerto USB le llegan clics.
* En nuestro código hacemos lo mismo:
  * El **Dominio** es el cerebro (las reglas de qué es un chofer y si puede manejar).
  * Los **Puertos** son las ranuras USB.
  * Los **Adaptadores** son los enchufes: hay un enchufe para la Web (HTML), otro enchufe para la API REST (JSON) y otro enchufe para guardar datos en la Base de Datos (PostgreSQL).
  * Si mañana la cátedra dice *"Ya no usen PostgreSQL, usen MongoDB"*, el cerebro del programa no cambia; solo cambiamos el enchufe (adaptador).

---

## 2. El Gran Mapa del Proyecto: Qué hay en cada carpeta y qué hace cada archivo

A continuación tenés la lista explicada de absolutamente todo lo que hay en el proyecto. Podés hacer clic en cualquiera de los enlaces para abrir el archivo directamente.

### 📁 Raíz del Proyecto (Archivos de Configuración General)
* [pom.xml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/pom.xml): Es la "lista de compras" del proyecto Java (Maven). Acá le decimos qué librerías necesitamos (Spring Boot, Hibernate/JPA, Thymeleaf para las páginas web, PostgreSQL, herramientas de pruebas, etc.).
* [Dockerfile](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/Dockerfile): Es la "receta de cocina" para fabricar la imagen Docker de M3. Compila el código Java en una primera etapa y luego crea una imagen liviana para ejecutarlo.
* [compose.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/compose.yaml): El archivo de orquestación. Define que al escribir `docker compose up` se deben crear dos contenedores: uno con la base de datos PostgreSQL (`m3_postgres`) y otro con nuestra app (`m3_drivers_vehicles`), conectándolos por una red interna.
* [.env.example](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/.env.example): Un archivo de texto de ejemplo que muestra qué contraseñas o variables secretas se pueden configurar en el sistema.
* [.gitignore](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/.gitignore): Le avisa a Git qué cosas NO debe subir a GitHub (por ejemplo, carpetas temporales de compilación como `/target` o archivos propios de tu editor).
* [.dockerignore](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/.dockerignore): Le dice a Docker qué archivos pesados o innecesarios no debe copiar dentro de la imagen.
* [mvnw](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/mvnw) y [mvnw.cmd](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/mvnw.cmd): El "Maven Wrapper". Son pequeños scripts (uno para Linux/Mac y otro para Windows) que descargan y ejecutan la versión exacta de Maven sin que tengas que instalar nada raro en tu computadora.
* [README.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/README.md): La portada oficial del repositorio, con la descripción general, badges, comandos de inicio e información técnica.
* [AGENTS.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/AGENTS.md): Reglas estrictas de arquitectura destinadas a asistentes de IA y desarrolladores para mantener la limpieza del código.
* [DOCUMENTACION_HU02.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/DOCUMENTACION_HU02.md): Documento técnico detallado que explica la entrega de la Historia de Usuario 2 (Consultar mi perfil).

---

### 📁 Carpeta `contracts/` (Los Contratos con Otros Equipos)
En un sistema de microservicios, antes de programar se acuerda cómo se van a hablar los módulos. Esos acuerdos se llaman "contratos":
* [contracts/openapi.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/contracts/openapi.yaml): El "menú del restaurante" de M3. Especifica qué endpoints ofrece M3 al mundo (por ejemplo `GET /api/v1/me/driver` y `GET /api/v1/drivers/{id}`) y qué datos responde.
* [contracts/events.catalog.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/contracts/events.catalog.yaml): El catálogo de noticias/eventos que M3 puede emitir (por ejemplo: *"Se aprobó un conductor"* o *"Se suspendió un conductor"*).
* [contracts/dependencies/m4-availability.openapi.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/contracts/dependencies/m4-availability.openapi.yaml): La copia del menú de M4 (Disponibilidad), para saber qué endpoints tiene ese otro módulo cuando necesitemos hablarle.
* [contracts/dependencies/m6-rating-context.openapi.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/contracts/dependencies/m6-rating-context.openapi.yaml): La copia del menú de M6 (Calificaciones), para saber cómo pedirle el promedio de estrellas de un conductor.

---

### 📁 Carpeta `docs/` (Documentación del Proyecto)
* [docs/delivery-1.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/docs/delivery-1.md): El checklist y resumen de entrega de la Entrega 1 (E1).
* [docs/runbook.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/docs/runbook.md): La guía de operación rápida para levantar, probar y verificar el sistema.
* [docs/adr/001-hexagonal-architecture.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/docs/adr/001-hexagonal-architecture.md): Documento de decisión arquitectónica (ADR) que fundamenta por qué se eligió la arquitectura hexagonal.
* **[docs/EXPLICACION_PROYECTO.md](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/docs/EXPLICACION_PROYECTO.md)**: Este mismo documento que estás leyendo.

---

### 📁 Carpeta de Código Fuente: `src/main/java/ar/edu/utn/frre/m3/`

Acá está la magia de la programación. Está organizado en capas según la Arquitectura Hexagonal:

#### 1. Archivo Principal (El botón de encendido)
* [M3Application.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/M3Application.java): Tiene el método `main`. Al ejecutar este archivo, arranca Spring Boot, se conecta a la base de datos y pone a escuchar el servidor en el puerto `3003`.

#### 2. Capa `domain/` (El Corazón del Negocio — Java Puro)
*Esta capa no sabe qué es Spring, ni qué es una base de datos, ni qué es una página web. Solo modela la realidad del negocio.*
* [Driver.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/domain/Driver.java): Representa al Conductor. Guarda su ID, su estado actual, si tiene notas del operador, su versión y sus datos. Contiene reglas lógicas como `isEligibleToDrive()` (que devuelve si puede o no salir a manejar).
* [DriverStatus.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/domain/DriverStatus.java): La lista de estados posibles de un conductor:
  * `PENDING`: Se registró pero aún nadie revisó sus papeles.
  * `APPROVED`: Papeles aprobados, puede trabajar.
  * `OBSERVED`: Algún documento tiene problemas (ej: foto borrosa) y debe corregirlo.
  * `SUSPENDED`: Sancionado temporalmente por reclamos o infracciones.
  * `REJECTED`: Rechazado definitivamente.
* [PersonalInfo.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/domain/PersonalInfo.java): Agrupa los datos personales del chofer: nombre, apellido, DNI, fecha de nacimiento, email, teléfono y dirección.
* [QualificationInfo.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/domain/QualificationInfo.java): Agrupa la información de desempeño: calificación promedio (ej: 4.9 estrellas), viajes completados y fecha de ingreso.

#### 3. Capa `application/` (Los Casos de Uso y los Puertos)
*Esta capa dice "qué cosas puede hacer el sistema", coordinando el dominio con los puertos.*
* **Puertos de Entrada (Ports In)**: Las acciones que alguien de afuera puede pedirle a la aplicación.
  * [GetDriverProfileUseCase.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/port/in/GetDriverProfileUseCase.java): Interfaz que dice: *"Doy la funcionalidad de buscar el perfil de cualquier chofer por su ID"*.
  * [GetMyDriverProfileUseCase.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/port/in/GetMyDriverProfileUseCase.java): Interfaz que dice: *"Doy la funcionalidad de buscar el perfil del chofer que está logueado en este momento"*.
* **Puertos de Salida (Ports Out)**: Cosas que la aplicación necesita pedirle al exterior para cumplir su tarea.
  * [DriverRepositoryPort.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/port/out/DriverRepositoryPort.java): Ranura que dice: *"Necesito que alguien me busque un conductor por su ID o por su identidad de usuario en alguna base de datos"*.
  * [CurrentPrincipalPort.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/port/out/CurrentPrincipalPort.java): Ranura que dice: *"Necesito saber quién es la persona que está sentada frente a la pantalla en esta sesión"*.
  * [ClockPort.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/port/out/ClockPort.java): Ranura que dice: *"Necesito saber qué hora y fecha es actualmente"*.
* **Servicio (Caso de Uso)**:
  * [GetDriverProfileService.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/usecase/GetDriverProfileService.java): Es el empleado que hace el trabajo: toma el pedido de consultar el perfil, llama al repositorio para buscar los datos y, si no existe el chofer, levanta una alarma.
* **Excepciones (Errores de Negocio)**:
  * [DriverNotFoundException.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/exception/DriverNotFoundException.java): El error que salta si se busca un conductor que no existe en el sistema.
  * [DriverAccessDeniedException.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/exception/DriverAccessDeniedException.java): El error que salta si alguien intenta espiar el perfil de otro conductor sin tener permisos de administrador.

#### 4. Capa `adapter/` (Los Enchufes al Mundo Real)
*Acá se programan las tecnologías concretas que conectan con los puertos.*

* **Subcarpeta `web/` (Para usuarios que entran por el navegador):**
  * [DriverProfileWebController.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/web/DriverProfileWebController.java): Atiende cuando alguien entra a `http://localhost:3003/ui/profile`. Llama al caso de uso, transforma los datos a un formato amigable para la vista y le pasa los datos a la plantilla HTML.
  * [DriverProfileViewModel.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/web/DriverProfileViewModel.java): Un objeto empaquetador especial para la pantalla. Contiene los textos listos para mostrar, los colores de los cartelitos (verde para aprobado, rojo para suspendido) y las iniciales del chofer para su foto de perfil.
* **Subcarpeta `rest/` (Para apps móviles u otros programas que piden datos en formato JSON):**
  * [DriverQueryRestController.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/rest/DriverQueryRestController.java): Atiende peticiones HTTP en `/api/v1/me/driver` y `/api/v1/drivers/{id}`. Devuelve los datos en formato JSON y agrega la cabecera `ETag` (para control de concurrencia RNF-08).
  * [HealthRestController.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/rest/HealthRestController.java): Responde en `/health/live` y `/health/ready` si el sistema está vivo y si la base de datos está respondiendo bien.
  * [DriverProfileResponse.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/rest/DriverProfileResponse.java) y [PersonalInfoResponse.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/rest/PersonalInfoResponse.java): Moldes que definen exactamente la forma que tendrá el JSON que viaja por internet hacia quien consultó la API.
  * [RestExceptionHandler.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/rest/RestExceptionHandler.java): Atrapa cualquier error que ocurra en la API y lo transforma en un mensaje de error prolijo y estándar (formato RFC 9457 `ProblemDetail`).
* **Subcarpeta `persistence/` (Para hablar con la Base de Datos PostgreSQL):**
  * [DriverJpaEntity.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/persistence/DriverJpaEntity.java): Es la representación exacta de la tabla `drivers` en la base de datos relacional (con sus columnas `id`, `first_name`, `status`, `version`, etc.).
  * [SpringDataDriverRepository.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/persistence/SpringDataDriverRepository.java): Una interfaz de Spring Data JPA que genera automáticamente las consultas SQL sin que tengamos que escribirlas a mano.
  * [DriverMapper.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/persistence/DriverMapper.java): El "traductor". Pasa un `DriverJpaEntity` (objeto de base de datos) a un `Driver` (objeto de dominio puro) y viceversa.
  * [DriverPersistenceAdapter.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/persistence/DriverPersistenceAdapter.java): Conecta el puerto [DriverRepositoryPort](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/port/out/DriverRepositoryPort.java) con la base de datos real.
* **Subcarpeta `security/` (La Seguridad del Sistema):**
  * [SecurityConfig.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/security/SecurityConfig.java): Configura las reglas de acceso: qué URLs son públicas (como `/ui/**`, `/health/**`, Swagger) y cómo se protegen las APIs.
  * [SecurityCurrentPrincipalAdapter.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/security/SecurityCurrentPrincipalAdapter.java): Obtiene el usuario que está navegando en la sesión actual.
  * [PrincipalContextHolder.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/security/PrincipalContextHolder.java): Permite cambiar fácilmente de usuario en la pantalla de demostración para probar los diferentes estados de prueba con un solo clic.
* **Subcarpetas `integration/` y `messaging/`:**
  * Preparadas para conectar en las próximas entregas con las APIs de otros microservicios (mediante clientes HTTP) o con colas de mensajería (como RabbitMQ o Kafka).

#### 5. Capa `config/` (El Ensamblador)
* [ApplicationServiceConfig.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/config/ApplicationServiceConfig.java): Le dice a Spring: *"Tomá este repositorio de base de datos y enchufáselo al servicio [GetDriverProfileService](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/usecase/GetDriverProfileService.java)"*. Esto permite que el servicio no tenga anotaciones raras de Spring y se mantenga limpio e independiente.
* [ClockConfig.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/config/ClockConfig.java): Configura el reloj del sistema.
* [DataInitializer.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/config/DataInitializer.java): Cuando arranca la app por primera vez, este archivo inserta automáticamente 4 conductores de prueba sintéticos con diferentes estados (Aprobado, Pendiente, Observado, Suspendido) para que podamos ver la pantalla con datos reales de inmediato sin tener que cargar nada a mano.

---

### 📁 Carpeta de Recursos: `src/main/resources/`
* [application.yml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/application.yml): Configuración general de Spring Boot (nombre de la aplicación, puerto 3003, configuración de Flyway, etc.).
* [application-local.yml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/application-local.yml): Ajustes que se usan cuando corrés la app localmente en tu computadora.
* [application-test.yml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/application-test.yml): Ajustes que se usan cuando se ejecutan los tests automáticos (usa una base de datos en memoria ultrarrápida H2).
* [db/migration/V1__create_drivers_table.sql](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/db/migration/V1__create_drivers_table.sql): Script SQL administrado por Flyway. Al iniciar, Flyway ejecuta este archivo y crea la tabla `drivers` en PostgreSQL con todas sus columnas, tipos de datos y claves primarias.
* [static/css/style.css](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/static/css/style.css): Hoja de estilos con el diseño visual: paleta de colores, sombras suaves, bordes redondeados y badges para cada estado del chofer.
* [templates/profile.html](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/templates/profile.html): La pantalla web del perfil (HTML + Thymeleaf + Bootstrap 5). Contiene la barra de navegación, la botonera de prueba de estados, el banner con el estado actual, las tarjetas con los datos personales y el panel de auditoría técnica.
* [templates/error.html](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/templates/error.html): Pantalla de error amigable por si alguien ingresa una dirección que no existe o busca un conductor inexistente.

---

### 📁 Carpeta de Pruebas Automatizadas: `src/test/java/`
Para estar 100% seguros de que todo funciona y que ningún cambio rompe nada, tenemos una batería de pruebas automatizadas:
* [DriverTest.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/test/java/ar/edu/utn/frre/m3/domain/DriverTest.java): Prueba las reglas lógicas del chofer (por ejemplo, que solo un chofer en estado `APPROVED` pueda manejar).
* [GetDriverProfileServiceTest.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/test/java/ar/edu/utn/frre/m3/application/usecase/GetDriverProfileServiceTest.java): Prueba el caso de uso con objetos simulados (Mocks).
* [DriverPersistenceAdapterTest.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/test/java/ar/edu/utn/frre/m3/adapter/persistence/DriverPersistenceAdapterTest.java): Prueba que los datos se guarden y lean correctamente de la base de datos.
* [DriverQueryRestControllerTest.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/test/java/ar/edu/utn/frre/m3/adapter/rest/DriverQueryRestControllerTest.java): Simula peticiones HTTP a la API REST y verifica que devuelvan el código 200 OK, el formato JSON y el header `ETag`.
* [DriverProfileWebControllerTest.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/test/java/ar/edu/utn/frre/m3/adapter/web/DriverProfileWebControllerTest.java): Simula la navegación web y comprueba que la pantalla HTML cargue sin fallas.
* [HexagonalArchitectureTest.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/test/java/ar/edu/utn/frre/m3/architecture/HexagonalArchitectureTest.java): Un test de arquitectura estricto (con ArchUnit) que analiza el código y garantiza que ninguna regla de la arquitectura hexagonal haya sido violada (por ejemplo, que nadie haya importado Hibernate en el dominio).

---

## 3. ¿Cómo hicimos funcionar todo? (El Paso a Paso Detallado)

Si tuvieras que contarle a alguien cómo se construyó este microservicio desde cero, este fue el camino:

```
Paso 1: Contratos (OpenAPI)
         │
         ▼
Paso 2: Dominio en Java Puro (Driver, PersonalInfo, Status)
         │
         ▼
Paso 3: Base de Datos con Flyway (V1__create_drivers_table.sql)
         │
         ▼
Paso 4: Casos de Uso y Puertos (application/port y application/usecase)
         │
         ▼
Paso 5: Adaptadores de Salida (Persistencia JPA + PostgreSQL)
         │
         ▼
Paso 6: Adaptadores de Entrada (API REST + Controlador Web Thymeleaf)
         │
         ▼
Paso 7: Interfaz Visual (HTML5 + Bootstrap 5 + CSS personalizado)
         │
         ▼
Paso 8: Empaquetado en Contenedores (Dockerfile + compose.yaml)
```

1. **Definimos las reglas del negocio:** Creamos las clases en [domain/](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/domain/) que definen qué datos tiene un chofer y cuáles son sus estados (`APPROVED`, `PENDING`, `OBSERVED`, etc.).
2. **Creamos la tabla de la base de datos:** Escribimos el script SQL [V1__create_drivers_table.sql](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/db/migration/V1__create_drivers_table.sql). Usamos una herramienta llamada **Flyway**, que es como un "control de versiones para bases de datos": cuando la app arranca, revisa si la tabla existe; si no existe, la crea sola.
3. **Escribimos los casos de uso:** Creamos el servicio [GetDriverProfileService.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/application/usecase/GetDriverProfileService.java) que busca al conductor y maneja qué pasa si existe o si no existe.
4. **Conectamos con la base de datos real:** En [adapter/persistence/](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/persistence/) implementamos el adaptador que ejecuta las consultas en PostgreSQL y devuelve los datos limpios al caso de uso.
5. **Creamos las dos formas de consultar la información:**
   * **Para computadoras y celulares:** El controlador web [DriverProfileWebController.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/web/DriverProfileWebController.java) que genera la vista HTML [profile.html](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/templates/profile.html).
   * **Para sistemas externos:** El controlador REST [DriverQueryRestController.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/adapter/rest/DriverQueryRestController.java) que devuelve el JSON con el encabezado `ETag`.
6. **Cargamos datos de demostración:** Creamos [DataInitializer.java](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/java/ar/edu/utn/frre/m3/config/DataInitializer.java) para que al prender la aplicación ya tengamos conductores de ejemplo sin tener que cargarlos a mano.
7. **Empaquetamos todo con Docker:** Creamos el [Dockerfile](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/Dockerfile) y el [compose.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/compose.yaml) para que cualquiera pueda prender la base de datos y la aplicación con un único comando: `docker compose up --build`.

---

## 4. ¿Cómo fusionar tu Historia de Usuario (HU-02) con las de tus compañeros sin romper nada?

Esta es una de las dudas más frecuentes cuando se trabaja en equipo. Si vos hiciste **HU-02 (Consultar perfil)** y tus compañeros están haciendo, por ejemplo:
* **HU-01:** Registrar nuevo conductor y subir documentación.
* **HU-03:** Registrar vehículo y asociarlo al conductor.
* **HU-04:** Consultar y gestionar vehículos habilitados.

¿Cómo hacen para unir todo en el mismo proyecto sin pisarse ni romper la interfaz gráfica (UI) que ya hiciste?

### A. La Regla de Oro de Git: Ramas y Pull Requests
* **Nunca trabajen todos directo sobre la rama `main`:** Cada integrante debe crear su propia "rama" (branch) a partir de `main`. Por ejemplo:
  * Tu rama: `feature/hu-02-consultar-perfil`
  * La de tu compañero: `feature/hu-03-registrar-vehiculo`
* Cuando un compañero termina su parte, abre un **Pull Request (PR)** para revisar el código antes de fusionarlo a `main`.

### B. ¿Cómo no chocar en la Interfaz Gráfica (Web UI)?
Para que nadie rompa la pantalla que ya hiciste, sigan estas recomendaciones súper sencillas:

1. **Cada Historia de Usuario debe tener su propia plantilla HTML:**
   * Vos tenés [profile.html](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/templates/profile.html) (para ver el perfil).
   * Tu compañero que haga vehículos debe crear un archivo nuevo, por ejemplo `vehicles.html` o `register-vehicle.html`, en la misma carpeta `templates/`.
   * **¡Nunca deben meter todo en un solo HTML gigante!** Tener archivos separados evita cualquier conflicto de edición.
2. **Cómo integrar la Barra de Navegación (`navbar`) sin pisarse:**
   * Si abrís [profile.html](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/templates/profile.html#L27-L48), vas a ver que ya dejamos preparados los botones de navegación con la clase `disabled`:
     ```html
     <li class="nav-item">
         <a class="nav-link active" th:href="@{/ui/profile}">Mi Perfil (HU-02)</a>
     </li>
     <li class="nav-item">
         <a class="nav-link disabled" href="#">Mis Vehículos</a>
     </li>
     <li class="nav-item">
         <a class="nav-link disabled" href="#">Documentación</a>
     </li>
     ```
   * Cuando tu compañero termine la pantalla de vehículos, lo único que tiene que hacer en la barra de navegación es quitarle el `disabled` y ponerle el enlace a su nueva pantalla: `th:href="@{/ui/vehicles}"`.
3. **No chocar en los estilos CSS ([style.css](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/static/css/style.css)):**
   * En vez de modificar estilos genéricos, usen prefijos en los nombres de las clases.
   * Por ejemplo: las clases de tu perfil empiezan con `.profile-*` (como `.profile-card`, `.profile-avatar`).
   * Tu compañero de vehículos debe nombrar sus clases con `.vehicle-*` (como `.vehicle-card`, `.vehicle-badge`). Así nunca se pisarán los estilos entre sí.

### C. ¿Cómo no chocar en la Base de Datos? (La Magia de Flyway)
* En las bases de datos comunes suele haber peleas porque dos personas cambian la misma tabla al mismo tiempo.
* Con **Flyway** eso no pasa si siguen una regla muy simple:
  * **REGLA SAGRADA:** **¡Nunca se modifica un archivo de migración que ya existe!** El archivo [V1__create_drivers_table.sql](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/src/main/resources/db/migration/V1__create_drivers_table.sql) ya no se toca más.
  * Si tu compañero necesita crear la tabla de vehículos, crea un archivo nuevo llamado:  
    `V2__create_vehicles_table.sql`
  * Si otro necesita agregar columnas para documentos:  
    `V3__create_documents_table.sql`
* Cuando el programa arranca, Flyway lee los números en orden: primero ejecuta `V1`, después `V2`, después `V3`. Todo se crea en orden y sin ningún error.

### D. ¿Cómo no chocar en el Código Java?
Gracias a la **Arquitectura Hexagonal**, el código está dividido en piezas independientes:
* Tu compañero de vehículos agregará:
  * Una clase `Vehicle.java` en `domain/`.
  * Sus propios casos de uso en `application/port/in/` (ej: `RegisterVehicleUseCase.java`).
  * Su propio controlador en `adapter/web/` (ej: `VehicleWebController.java`).
  * Su propia entidad en `adapter/persistence/` (ej: `VehicleJpaEntity.java`).
* Al ser archivos nuevos, Git puede unir las ramas de manera 100% automática sin ningún conflicto de código.

---

## 5. ¿Cómo usar y comunicarse con los Microservicios de otros equipos (M1, M4, M6)?

En este trabajo práctico, diferentes equipos o integrantes implementan diferentes microservicios (módulos). Por ejemplo:
* **M1:** Autenticación y Cuentas de Usuario.
* **M4:** Asignación de viajes y Disponibilidad de choferes.
* **M6:** Calificaciones y Reputación.

¿Cómo se conecta M3 con ellos según las reglas de la cátedra?

```
               +-----------------------------------+
               |       M1 (Identidad / Login)      |
               +-----------------------------------+
                                 │
                   (Token JWT con identity_subject)
                                 ▼
+-----------------------------------------------------------------+
|                    M3 (Conductores y Vehículos)                 |
|                                                                 |
|   [ adapter/integration/ ]              [ adapter/messaging/ ]  |
|     (Llamadas REST HTTP)                  (Publicar Eventos)    |
+-----------------------------------------------------------------+
        │                                         │
        │ GET /api/v1/drivers/{id}/ratings        │ Evento:
        ▼ (Preguntar reputación)                  ▼ DriverApprovedEvent
+-----------------------------------+   +-----------------------------------+
|        M6 (Calificaciones)        |   |         M4 (Disponibilidad)       |
+-----------------------------------+   +-----------------------------------+
```

### Regla 1: Cada Microservicio es dueño absoluto de su Base de Datos (RNF-05)
* **Nunca se hace una conexión directa a la base de datos de otro módulo.** M3 no puede hacer un `SELECT` a las tablas de M1 ni de M4.
* Las bases de datos están totalmente separadas. Si M3 necesita saber algo de M6, se lo tiene que pedir amablemente a través de su API o por eventos.

### Regla 2: ¿Cómo sabemos de qué usuario estamos hablando? (El "DNI" común)
* Cuando una persona se registra en la plataforma, **M1** le asigna un identificador único de identidad llamado `identity_subject` (un código como `user-sub-123`).
* Cuando ese usuario entra a M3, M3 guarda en su tabla `drivers` una columna llamada `identity_subject`.
* No necesitamos saber su contraseña ni nada privado de M1: solo guardamos esa referencia para saber a qué cuenta pertenece este chofer.

### Regla 3: Comunicación Directa por HTTP (Peticiones REST)
Si M3 necesita información inmediata de otro módulo (por ejemplo, consultar la calificación acumulada en M6):
1. **Miramos el contrato:** Vamos a [contracts/dependencies/m6-rating-context.openapi.yaml](file:///c:/Users/Lauta/Documents/Lautaro/Facultad/desarrollo/contracts/dependencies/m6-rating-context.openapi.yaml) y leemos qué endpoint tiene M6 para pedir calificaciones.
2. **Creamos un Puerto en M3:** En `application/port/out/` creamos una interfaz (ej: `RatingClientPort.java`) con el método `getRatingForDriver(UUID driverId)`.
3. **Creamos el Adaptador en `adapter/integration/`:** Creamos una clase (ej: `M6RatingRestAdapter.java`) que usa el cliente HTTP de Spring (`RestClient` o `WebClient`) para hacer un `GET` a la URL donde esté corriendo M6 (por ejemplo `http://m6-service:3006/api/v1/...`).
4. **Ventaja:** Nuestro caso de uso solo llama a `RatingClientPort`. Si M6 cambia de servidor o de puerto, solo tocamos el adaptador de integración, nada más.

### Regla 4: Comunicación por Eventos (Notificaciones)
A veces no queremos esperar una respuesta, sino solo avisar que algo pasó. Por ejemplo:
* Cuando un chofer es aprobado en M3, M3 emite un evento: `DriverApprovedEvent`.
* **M4 (Disponibilidad)** escucha ese evento en segundo plano y ya sabe que ese chofer puede encender su botón de "Disponible para recibir viajes".
* Para esto está reservada la carpeta `adapter/messaging/`: allí se conectan las librerías de mensajería (RabbitMQ, Kafka, etc.) para publicar o recibir esos mensajes.

---

## 6. Diccionario de Supervivencia Tecnológica

Una lista rápida de términos que vas a escuchar todo el tiempo, explicados en una sola oración clara:

| Término | ¿Qué significa en palabras simples? |
| :--- | :--- |
| **API REST** | Una forma estándar en la que dos programas se comunican por internet intercambiando datos en texto. |
| **Endpoint** | La dirección URL específica a la que se le hace una petición (por ejemplo `/api/v1/me/driver`). |
| **JSON** | Formato de texto con llaves `{ "nombre": "Lautaro" }` que usan las computadoras para pasarse datos ordenados. |
| **DTO / Response** | Una clase sencilla que sirve como paquete o caja de envío para mandar datos por JSON hacia afuera. |
| **Entity (JPA)** | Una clase de Java que representa una fila exacta dentro de una tabla de la base de datos PostgreSQL. |
| **UUID** | Un código identificador largo y único en el mundo (ej: `a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11`). |
| **ETag** | Una etiqueta de versión que viaja en la respuesta HTTP para evitar que dos personas editen lo mismo a la vez. |
| **Thymeleaf** | El motor de plantillas de Java que toma una página HTML y le reemplaza variables con datos reales antes de enviarla al navegador. |
| **Bootstrap 5** | Una librería de diseño web con estilos listos para que los botones, tarjetas y grillas se vean modernos y adaptados a celulares. |
| **Flyway** | La herramienta que crea y actualiza las tablas de la base de datos automáticamente mediante scripts numerados (`V1`, `V2`, etc.). |
| **Docker Compose** | El archivo que enciende varios contenedores a la vez (por ejemplo, nuestra app y la base de datos juntas) con un solo comando. |
| **Mock** | Un objeto falso que se usa en las pruebas automatizadas para simular que la base de datos responde sin tener que encenderla de verdad. |
| **ArchUnit** | Una herramienta de test que analiza las carpetas de Java y da error si alguien rompe las reglas de la arquitectura hexagonal. |

---

## 7. Comandos Rápidos para Probar Todo

Cuando quieras encender el proyecto para mostrárselo a tus compañeros o al profesor, tenés dos opciones:

### Opción 1: Con Docker Compose (La forma oficial y recomendada)
Abrí una terminal (PowerShell) en la carpeta del proyecto y ejecutá:
```powershell
docker compose up --build
```
* Esto compila todo, levanta PostgreSQL y levanta la aplicación en el puerto `3003`.
* Para apagarlo cuando termines: `docker compose down`.

### Opción 2: Con Maven en tu máquina (Sin Docker)
Si tenés Java 21 instalado en tu computadora:
```powershell
.\mvnw.cmd spring-boot:run
```

---

### ¿Dónde hacés clic una vez que arrancó?
* **Para ver la pantalla web del perfil (HU-02):**  
  👉 [http://localhost:3003/ui/profile](http://localhost:3003/ui/profile)
  *(En esa pantalla tenés una botonera arriba para probar al instante cómo se ve un conductor Aprobado, Pendiente, Observado o Suspendido).*
* **Para ver la documentación interactiva Swagger (API REST):**  
  👉 [http://localhost:3003/swagger-ui/index.html](http://localhost:3003/swagger-ui/index.html)
* **Para verificar que el sistema esté saludable (Health Check):**  
  👉 [http://localhost:3003/health/ready](http://localhost:3003/health/ready)

---

### Conclusión y Consejo para el Trabajo en Equipo
Todo el proyecto fue diseñado con piezas separadas e independientes a propósito. Nadie tiene por qué pisarle el trabajo a nadie:
1. Cada uno trabaja en su propia **rama de Git**.
2. Cada nueva pantalla web es un **archivo HTML nuevo** en `templates/`.
3. Cada cambio en la base de datos es un **archivo SQL nuevo** con el siguiente número (`V2`, `V3`, etc.).
4. Cada consulta hacia otros módulos pasa por los **adaptadores de integración** sin tocar el núcleo del sistema.

¡Con esta estructura tienen un proyecto sólido, modular y listo para seguir creciendo en las próximas entregas!
