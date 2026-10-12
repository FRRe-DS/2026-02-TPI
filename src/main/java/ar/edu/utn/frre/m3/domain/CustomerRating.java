package ar.edu.utn.frre.m3.domain;
import java.time.Instant;
import java.util.UUID;
public record CustomerRating(UUID id, UUID driverId, String tripId, String clientId, int score, String comment, Instant createdAt) {}
