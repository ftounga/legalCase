package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-216-01 : repository JPA pour la prestation compensatoire (art. 270-281 Cciv).
 */
public interface PrestationCompensatoireRepositorySF216
        extends JpaRepository<PrestationCompensatoireAnalysisSF216, UUID> {

    Optional<PrestationCompensatoireAnalysisSF216> findByCaseFileId(UUID caseFileId);
}
