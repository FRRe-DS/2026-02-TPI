package ar.edu.utn.frre.m3.adapter.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity mapping the {@code idempotency_records} table.
 */
@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "idempotency_uq",
        columnNames = {"principal", "http_method", "path", "idempotency_key"}
    )
)
public class IdempotencyRecordJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "principal", nullable = false, length = 200)
    private String principal;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "idempotency_key", nullable = false, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecordJpaEntity() {}

    public IdempotencyRecordJpaEntity(
            UUID id, String principal, String httpMethod, String path,
            String idempotencyKey, String requestHash,
            int responseStatus, String responseBody,
            Instant createdAt, Instant expiresAt) {
        this.id = id;
        this.principal = principal;
        this.httpMethod = httpMethod;
        this.path = path;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public String getPrincipal() { return principal; }
    public String getHttpMethod() { return httpMethod; }
    public String getPath() { return path; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public int getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
}
