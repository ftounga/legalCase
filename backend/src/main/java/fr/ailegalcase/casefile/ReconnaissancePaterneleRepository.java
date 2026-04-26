package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReconnaissancePaterneleRepository
        extends JpaRepository<ReconnaissancePaterneleAnalysis, UUID> {

    Optional<ReconnaissancePaterneleAnalysis> findByCaseFileId(UUID caseFileId);
}
