package ar.edu.utn.frre.m3.domain;

import java.util.Objects;

/**
 * Value Object inmutable que representa la situación de habilitación del conductor,
 * incluyendo su estado y el motivo fundamentado en caso de observación, rechazo o suspensión.
 */
public final class QualificationInfo {
    private final DriverStatus status;
    private final String reason;

    public QualificationInfo(DriverStatus status, String reason) {
        if (status == null) {
            throw new IllegalArgumentException("El estado de habilitación no puede ser nulo");
        }
        if ((status == DriverStatus.OBSERVED || status == DriverStatus.REJECTED || status == DriverStatus.SUSPENDED)
                && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Los estados OBSERVED, REJECTED o SUSPENDED requieren un motivo descriptivo");
        }
        this.status = status;
        this.reason = reason != null ? reason.trim() : null;
    }

    public static QualificationInfo pending() {
        return new QualificationInfo(DriverStatus.PENDING, "Perfil registrado, pendiente de revisión administrativa");
    }

    public static QualificationInfo approved() {
        return new QualificationInfo(DriverStatus.APPROVED, null);
    }

    public static QualificationInfo observed(String reason) {
        return new QualificationInfo(DriverStatus.OBSERVED, reason);
    }

    public static QualificationInfo rejected(String reason) {
        return new QualificationInfo(DriverStatus.REJECTED, reason);
    }

    public static QualificationInfo suspended(String reason) {
        return new QualificationInfo(DriverStatus.SUSPENDED, reason);
    }

    public DriverStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public boolean isOperable() {
        return status.isOperable();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QualificationInfo that = (QualificationInfo) o;
        return status == that.status && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, reason);
    }

    @Override
    public String toString() {
        return "QualificationInfo{" +
                "status=" + status +
                ", reason='" + reason + '\'' +
                '}';
    }
}
