package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-213-10 : accès JPA aux {@link LicenciementBeCct109DeraisonnableAnalysis}
 * — unicité fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface LicenciementBeCct109DeraisonnableAnalysisRepository
        extends JpaRepository<LicenciementBeCct109DeraisonnableAnalysis, UUID> {

    Optional<LicenciementBeCct109DeraisonnableAnalysis> findByCaseFileId(UUID caseFileId);
}
