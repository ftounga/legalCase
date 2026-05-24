package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MutationClauseMobiliteRepository
        extends JpaRepository<MutationClauseMobiliteAnalysis, UUID> {

    Optional<MutationClauseMobiliteAnalysis> findByCaseFileId(UUID caseFileId);
}
