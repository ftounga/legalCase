package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-04 : accès JPA aux {@link LicenciementBeFormuleClaeysAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface LicenciementBeFormuleClaeysAnalysisRepository
        extends JpaRepository<LicenciementBeFormuleClaeysAnalysis, UUID> {

    Optional<LicenciementBeFormuleClaeysAnalysis> findByCaseFileId(UUID caseFileId);
}
