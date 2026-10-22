# Checklist — UX de revisión administrativa de documentos

Alcance: sólo presentación (Thymeleaf + ViewModel). Sin cambios de reglas de negocio,
estados, aprobación, historial ni arquitectura.

[*] Revisar datos disponibles para documentos y vehículos
[*] Agrupar documentos por conductor/vehículo
[*] Agregar nombres humanos para tipos de documentos
[*] Mejorar tarjetas de revisión
[*] Mostrar estado, revisión y vencimiento
[*] Mantener decisión administrativa separada
[*] Verificar responsive
[*] Ejecutar pruebas existentes

## Datos disponibles (inspección)

- `Document`: `id`, `vehicleId`, `documentType`, `revision`, `status`, `validUntil`,
  `current`, `rejectionReason`, `reviewedAt`, `reviewedBy`.
- `Vehicle`: `id`, `make`, `model`, `plate`, `vehicleType`, `modelYear`,
  `qualificationStatus`.
- `WorkspaceWebController.detail` ya obtiene `driver`, `vehicles` y `documents` mediante
  `DriverWorkspaceService` (puertos/repositorios). No se accede a JPA desde el controller.

## Verificación realizada

- `OperatorReviewViewModelTest`: 3 pruebas unitarias (agrupación, estados/fechas,
  historial y revisiones no actuales).
- `WorkspaceFlowTest`: 8 pruebas de integración web; incluye render de la pantalla
  con agrupación, nombres humanos y guardado de revisión.
- Total de las pruebas relevantes: 11, sin fallas.
- Captura desktop y mobile generadas contra una instancia local con datos reales
  (María Demo: licencia + Honda Wave + Toyota Corolla).

Nota: la suite completa bajo el JDK 27 del entorno tiene fallas preexistentes en
pruebas `@WebMvcTest` por falta del bean `RoleNavigation`; no están relacionadas con
esta pantalla. Con `-Dnet.bytebuddy.experimental=true` las pruebas de Mockito pasan.
