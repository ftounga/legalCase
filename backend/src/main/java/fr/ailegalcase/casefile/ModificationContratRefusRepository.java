package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModificationContratRefusRepository
        extends JpaRepository<ModificationContratRefusAnalysis, UUID> {

    Optional<ModificationContratRefusAnalysis> findByCaseFileId(UUID caseFileId);
}
