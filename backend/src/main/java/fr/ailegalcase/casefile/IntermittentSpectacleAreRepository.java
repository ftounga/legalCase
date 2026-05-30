package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IntermittentSpectacleAreRepository
        extends JpaRepository<IntermittentSpectacleAreAnalysis, UUID> {

    Optional<IntermittentSpectacleAreAnalysis> findByCaseFileId(UUID caseFileId);
}
