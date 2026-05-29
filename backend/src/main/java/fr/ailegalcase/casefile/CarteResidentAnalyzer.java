package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-214-23 : analyseur de l'éligibilité à la carte de résident 10 ans
 * (L. 426-1 CESEDA). Outil single-country FR.
 *
 * <p>Critères L. 426-1 : ≥ 5 ans de séjour régulier en France + intégration
 * républicaine (langue, valeurs — appréciée via {@code niveauIntegration} ≠ FAIBLE)
 * + ressources stables et suffisantes (≥ SMIC mensuel net). Une condamnation pénale
 * grave rend la demande irrecevable (menace pour l'ordre public — appréciation
 * prioritaire sur tous les autres critères).</p>
 *
 * <p>Source juridique :
 * <ul>
 *   <li>L. 426-1 à L. 426-12 CESEDA — carte de résident 10 ans (recodification
 *       2021, anciens L. 314-1+)</li>
 *   <li>R. 426-1 à R. 426-6 CESEDA — conditions et procédure</li>
 *   <li>Loi 26/01/2024 — renforcement des exigences d'intégration (test de langue)</li>
 * </ul>
 */
public final class CarteResidentAnalyzer {

    public static final String VERDICT_ELIGIBLE = "ELIGIBLE";
    public static final String VERDICT_ELIGIBLE_SOUS_RESERVE = "ELIGIBLE_SOUS_RESERVE";
    public static final String VERDICT_NON_ELIGIBLE_DELAI = "NON_ELIGIBLE_DELAI";
    public static final String VERDICT_NON_ELIGIBLE_INTEGRATION = "NON_ELIGIBLE_INTEGRATION";
    public static final String VERDICT_NON_ELIGIBLE_RESSOURCES = "NON_ELIGIBLE_RESSOURCES";
    public static final String VERDICT_INADMISSIBLE = "INADMISSIBLE";

    public static final String INTEGRATION_FORT = "FORT";
    public static final String INTEGRATION_MOYEN = "MOYEN";
    public static final String INTEGRATION_FAIBLE = "FAIBLE";

    public static final Set<String> NIVEAUX_INTEGRATION_VALIDES = Set.of(
            INTEGRATION_FORT, INTEGRATION_MOYEN, INTEGRATION_FAIBLE);

    /** Codes de critères non remplis exposés sous forme de chips. */
    public static final String CHIP_SEJOUR_INSUFFISANT = "SEJOUR_INSUFFISANT";
    public static final String CHIP_INTEGRATION_INSUFFISANTE = "INTEGRATION_INSUFFISANTE";
    public static final String CHIP_RESSOURCES_INSUFFISANTES = "RESSOURCES_INSUFFISANTES";

    /** Durée minimale de séjour régulier (L. 426-1 CESEDA) — 5 ans. */
    public static final int DUREE_SEJOUR_REGULIER_MINIMALE_ANNEES = 5;

    /**
     * SMIC net mensuel 2026 — à actualiser annuellement.
     *
     * <p>Le seuil de ressources « stables et suffisantes » de la carte de résident
     * est apprécié par référence au SMIC mensuel NET. On réutilise la même valeur
     * de référence que {@code RegroupementFamilialAnalyzer.SMIC_MENSUEL_NET_EUR}
     * (≈ 1426,30 €/mois en 2026), mais en déclarant une constante locale dédiée :
     * les deux outils décisionnels sont des simulateurs indépendants (cf. invariant
     * « un outil = une situation »), aucun ne doit dépendre des constantes d'un
     * autre. Arbitrage documenté en PR.</p>
     */
    public static final double SMIC_MENSUEL_NET_EUR = 1426.30;

    private static final String BASE_JURIDIQUE =
            "CESEDA L. 426-1 à L. 426-12 (carte de résident 10 ans) ; "
            + "R. 426-1 à R. 426-6 (conditions et procédure) ; "
            + "loi 26/01/2024 (renforcement des exigences d'intégration)";

    private CarteResidentAnalyzer() {}

