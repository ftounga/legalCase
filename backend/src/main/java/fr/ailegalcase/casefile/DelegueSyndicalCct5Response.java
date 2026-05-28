package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-219-10 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/delegue-syndical-cct-5}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * TransfertEntrepriseCct32bisResponse} SF-219-08).</p>
 */
public record DelegueSyndicalCct5Response(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate dateDesignation,
        int effectifEntreprise,
        int seuilSectorielRequis,
        boolean presenceOsRepresentative,
        DelegueSyndicalCct5StatutDesignation statutDesignation,
        boolean ceExistant,
        boolean cpptExistant,

        // Outputs
        DelegueSyndicalCct5Verdict verdict,
        boolean eligibleStatutDs,
        boolean entrepriseDansChamp,
        boolean designationReguliere,
        List<String> missionsExercables,
        LocalDate dateFinMandatIndicative,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
