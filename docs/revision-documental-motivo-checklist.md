# Checklist — Motivo en revisión documental

[*] Revisar formulario y DTO de revisión documental
[*] Revisar validación actual del motivo
[*] Hacer motivo opcional para APPROVED
[*] Mantener motivo obligatorio para OBSERVED/REJECTED/SUSPENDED
[*] Ajustar mensajes de la UI
[*] Agregar pruebas
[*] Ejecutar pruebas existentes

## Alcance

La revisión individual de documentos sólo admite `APPROVED`, `OBSERVED` y
`REJECTED`; `SUSPENDED` corresponde a la decisión administrativa de
conductor o vehículo y no se modifica en esta tarea.

## Ejecución

- Las pruebas focalizadas de revisión documental finalizaron correctamente.
- La UI oculta el campo de motivo al aprobar y lo muestra como obligatorio al
  observar o rechazar.
- La suite completa finalizó correctamente con JDK 21, la misma versión que
  usa CI. Con Java 27, Byte Buddy/Mockito no es compatible.
