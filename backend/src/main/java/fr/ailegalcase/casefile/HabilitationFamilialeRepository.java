package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-222-03 : repository JPA pour l'analyse des conditions de l'habilitation
 * familiale (art. 494-1 et s. Cciv).
 */
public interface HabilitationFamilialeRepository extends JpaRepository<HabilitationFamilialeAnalysis, UUID> {

    Optional<HabilitationFamilialeAnalysis> findByCaseFileId(UUID caseFileId);
}
