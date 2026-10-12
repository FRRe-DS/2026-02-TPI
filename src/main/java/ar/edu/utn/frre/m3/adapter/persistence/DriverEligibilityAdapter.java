package ar.edu.utn.frre.m3.adapter.persistence;

import ar.edu.utn.frre.m3.adapter.persistence.entity.DocumentJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.DriverJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.entity.VehicleJpaEntity;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDocumentRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataDriverRepository;
import ar.edu.utn.frre.m3.adapter.persistence.repository.SpringDataVehicleRepository;
import ar.edu.utn.frre.m3.application.port.out.DriverEligibilityPort;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.ServiceType;
import ar.edu.utn.frre.m3.domain.eligibility.DocumentSnapshot;
import ar.edu.utn.frre.m3.domain.eligibility.DriverSnapshot;
import ar.edu.utn.frre.m3.domain.eligibility.VehicleSnapshot;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DriverEligibilityAdapter implements DriverEligibilityPort {

    private final SpringDataDriverRepository driverRepository;
    private final SpringDataVehicleRepository vehicleRepository;
    private final SpringDataDocumentRepository documentRepository;

    public DriverEligibilityAdapter(SpringDataDriverRepository driverRepository,
                                  SpringDataVehicleRepository vehicleRepository,
                                  SpringDataDocumentRepository documentRepository) {
        this.driverRepository = Objects.requireNonNull(driverRepository);
        this.vehicleRepository = Objects.requireNonNull(vehicleRepository);
        this.documentRepository = Objects.requireNonNull(documentRepository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DriverSnapshot> findSnapshotById(UUID driverId) {
        return driverRepository.findById(driverId).map(this::mapToSnapshot);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DriverSnapshot> findSnapshotByIdentitySubject(String identitySubject) {
        return driverRepository.findByIdentitySubject(identitySubject).map(this::mapToSnapshot);
    }

    private DriverSnapshot mapToSnapshot(DriverJpaEntity driver) {
        UUID driverId = driver.getId();
        List<VehicleJpaEntity> vehicles = vehicleRepository.findByDriverId(driverId);

        VehicleSnapshot selectedVehicle = null;
        if (!vehicles.isEmpty()) {
            VehicleJpaEntity bestVehicle = vehicles.stream()
                    .filter(v -> "APPROVED".equalsIgnoreCase(v.getQualificationStatus()))
                    .findFirst()
                    .orElse(vehicles.get(0));
            selectedVehicle = mapVehicle(bestVehicle);
        }

        DocumentSnapshot licenseSnapshot = documentRepository
                .findByDriverIdAndDocumentTypeAndVehicleIdIsNullAndCurrentTrue(driverId, DocumentType.LICENSE)
                .map(this::mapDocument)
                .orElse(null);

        QualificationStatus driverStatus = QualificationStatus.PENDING;
        if (driver.getQualificationStatus() != null) {
            try {
                driverStatus = QualificationStatus.valueOf(driver.getQualificationStatus().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        int version = driver.getVersion() != null ? driver.getVersion().intValue() : 1;

        return new DriverSnapshot(
                driver.getId(),
                driver.getIdentitySubject(),
                driver.getDisplayName(),
                driverStatus,
                selectedVehicle,
                licenseSnapshot,
                version
        );
    }

    private VehicleSnapshot mapVehicle(VehicleJpaEntity vehicle) {
        DocumentSnapshot insuranceSnapshot = documentRepository
                .findByVehicleIdAndDocumentTypeAndCurrentTrue(vehicle.getId(), DocumentType.INSURANCE)
                .map(this::mapDocument)
                .orElse(null);

        Set<ServiceType> serviceTypes = new HashSet<>();
        if (vehicle.getServiceTypes() != null && !vehicle.getServiceTypes().isBlank()) {
            for (String s : vehicle.getServiceTypes().split(",")) {
                try {
                    serviceTypes.add(ServiceType.valueOf(s.trim().toUpperCase()));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        QualificationStatus vehicleStatus = QualificationStatus.PENDING;
        if (vehicle.getQualificationStatus() != null) {
            try {
                vehicleStatus = QualificationStatus.valueOf(vehicle.getQualificationStatus().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        return new VehicleSnapshot(
                vehicle.getId(),
                vehicleStatus,
                serviceTypes,
                insuranceSnapshot
        );
    }

    private DocumentSnapshot mapDocument(DocumentJpaEntity document) {
        QualificationStatus docStatus = QualificationStatus.PENDING;
        if (document.getStatus() != null) {
            try {
                docStatus = QualificationStatus.valueOf(document.getStatus().name());
            } catch (IllegalArgumentException ignored) {}
        }

        return new DocumentSnapshot(
                document.getId(),
                document.getDocumentType(),
                docStatus,
                document.getValidUntil()
        );
    }
}
