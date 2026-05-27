package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-06 : accès JPA aux {@link
 * LicenciementBeFermetureEntrepriseAnalysis} — unicité fonctionnelle
 * sur {@code case_file_id} garantie par la migration Liquibase
 * (contrainte UNIQUE + index).
 */
public interface LicenciementBeFermetureEntrepriseAnalysisRepository
        extends JpaRepository<LicenciementBeFermetureEntrepriseAnalysis, UUID> {

    Optional<LicenciementBeFermetureEntrepriseAnalysis> findByCaseFileId(UUID caseFileId);
}
