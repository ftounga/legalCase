package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LanceurAlerteProtectionRepository
        extends JpaRepository<LanceurAlerteProtectionAnalysis, UUID> {

    Optional<LanceurAlerteProtectionAnalysis> findByCaseFileId(UUID caseFileId);
}
