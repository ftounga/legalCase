package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.CaseFileContext;
import fr.ailegalcase.workspace.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaseFileRepository extends JpaRepository<CaseFile, UUID> {

    Page<CaseFile> findByWorkspace(fr.ailegalcase.workspace.Workspace workspace, Pageable pageable);

    Page<CaseFile> findByWorkspaceAndDeletedAtIsNull(fr.ailegalcase.workspace.Workspace workspace, Pageable pageable);

    List<CaseFile> findByWorkspace_Id(UUID workspaceId);

    Optional<CaseFile> findByIdAndDeletedAtIsNull(UUID id);

    @Query("SELECT c.createdBy.id FROM CaseFile c WHERE c.id = :id")
    Optional<UUID> findCreatedByUserIdById(@Param("id") UUID id);

    @Query("SELECT c.workspace.id FROM CaseFile c WHERE c.id = :id")
    Optional<UUID> findWorkspaceIdById(@Param("id") UUID id);

    @Query("SELECT c.workspace.legalDomain FROM CaseFile c WHERE c.id = :id")
    Optional<String> findLegalDomainById(@Param("id") UUID id);

    @Query("SELECT c.workspace.country FROM CaseFile c WHERE c.id = :id")
    Optional<String> findCountryById(@Param("id") UUID id);

    long countByWorkspace_IdAndStatus(UUID workspaceId, String status);

    long countByWorkspace_IdAndStatusAndDeletedAtIsNull(UUID workspaceId, String status);

    long countByWorkspace_IdAndDeletedAtIsNull(UUID workspaceId);

    @Query("SELECT c.title FROM CaseFile c WHERE c.id = :id")
    Optional<String> findTitleById(@Param("id") UUID id);

    @Query("SELECT c.createdBy.email FROM CaseFile c WHERE c.id = :id")
    Optional<String> findCreatorEmailById(@Param("id") UUID id);

    List<CaseFile> findTop20ByWorkspaceAndDeletedAtIsNullAndStatusNotOrderByCreatedAtDesc(Workspace workspace, String status);

    long countByWorkspaceAndDeletedAtIsNullAndStatusNot(Workspace workspace, String status);

    @Query("SELECT new fr.ailegalcase.analysis.CaseFileContext(c.workspace.id, c.workspace.legalDomain, c.workspace.country, c.createdBy.id) FROM CaseFile c WHERE c.id = :id")
    Optional<CaseFileContext> findContextById(@Param("id") UUID id);
}
