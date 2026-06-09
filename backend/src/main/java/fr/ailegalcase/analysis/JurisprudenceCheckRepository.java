package fr.ailegalcase.analysis;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * F-179 SF-179-01 — accès aux {@link JurisprudenceCheck}.
 */
public interface JurisprudenceCheckRepository extends JpaRepository<JurisprudenceCheck, UUID> {

    /** Checks produits par une analyse donnée, ordre stable par document puis référence. */
    List<JurisprudenceCheck> findByCaseAnalysisIdOrderByDocumentNameAscReferenceAsc(UUID caseAnalysisId);

    List<JurisprudenceCheck> findByCaseFileId(UUID caseFileId);

    /**
     * F-98 / SF-98-56 — checks d'un dossier marqués « adverse à réfuter » par l'avocat
     * ET dont le statut est réfutable ({@code SUSPECT} / {@code NOT_FOUND}). Seuls ces
     * checks alimentent la section de réfutation du prompt de génération des conclusions.
     */
    List<JurisprudenceCheck> findByCaseFileIdAndStatutInAndMarkedAdverseTrue(
            UUID caseFileId, Collection<JurisprudenceCheckStatus> statuts);
}
