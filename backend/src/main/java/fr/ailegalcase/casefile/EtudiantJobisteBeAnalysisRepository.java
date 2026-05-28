package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-13 : accès JPA aux {@link EtudiantJobisteBeAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface EtudiantJobisteBeAnalysisRepository
        extends JpaRepository<EtudiantJobisteBeAnalysis, UUID> {

    Optional<EtudiantJobisteBeAnalysis> findByCaseFileId(UUID caseFileId);
}
