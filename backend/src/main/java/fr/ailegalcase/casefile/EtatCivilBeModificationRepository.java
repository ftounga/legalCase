package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EtatCivilBeModificationRepository
        extends JpaRepository<EtatCivilBeModificationAnalysis, UUID> {

    Optional<EtatCivilBeModificationAnalysis> findByCaseFileId(UUID caseFileId);
}
