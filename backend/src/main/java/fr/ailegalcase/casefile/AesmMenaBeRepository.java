package fr.ailegalcase.casefile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * SF-215-11 : repository de l'analyse AESM + tutelle MENA BE — F-IM-30-aesm-mena-be
 * (Loi 04/05/2007 tutelle MENA + loi 15/12/1980 art. 9bis adapté MENA + circulaire
 * OE 15/09/2005).
 */
public interface AesmMenaBeRepository extends JpaRepository<AesmMenaBeAnalysis, UUID> {

    Optional<AesmMenaBeAnalysis> findByCaseFileId(UUID caseFileId);
}
