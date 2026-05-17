package fr.ailegalcase.casefile.conclusion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-98 / SF-98-52 — accès aux versions de conclusions. Relation 1:N avec
 * {@code case_files} : un dossier porte une liste de versions, lues triées par
 * {@code version_number} décroissant (version la plus récente d'abord).
 */
public interface CaseConclusionRepository extends JpaRepository<CaseConclusion, UUID> {

    /** Toutes les versions du dossier, version la plus récente d'abord. */
    List<CaseConclusion> findByCaseFileIdOrderByVersionNumberDesc(UUID caseFileId);

    /** Version la plus récente du dossier, ou vide si aucune. */
    Optional<CaseConclusion> findFirstByCaseFileIdOrderByVersionNumberDesc(UUID caseFileId);

    /** Vrai si le dossier porte au moins une version dans l'un des statuts donnés. */
    boolean existsByCaseFileIdAndStatusIn(UUID caseFileId, Collection<CaseConclusionStatus> statuses);

    /** Charge une version par id en s'assurant qu'elle est rattachée au dossier (isolation). */
    Optional<CaseConclusion> findByIdAndCaseFileId(UUID id, UUID caseFileId);
}
