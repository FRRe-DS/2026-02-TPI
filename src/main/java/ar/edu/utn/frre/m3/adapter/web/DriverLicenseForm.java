package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.domain.ServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Set;

public class DriverLicenseForm {
    @NotBlank @Size(max = 80)
    private String documentNumber;
    @NotNull
    private LocalDate issuedAt;
    @NotNull
    private LocalDate validUntil;
    @NotEmpty
    private Set<ServiceType> serviceTypes;
    private long expectedVersion;

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }
    public LocalDate getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDate issuedAt) { this.issuedAt = issuedAt; }
    public LocalDate getValidUntil() { return validUntil; }
    public void setValidUntil(LocalDate validUntil) { this.validUntil = validUntil; }
    public Set<ServiceType> getServiceTypes() { return serviceTypes; }
    public void setServiceTypes(Set<ServiceType> serviceTypes) { this.serviceTypes = serviceTypes; }
    public long getExpectedVersion() { return expectedVersion; }
    public void setExpectedVersion(long expectedVersion) { this.expectedVersion = expectedVersion; }
}
