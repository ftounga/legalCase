package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditTempsBeRepository
        extends JpaRepository<CreditTempsBeAnalysis, UUID> {

    Optional<CreditTempsBeAnalysis> findByCaseFileId(UUID caseFileId);
}
