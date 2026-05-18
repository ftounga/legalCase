package fr.ailegalcase.casefile.jurisprudence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-242 / SF-242-01 — accès aux {@link JurisprudenceCitation}.
 */
public interface JurisprudenceCitationRepository extends JpaRepository<JurisprudenceCitation, UUID> {

    /**
     * Citations d'un dossier, ordre stable par index de point juridique puis date de
     * création — garantit un regroupement déterministe dans le prompt de conclusions.
     */
    List<JurisprudenceCitation> findByCaseFileIdOrderByPointJuridiqueIndexAscCreatedAtAsc(UUID caseFileId);

    /** Charge une citation par id en exigeant son rattachement au dossier (isolation). */
    Optional<JurisprudenceCitation> findByIdAndCaseFileId(UUID id, UUID caseFileId);

    /**
     * F-242 / SF-242-01 — instant de dernière mutation parmi les citations d'un dossier
     * (alimente le calcul de péremption des conclusions, SF-98-53). {@code null} si le
     * dossier n'a aucune citation.
     */
    @Query("select max(c.updatedAt) from JurisprudenceCitation c where c.caseFile.id = :caseFileId")
    Instant findMaxUpdatedAtByCaseFileId(@Param("caseFileId") UUID caseFileId);
}
