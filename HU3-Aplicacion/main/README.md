# HU-03 — Registro de vehículos

## Carpetas

- `main/` contiene el proyecto Java: código, pantallas, Maven Wrapper, librerías descargadas y archivos compilados.
- `Iniciar-HU03.cmd`, un nivel arriba, inicia el servidor y abre el navegador.
- `vehiculos/`, junto a `main/`, contiene `vehiculos.csv`, donde se agregan los vehículos registrados.

## Uso

Hacé doble clic en `Iniciar-HU03.cmd`. Si todavía no está creado el JAR, el iniciador llama a `Crear-JAR-HU03.cmd` dentro de `main/`.

Para volver a construir el JAR después de modificar el código, ejecutá `main/Crear-JAR-HU03.cmd`. Este archivo usa Maven Wrapper y guarda Maven y sus librerías en `main/.maven-cache`; los archivos compilados quedan en `main/target`.

El iniciador muestra los mensajes de Spring Boot en una única ventana. Dejala abierta mientras usás la aplicación; para detener el servidor, presioná `Ctrl+C` en esa ventana.

Los datos viejos del CSV se conservan. La próxima vez que guardes un vehículo, se agregarán las columnas nuevas; los datos que no existían antes (color, capacidad y servicios) quedarán vacíos en esos registros anteriores.

Java debe estar instalado en Windows. No se usan las unidades M:, X: ni Z:.