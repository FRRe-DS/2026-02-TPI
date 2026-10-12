package ar.edu.utn.frre.m3.adapter.persistence;
import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class IncompleteProfileMappingTest {
    @Test void missingPersonalDataStaysMissingAfterReadingAndSaving() {
        var original=new DriverJpaEntity(UUID.randomUUID(),"identity-123","Nombre registrado","PENDING",1L,Instant.now(),Instant.now());
        var mapper=new DriverMapper();var domain=mapper.toDomain(original);var saved=mapper.toJpaEntity(domain);
        assertEquals("Nombre registrado",domain.getDisplayName());
        assertNull(domain.getPersonalInfo().getDni());assertNull(domain.getPersonalInfo().getEmail());
        assertNull(saved.getDni());assertNull(saved.getEmail());
    }
}
