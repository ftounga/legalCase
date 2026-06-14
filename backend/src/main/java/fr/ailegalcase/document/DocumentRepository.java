package fr.ailegalcase.document;

import fr.ailegalcase.casefile.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCaseFileOrderByCreatedAtDesc(CaseFile caseFile);

    /** Charge un document par id en s'assurant qu'il est rattaché au dossier (isolation). */
    Optional<Document> findByIdAndCaseFile_Id(UUID id, UUID caseFileId);

    List<Document> findByCaseFile_IdOrderByCreatedAtDesc(UUID caseFileId);

    /**
     * F-283 / SF-283-02 — pièces ajoutées après un horodatage (vague de pièces
     * en attente d'analyse), les plus récentes d'abord.
     */
    List<Document> findByCaseFile_IdAndCreatedAtAfterOrderByCreatedAtDesc(UUID caseFileId, Instant since);

    long countByCaseFileId(UUID caseFileId);

    List<Document> findByCaseFileIdIn(Collection<UUID> caseFileIds);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.caseFile.workspace.id = :workspaceId AND d.createdAt >= :since")
    long countByWorkspaceIdAndCreatedAtAfter(@Param("workspaceId") UUID workspaceId, @Param("since") Instant since);
}
