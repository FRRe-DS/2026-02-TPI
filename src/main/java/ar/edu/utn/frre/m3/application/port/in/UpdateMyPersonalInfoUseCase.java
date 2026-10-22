package ar.edu.utn.frre.m3.application.port.in;

import ar.edu.utn.frre.m3.domain.Driver;
import ar.edu.utn.frre.m3.domain.PersonalInfo;

/** Updates the personal data of the driver associated with the current session. */
public interface UpdateMyPersonalInfoUseCase {
    Driver updatePersonalInfo(PersonalInfo personalInfo, long version);
}
