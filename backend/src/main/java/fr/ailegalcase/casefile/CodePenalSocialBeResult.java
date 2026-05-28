package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-24 : résultat brut produit par
 * {@link CodePenalSocialBeChecker} — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link CodePenalSocialBeService} dans
 * {@link CodePenalSocialBeResponse}).</p>
 *
 * <p>Outputs dérivés : niveau de sanction qualifié (1 à 4), bornes
 * d'amende (min / max non indexés), emprisonnement éventuel,
 * majorations cumulées (× nombre travailleurs, × 5 personne morale,
 * × 2 récidive).</p>
 */
public record CodePenalSocialBeResult(
        // Inputs (snapshot)
        CodePenalSocialBeTypeInfraction typeInfraction,
        Integer niveauPropose,
        LocalDate dateFaits,
        int nombreTravailleursConcernes,
        boolean personneMorale,
        boolean recidiveDansLAn,
        boolean prevenuPreposeOuMandataire,
        boolean elementMoralIntentionnel,

        // Outputs analytiques
        CodePenalSocialBeVerdict verdict,
        int niveauSanction,
        int amendeAdminMin,
        int amendeAdminMax,
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
