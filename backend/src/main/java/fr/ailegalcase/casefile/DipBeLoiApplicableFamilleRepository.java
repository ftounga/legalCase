package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DipBeLoiApplicableFamilleRepository
        extends JpaRepository<DipBeLoiApplicableFamilleAnalysis, UUID> {

    Optional<DipBeLoiApplicableFamilleAnalysis> findByCaseFileId(UUID caseFileId);
}
