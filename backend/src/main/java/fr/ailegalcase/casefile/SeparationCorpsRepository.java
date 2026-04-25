package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SeparationCorpsRepository extends JpaRepository<SeparationCorpsAnalysis, UUID> {

    Optional<SeparationCorpsAnalysis> findByCaseFileId(UUID caseFileId);
}
