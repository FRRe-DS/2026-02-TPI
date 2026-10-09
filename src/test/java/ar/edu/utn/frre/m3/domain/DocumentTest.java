package ar.edu.utn.frre.m3.domain;

import ar.edu.utn.frre.m3.domain.exception.InvalidDocumentPeriodException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DocumentTest {

    @Test
    @DisplayName("Should successfully create a pending document revision when dates are valid")
    void shouldCreatePendingRevisionSuccessfully() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(5, ChronoUnit.DAYS);
        Instant validUntil = Instant.now().plus(365, ChronoUnit.DAYS);

        Document doc = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-998877",
            "La Segunda Seguros",
            issuedAt,
            validUntil,
            "storage-ref-123",
            1
        );

        assertNotNull(doc.getId());
        assertEquals(driverId, doc.getDriverId());
        assertEquals(vehicleId, doc.getVehicleId());
        assertEquals(DocumentType.INSURANCE, doc.getDocumentType());
        assertEquals("POL-998877", doc.getDocumentNumber());
        assertEquals("La Segunda Seguros", doc.getIssuingEntity());
        assertEquals(issuedAt, doc.getIssuedAt());
        assertEquals(validUntil, doc.getValidUntil());
        assertEquals("storage-ref-123", doc.getStorageReference());
        assertEquals(1, doc.getRevision());
        assertEquals(DocumentStatus.PENDING, doc.getStatus());
        assertTrue(doc.isCurrent());
        assertNotNull(doc.getCreatedAt());
    }

    @Test
    @DisplayName("Should throw InvalidDocumentPeriodException when issuedAt is after validUntil")
    void shouldThrowWhenIssuedAtIsAfterValidUntil() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().plus(10, ChronoUnit.DAYS);
        Instant validUntil = Instant.now();

        assertThrows(InvalidDocumentPeriodException.class, () ->
            Document.createPendingRevision(
                driverId,
                vehicleId,
                DocumentType.VEHICLE_REGISTRATION,
                "REG-1234",
                "DNRPA",
                issuedAt,
                validUntil,
                null,
                1
            )
        );
    }

    @Test
    @DisplayName("Should throw InvalidDocumentPeriodException when issuedAt equals validUntil")
    void shouldThrowWhenIssuedAtEqualsValidUntil() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant timestamp = Instant.now();

        assertThrows(InvalidDocumentPeriodException.class, () ->
            Document.createPendingRevision(
                driverId,
                vehicleId,
                DocumentType.INSURANCE,
                "POL-1111",
                "Sancor Seguros",
                timestamp,
                timestamp,
                null,
                1
            )
        );
    }

    @Test
    @DisplayName("Should mark previous revision as non-current")
    void shouldMarkAsNonCurrent() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(10, ChronoUnit.DAYS);
        Instant validUntil = Instant.now().plus(30, ChronoUnit.DAYS);

        Document doc = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-001",
            "Federacion Patronal",
            issuedAt,
            validUntil,
            null,
            1
        );

        assertTrue(doc.isCurrent());
        doc.markAsNonCurrent();
        assertFalse(doc.isCurrent());
    }

    @Test
    @DisplayName("Should correctly evaluate document expiration relative to current instant")
    void shouldEvaluateExpiration() {
        UUID driverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        Instant issuedAt = Instant.now().minus(400, ChronoUnit.DAYS);
        Instant validUntil = Instant.now().minus(35, ChronoUnit.DAYS);

        Document expiredDoc = Document.createPendingRevision(
            driverId,
            vehicleId,
            DocumentType.INSURANCE,
            "POL-OLD",
            "Federacion Patronal",
            issuedAt,
            validUntil,
            null,
            1
        );

        assertTrue(expiredDoc.isExpired(Instant.now()));
    }

    @Test
    @DisplayName("Vehicle domain model should verify ownership accurately")
    void shouldVerifyVehicleOwnership() {
        UUID ownerId = UUID.randomUUID();
        UUID otherDriverId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();

        Vehicle vehicle = new Vehicle(vehicleId, ownerId);

        assertTrue(vehicle.belongsToDriver(ownerId));
        assertFalse(vehicle.belongsToDriver(otherDriverId));
    }
}

