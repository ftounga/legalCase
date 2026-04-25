package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PacsDissolutionRepository extends JpaRepository<PacsDissolutionAnalysis, UUID> {

    Optional<PacsDissolutionAnalysis> findByCaseFileId(UUID caseFileId);
}
