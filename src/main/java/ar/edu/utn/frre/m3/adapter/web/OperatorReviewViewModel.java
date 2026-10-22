package ar.edu.utn.frre.m3.adapter.web;

import ar.edu.utn.frre.m3.domain.Document;
import ar.edu.utn.frre.m3.domain.DocumentType;
import ar.edu.utn.frre.m3.domain.Vehicle;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * ViewModel de sólo presentación para la revisión administrativa de documentos.
 *
 * <p>Agrupa la licencia del conductor y los documentos de cada vehículo, y expone
 * nombres, estados y fechas ya formateados para Thymeleaf. No modifica reglas de
 * negocio: se construye a partir de los objetos de dominio que ya entrega
 * {@code DriverWorkspaceService}.</p>
 */
public record OperatorReviewViewModel(
        List<DocumentCard> driverDocuments,
        List<VehicleGroup> vehicleGroups,
        List<HistoryRow> history
) {

    private static final ZoneId ZONE = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static OperatorReviewViewModel from(List<Vehicle> vehicles, List<Document> documents) {
        List<Vehicle> safeVehicles = vehicles != null ? vehicles : List.of();
        List<Document> safeDocuments = documents != null ? documents : List.of();

        Map<UUID, Vehicle> vehiclesById = new LinkedHashMap<>();
        for (Vehicle vehicle : safeVehicles) {
            vehiclesById.put(vehicle.getId(), vehicle);
        }

        List<DocumentCard> driverDocuments = new ArrayList<>();
        List<HistoryRow> history = new ArrayList<>();
        Map<UUID, List<DocumentCard>> cardsByVehicle = new LinkedHashMap<>();
        Map<UUID, Vehicle> unknownVehicles = new LinkedHashMap<>();

        for (Document document : safeDocuments) {
            UUID vehicleId = document.getVehicleId();
            Vehicle vehicle = vehicleId != null ? vehiclesById.get(vehicleId) : null;
            if (vehicleId != null && vehicle == null) {
                unknownVehicles.putIfAbsent(vehicleId, null);
            }
            history.add(historyRow(document, vehicle));

            if (!document.isCurrent()) {
                continue;
            }
            if (document.getDocumentType() == DocumentType.LICENSE || vehicleId == null) {
                driverDocuments.add(card(document, null));
                continue;
            }
            cardsByVehicle.computeIfAbsent(vehicleId, key -> new ArrayList<>())
                    .add(card(document, vehicleContext(vehicle)));
        }

        List<VehicleGroup> groups = new ArrayList<>();
        for (Vehicle vehicle : safeVehicles) {
            groups.add(group(vehicle, cardsByVehicle.getOrDefault(vehicle.getId(), List.of())));
        }
        for (UUID unknownId : unknownVehicles.keySet()) {
            groups.add(new VehicleGroup(
                    unknownId,
                    "Vehículo no identificado",
                    null,
                    null,
                    null,
                    "PENDING",
                    statusLabel("PENDING"),
                    List.copyOf(cardsByVehicle.getOrDefault(unknownId, List.of()))
            ));
        }

        return new OperatorReviewViewModel(
                List.copyOf(driverDocuments), List.copyOf(groups), List.copyOf(history));
    }

    private static HistoryRow historyRow(Document document, Vehicle vehicle) {
        Instant validUntil = document.getValidUntil();
        return new HistoryRow(
                document.getId(),
                document.getDocumentType().name(),
                typeLabel(document.getDocumentType()),
                document.getRevision(),
                document.getStatus() != null ? document.getStatus().name() : "PENDING",
                statusLabel(document.getStatus() != null ? document.getStatus().name() : "PENDING"),
                validUntil != null ? DATE.format(validUntil.atZone(ZONE)) : "-",
                document.isCurrent(),
                vehicle != null ? vehicleTitle(vehicle) + " · " + vehicle.getPlate() : "Conductor"
        );
    }

    private static VehicleGroup group(Vehicle vehicle, List<DocumentCard> documents) {
        return new VehicleGroup(
                vehicle.getId(),
                vehicleTitle(vehicle),
                vehicle.getPlate(),
                vehicleTypeLabel(vehicle.getVehicleType()),
                vehicle.getModelYear(),
                vehicle.getQualificationStatus(),
                statusLabel(vehicle.getQualificationStatus()),
                List.copyOf(documents)
        );
    }

    private static DocumentCard card(Document document, String vehicleContext) {
        Instant validUntil = document.getValidUntil();
        Instant reviewedAt = document.getReviewedAt();
        return new DocumentCard(
                document.getId(),
                document.getDocumentType().name(),
                typeLabel(document.getDocumentType()),
                document.getRevision(),
                document.isCurrent(),
                document.getStatus() != null ? document.getStatus().name() : "PENDING",
                statusLabel(document.getStatus() != null ? document.getStatus().name() : "PENDING"),
                validUntil != null ? DATE.format(validUntil.atZone(ZONE)) : "-",
                vehicleContext,
                blankToNull(document.getRejectionReason()),
                reviewedAt != null ? DATE_TIME.format(reviewedAt.atZone(ZONE)) : null,
                blankToNull(document.getReviewedBy())
        );
    }

    private static String vehicleTitle(Vehicle vehicle) {
        if (vehicle == null) {
            return "Vehículo";
        }
        String make = vehicle.getMake() != null ? vehicle.getMake().trim() : "";
        String model = vehicle.getModel() != null ? vehicle.getModel().trim() : "";
        String title = (make + " " + model).trim();
        return title.isBlank() ? "Vehículo" : title;
    }

    private static String vehicleContext(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        String plate = vehicle.getPlate() != null ? vehicle.getPlate() : "";
        return (vehicleTitle(vehicle) + " · " + plate).trim();
    }

    public static String typeLabel(DocumentType type) {
        if (type == null) {
            return "Documento";
        }
        return switch (type) {
            case LICENSE -> "Licencia de conducir";
            case INSURANCE -> "Seguro";
            case VEHICLE_REGISTRATION -> "Documentación del vehículo";
        };
    }

    public static String statusLabel(String status) {
        if (status == null || status.isBlank()) {
            return "Pendiente";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "APPROVED" -> "Aprobado";
            case "PENDING" -> "Pendiente";
            case "OBSERVED" -> "Observado";
            case "REJECTED" -> "Rechazado";
            case "SUSPENDED" -> "Suspendido";
            default -> status;
        };
    }

    private static String vehicleTypeLabel(String type) {
        if (type == null || type.isBlank()) {
            return "Vehículo";
        }
        return switch (type.toUpperCase(Locale.ROOT)) {
            case "AUTO" -> "Auto";
            case "MOTO" -> "Moto";
            default -> type;
        };
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record DocumentCard(
            UUID id,
            String typeCode,
            String typeLabel,
            int revision,
            boolean current,
            String status,
            String statusLabel,
            String validUntil,
            String vehicleContext,
            String rejectionReason,
            String reviewedAt,
            String reviewedBy
    ) {
        // Accesores JavaBean para Thymeleaf (mismo criterio que DriverProfileViewModel).
        public UUID getId() { return id; }
        public String getTypeCode() { return typeCode; }
        public String getTypeLabel() { return typeLabel; }
        public int getRevision() { return revision; }
        public boolean isCurrent() { return current; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return statusLabel; }
        public String getValidUntil() { return validUntil; }
        public String getVehicleContext() { return vehicleContext; }
        public String getRejectionReason() { return rejectionReason; }
        public String getReviewedAt() { return reviewedAt; }
        public String getReviewedBy() { return reviewedBy; }
    }

    public record VehicleGroup(
            UUID id,
            String title,
            String plate,
            String typeLabel,
            Integer modelYear,
            String qualificationStatus,
            String qualificationStatusLabel,
            List<DocumentCard> documents
    ) {
        // Accesores JavaBean para Thymeleaf.
        public UUID getId() { return id; }
        public String getTitle() { return title; }
        public String getPlate() { return plate; }
        public String getTypeLabel() { return typeLabel; }
        public Integer getModelYear() { return modelYear; }
        public String getQualificationStatus() { return qualificationStatus; }
        public String getQualificationStatusLabel() { return qualificationStatusLabel; }
        public List<DocumentCard> getDocuments() { return documents; }

        public String getMetaLine() {
            StringBuilder meta = new StringBuilder();
            if (plate != null && !plate.isBlank()) {
                meta.append(plate);
            }
            if (typeLabel != null && !typeLabel.isBlank()) {
                if (!meta.isEmpty()) meta.append(" · ");
                meta.append(typeLabel);
            }
            if (modelYear != null) {
                if (!meta.isEmpty()) meta.append(" · ");
                meta.append(modelYear);
            }
            return meta.toString();
        }
    }

    public record HistoryRow(
            UUID id,
            String typeCode,
            String typeLabel,
            int revision,
            String status,
            String statusLabel,
            String validUntil,
            boolean current,
            String ownerLabel
    ) {
        // Accesores JavaBean para Thymeleaf.
        public UUID getId() { return id; }
        public String getTypeCode() { return typeCode; }
        public String getTypeLabel() { return typeLabel; }
        public int getRevision() { return revision; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return statusLabel; }
        public String getValidUntil() { return validUntil; }
        public boolean isCurrent() { return current; }
        public String getOwnerLabel() { return ownerLabel; }
    }
}
