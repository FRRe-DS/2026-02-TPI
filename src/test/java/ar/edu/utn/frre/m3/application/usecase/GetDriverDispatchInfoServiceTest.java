package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.exception.DriverNotFoundException;
import ar.edu.utn.frre.m3.application.port.AvailabilityIntentRepository;
import ar.edu.utn.frre.m3.application.port.DriverRepository;
import ar.edu.utn.frre.m3.application.port.VehicleRepository;
import ar.edu.utn.frre.m3.domain.AvailabilityIntent;
import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.Vehicle;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetDriverDispatchInfoService")
class GetDriverDispatchInfoServiceTest {

    private static final UUID DRIVER_ID = UUID.fromString("a3e4d8c2-75b1-4f92-8b12-456789abcdef");
    private static final UUID VEHICLE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-10-16T12:00:00Z");

    @Mock private DriverRepository drivers;
    @Mock private AvailabilityIntentRepository availabilityIntents;
    @Mock private VehicleRepository vehicles;

    private GetDriverDispatchInfoService service;

    @BeforeEach
    void setUp() {
        service = new GetDriverDispatchInfoService(drivers, availabilityIntents, vehicles);
    }

    @Test
    @DisplayName("returns an enabled and available driver with the selected AUTO")
    void returnsSelectedAuto() {
        givenDriver(QualificationStatus.APPROVED);
        givenIntent(true, true);
        givenVehicle("AUTO");

        DriverDispatchInfo result = service.execute(DRIVER_ID);

        assertThat(result.habilitado()).isTrue();
        assertThat(result.disponible()).isTrue();
        assertThat(result.vehiculo().vehiculoId()).isEqualTo(VEHICLE_ID);
        assertThat(result.vehiculo().tipoVehiculo()).isEqualTo("AUTO");
        verifyReadOnly();
    }

    @Test
    @DisplayName("returns an enabled and available driver with the selected MOTO")
    void returnsSelectedMoto() {
        givenDriver(QualificationStatus.APPROVED);
        givenIntent(true, true);
        givenVehicle("MOTO");

        DriverDispatchInfo result = service.execute(DRIVER_ID);

        assertThat(result.habilitado()).isTrue();
        assertThat(result.disponible()).isTrue();
        assertThat(result.vehiculo().tipoVehiculo()).isEqualTo("MOTO");
        verifyReadOnly();
    }

    @Test
    @DisplayName("reports the existing driver qualification state without creating a new one")
    void returnsDisabledDriver() {
        givenDriver(QualificationStatus.SUSPENDED);
        when(availabilityIntents.findByDriverId(DRIVER_ID)).thenReturn(Optional.empty());

        DriverDispatchInfo result = service.execute(DRIVER_ID);

        assertThat(result.habilitado()).isFalse();
        assertThat(result.disponible()).isFalse();
        assertThat(result.vehiculo()).isNull();
        verifyReadOnly();
    }

    @Test
    @DisplayName("returns unavailable when the driver does not want to receive requests")
    void returnsUnavailableWhenIntentDoesNotAcceptRequests() {
        givenDriver(QualificationStatus.APPROVED);
        givenIntent(true, false);
        givenVehicle("AUTO");

        DriverDispatchInfo result = service.execute(DRIVER_ID);

        assertThat(result.disponible()).isFalse();
        assertThat(result.vehiculo().vehiculoId()).isEqualTo(VEHICLE_ID);
        verifyReadOnly();
    }

    @Test
    @DisplayName("returns unavailable and null vehicle when no vehicle is selected for availability")
    void returnsNullVehicleWhenThereIsNoAvailabilityIntent() {
        givenDriver(QualificationStatus.APPROVED);
        when(availabilityIntents.findByDriverId(DRIVER_ID)).thenReturn(Optional.empty());

        DriverDispatchInfo result = service.execute(DRIVER_ID);

        assertThat(result.disponible()).isFalse();
        assertThat(result.vehiculo()).isNull();
        verify(vehicles, never()).findByIdAndDriverId(VEHICLE_ID, DRIVER_ID);
        verifyReadOnly();
    }

    @Test
    @DisplayName("does not expose availability when an intent references a missing selected vehicle")
    void returnsNullVehicleForStaleSelectedVehicle() {
        givenDriver(QualificationStatus.APPROVED);
        givenIntent(true, true);
        when(vehicles.findByIdAndDriverId(VEHICLE_ID, DRIVER_ID)).thenReturn(Optional.empty());

        DriverDispatchInfo result = service.execute(DRIVER_ID);

        assertThat(result.disponible()).isFalse();
        assertThat(result.vehiculo()).isNull();
        verifyReadOnly();
    }

    @Test
    @DisplayName("returns not found for an unknown driver")
    void throwsNotFoundForUnknownDriver() {
        when(drivers.findById(DRIVER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(DRIVER_ID))
                .isInstanceOf(DriverNotFoundException.class);

        verify(availabilityIntents, never()).findByDriverId(DRIVER_ID);
        verify(vehicles, never()).findByIdAndDriverId(VEHICLE_ID, DRIVER_ID);
        verifyReadOnly();
    }

    private void givenDriver(QualificationStatus status) {
        when(drivers.findById(DRIVER_ID)).thenReturn(Optional.of(new Driver(
                DRIVER_ID, "subject-1", "Driver", status, 7, NOW, NOW)));
    }

    private void givenIntent(boolean connected, boolean available) {
        when(availabilityIntents.findByDriverId(DRIVER_ID)).thenReturn(Optional.of(
                AvailabilityIntent.create(DRIVER_ID, VEHICLE_ID, connected, available, NOW)));
    }

    private void givenVehicle(String type) {
        when(vehicles.findByIdAndDriverId(VEHICLE_ID, DRIVER_ID)).thenReturn(Optional.of(new Vehicle(
                VEHICLE_ID, DRIVER_ID, type, Set.of(type), "AR", "ABC123", "Make", "Model", 2024,
                "APPROVED", 7L, NOW)));
    }

    private void verifyReadOnly() {
        verify(drivers, never()).save(org.mockito.ArgumentMatchers.any());
        verify(availabilityIntents, never()).save(org.mockito.ArgumentMatchers.any());
        verify(vehicles, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
