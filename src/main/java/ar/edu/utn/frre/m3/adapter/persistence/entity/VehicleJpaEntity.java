package ar.edu.utn.frre.m3.adapter.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity mapping the 'vehicles' table for read-only validation queries in HU-06.
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
        this.plate = plate;
        this.make = make;
        this.model = model;
        this.modelYear = modelYear;
        this.qualificationStatus = qualificationStatus;
        this.driverVersion = driverVersion;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
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

    public Integer getModelYear() {
        return modelYear;
    }

    public void setModelYear(Integer modelYear) {
        this.modelYear = modelYear;
    }

    public String getQualificationStatus() {
        return qualificationStatus;
    }

    public void setQualificationStatus(String qualificationStatus) {
        this.qualificationStatus = qualificationStatus;
    }

    public Long getDriverVersion() {
        return driverVersion;
    }

    public void setDriverVersion(Long driverVersion) {
        this.driverVersion = driverVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

