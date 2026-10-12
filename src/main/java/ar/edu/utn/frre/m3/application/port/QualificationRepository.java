package ar.edu.utn.frre.m3.application.port;
import ar.edu.utn.frre.m3.domain.SavedQualification;
import ar.edu.utn.frre.m3.domain.QualificationDecision;
import java.util.*;
public interface QualificationRepository {
    Optional<SavedQualification> findByKey(UUID targetId, String actor, String key);
    void save(QualificationDecision decision, String key, String fingerprint);
    List<QualificationDecision> history(UUID driverId, UUID vehicleId, int offset, int limit);
}
