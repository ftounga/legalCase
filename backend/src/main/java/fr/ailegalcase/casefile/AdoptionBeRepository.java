package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdoptionBeRepository
        extends JpaRepository<AdoptionBeAnalysis, UUID> {

    Optional<AdoptionBeAnalysis> findByCaseFileId(UUID caseFileId);
}
