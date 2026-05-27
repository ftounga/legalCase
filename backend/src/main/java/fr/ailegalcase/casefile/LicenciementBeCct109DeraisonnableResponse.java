package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * SF-213-10 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/licenciement-be-cct109-deraisonnable}.
 *
 * <p>Snapshot complet (inputs + outputs : échelon, indemnité, justification,
 * cumul ICP, avertissement) — pattern uniforme avec les autres outils
 * décisionnels BE de la F-213 (miroir
 * {@link LicenciementBeProtectionDelegueeResponse} SF-213-08).</p>
 */
public record LicenciementBeCct109DeraisonnableResponse(
        UUID caseFileId,

        Boolean motifCommunique,
        LicenciementBeCct109MotifLieAPersonneEnum motifLieAPersonne,
        boolean discriminationSuspectee,
        boolean represaillesSuspectees,
        boolean proceduresRespectees,
        BigDecimal remunerationHebdomadaireBrute,
        String argumentsPatronal,

        LicenciementBeCct109EchelonEnum echelonCct109,
        int nombreSemaines,
        BigDecimal indemniteCct109,
        String justificationEchelon,
        boolean cumulAvecIcp,
        String baseJuridique,
        String avertissement
) {
}
