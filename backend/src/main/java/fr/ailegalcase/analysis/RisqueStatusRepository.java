package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-195 SF-195-01 — Repository pour {@link RisqueStatus}.
 */
public interface RisqueStatusRepository extends JpaRepository<RisqueStatus, UUID> {

    /** Recherche par clé d'idempotence {@code (case_file_id, risque_libelle_normalise)}. */
    Optional<RisqueStatus> findByCaseFileIdAndRisqueLibelleNormalise(UUID caseFileId,
                                                                     String risqueLibelleNormalise);

    /** Liste tous les statuts pour un dossier — utilisé par matérialisation et collectForEnrichment. */
    List<RisqueStatus> findByCaseFileId(UUID caseFileId);
}
