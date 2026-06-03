package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DipBeReconnaissanceDecisionEtrangereRepository
        extends JpaRepository<DipBeReconnaissanceDecisionEtrangereAnalysis, UUID> {

    Optional<DipBeReconnaissanceDecisionEtrangereAnalysis> findByCaseFileId(UUID caseFileId);
}
