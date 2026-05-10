package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MediationFamilialePreSaisineRepository
        extends JpaRepository<MediationFamilialePreSaisineAnalysis, UUID> {

    Optional<MediationFamilialePreSaisineAnalysis> findByCaseFileId(UUID caseFileId);
}
