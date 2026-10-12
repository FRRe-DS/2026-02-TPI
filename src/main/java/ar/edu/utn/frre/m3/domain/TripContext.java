package ar.edu.utn.frre.m3.domain;
import java.util.UUID;
public record TripContext(String tripId, UUID driverId, String clientId, String status) {}
