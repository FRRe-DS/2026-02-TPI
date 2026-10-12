package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.Driver;
import java.util.List;
import java.util.UUID;

public interface RegisterDriverLicenseUseCase {
    Document register(RegisterDriverLicenseCommand command);
    List<Document> history(UUID driverId);
    Driver driver(UUID driverId);
}
