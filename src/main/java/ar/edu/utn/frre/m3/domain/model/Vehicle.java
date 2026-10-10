package ar.edu.utn.frre.m3.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Vehicle {

    private final UUID id;
    private final UUID driverId;
    private VehicleType vehicleType;
    private List<ServiceType> serviceTypes;
    private String countryCode;
    private String plate;
    private String make;
    private String model;
    private int modelYear;
    private QualificationStatus qualificationStatus;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    public Vehicle(UUID id, UUID driverId, VehicleType vehicleType, List<ServiceType> serviceTypes,
                   String countryCode, String plate, String make, String model,
                   int modelYear, QualificationStatus qualificationStatus, long version,
                   Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "Vehicle ID must not be null");
        this.driverId = Objects.requireNonNull(driverId, "Driver ID must not be null");
        this.vehicleType = Objects.requireNonNull(vehicleType, "Vehicle type must not be null");
        this.serviceTypes = serviceTypes != null ? new ArrayList<>(serviceTypes) : Collections.emptyList();
        this.countryCode = countryCode != null ? countryCode : "AR";
        this.plate = Objects.requireNonNull(plate, "Plate must not be null");
        this.make = Objects.requireNonNull(make, "Make must not be null");
        this.model = Objects.requireNonNull(model, "Model must not be null");
        this.modelYear = modelYear;
        this.qualificationStatus = qualificationStatus != null ? qualificationStatus : QualificationStatus.PENDING;
        this.version = version > 0 ? version : 1;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public List<ServiceType> getServiceTypes() {
        return Collections.unmodifiableList(serviceTypes);
    }

    public void setServiceTypes(List<ServiceType> serviceTypes) {
        this.serviceTypes = serviceTypes != null ? new ArrayList<>(serviceTypes) : Collections.emptyList();
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getModelYear() {
        return modelYear;
    }

    public void setModelYear(int modelYear) {
        this.modelYear = modelYear;
    }

    public QualificationStatus getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(QualificationStatus qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
