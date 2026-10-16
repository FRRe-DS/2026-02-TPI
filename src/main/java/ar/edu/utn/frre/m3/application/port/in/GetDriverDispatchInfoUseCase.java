package ar.edu.utn.frre.m3.application.port.in;

import ar.edu.utn.frre.m3.application.usecase.DriverDispatchInfo;
import java.util.UUID;

/**
 * Reads the M3-owned information that M5 needs before dispatching a driver.
 */
public interface GetDriverDispatchInfoUseCase {

    DriverDispatchInfo execute(UUID conductorId);
}
