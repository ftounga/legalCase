package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-09 : repository JPA pour les analyses délégation autorité parentale
 * FR (art. 376-1 Cciv).
 */
public interface DelegationApFrRepository
        extends JpaRepository<DelegationApFrAnalysis, UUID> {

    Optional<DelegationApFrAnalysis> findByCaseFileId(UUID caseFileId);
}
