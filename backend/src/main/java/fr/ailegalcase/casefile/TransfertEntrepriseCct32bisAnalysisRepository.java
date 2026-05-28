package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-08 : accès JPA aux {@link
 * TransfertEntrepriseCct32bisAnalysis} — unicité fonctionnelle sur
 * {@code case_file_id} garantie par la migration Liquibase (contrainte
 * UNIQUE + index).
 */
public interface TransfertEntrepriseCct32bisAnalysisRepository
        extends JpaRepository<TransfertEntrepriseCct32bisAnalysis, UUID> {

    Optional<TransfertEntrepriseCct32bisAnalysis> findByCaseFileId(UUID caseFileId);
}
