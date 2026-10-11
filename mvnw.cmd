@REM ----------------------------------------------------------------------------
@REM Maven Start Up Batch script
@REM ----------------------------------------------------------------------------
@IF "%DEBUG%" == "" @ECHO OFF
@SETLOCAL EnableExtensions EnableDelayedExpansion

set ERROR_CODE=0

set "DIRNAME=%~dp0"
if "%DIRNAME:~-1%"=="\" set "DIRNAME=%DIRNAME:~0,-1%"
set "MAVEN_PROJECTBASEDIR=%DIRNAME%"

if not "%JAVA_HOME%" == "" (
  set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
  goto OkJava
)

for %%i in (java.exe) do set "JAVA_EXE=%%~$PATH:i"
if "%JAVA_EXE%" == "" (
  echo Error: java.exe not found in PATH and JAVA_HOME not set.
  goto error
)

:OkJava
set "MAVEN_WRAPPER_JAR=%DIRNAME%\.mvn\wrapper\maven-wrapper.jar"
set "MAVEN_WRAPPER_PROPERTIES=%DIRNAME%\.mvn\wrapper\maven-wrapper.properties"

set WRAPPER_LAUNCHER=org.apache.maven.wrapper.MavenWrapperMain

if exist "%MAVEN_WRAPPER_JAR%" goto run

echo Downloading Maven Wrapper...
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object Net.WebClient).DownloadFile('https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar', '%MAVEN_WRAPPER_JAR%')}"

:run
"%JAVA_EXE%" -classpath "%MAVEN_WRAPPER_JAR%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" %WRAPPER_LAUNCHER% %*
if ERRORLEVEL 1 goto error
goto end

:error
set ERROR_CODE=1

:end
@endlocal & set ERROR_CODE=%ERROR_CODE%
exit /B %ERROR_CODE%
