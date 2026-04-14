package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RuptureConvAnalysisRepository extends JpaRepository<RuptureConvAnalysis, UUID> {

    Optional<RuptureConvAnalysis> findByCaseFileId(UUID caseFileId);
}
