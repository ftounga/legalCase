package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-28 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/mp-fedris-reconnaissance}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link TravailNoirBeDimonaResponse} SF-219-26).</p>
 */
public record MpFedrisReconnaissanceResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        MpFedrisReconnaissanceTypeMaladie typeMaladie,
        String codeMaladieListe,
        String libelleMaladie,
        LocalDate dateConstatationMedicale,
        LocalDate dateConnaissanceProfessionnel,
        LocalDate dateDeclarationEnvisagee,
        MpFedrisReconnaissanceExposition exposition,
        MpFedrisReconnaissanceCausalite causalite,

        // Outputs
        MpFedrisReconnaissanceVerdict verdict,
        boolean presomptionApplicable,
        boolean causaliteRequise,
        LocalDate datePrescription,
        boolean prescriteAuJour,
        long joursAvantPrescription,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
