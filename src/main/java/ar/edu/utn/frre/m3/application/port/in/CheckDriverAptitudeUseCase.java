package ar.edu.utn.frre.m3.application.port.in;

import ar.edu.utn.frre.m3.domain.eligibility.EligibilityResult;
import java.util.UUID;

public interface CheckDriverAptitudeUseCase {
    EligibilityResult execute(UUID driverId);
    EligibilityResult executeForSelf();
    EligibilityResult executeBySubject(String identitySubject);
}
