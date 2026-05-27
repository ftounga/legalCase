package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-01 : accès JPA aux {@link RccBeMetiersLourdsAnalysis} — unicité
 * fonctionnelle sur {@code case_file_id} garantie par la migration
 * Liquibase (contrainte UNIQUE + index).
 */
public interface RccBeMetiersLourdsAnalysisRepository
        extends JpaRepository<RccBeMetiersLourdsAnalysis, UUID> {

    Optional<RccBeMetiersLourdsAnalysis> findByCaseFileId(UUID caseFileId);
}
