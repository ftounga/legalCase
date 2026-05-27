package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-05 : accès JPA aux {@link OutplacementBeGeneral30semAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface OutplacementBeGeneral30semAnalysisRepository
        extends JpaRepository<OutplacementBeGeneral30semAnalysis, UUID> {

    Optional<OutplacementBeGeneral30semAnalysis> findByCaseFileId(UUID caseFileId);
}
