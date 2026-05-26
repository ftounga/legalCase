package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-03 : accès JPA aux {@link LicenciementBeStatutUniquePreavisAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface LicenciementBeStatutUniquePreavisAnalysisRepository
        extends JpaRepository<LicenciementBeStatutUniquePreavisAnalysis, UUID> {

    Optional<LicenciementBeStatutUniquePreavisAnalysis> findByCaseFileId(UUID caseFileId);
}
