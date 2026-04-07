package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LicenciementAnalysisRepository extends JpaRepository<LicenciementAnalysis, UUID> {

    Optional<LicenciementAnalysis> findByCaseFileId(UUID caseFileId);
}
