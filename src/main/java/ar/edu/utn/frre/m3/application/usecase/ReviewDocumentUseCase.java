package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.domain.Document;

public interface ReviewDocumentUseCase {
    Document reviewDocument(ReviewDocumentCommand command);
}
