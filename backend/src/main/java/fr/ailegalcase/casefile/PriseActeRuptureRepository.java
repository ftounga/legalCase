package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PriseActeRuptureRepository
        extends JpaRepository<PriseActeRuptureAnalysis, UUID> {

    Optional<PriseActeRuptureAnalysis> findByCaseFileId(UUID caseFileId);
}
