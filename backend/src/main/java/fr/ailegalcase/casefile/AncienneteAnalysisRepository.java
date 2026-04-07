package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AncienneteAnalysisRepository extends JpaRepository<AncienneteAnalysis, UUID> {

    Optional<AncienneteAnalysis> findByCaseFileId(UUID caseFileId);
}
