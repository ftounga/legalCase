package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-219-05 : résultat brut produit par
 * {@link OutplacementBeGeneral30semChecker} — fonction <b>pure</b>.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link OutplacementBeGeneral30semService} dans
 * {@link OutplacementBeGeneral30semResponse}).</p>
 *
 * <p>{@code indemniteForfaitaireTravailleur} est non nulle uniquement
 * lorsque l'employeur a manqué à son obligation d'offre conforme
 * (verdicts {@code NON_CONFORME_*}) — sanction estimative à charge de
 * l'employeur. {@code avertissement} est non null si une alerte est
 * levée (toujours présente dès qu'une sanction est calculée).</p>
 */
public record OutplacementBeGeneral30semResult(
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

        // Verdict et conformité par condition
        OutplacementBeGeneral30semVerdict verdict,
        boolean conforme,
        boolean conditionLicenciementRemplie,
        boolean conditionMotifNonGraveRemplie,
        boolean conditionPreavis30semRemplie,
        boolean conditionOffreDansLes15joursRemplie,
        boolean conditionDureeProgrammeRemplie,
        boolean conditionFormeOffreRemplie,

        // Sanction indicative (travailleur — indemnité forfaitaire)
        BigDecimal indemniteForfaitaireTravailleur,

        // Synthèse + base juridique
        String synthese,
        String baseJuridique,
        String avertissement
) {
}
