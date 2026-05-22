package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-11 : repository JPA pour les analyses retrait autorité parentale
 * FR (art. 378-381 Cciv + loi 2022-140 LMVSS).
 */
public interface RetraitApAnalysisRepository
        extends JpaRepository<RetraitApAnalysis, UUID> {

    Optional<RetraitApAnalysis> findByCaseFileId(UUID caseFileId);
}
