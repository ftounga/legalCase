package fr.ailegalcase.casefile.conclusion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * F-98 / SF-98-01 — accès aux projets de conclusions. Relation 1:1 avec
 * {@code case_files}, d'où la lecture par {@code findByCaseFileId}.
 */
public interface CaseConclusionRepository extends JpaRepository<CaseConclusion, UUID> {

    Optional<CaseConclusion> findByCaseFileId(UUID caseFileId);
}
