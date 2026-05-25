package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CongeMaternitePaterniteRepository
        extends JpaRepository<CongeMaternitePaterniteAnalysis, UUID> {

    Optional<CongeMaternitePaterniteAnalysis> findByCaseFileId(UUID caseFileId);
}
