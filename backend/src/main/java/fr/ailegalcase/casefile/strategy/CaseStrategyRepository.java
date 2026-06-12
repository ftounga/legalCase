package fr.ailegalcase.casefile.strategy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * F-286 / SF-286-01 — accès à la stratégie courante d'un dossier (1 ligne par dossier).
 */
public interface CaseStrategyRepository extends JpaRepository<CaseStrategy, UUID> {

    Optional<CaseStrategy> findByCaseFileId(UUID caseFileId);
}
