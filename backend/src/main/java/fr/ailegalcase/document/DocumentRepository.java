package fr.ailegalcase.document;

import fr.ailegalcase.casefile.CaseFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByCaseFileOrderByCreatedAtDesc(CaseFile caseFile);

    long countByCaseFileId(UUID caseFileId);

    List<Document> findByCaseFileIdIn(Collection<UUID> caseFileIds);

    void deleteByCaseFileIdIn(Collection<UUID> caseFileIds);

    @Query("SELECT COUNT(d) FROM Document d WHERE d.caseFile.workspace.id = :workspaceId AND d.createdAt >= :since")
    long countByWorkspaceIdAndCreatedAtAfter(@Param("workspaceId") UUID workspaceId, @Param("since") Instant since);
}
