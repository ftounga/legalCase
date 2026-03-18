package fr.ailegalcase.casefile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CaseFileRepository extends JpaRepository<CaseFile, UUID> {

    Page<CaseFile> findByWorkspace(fr.ailegalcase.workspace.Workspace workspace, Pageable pageable);

    @Query("SELECT c.createdBy.id FROM CaseFile c WHERE c.id = :id")
    Optional<UUID> findCreatedByUserIdById(@Param("id") UUID id);

    long countByWorkspace_IdAndStatus(UUID workspaceId, String status);
}
