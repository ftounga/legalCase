package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RttMonetisationRepository
        extends JpaRepository<RttMonetisationAnalysis, UUID> {

    Optional<RttMonetisationAnalysis> findByCaseFileId(UUID caseFileId);
}
