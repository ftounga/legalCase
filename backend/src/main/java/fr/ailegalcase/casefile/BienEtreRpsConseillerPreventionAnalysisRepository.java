package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-30 : accès JPA aux
 * {@link BienEtreRpsConseillerPreventionAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface BienEtreRpsConseillerPreventionAnalysisRepository
        extends JpaRepository<BienEtreRpsConseillerPreventionAnalysis, UUID> {

    Optional<BienEtreRpsConseillerPreventionAnalysis> findByCaseFileId(
            UUID caseFileId);
}
