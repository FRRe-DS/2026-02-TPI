package ar.edu.utn.frre.m3.application.port.out;

import ar.edu.utn.frre.m3.domain.model.Document;

import java.util.Optional;
import java.util.UUID;

public interface DocumentRepositoryPort {
    Optional<Document> findById(UUID documentId);
    Document save(Document document);
}
