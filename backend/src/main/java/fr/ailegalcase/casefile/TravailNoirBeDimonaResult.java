package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-26 : résultat brut produit par
 * {@link TravailNoirBeDimonaChecker} — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link TravailNoirBeDimonaService} dans
 * {@link TravailNoirBeDimonaResponse}).</p>
 *
 * <p>Outputs dérivés : verdict DIMONA, durée non déclarée en jours,
 * cotisations ONSS rétroactives employeur + travailleur, amende
 * ONSS forfaitaire 3 × cotisations, bornes amende pénale art. 181
 * C. pén. soc. niveau 4 (300/600 € à 3000/6000 €), emprisonnement
 * 6 à 36 mois, drapeaux majorations (× nb travailleurs, × 5 personne
 * morale, × 2 récidive art. 110).</p>
 */
public record TravailNoirBeDimonaResult(
        // Inputs (snapshot)
        TravailNoirBeDimonaStatutDimona statutDimona,
        LocalDate dateDebutOccupation,
        LocalDate dateDimonaEffective,
        LocalDate dateControle,
        BigDecimal salaireBrutMensuel,
        int nombreTravailleursConcernes,
        boolean personneMorale,
        boolean recidiveDansLAn,
        TravailNoirBeDimonaElementsSubordination elementsSubordination,

        // Outputs analytiques
        TravailNoirBeDimonaVerdict verdict,
        long dureeNonDeclareeJours,
        BigDecimal cotisationsOnssEmployeur,
        BigDecimal cotisationsOnssTravailleur,
        BigDecimal cotisationsOnssTotal,
        BigDecimal amendeOnssForfaitaire3x,
        boolean sanctionPenaleNiveau4Applicable,
        int amendePenaleAdminMin,
        int amendePenaleAdminMax,
        int amendePenaleMin,
        int amendePenaleMax,
        boolean emprisonnementApplicable,
        int emprisonnementMinMois,
        int emprisonnementMaxMois,
        boolean multiplicationParTravailleur,
        boolean majorationPersonneMorale,
        boolean majorationRecidive,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
