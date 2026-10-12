package ar.edu.utn.frre.m3.application.usecase;

/**
 * Input port for the "Request Availability" use case (HU-10).
 */
public interface RequestAvailabilityUseCase {

    /**
     * Validates, persists and enqueues the driver's availability intent.
     *
     * @param command The command carrying driver intent and idempotency data.
     * @return The result ready for the REST controller to build a 202 response.
     */
    RequestAvailabilityResult execute(RequestAvailabilityCommand command);
}
