package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentStatus;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.Vehicle;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperatorReviewViewModelTest {

    private static final UUID DRIVER_ID = UUID.randomUUID();
    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final Instant ISSUED = LocalDate.of(2026, 1, 1).atStartOfDay(ZONE).toInstant();
    private static final Instant VALID_UNTIL = LocalDate.of(2027, 4, 10).atStartOfDay(ZONE).toInstant();

    @Test
    void groupsDriverLicenseSeparatelyFromEachVehicle() {
        Vehicle car = vehicle("Toyota", "Corolla", "AB123CD", "AUTO", 2023, "PENDING");
        Vehicle moto = vehicle("Honda", "Wave 110S", "A123BCD", "MOTO", 2022, "APPROVED");

        List<Document> documents = List.of(
                document(null, DocumentType.LICENSE, DocumentStatus.APPROVED, 1, true),
                document(car.getId(), DocumentType.INSURANCE, DocumentStatus.OBSERVED, 1, true),
                document(car.getId(), DocumentType.VEHICLE_REGISTRATION, DocumentStatus.REJECTED, 1, true),
                document(moto.getId(), DocumentType.INSURANCE, DocumentStatus.PENDING, 1, true)
        );

        OperatorReviewViewModel view = OperatorReviewViewModel.from(List.of(car, moto), documents);

        assertEquals(1, view.driverDocuments().size());
        assertEquals("Licencia de conducir", view.driverDocuments().getFirst().typeLabel());
        assertNull(view.driverDocuments().getFirst().vehicleContext());

        assertEquals(2, view.vehicleGroups().size());

        OperatorReviewViewModel.VehicleGroup carGroup = view.vehicleGroups().get(0);
        assertEquals("Toyota Corolla", carGroup.title());
        assertEquals("AB123CD", carGroup.plate());
        assertEquals("Auto", carGroup.typeLabel());
        assertEquals("AB123CD · Auto · 2023", carGroup.getMetaLine());
        assertEquals(2, carGroup.documents().size());
        assertEquals("Seguro", carGroup.documents().get(0).typeLabel());
        assertEquals("Toyota Corolla · AB123CD", carGroup.documents().get(0).vehicleContext());
        assertEquals("Documentación del vehículo", carGroup.documents().get(1).typeLabel());

        OperatorReviewViewModel.VehicleGroup motoGroup = view.vehicleGroups().get(1);
        assertEquals("Honda Wave 110S", motoGroup.title());
        assertEquals(1, motoGroup.documents().size());
        assertEquals("Seguro", motoGroup.documents().getFirst().typeLabel());
        assertEquals("Honda Wave 110S · A123BCD", motoGroup.documents().getFirst().vehicleContext());

        assertEquals(4, view.history().size());
        assertEquals("Licencia de conducir", view.history().get(0).typeLabel());
        assertEquals("Conductor", view.history().get(0).ownerLabel());
        assertEquals("Toyota Corolla · AB123CD", view.history().get(1).ownerLabel());
        assertEquals("Rechazado", view.history().get(2).statusLabel());
    }

    @Test
    void exposesHumanStatusesDatesAndOnlyCurrentRevisions() {
        Vehicle car = vehicle("Toyota", "Corolla", "AB123CD", "AUTO", 2023, "PENDING");
        Document oldRevision = document(car.getId(), DocumentType.INSURANCE, DocumentStatus.APPROVED, 1, false);
        Document current = document(car.getId(), DocumentType.INSURANCE, DocumentStatus.OBSERVED, 2, true);

        OperatorReviewViewModel view = OperatorReviewViewModel.from(List.of(car), List.of(oldRevision, current));

        assertEquals(1, view.vehicleGroups().getFirst().documents().size());
        OperatorReviewViewModel.DocumentCard card = view.vehicleGroups().getFirst().documents().getFirst();
        assertEquals(2, card.revision());
        assertTrue(card.current());
        assertEquals("OBSERVED", card.status());
        assertEquals("Observado", card.statusLabel());
        assertEquals("10/04/2027", card.validUntil());

        assertEquals(2, view.history().size());
        assertFalse(view.history().get(0).current());
        assertTrue(view.history().get(1).current());
        assertEquals("10/04/2027", view.history().get(1).validUntil());
    }

    @Test
    void translatesEveryStatusWithoutRelyingOnColour() {
        assertEquals("Aprobado", OperatorReviewViewModel.statusLabel("APPROVED"));
        assertEquals("Pendiente", OperatorReviewViewModel.statusLabel("PENDING"));
        assertEquals("Observado", OperatorReviewViewModel.statusLabel("OBSERVED"));
        assertEquals("Rechazado", OperatorReviewViewModel.statusLabel("REJECTED"));
        assertEquals("Suspendido", OperatorReviewViewModel.statusLabel("SUSPENDED"));
        assertFalse(OperatorReviewViewModel.statusLabel("PENDING").isBlank());
    }

    private static Vehicle vehicle(String make, String model, String plate, String type, int year, String status) {
        return new Vehicle(UUID.randomUUID(), DRIVER_ID, type, Set.of(type), "AR", plate, make, model,
                year, status, 1L, Instant.now());
    }

    private static Document document(UUID vehicleId, DocumentType type, DocumentStatus status, int revision,
            boolean current) {
        return new Document(UUID.randomUUID(), DRIVER_ID, vehicleId, type, "DOC-" + revision,
                "Entidad de prueba", ISSUED, VALID_UNTIL, null, revision, status, current, 1L,
                status == DocumentStatus.APPROVED ? null : "Motivo de prueba",
                status == DocumentStatus.APPROVED ? null : Instant.now(), "operador-demo", Instant.now());
    }
}
