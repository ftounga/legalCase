package fr.ailegalcase.casefile;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CaseFileRepository extends JpaRepository<CaseFile, UUID> {

    Page<CaseFile> findByWorkspace(fr.ailegalcase.workspace.Workspace workspace, Pageable pageable);
}
