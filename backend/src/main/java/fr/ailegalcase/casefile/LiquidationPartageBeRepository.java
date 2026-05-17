package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LiquidationPartageBeRepository
        extends JpaRepository<LiquidationPartageBeAnalysis, UUID> {

    Optional<LiquidationPartageBeAnalysis> findByCaseFileId(UUID caseFileId);
}
