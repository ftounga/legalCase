package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * F-179 SF-179-01 — accès aux {@link JurisprudenceCheck}.
 */
public interface JurisprudenceCheckRepository extends JpaRepository<JurisprudenceCheck, UUID> {

    /** Checks produits par une analyse donnée, ordre stable par document puis référence. */
    List<JurisprudenceCheck> findByCaseAnalysisIdOrderByDocumentNameAscReferenceAsc(UUID caseAnalysisId);

    List<JurisprudenceCheck> findByCaseFileId(UUID caseFileId);
}
