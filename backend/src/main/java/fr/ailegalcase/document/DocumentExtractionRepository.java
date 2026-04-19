package fr.ailegalcase.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, UUID> {

    Optional<DocumentExtraction> findByDocumentId(UUID documentId);

    boolean existsByDocumentCaseFileIdAndExtractionStatusIn(UUID caseFileId, Collection<ExtractionStatus> statuses);

    @Query("SELECT e.document.caseFile.id FROM DocumentExtraction e WHERE e.id = :id")
    Optional<UUID> findCaseFileIdById(@Param("id") UUID id);

    List<DocumentExtraction> findByDocumentIdIn(Collection<UUID> documentIds);

    void deleteByDocumentIdIn(Collection<UUID> documentIds);

    /**
     * SF-122-05 : récupère les extractions FAILED d'un dossier avec un motif éligible à
     * l'OCR retry (EMPTY_TEXT ou OCR_FAILED) — exclut UNSUPPORTED_FORMAT / CORRUPTED /
     * OCR_UNSUPPORTED_SIZE pour lesquels l'OCR n'aiderait pas.
     */
    @Query("""
            SELECT e FROM DocumentExtraction e
            WHERE e.document.caseFile.id = :caseFileId
              AND e.extractionStatus = fr.ailegalcase.document.ExtractionStatus.FAILED
              AND e.failureReason IN :reasons
            """)
    List<DocumentExtraction> findRetryableByCaseFile(
            @Param("caseFileId") UUID caseFileId,
            @Param("reasons") Collection<ExtractionFailureReason> reasons);
}
