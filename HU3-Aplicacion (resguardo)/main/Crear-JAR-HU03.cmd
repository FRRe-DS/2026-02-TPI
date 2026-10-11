@echo off
setlocal
set "APP_DIR=%~sdp0"
set "MAVEN_USER_HOME=%APP_DIR%.maven-cache"
pushd "%APP_DIR%"

call "%APP_DIR%mvnw.cmd" "-Dmaven.repo.local=%APP_DIR%.maven-cache\repository" -DskipTests package
set "BUILD_RESULT=%ERRORLEVEL%"

popd
if not "%BUILD_RESULT%"=="0" (
    echo.
    echo No se pudo crear el JAR. Revisá el mensaje de error anterior.
    pause
    exit /b %BUILD_RESULT%
)

echo.
echo JAR creado en:
echo %APP_DIR%target\hu03-registro-vehiculos-0.0.1-SNAPSHOT-exec.jar
pause