    /**
     * Analyse l'éligibilité à la carte de résident 10 ans.
     *
     * @param dureeSejourRegulierAnnees durée du séjour régulier en années (≥ 0)
     * @param typesTitresAnterieurs     types de titres antérieurs détenus (libre, optionnel)
     * @param niveauIntegration         FORT | MOYEN | FAIBLE
     * @param ressourcesMensuellesNettes ressources mensuelles nettes (> 0)
     * @param condamnationsPenalesGraves true si condamnation(s) pénale(s) grave(s)
     * @return résultat de l'analyse
     */
    public static CarteResidentResult analyze(int dureeSejourRegulierAnnees,
                                              String typesTitresAnterieurs,
                                              String niveauIntegration,
                                              double ressourcesMensuellesNettes,
                                              boolean condamnationsPenalesGraves) {
        validateInputs(dureeSejourRegulierAnnees, niveauIntegration, ressourcesMensuellesNettes);

        List<String> chipsCriteresNonRemplis = new ArrayList<>();
        List<String> atouts = new ArrayList<>();

        // Critère 1 — durée de séjour régulier (L. 426-1) : ≥ 5 ans.
        boolean sejourSuffisant = dureeSejourRegulierAnnees >= DUREE_SEJOUR_REGULIER_MINIMALE_ANNEES;
        if (!sejourSuffisant) {
            chipsCriteresNonRemplis.add(CHIP_SEJOUR_INSUFFISANT);
        } else {
            atouts.add("Séjour régulier de " + dureeSejourRegulierAnnees
                    + " ans (≥ 5 ans requis)");
        }

        // Critère 2 — intégration républicaine : niveau ≠ FAIBLE.
        boolean integrationSuffisante = !INTEGRATION_FAIBLE.equals(niveauIntegration);
        if (!integrationSuffisante) {
            chipsCriteresNonRemplis.add(CHIP_INTEGRATION_INSUFFISANTE);
        } else if (INTEGRATION_FORT.equals(niveauIntegration)) {
            atouts.add("Intégration républicaine forte et documentée");
        }

        // Critère 3 — ressources stables et suffisantes : ≥ SMIC net mensuel.
        boolean ressourcesSuffisantes = ressourcesMensuellesNettes >= SMIC_MENSUEL_NET_EUR;
        if (!ressourcesSuffisantes) {
            chipsCriteresNonRemplis.add(CHIP_RESSOURCES_INSUFFISANTES);
        } else {
            atouts.add(String.format("Ressources mensuelles (%.2f €) ≥ SMIC net (%.2f €)",
                    ressourcesMensuellesNettes, SMIC_MENSUEL_NET_EUR));
        }

        if (typesTitresAnterieurs != null && !typesTitresAnterieurs.isBlank()) {
            atouts.add("Titres antérieurs détenus : " + typesTitresAnterieurs.trim());
        }

        String verdict = determineVerdict(condamnationsPenalesGraves, sejourSuffisant,
                integrationSuffisante, ressourcesSuffisantes, niveauIntegration);

        return new CarteResidentResult(
                dureeSejourRegulierAnnees,
                typesTitresAnterieurs,
                niveauIntegration,
                ressourcesMensuellesNettes,
                condamnationsPenalesGraves,
                verdict,
                roundCents(SMIC_MENSUEL_NET_EUR),
                chipsCriteresNonRemplis,
                atouts,
                BASE_JURIDIQUE);
    }

    private static String determineVerdict(boolean condamnationsPenalesGraves,
                                           boolean sejourSuffisant,
                                           boolean integrationSuffisante,
                                           boolean ressourcesSuffisantes,
                                           String niveauIntegration) {
        // Une condamnation pénale grave caractérise une menace pour l'ordre public
        // et rend la demande irrecevable, quel que soit l'état des autres critères.
        if (condamnationsPenalesGraves) {
            return VERDICT_INADMISSIBLE;
        }
        // La condition de délai est rédhibitoire et prioritaire : sans 5 ans de
        // séjour régulier, la demande ne peut aboutir.
        if (!sejourSuffisant) {
            return VERDICT_NON_ELIGIBLE_DELAI;
        }
        if (!integrationSuffisante) {
            return VERDICT_NON_ELIGIBLE_INTEGRATION;
        }
        if (!ressourcesSuffisantes) {
            return VERDICT_NON_ELIGIBLE_RESSOURCES;
        }
        // Tous les critères sont remplis. ELIGIBLE_SOUS_RESERVE est réservé au cas
        // d'une intégration seulement MOYEN : la préfecture conserve une marge
        // d'appréciation (test de langue renforcé loi 2024), à consolider.
        return INTEGRATION_MOYEN.equals(niveauIntegration)
                ? VERDICT_ELIGIBLE_SOUS_RESERVE
                : VERDICT_ELIGIBLE;
    }

    private static void validateInputs(int dureeSejourRegulierAnnees,
                                       String niveauIntegration,
                                       double ressourcesMensuellesNettes) {
        if (dureeSejourRegulierAnnees < 0) {
            throw new IllegalArgumentException("dureeSejourRegulierAnnees ne peut pas être négatif");
        }
        if (niveauIntegration == null || !NIVEAUX_INTEGRATION_VALIDES.contains(niveauIntegration)) {
            throw new IllegalArgumentException(
                    "niveauIntegration inconnu — valeurs attendues : " + NIVEAUX_INTEGRATION_VALIDES);
        }
        if (ressourcesMensuellesNettes <= 0) {
            throw new IllegalArgumentException("ressourcesMensuellesNettes doit être strictement positif");
        }
    }

    private static double roundCents(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
