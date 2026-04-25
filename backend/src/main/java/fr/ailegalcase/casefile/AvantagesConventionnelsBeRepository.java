package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AvantagesConventionnelsBeRepository
        extends JpaRepository<AvantagesConventionnelsBeAnalysis, UUID> {

    Optional<AvantagesConventionnelsBeAnalysis> findByCaseFileId(UUID caseFileId);
}
