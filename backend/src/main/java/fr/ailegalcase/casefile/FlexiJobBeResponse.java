package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-12 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/flexi-job-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir {@link
 * DelegueSyndicalCct5Response} SF-219-10).</p>
 */
public record FlexiJobBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        LocalDate datePremiereOccupationFlexi,
        FlexiJobBeStatutTravailleur statutTravailleur,
        FlexiJobBeSecteurEmployeur secteurEmployeur,
        boolean dejaOccupeChezMemeEmployeur,
        boolean contratCadreSigne,
        boolean dimonaFlxDeclaree,
        BigDecimal flexiSalaireHoraireBrut,
        BigDecimal flexiSalaireMinimumApplicable,
        BigDecimal revenuAnnuelFlexiCumule,
        BigDecimal plafondAnnuelExonere,

        // Outputs
        FlexiJobBeVerdict verdict,
        boolean travailleurEligible,
        boolean secteurEligible,
        boolean cumulRespecte,
        boolean formalismeRespecte,
        boolean remunerationConforme,
        BigDecimal montantExcedentaireAuPlafond,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
