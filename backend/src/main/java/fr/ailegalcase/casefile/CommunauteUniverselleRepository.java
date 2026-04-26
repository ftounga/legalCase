package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CommunauteUniverselleRepository
        extends JpaRepository<CommunauteUniverselleAnalysis, UUID> {

    Optional<CommunauteUniverselleAnalysis> findByCaseFileId(UUID caseFileId);
}
