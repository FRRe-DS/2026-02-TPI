package ar.edu.utn.frre.m3.adapter.rest;

import ar.edu.utn.frre.m3.adapter.rest.dto.DocumentResponse;
import ar.edu.utn.frre.m3.adapter.rest.dto.SubmitVehicleDocumentRequest;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentCommand;
import ar.edu.utn.frre.m3.application.usecase.RegisterVehicleDocumentUseCase;
import ar.edu.utn.frre.m3.domain.Document;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for Vehicle Documentation management (HU-06 / RF-3.3).
 * Contractual endpoint: /api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents
 */
@RestController
@RequestMapping("/api/v1/drivers/{driverId}/vehicles/{vehicleId}/documents")
public class VehicleDocumentRestController {

    private final RegisterVehicleDocumentUseCase registerVehicleDocumentUseCase;

    public VehicleDocumentRestController(RegisterVehicleDocumentUseCase registerVehicleDocumentUseCase) {
        this.registerVehicleDocumentUseCase = Objects.requireNonNull(registerVehicleDocumentUseCase, "registerVehicleDocumentUseCase cannot be null");
    }

    /**
     * Registers a new revision of insurance or vehicle documentation metadata.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DocumentResponse> submitVehicleDocument(
        @PathVariable UUID driverId,
        @PathVariable UUID vehicleId,
        @Valid @RequestBody SubmitVehicleDocumentRequest request
    ) {
        RegisterVehicleDocumentCommand command = new RegisterVehicleDocumentCommand(
            driverId,
            vehicleId,
            request.documentType(),
            request.documentNumber(),
            request.issuingEntity(),
            request.issuedAt(),
            request.validUntil(),
            request.storageReference()
        );

        Document created = registerVehicleDocumentUseCase.registerVehicleDocument(command);

        URI location = URI.create(String.format(
            "/api/v1/drivers/%s/vehicles/%s/documents/%s",
            driverId,
            vehicleId,
            created.getId()
        ));

        return ResponseEntity.created(location)
            .body(DocumentResponse.fromDomain(created));
    }

    /**
     * Lists the document revision history for a vehicle belonging to the driver.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DocumentResponse>> listVehicleDocuments(
        @PathVariable UUID driverId,
        @PathVariable UUID vehicleId
    ) {
        List<Document> documents = registerVehicleDocumentUseCase.listVehicleDocuments(driverId, vehicleId);
        List<DocumentResponse> response = documents.stream()
            .map(DocumentResponse::fromDomain)
            .toList();

        return ResponseEntity.ok(response);
    }
}

