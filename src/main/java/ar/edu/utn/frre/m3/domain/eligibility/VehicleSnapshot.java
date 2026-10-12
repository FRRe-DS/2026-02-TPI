package ar.edu.utn.frre.m3.domain.eligibility;

import ar.edu.utn.frre.m3.domain.QualificationStatus;
import ar.edu.utn.frre.m3.domain.ServiceType;

import java.util.Set;
import java.util.UUID;

public record VehicleSnapshot(
        UUID id,
        QualificationStatus qualificationStatus,
        Set<ServiceType> serviceTypes,
        DocumentSnapshot insurance
) {
    public boolean supportsService(ServiceType serviceType) {
        return serviceTypes != null && serviceTypes.contains(serviceType);
    }
}
