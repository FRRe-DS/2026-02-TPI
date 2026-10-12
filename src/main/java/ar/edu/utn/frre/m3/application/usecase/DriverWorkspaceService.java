package ar.edu.utn.frre.m3.application.usecase;

import ar.edu.utn.frre.m3.application.port.*;
import ar.edu.utn.frre.m3.application.port.out.CurrentPrincipalPort;
import ar.edu.utn.frre.m3.application.exception.DriverAccessDeniedException;
import ar.edu.utn.frre.m3.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

/** Application boundary for the integrated web workspace. */
public class DriverWorkspaceService {
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final DocumentRepository documents;
    private final AvailabilityIntentRepository availability;
    private final CurrentPrincipalPort principal;
    private final UnitOfWork transactions;
    private final Clock clock;
    public DriverWorkspaceService(DriverRepository drivers, VehicleRepository vehicles,
            DocumentRepository documents, AvailabilityIntentRepository availability,
            CurrentPrincipalPort principal, UnitOfWork transactions, Clock clock) {
        this.drivers=drivers; this.vehicles=vehicles; this.documents=documents;
        this.availability=availability; this.principal=principal; this.transactions=transactions; this.clock=clock;
    }
    public Driver current() {
        String subject = principal.getSubject().orElseThrow(() -> new DriverAccessDeniedException("No hay una sesión de conductor."));
        return drivers.findByIdentitySubject(subject).orElseThrow(() -> new IllegalArgumentException("Primero registrá tu perfil."));
    }
    public Driver driver(UUID id) {
        Driver d=drivers.findById(id).orElseThrow(() -> new IllegalArgumentException("No se encontró el conductor."));
        if (!isOperator() && !principal.getSubject().filter(d.getIdentitySubject()::equals).isPresent())
            throw new DriverAccessDeniedException("No tenés acceso a este conductor.");
        return d;
    }
    public List<Driver> allDrivers() { requireOperator(); return drivers.findAll(); }
    private boolean isOperator() { return principal.hasRole("OPERATOR") || principal.hasRole("OPERADOR") || principal.hasRole("ADMIN") || principal.hasRole("ADMINISTRADOR"); }
    public void requireOperator() {
        if (!isOperator()) throw new DriverAccessDeniedException("Esta acción requiere un operador.");
    }
    public String actor() { return principal.getSubject().orElseThrow(() -> new DriverAccessDeniedException("No hay sesión.")); }
    public List<Vehicle> vehicles(UUID id) { driver(id); return vehicles.findByDriverId(id); }
    public List<Document> documents(UUID id) {
        driver(id);
        var result = new ArrayList<>(documents.findLicensesByDriverId(id));
        for (Vehicle v : vehicles.findByDriverId(id)) result.addAll(documents.findByVehicleIdOrderByRevisionDesc(v.getId()));
        return List.copyOf(result);
    }
    public AvailabilityIntent availability(UUID id) { driver(id); return availability.findByDriverId(id).orElse(null); }
    public Vehicle registerVehicle(String type, String plate, String make, String model, Integer year, long version) {
        return registerVehicle(type,plate,make,model,year,version,null,null,Set.of());
    }
    public Vehicle registerVehicle(String type, String plate, String make, String model, Integer year, long version,
            String color, Integer capacity, Set<String> categories) {
        Driver owner=current();
        if (color != null) required(color,30);
        if (capacity != null && (capacity < 1 || capacity > 6)) throw new IllegalArgumentException("La capacidad debe ser entre 1 y 6 pasajeros.");
        if (categories == null || !Set.of("STANDARD","PREMIUM","DELIVERY","PETS").containsAll(categories))
            throw new IllegalArgumentException("Seleccioná servicios válidos.");
        if (!Set.of("AUTO", "MOTO").contains(type)) throw new IllegalArgumentException("Seleccioná Auto o Moto.");
        if (year == null || year < 1886 || year > java.time.Year.now(clock).getValue()+1)
            throw new IllegalArgumentException("Ingresá un año válido.");
        String normalizedPlate=required(plate,20).toUpperCase(Locale.ROOT).replace(" ", "");
        String normalizedMake=required(make,60), normalizedModel=required(model,60);
        return transactions.executeInTransaction(() -> {
            Driver locked=drivers.findByIdWithPessimisticLock(owner.getId()).orElseThrow();
            locked.validateVersion(version);
            if (vehicles.findByDriverId(owner.getId()).stream().anyMatch(v -> normalizedPlate.equalsIgnoreCase(v.getPlate())))
                throw new IllegalArgumentException("Ya registraste un vehículo con esa patente.");
            locked.incrementVersion(); drivers.save(locked);
            return vehicles.save(new Vehicle(UUID.randomUUID(),owner.getId(),type,Set.of(type),"AR",
                normalizedPlate,normalizedMake,normalizedModel,year,"PENDING",(long) locked.getVersion(),Instant.now(clock),color,capacity,categories));
        });
    }
    private String required(String value,int max) {
        if(value==null || value.isBlank() || value.trim().length()>max) throw new IllegalArgumentException("Completá los campos obligatorios sin exceder su longitud máxima.");
        return value.trim();
    }
}
