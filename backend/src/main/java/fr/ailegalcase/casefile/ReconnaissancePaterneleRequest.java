package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-18-01 : requête d'analyse de recevabilité d'une reconnaissance
 * paternelle (FR — art. 316 Cciv).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record ReconnaissancePaterneleRequest(
        ReconnaissancePaterneleCalculator.SousType sousType,
        LocalDate dateNaissanceEnfant,
        LocalDate dateReconnaissance,
        Boolean consentementLibreDuPere,
        Boolean paterniteVraisemblable,
        Boolean enfantNonReconnuParAutrePere,
        Boolean procedureRespectee,
        Boolean presenceParProcuration
) {}
