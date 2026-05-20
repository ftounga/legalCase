package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CongesPayesArretMaladieRepository
        extends JpaRepository<CongesPayesArretMaladieAnalysis, UUID> {

    Optional<CongesPayesArretMaladieAnalysis> findByCaseFileId(UUID caseFileId);
}
