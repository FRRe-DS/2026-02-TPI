package ar.edu.utn.frre.m3.adapter.rest.mapper;

import ar.edu.utn.frre.m3.adapter.rest.dto.DocumentResponse;
import ar.edu.utn.frre.m3.domain.model.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentRestMapper {

    public DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }
        return new DocumentResponse(
                document.getId(),
                document.getDriverId(),
                document.getVehicleId(),
                document.getKind(),
                document.getRevision(),
                document.getReviewStatus(),
                document.getIssuedAt(),
                document.getValidUntil(),
                document.isCurrent(),
                document.getServiceTypes(),
                document.getDriverVersion(),
                document.getCreatedAt()
        );
    }
}
