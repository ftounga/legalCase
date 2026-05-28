package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-219-21 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/eco-cheques-cheques-repas-be}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les
 * autres outils décisionnels BE de la F-219 (miroir
 * {@link PeculeVacancesBeResponse} SF-219-20,
 * {@link DroitDeconnexionBeResponse} SF-219-19).</p>
 */
public record EcoChequesChequesRepasBeResponse(
        UUID caseFileId,

        // Inputs (snapshot)
        EcoChequesChequesRepasBeTypeAvantage typeAvantage,
        BigDecimal montantAnnuelEur,
        BigDecimal valeurFacialeUnitaireEur,
        BigDecimal contributionTravailleurUnitaireEur,
        int joursEffectivementPrestes,
        boolean cctSectorielleOuEntrepriseExiste,
        boolean conventionIndividuelleEcrite,
        boolean paiementElectronique,
        boolean cumulFraisBouche,
        boolean substitutionRemuneration,
        LocalDate dateAttribution,

        // Outputs calculés
        BigDecimal plafondLegalApplicableEur,
        BigDecimal montantExonereEur,
        BigDecimal montantRequalifieEnRemunerationEur,
        BigDecimal cotisationsOnssEstimeesEur,

        // Verdict + métadonnées légales
        EcoChequesChequesRepasBeVerdict verdict,
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
