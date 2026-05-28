package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-08 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/transfert-entreprise-cct-32bis}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * LicenciementBeFermetureEntrepriseResponse} SF-219-06).</p>
 */
public record TransfertEntrepriseCct32bisResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateTransfert,
        TransfertEntrepriseCct32bisTypeOperation typeOperation,
        TransfertEntrepriseCct32bisIdentiteEconomique identiteEconomique,
        boolean infoConsultPrealableRespectee,
        boolean conseilEntrepriseConsulte,
        boolean maintienContratsAutomatique,
        boolean maintienAncienneteRespecte,
        boolean maintienRemunerationRespecte,
        boolean conventionsCollectivesMaintenues,

        // Outputs
        TransfertEntrepriseCct32bisVerdict verdict,
        boolean eligibleCct32bis,
        boolean procedureConforme,
        boolean droitsMaintenus,
        LocalDate dateFinResponsabiliteSolidaire,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
