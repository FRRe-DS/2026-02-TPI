package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.application.port.in.GetDriverDispatchInfoUseCase;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.Driver;
import java.util.Objects;
import java.util.UUID;

/**
 * Application query for the M5 dispatch contract.
 *
 * <p>The returned availability is exclusively the driver's persisted M3
 * intention: connected and willing to receive requests. M4 is intentionally
 * not part of this use case because occupancy, reservations and location
 * freshness are M4 concerns.</p>
 */
public class GetDriverDispatchInfoService implements GetDriverDispatchInfoUseCase {

    private final DriverRepository drivers;
    private final AvailabilityIntentRepository availabilityIntents;
    private final VehicleRepository vehicles;

    public GetDriverDispatchInfoService(
            DriverRepository drivers,
            AvailabilityIntentRepository availabilityIntents,
            VehicleRepository vehicles) {
        this.drivers = Objects.requireNonNull(drivers, "drivers is required");
        this.availabilityIntents = Objects.requireNonNull(availabilityIntents, "availabilityIntents is required");
        this.vehicles = Objects.requireNonNull(vehicles, "vehicles is required");
    }

    @Override
    public DriverDispatchInfo execute(UUID conductorId) {
        Objects.requireNonNull(conductorId, "conductorId is required");

        Driver driver = drivers.findById(conductorId)
                .orElseThrow(() -> DriverNotFoundException.forId(conductorId));

        AvailabilityIntent intent = availabilityIntents.findByDriverId(conductorId).orElse(null);
        if (intent == null) {
            return withoutSelectedVehicle(driver);
        }

        return vehicles.findByIdAndDriverId(intent.getVehicleId(), conductorId)
                .map(vehicle -> new DriverDispatchInfo(
                        conductorId,
                        driver.isOperable(),
                        intent.isConnected() && intent.isAvailable(),
                        new DriverDispatchInfo.VehicleInfo(vehicle.getId(), vehicle.getVehicleType()),
                        driver.getVersion()))
                // A stale intent must not make M5 see a driver as available
                // without a current M3-selected vehicle.
                .orElseGet(() -> withoutSelectedVehicle(driver));
    }

    private static DriverDispatchInfo withoutSelectedVehicle(Driver driver) {
        return new DriverDispatchInfo(
                driver.getId(),
                driver.isOperable(),
                false,
                null,
                driver.getVersion());
    }
}
