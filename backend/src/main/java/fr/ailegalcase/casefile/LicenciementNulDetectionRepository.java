package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LicenciementNulDetectionRepository
        extends JpaRepository<LicenciementNulDetectionAnalysis, UUID> {

    Optional<LicenciementNulDetectionAnalysis> findByCaseFileId(UUID caseFileId);
}
