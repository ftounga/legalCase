package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-08 : accès JPA aux {@link LicenciementBeProtectionDelegueeAnalysis}
 * — unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface LicenciementBeProtectionDelegueeAnalysisRepository
        extends JpaRepository<LicenciementBeProtectionDelegueeAnalysis, UUID> {

    Optional<LicenciementBeProtectionDelegueeAnalysis> findByCaseFileId(UUID caseFileId);
}
