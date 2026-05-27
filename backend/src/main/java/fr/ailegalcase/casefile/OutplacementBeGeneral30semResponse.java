package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-05 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/outplacement-be-general-30sem}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 / F-219 (miroir SF-219-03 / SF-219-04).</p>
 */
public record OutplacementBeGeneral30semResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        boolean licenciementEffectif,
        boolean motifGrave,
        Integer semainesPreavisOuIndemnite,
        LocalDate dateFinContrat,
        boolean offreNotifiee,
        LocalDate dateNotificationOffre,
        Integer heuresProgramme,
        Integer dureeProgrammeMois,
        boolean offreEcrite,
        boolean offreIndividuelle,
        boolean contenuMinimumRespecte,

        // Verdict et conformité
        OutplacementBeGeneral30semVerdict verdict,
        boolean conforme,
        boolean conditionLicenciementRemplie,
        boolean conditionMotifNonGraveRemplie,
        boolean conditionPreavis30semRemplie,
        boolean conditionOffreDansLes15joursRemplie,
        boolean conditionDureeProgrammeRemplie,
        boolean conditionFormeOffreRemplie,

        // Sanction indicative
        BigDecimal indemniteForfaitaireTravailleur,

        // Synthèse + métadonnées légales
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
