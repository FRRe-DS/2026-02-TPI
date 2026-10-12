package ar.edu.utn.frre.m3.domain;
import java.util.UUID;
public record AvailabilityMessage(UUID id, UUID driverId,long version,String payload,int attempt) {}
