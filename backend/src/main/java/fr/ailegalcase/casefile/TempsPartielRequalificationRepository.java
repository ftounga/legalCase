package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TempsPartielRequalificationRepository
        extends JpaRepository<TempsPartielRequalificationAnalysis, UUID> {

    Optional<TempsPartielRequalificationAnalysis> findByCaseFileId(UUID caseFileId);
}
