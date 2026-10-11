package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.domain.PersonalInfo;
import java.time.LocalDate;

/**
 * DTO de salida con los datos personales del conductor.
 */
public record PersonalInfoResponse(
        String firstName,
        String lastName,
        String fullName,
        String dni,
        LocalDate birthDate,
        String address,
        String phone,
        String email
) {
    public static PersonalInfoResponse from(PersonalInfo info) {
        if (info == null) return null;
        return new PersonalInfoResponse(
                info.getFirstName(),
                info.getLastName(),
                info.getFullName(),
                info.getDni(),
                info.getBirthDate(),
                info.getAddress(),
                info.getPhone(),
                info.getEmail()
        );
    }
}
