package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-07 : accès JPA aux {@link
 * LicenciementBeCollectifRenaultAnalysis} — unicité fonctionnelle sur
 * {@code case_file_id} garantie par la migration Liquibase (contrainte
 * UNIQUE + index).
 */
public interface LicenciementBeCollectifRenaultAnalysisRepository
        extends JpaRepository<LicenciementBeCollectifRenaultAnalysis, UUID> {

    Optional<LicenciementBeCollectifRenaultAnalysis> findByCaseFileId(UUID caseFileId);
}
