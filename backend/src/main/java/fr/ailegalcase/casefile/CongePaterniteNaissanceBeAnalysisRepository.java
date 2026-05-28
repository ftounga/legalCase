package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-31 : accès JPA aux
 * {@link CongePaterniteNaissanceBeAnalysis} — unicité fonctionnelle sur
 * {@code case_file_id} garantie par la migration Liquibase (contrainte
 * UNIQUE + index).
 */
public interface CongePaterniteNaissanceBeAnalysisRepository
        extends JpaRepository<CongePaterniteNaissanceBeAnalysis, UUID> {

    Optional<CongePaterniteNaissanceBeAnalysis> findByCaseFileId(
            UUID caseFileId);
}
