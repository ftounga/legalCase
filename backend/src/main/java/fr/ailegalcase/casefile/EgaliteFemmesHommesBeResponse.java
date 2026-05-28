package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-22 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/egalite-femmes-hommes-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link DroitDeconnexionBeResponse} SF-219-19).</p>
 */
public record EgaliteFemmesHommesBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        int effectifEntreprise,
        EgaliteFemmesHommesBeStatutRapport statutRapport,
        LocalDate dateDepotRapport,
        boolean ventilationNiveauFonctionFournie,
        boolean ventilationAncienneteFournie,
        boolean ventilationQualificationFournie,
        boolean ventilationRegimeTravailFournie,
        boolean ventilationComposantsRemunerationFournie,
        boolean ecartSalarialNonJustifieConstate,
        BigDecimal pourcentageEcartConstate,
        boolean planActionEtabli,
        boolean mediateurDesigne,
        boolean plainteIefhOuInspectionEnCours,

        // Outputs
        EgaliteFemmesHommesBeVerdict verdict,
        boolean seuilAtteint,
        boolean rapportDepose,
        boolean ventilationComplete,
        boolean planActionRequis,
        boolean planActionConforme,
        String typeFormulaireRequis,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
