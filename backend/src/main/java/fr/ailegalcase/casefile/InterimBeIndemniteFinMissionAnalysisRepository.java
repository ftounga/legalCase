package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-15 : accès JPA aux {@link InterimBeIndemniteFinMissionAnalysis}
 * — unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface InterimBeIndemniteFinMissionAnalysisRepository
        extends JpaRepository<InterimBeIndemniteFinMissionAnalysis, UUID> {

    Optional<InterimBeIndemniteFinMissionAnalysis> findByCaseFileId(UUID caseFileId);
}
