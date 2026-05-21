package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MariageEtrangerBeReconnaissanceRepository
        extends JpaRepository<MariageEtrangerBeReconnaissanceAnalysis, UUID> {

    Optional<MariageEtrangerBeReconnaissanceAnalysis> findByCaseFileId(UUID caseFileId);
}
