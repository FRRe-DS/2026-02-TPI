package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import ar.edu.utn.frre.m3.application.port.DocumentRepository;
import ar.edu.utn.frre.m3.application.usecase.RegisterDriverLicenseCommand;
import ar.edu.utn.frre.m3.application.usecase.RegisterDriverLicenseUseCase;
import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.ServiceType;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "M3_PG_TEST_URL", matches = ".+")
@Transactional
class DriverLicensePostgresTest {
    @DynamicPropertySource
    static void postgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv("M3_PG_TEST_URL"));
        registry.add("spring.datasource.username", () -> System.getenv().getOrDefault("M3_PG_TEST_USER", "m3_user"));
        registry.add("spring.datasource.password", () -> System.getenv().getOrDefault("M3_PG_TEST_PASSWORD", "m3_password"));
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired SpringDataDriverRepository drivers;
    @Autowired RegisterDriverLicenseUseCase licenses;
    @Autowired DocumentRepository documents;

    @Test
    void renewalRespectsPartialUniqueIndexAndRetainsPreviousRow() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        drivers.saveAndFlush(new DriverJpaEntity(id, "subject-" + id, "Conductor Prueba",
                "PENDING", 1L, now, now));
        Document first = licenses.register(new RegisterDriverLicenseCommand(id, "LIC-1",
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2027-01-01T00:00:00Z"),
                Set.of(ServiceType.AUTO), null, 1));
        Document second = licenses.register(new RegisterDriverLicenseCommand(id, "LIC-2",
                Instant.parse("2027-01-01T00:00:00Z"), Instant.parse("2028-01-01T00:00:00Z"),
                Set.of(ServiceType.MOTO), null, 2));

        List<Document> history = documents.findLicensesByDriverId(id);
        assertEquals(2, history.size());
        assertEquals(2, second.getRevision());
        assertEquals(first.getId(), history.get(1).getId());
        assertFalse(history.get(1).isCurrent());
        assertTrue(history.get(0).isCurrent());
    }
}
