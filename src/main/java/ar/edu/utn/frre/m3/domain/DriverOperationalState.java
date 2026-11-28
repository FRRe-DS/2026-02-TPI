package ar.edu.utn.frre.m3.domain;

/**
 * Estado operativo del conductor tal como lo expone M3 a otros módulos (contrato M3 2.0.0,
 * {@code EstadoOperativo}). Se deriva de la intención de disponibilidad, la habilitación vigente
 * y si el conductor tiene un viaje en curso; no se persiste por separado.
 *
 * <ul>
 *   <li>{@link #OFFLINE} (DESCONECTADO): sin intención o con {@code connected=false}.</li>
 *   <li>{@link #CONNECTED} (CONECTADO): conectado, pero no recibe viajes o ya no cumple las condiciones.</li>
 *   <li>{@link #AVAILABLE} (DISPONIBLE): conectado, acepta viajes y cumple habilitación y documentación.</li>
 *   <li>{@link #BUSY} (OCUPADO): conectado y con un viaje asignado o en curso.</li>
 * </ul>
 */
public enum DriverOperationalState {
    OFFLINE,
    CONNECTED,
    AVAILABLE,
    BUSY
}
