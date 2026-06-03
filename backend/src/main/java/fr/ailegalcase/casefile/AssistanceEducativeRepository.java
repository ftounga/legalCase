package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-222-04 : repository JPA pour l'analyse assistance éducative — mineur en
 * danger (art. 375 et s. Cciv).
 */
public interface AssistanceEducativeRepository extends JpaRepository<AssistanceEducativeAnalysis, UUID> {

    Optional<AssistanceEducativeAnalysis> findByCaseFileId(UUID caseFileId);
}
