package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PdvRccConformiteRepository
        extends JpaRepository<PdvRccConformiteAnalysis, UUID> {

    Optional<PdvRccConformiteAnalysis> findByCaseFileId(UUID caseFileId);
}
