package ar.edu.utn.frre.m3.adapter.persistence.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Entidad JPA que mapea la tabla 'vehicles' y su relación con 'vehicle_services'.
 */
@Entity
@Table(name = "vehicles")
public class VehicleJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "driver_id", nullable = false)
    private UUID driverId;

    @Column(name = "vehicle_type")
    private String vehicleType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "vehicle_services", joinColumns = @JoinColumn(name = "vehicle_id"))
    @Column(name = "service_type")
    private Set<String> serviceTypes = new HashSet<>();

    @Column(name = "country_code", length = 2)
    private String countryCode = "AR";

    @Column(name = "plate")
    private String plate;

    @Column(name = "make")
    private String make;

    @Column(name = "model")
    private String model;

    @Column(name = "model_year")
    private Integer modelYear;

    @Column(name = "qualification_status")
    private String qualificationStatus;

    @Column(name = "driver_version")
    private Long driverVersion;

    @Column(name = "created_at")
    private Instant createdAt;

    public VehicleJpaEntity() {
    }

    public VehicleJpaEntity(
        UUID id,
        UUID driverId,
        String vehicleType,
        Set<String> serviceTypes,
        String countryCode,
        String plate,
        String make,
        String model,
        Integer modelYear,
        String qualificationStatus,
        Long driverVersion,
        Instant createdAt
    ) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleType = vehicleType;
        this.serviceTypes = serviceTypes != null ? serviceTypes : new HashSet<>();
        this.countryCode = countryCode != null ? countryCode : "AR";
        this.plate = plate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.qualificationStatus = qualificationStatus;
        this.driverVersion = driverVersion;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getDriverId() { return driverId; }
    public void setDriverId(UUID driverId) { this.driverId = driverId; }

    public String getVehicleType() { return vehicleType; }
    public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

    public Set<String> getServiceTypes() { return serviceTypes; }
    public void setServiceTypes(Set<String> serviceTypes) { this.serviceTypes = serviceTypes; }

    public String getCountryCode() { return countryCode; }
    public void setCountryCode(String countryCode) { this.countryCode = countryCode; }

    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }

    public String getMake() { return make; }
    public void setMake(String make) { this.make = make; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Integer getModelYear() { return modelYear; }
    public void setModelYear(Integer modelYear) { this.modelYear = modelYear; }

    public String getQualificationStatus() { return qualificationStatus; }
    public void setQualificationStatus(String qualificationStatus) { this.qualificationStatus = qualificationStatus; }

    public Long getDriverVersion() { return driverVersion; }
    public void setDriverVersion(Long driverVersion) { this.driverVersion = driverVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}