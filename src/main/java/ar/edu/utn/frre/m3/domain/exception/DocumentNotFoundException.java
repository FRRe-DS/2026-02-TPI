package ar.edu.utn.frre.m3.domain.exception;

import java.util.UUID;

public class DocumentNotFoundException extends RuntimeException {
    private final UUID documentId;

    public DocumentNotFoundException(UUID documentId) {
        super("Document not found or does not belong to the specified driver: " + documentId);
        this.documentId = documentId;
    }

    public UUID getDocumentId() {
        return documentId;
    }
}
