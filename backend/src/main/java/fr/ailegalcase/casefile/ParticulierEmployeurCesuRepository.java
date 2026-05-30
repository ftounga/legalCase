package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ParticulierEmployeurCesuRepository
        extends JpaRepository<ParticulierEmployeurCesuAnalysis, UUID> {

    Optional<ParticulierEmployeurCesuAnalysis> findByCaseFileId(UUID caseFileId);
}
