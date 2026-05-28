package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-219-28 : résultat brut produit par
 * {@link MpFedrisReconnaissanceChecker} — fonction <b>pure</b> (sans
 * dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link MpFedrisReconnaissanceService} dans
 * {@link MpFedrisReconnaissanceResponse}).</p>
 *
 * <p>Outputs dérivés : verdict reconnaissance, prescription
 * (datePrescription = dateConnaissanceProfessionnel + 3 ans, drapeau
 * prescriteAuJour, joursAvantPrescription), drapeau
 * presomptionApplicable (liste fermée + exposition AVEREE), drapeau
 * causaliteRequise (système ouvert), base juridique stable +
 * avertissement avocat BE.</p>
 */
public record MpFedrisReconnaissanceResult(
        // Inputs (snapshot)
        MpFedrisReconnaissanceTypeMaladie typeMaladie,
        String codeMaladieListe,
        String libelleMaladie,
        LocalDate dateConstatationMedicale,
        LocalDate dateConnaissanceProfessionnel,
        LocalDate dateDeclarationEnvisagee,
        MpFedrisReconnaissanceExposition exposition,
        MpFedrisReconnaissanceCausalite causalite,

        // Outputs analytiques
        MpFedrisReconnaissanceVerdict verdict,
        boolean presomptionApplicable,
        boolean causaliteRequise,
        LocalDate datePrescription,
        boolean prescriteAuJour,
        long joursAvantPrescription,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
