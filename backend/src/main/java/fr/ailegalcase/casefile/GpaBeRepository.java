package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GpaBeRepository
        extends JpaRepository<GpaBeAnalysis, UUID> {

    Optional<GpaBeAnalysis> findByCaseFileId(UUID caseFileId);
}
