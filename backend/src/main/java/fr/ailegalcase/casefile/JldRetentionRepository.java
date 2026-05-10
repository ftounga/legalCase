package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JldRetentionRepository extends JpaRepository<JldRetentionAnalysis, UUID> {

    Optional<JldRetentionAnalysis> findByCaseFileId(UUID caseFileId);
}
