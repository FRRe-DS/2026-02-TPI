package ar.edu.utn.frre.m3.application.usecase;
import ar.edu.utn.frre.m3.application.port.*;
import ar.edu.utn.frre.m3.domain.*;
import ar.edu.utn.frre.m3.domain.exception.*;
import java.time.*;
import java.util.*;

public class QualificationService {
    private final DriverRepository drivers;
    private final VehicleRepository vehicles;
    private final DocumentRepository documents;
    private final QualificationRepository history;
    private final AuditLogRepository audit;
    private final UnitOfWork transactions;
    private final DriverAccessService access;
    private final Clock clock;
    public QualificationService(DriverRepository drivers, VehicleRepository vehicles, DocumentRepository documents,
            QualificationRepository history, AuditLogRepository audit, UnitOfWork transactions, DriverAccessService access, Clock clock) {
        this.drivers=drivers; this.vehicles=vehicles; this.documents=documents; this.history=history;
        this.audit=audit; this.transactions=transactions; this.access=access; this.clock=clock;
    }
    public QualificationDecision decide(UUID driverId, UUID vehicleId, QualificationStatus decision,
            String reason, long expectedVersion, String key) {
        access.requireOperator(); access.driver(driverId);
        if (decision == null || decision == QualificationStatus.PENDING) throw new DomainValidationException("Seleccioná una decisión administrativa válida.");
        if (reason == null || reason.isBlank() || reason.length()>500) throw new DomainValidationException("Ingresá un motivo de 1 a 500 caracteres.");
        if (key==null || key.length()<16 || key.length()>128) throw new IllegalArgumentException("Idempotency-Key debe tener entre 16 y 128 caracteres.");
        String actor=access.actor(), fingerprint=decision.name()+":"+reason.trim();
        return transactions.executeInTransaction(() -> {
            Driver driver=drivers.findByIdWithPessimisticLock(driverId).orElseThrow(() -> new DriverNotFoundException(driverId));
            Vehicle vehicle=vehicleId==null ? null : vehicles.findByIdAndDriverId(vehicleId,driverId).orElseThrow(() -> new VehicleNotFoundException(vehicleId));
            var previous=history.findByKey(vehicleId==null ? driverId : vehicleId,actor,key);
            if(previous.isPresent()) {
                if(!previous.get().fingerprint().equals(fingerprint)) throw new IdempotencyConflictException("La clave ya se usó con otra decisión.");
                return previous.get().decision();
            }
            driver.validateVersion(expectedVersion);
            QualificationStatus from=vehicle==null ? driver.getQualificationStatus() : QualificationStatus.valueOf(vehicle.getQualificationStatus());
            if(from==decision) throw new DomainValidationException("El objeto ya tiene ese estado.");
            if(decision!=QualificationStatus.SUSPENDED) {
                boolean allowed=switch(from) {
                    case PENDING -> decision==QualificationStatus.APPROVED || decision==QualificationStatus.OBSERVED || decision==QualificationStatus.REJECTED;
                    case APPROVED -> decision==QualificationStatus.OBSERVED;
                    case OBSERVED,REJECTED -> false;
                    case SUSPENDED -> true;
                };
                if(!allowed) throw new DomainValidationException("La transición requiere corregir y volver a presentar la documentación.");
            }
            Instant now=clock.instant();
            if(decision==QualificationStatus.APPROVED) {
                if(vehicle==null) requireValid(documents.findCurrentLicenseByDriverId(driverId).orElse(null),now,"licencia");
                else {
                    requireValid(documents.findCurrentByVehicleIdAndType(vehicleId,DocumentType.INSURANCE).orElse(null),now,"seguro");
                    requireValid(documents.findCurrentByVehicleIdAndType(vehicleId,DocumentType.VEHICLE_REGISTRATION).orElse(null),now,"cédula");
                }
            }
            if(vehicle==null) driver.updateQualification(new QualificationInfo(DriverStatus.valueOf(decision.name()),reason.trim()),now);
            else { driver.incrementVersion(); vehicles.save(vehicle.withQualification(decision,driver.getVersion())); }
            drivers.save(driver);
            QualificationDecision result=new QualificationDecision(UUID.randomUUID(),driverId,vehicleId,from,decision,reason.trim(),actor,driver.getVersion(),now);
            history.save(result,key,fingerprint);
            audit.save(new AuditLog(UUID.randomUUID(),vehicle==null?"DRIVER":"VEHICLE",vehicleId==null?driverId:vehicleId,
                    driverId,"QUALIFICATION",actor,from.name(),decision.name(),reason.trim(),null,now));
            return result;
        });
    }
    public List<QualificationDecision> history(UUID driverId, UUID vehicleId, int offset, int limit) {
        access.driver(driverId);
        if(vehicleId!=null && !vehicles.existsByIdAndDriverId(vehicleId,driverId)) throw new VehicleNotFoundException(vehicleId);
        if(offset<0 || limit<1 || limit>100) throw new IllegalArgumentException("Paginación inválida.");
        return history.history(driverId,vehicleId,offset,limit);
    }
    public static void requireValid(Document doc, Instant now, String name) {
        if(doc==null || !doc.isCurrent() || doc.getStatus()!=DocumentStatus.APPROVED || doc.getIssuedAt().isAfter(now) || !doc.getValidUntil().isAfter(now))
            throw new DomainValidationException("Se requiere "+name+" actual, aprobado y vigente.");
    }
}
