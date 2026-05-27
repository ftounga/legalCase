package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-05 : accès JPA aux {@link LicenciementBeProtectionGrossesseAnalysis}
 * — unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface LicenciementBeProtectionGrossesseAnalysisRepository
        extends JpaRepository<LicenciementBeProtectionGrossesseAnalysis, UUID> {

    Optional<LicenciementBeProtectionGrossesseAnalysis> findByCaseFileId(UUID caseFileId);
}
