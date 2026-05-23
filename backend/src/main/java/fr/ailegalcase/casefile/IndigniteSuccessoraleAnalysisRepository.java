package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-19 : repository JPA pour les analyses Indignité successorale FR
 * (art. 726-729-1 Cciv).
 */
public interface IndigniteSuccessoraleAnalysisRepository
        extends JpaRepository<IndigniteSuccessoraleAnalysis, UUID> {

    Optional<IndigniteSuccessoraleAnalysis> findByCaseFileId(UUID caseFileId);
}
