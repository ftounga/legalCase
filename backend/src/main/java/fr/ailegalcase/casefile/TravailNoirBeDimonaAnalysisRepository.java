package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-26 : accès JPA aux {@link TravailNoirBeDimonaAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface TravailNoirBeDimonaAnalysisRepository
        extends JpaRepository<TravailNoirBeDimonaAnalysis, UUID> {

    Optional<TravailNoirBeDimonaAnalysis> findByCaseFileId(UUID caseFileId);
}
