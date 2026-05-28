package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-23 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/discrimination-be-handicap-amenagement}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link EgaliteFemmesHommesBeResponse} SF-219-22).</p>
 */
public record DiscriminationBeHandicapAmenagementResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        DiscriminationBeHandicapAmenagementStatutHandicap statutHandicap,
        LocalDate dateDemandeAmenagement,
        String typeAmenagementDemande,
        BigDecimal coutEstimeAmenagement,
        boolean subsidesDemandes,
        int effectifEntreprise,
        BigDecimal chiffreAffairesAnnuel,
        DiscriminationBeHandicapAmenagementReponseEmployeur reponseEmployeur,
        boolean motivationDetailleeFournie,
        boolean avisSeppFavorable,
        boolean chargeDisproportionneeInvoquee,
        boolean devisExterneFourni,
        boolean mesuresAlternativesProposees,
        DiscriminationBeHandicapAmenagementSanctionSubie sanctionSubie,
        LocalDate dateSanction,
        BigDecimal salaireMensuelBrut,
        boolean procedureUniaSaisie,

        // Outputs
        DiscriminationBeHandicapAmenagementVerdict verdict,
        boolean handicapQualifie,
        boolean demandeFormalisee,
        boolean refusCaracterise,
        boolean chargeDisproportionneeDemontree,
        boolean representaillesPresumees,
        BigDecimal indemniteForfaitaire6Mois,

        // Synthèse + métadonnées légales
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
