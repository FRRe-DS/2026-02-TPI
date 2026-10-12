package ar.edu.utn.frre.m3.domain.eligibility;

import ar.edu.utn.frre.m3.domain.QualificationStatus;

import java.util.UUID;

public record DriverSnapshot(
        UUID id,
        String identitySubject,
        String displayName,
        QualificationStatus qualificationStatus,
        VehicleSnapshot selectedVehicle,
        DocumentSnapshot license,
        int version
) {
}
