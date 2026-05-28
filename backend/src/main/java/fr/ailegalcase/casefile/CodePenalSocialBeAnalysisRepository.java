package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-219-24 : accès JPA aux {@link CodePenalSocialBeAnalysis} —
 * unicité fonctionnelle sur {@code case_file_id} garantie par la
 * migration Liquibase (contrainte UNIQUE + index).
 */
public interface CodePenalSocialBeAnalysisRepository
        extends JpaRepository<CodePenalSocialBeAnalysis, UUID> {

    Optional<CodePenalSocialBeAnalysis> findByCaseFileId(UUID caseFileId);
}
