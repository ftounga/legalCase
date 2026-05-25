package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-213-01 : résultat brut produit par {@link ClauseNonConcurrenceBeValidator}.
 *
 * <p>Fonction pure — n'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link ClauseNonConcurrenceBeService} dans {@link ClauseNonConcurrenceBeResponse}).</p>
 */
public record ClauseNonConcurrenceBeResult(
        BigDecimal remunerationAnnuelleBrute,
        int dureeMois,
        ClauseNonConcurrenceBeZoneEnum zoneGeographique,
        boolean activiteInternationaleProuvee,
        BigDecimal salaireAnnuelSeuil,
        ClauseNonConcurrenceBeVerdict verdict,
        ClauseNonConcurrenceBeRaisonNullite raisonNullite,
        BigDecimal indemniteLegale,
        String indemniteLegaleFormule,
        String baseJuridique,
        String avertissement
) {}
