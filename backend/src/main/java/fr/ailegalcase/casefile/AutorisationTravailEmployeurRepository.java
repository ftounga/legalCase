package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AutorisationTravailEmployeurRepository
        extends JpaRepository<AutorisationTravailEmployeurAnalysis, UUID> {

    Optional<AutorisationTravailEmployeurAnalysis> findByCaseFileId(UUID caseFileId);
}
