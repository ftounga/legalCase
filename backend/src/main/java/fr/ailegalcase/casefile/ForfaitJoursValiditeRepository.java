package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ForfaitJoursValiditeRepository
        extends JpaRepository<ForfaitJoursValiditeAnalysis, UUID> {

    Optional<ForfaitJoursValiditeAnalysis> findByCaseFileId(UUID caseFileId);
}
