package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * SF-238-03 : repository des activations manuelles d'outils décisionnels.
 */
public interface ManualToolActivationRepository extends JpaRepository<ManualToolActivation, UUID> {

    /**
     * Active = {@code deactivated_at IS NULL}.
     */
    List<ManualToolActivation> findByCaseFileIdAndDeactivatedAtIsNull(UUID caseFileId);

    Optional<ManualToolActivation> findFirstByCaseFileIdAndToolIdAndDeactivatedAtIsNull(UUID caseFileId, String toolId);
}
