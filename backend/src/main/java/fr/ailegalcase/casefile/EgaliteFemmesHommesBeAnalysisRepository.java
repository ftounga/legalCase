package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-22 : accès JPA aux {@link EgaliteFemmesHommesBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface EgaliteFemmesHommesBeAnalysisRepository
        extends JpaRepository<EgaliteFemmesHommesBeAnalysis, UUID> {

    Optional<EgaliteFemmesHommesBeAnalysis> findByCaseFileId(UUID caseFileId);
}
