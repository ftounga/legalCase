package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-21 : résultat brut produit par
 * {@link EcoChequesChequesRepasBeCalculator} — fonction <b>pure</b>
 * (sans dépendance HTTP / persistance / horloge).
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link EcoChequesChequesRepasBeService} dans
 * {@link EcoChequesChequesRepasBeResponse}).</p>
 *
 * <p>Outputs : verdict + ventilation montant conforme vs requalifié,
 * plafond appliqué, base juridique stable.</p>
 */
public record EcoChequesChequesRepasBeResult(
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

        // Verdict + synthèse
        EcoChequesChequesRepasBeVerdict verdict,
        String raison,
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
