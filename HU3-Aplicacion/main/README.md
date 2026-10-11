# HU-03 — Registro de vehículos

## Carpetas

- `main/` contiene el proyecto Java: código, pantallas, Maven Wrapper, librerías descargadas, archivos compilados y este instructivo.
- `Iniciar-HU03.cmd`, un nivel arriba, inicia el servidor en una sola ventana y abre el navegador.
- Los registros y documentos se crean dentro de `main/` al usar la aplicación.

## Uso

Hacé doble clic en `Iniciar-HU03.cmd`. Si todavía no está creado el JAR, el iniciador llama a `Crear-JAR-HU03.cmd` dentro de `main/`.

Para volver a construir el JAR después de modificar el código, ejecutá `main/Crear-JAR-HU03.cmd`. Este archivo usa Maven Wrapper y guarda Maven y sus librerías en `main/.maven-cache`; los archivos compilados quedan en `main/target`.

El iniciador muestra los mensajes de Spring Boot en una única ventana. Dejala abierta mientras usás la aplicación; para detener el servidor, presioná `Ctrl+C` en esa ventana.

Java debe estar instalado en Windows. No se usan las unidades M:, X: ni Z:.
