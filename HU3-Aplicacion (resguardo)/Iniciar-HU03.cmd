@echo off
pushd "%~sdp0main"
start "" /B java -jar "target\hu03-registro-vehiculos-0.0.1-SNAPSHOT-exec.jar" --spring.profiles.active=hu03
timeout /t 5 /nobreak >nul
start "" "http://localhost:3003"
popd