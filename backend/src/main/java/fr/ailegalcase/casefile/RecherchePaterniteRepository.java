package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RecherchePaterniteRepository
        extends JpaRepository<RecherchePaterniteAnalysis, UUID> {

    Optional<RecherchePaterniteAnalysis> findByCaseFileId(UUID caseFileId);
}
