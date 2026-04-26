package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PossessionEtatRepository
        extends JpaRepository<PossessionEtatAnalysis, UUID> {

    Optional<PossessionEtatAnalysis> findByCaseFileId(UUID caseFileId);
}
