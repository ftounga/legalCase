package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-23 : résultat brut produit par
 * {@link DiscriminationBeHandicapAmenagementChecker} — fonction
 * <b>pure</b> (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link DiscriminationBeHandicapAmenagementService} dans
 * {@link DiscriminationBeHandicapAmenagementResponse}).</p>
 *
 * <p>Outputs dérivés : ventilation par dimension de conformité
 * (qualification handicap, demande formalisée, réponse employeur,
 * charge disproportionnée démontrée, représailles présumées,
 * indemnité forfaitaire 6 mois indicative).</p>
 */
public record DiscriminationBeHandicapAmenagementResult(
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

        // Outputs analytiques
        DiscriminationBeHandicapAmenagementVerdict verdict,
        boolean handicapQualifie,
        boolean demandeFormalisee,
        boolean refusCaracterise,
        boolean chargeDisproportionneeDemontree,
        boolean representaillesPresumees,
        BigDecimal indemniteForfaitaire6Mois,

        // Synthèse + base juridique
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
