package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AbandonPostePresomptionDemissionRepository
        extends JpaRepository<AbandonPostePresomptionDemissionAnalysis, UUID> {

    Optional<AbandonPostePresomptionDemissionAnalysis> findByCaseFileId(UUID caseFileId);
}
