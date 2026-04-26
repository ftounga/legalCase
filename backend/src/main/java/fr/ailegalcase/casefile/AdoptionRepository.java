package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AdoptionRepository
        extends JpaRepository<AdoptionAnalysis, UUID> {

    Optional<AdoptionAnalysis> findByCaseFileId(UUID caseFileId);
}
