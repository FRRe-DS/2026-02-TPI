package ar.edu.utn.frre.m3.application.port.in;

import ar.edu.utn.frre.m3.domain.model.Document;

public interface ReviewDocumentUseCase {
    Document reviewDocument(ReviewDocumentCommand command);
}
