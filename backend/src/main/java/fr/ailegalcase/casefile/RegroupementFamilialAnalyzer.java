package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-214-03 : analyseur de l'éligibilité au regroupement familial (L. 434-1+ CESEDA).
 * Outil single-country FR.
 *
 * <p>Source juridique :
 * <ul>
 *   <li>L. 434-1 à L. 434-13 CESEDA — regroupement familial (recodification 2021,
 *       anciens L. 411-1+)</li>
 *   <li>R. 434-4 CESEDA — durée de séjour régulier ≥ 18 mois</li>
 *   <li>R. 434-20 CESEDA — surface habitable minimale du logement</li>
 *   <li>R. 434-30 CESEDA — ressources stables et suffisantes (référence SMIC mensuel
 *       net, hors allocations familiales, APL, RSA)</li>
 *   <li>CE 22 mars 2010, n° 301750 — exclusion de l'APL du calcul des ressources</li>
 * </ul>
 */
public final class RegroupementFamilialAnalyzer {

    public static final String VERDICT_ELIGIBLE = "ELIGIBLE";
    public static final String VERDICT_ELIGIBLE_SOUS_RESERVE = "ELIGIBLE_SOUS_RESERVE";
    public static final String VERDICT_NON_ELIGIBLE_DELAI = "NON_ELIGIBLE_DELAI";
    public static final String VERDICT_NON_ELIGIBLE_RESSOURCES = "NON_ELIGIBLE_RESSOURCES";
    public static final String VERDICT_NON_ELIGIBLE_LOGEMENT = "NON_ELIGIBLE_LOGEMENT";

    public static final String TYPE_CONJOINT = "CONJOINT";
    public static final String TYPE_ENFANT_MINEUR = "ENFANT_MINEUR";
    public static final String TYPE_AUTRE = "AUTRE";

    public static final Set<String> TYPES_VALIDES = Set.of(
            TYPE_CONJOINT, TYPE_ENFANT_MINEUR, TYPE_AUTRE);

    /** Codes de critères non remplis exposés sous forme de chips. */
    public static final String CHIP_SEJOUR_INSUFFISANT = "SEJOUR_INSUFFISANT";
    public static final String CHIP_RESSOURCES_INSUFFISANTES = "RESSOURCES_INSUFFISANTES";
    public static final String CHIP_LOGEMENT_INSUFFISANT = "LOGEMENT_INSUFFISANT";

    /** Durée minimale de séjour régulier (R. 434-4 CESEDA) — 18 mois. */
    public static final int DUREE_SEJOUR_REGULIER_MINIMALE_MOIS = 18;

    /**
     * SMIC net mensuel 2026 — à actualiser annuellement.
     *
     * <p>R. 434-30 CESEDA exprime le seuil de ressources par référence au SMIC
     * mensuel NET (hors allocations familiales, APL, RSA). La seule constante SMIC
     * existante dans le code ({@code ChangementStatutCalculator.SMIC_MENSUEL_BRUT_EUR_2026}
     * = 1801,80 €) est exprimée en BRUT et n'est donc pas réutilisable ici. On déclare
     * une constante NET dédiée (≈ 1426,30 €/mois en 2026). Arbitrage documenté en PR.</p>
     */
    public static final double SMIC_MENSUEL_NET_EUR = 1426.30;

    /** Surface habitable minimale pour 1 personne (R. 434-20). */
    public static final int SURFACE_BASE_M2 = 16;

    /** Surface habitable additionnelle par personne au-delà de la 1ʳᵉ (R. 434-20). */
    public static final int SURFACE_PAR_PERSONNE_SUPPLEMENTAIRE_M2 = 9;

    private static final String BASE_JURIDIQUE =
            "CESEDA L. 434-1 à L. 434-13 ; R. 434-4 (durée séjour 18 mois) ; "
            + "R. 434-20 (logement) ; R. 434-30 (ressources SMIC net) ; "
            + "CE 22 mars 2010 n° 301750 (exclusion APL/RSA des ressources)";

    private RegroupementFamilialAnalyzer() {}

    /**
     * Analyse l'éligibilité au regroupement familial.
     *
     * @param dureeSejourRegulierMois   durée du séjour régulier en mois (≥ 0)
     * @param ressourcesMensuellesNettes ressources mensuelles nettes hors APL/RSA/alloc (> 0)
     * @param tailleLogementM2          surface habitable du logement en m² (> 0)
     * @param nombrePersonnesFoyer      nombre de personnes composant le foyer après regroupement (≥ 1)
     * @param typeRegroupement          CONJOINT | ENFANT_MINEUR | AUTRE
     * @param membresFamilleARegrouper  nombre de membres de famille à regrouper (1-6)
     * @return résultat de l'analyse
     */
    public static RegroupementFamilialResult analyze(int dureeSejourRegulierMois,
                                                     double ressourcesMensuellesNettes,
                                                     int tailleLogementM2,
                                                     int nombrePersonnesFoyer,
                                                     String typeRegroupement,
                                                     int membresFamilleARegrouper) {
        validateInputs(dureeSejourRegulierMois, ressourcesMensuellesNettes,
                tailleLogementM2, nombrePersonnesFoyer, typeRegroupement, membresFamilleARegrouper);

        List<String> chipsCriteresNonRemplis = new ArrayList<>();

        // Critère 1 — durée de séjour régulier (R. 434-4)
        boolean sejourSuffisant = dureeSejourRegulierMois >= DUREE_SEJOUR_REGULIER_MINIMALE_MOIS;
        if (!sejourSuffisant) {
            chipsCriteresNonRemplis.add(CHIP_SEJOUR_INSUFFISANT);
        }

        // Critère 2 — ressources (R. 434-30) : SMIC × (1.0 + 0.2 × (membres - 1))
        double ressourcesRequises = ressourcesRequises(membresFamilleARegrouper);
        boolean ressourcesSuffisantes = ressourcesMensuellesNettes >= ressourcesRequises;
        if (!ressourcesSuffisantes) {
            chipsCriteresNonRemplis.add(CHIP_RESSOURCES_INSUFFISANTES);
        }

        // Critère 3 — logement (R. 434-20) : surface habitable minimale
        int surfaceRequise = surfaceRequise(nombrePersonnesFoyer);
        boolean logementSuffisant = tailleLogementM2 >= surfaceRequise;
        if (!logementSuffisant) {
            chipsCriteresNonRemplis.add(CHIP_LOGEMENT_INSUFFISANT);
        }

        String verdict = determineVerdict(sejourSuffisant, ressourcesSuffisantes,
                logementSuffisant, typeRegroupement);

        return new RegroupementFamilialResult(
                dureeSejourRegulierMois,
                ressourcesMensuellesNettes,
                tailleLogementM2,
                nombrePersonnesFoyer,
                typeRegroupement,
                membresFamilleARegrouper,
                verdict,
                roundCents(ressourcesRequises),
                surfaceRequise,
                chipsCriteresNonRemplis,
                BASE_JURIDIQUE);
    }

    /** R. 434-30 : SMIC mensuel net × (1.0 + 0.2 × (membres - 1)). */
    public static double ressourcesRequises(int membresFamilleARegrouper) {
        double coefficient = 1.0 + 0.2 * (membresFamilleARegrouper - 1);
        return SMIC_MENSUEL_NET_EUR * coefficient;
    }

    /** R. 434-20 : 16 m² pour 1 personne, +9 m² par personne supplémentaire. */
    public static int surfaceRequise(int nombrePersonnesFoyer) {
        return SURFACE_BASE_M2
                + SURFACE_PAR_PERSONNE_SUPPLEMENTAIRE_M2 * (nombrePersonnesFoyer - 1);
    }

    private static String determineVerdict(boolean sejourSuffisant,
                                           boolean ressourcesSuffisantes,
                                           boolean logementSuffisant,
                                           String typeRegroupement) {
        // La condition de délai est rédhibitoire et prioritaire : sans 18 mois de
        // séjour régulier, la demande est irrecevable quel que soit le reste.
        if (!sejourSuffisant) {
            return VERDICT_NON_ELIGIBLE_DELAI;
        }
        if (!ressourcesSuffisantes) {
            return VERDICT_NON_ELIGIBLE_RESSOURCES;
        }
        if (!logementSuffisant) {
            return VERDICT_NON_ELIGIBLE_LOGEMENT;
        }
        // Tous les critères chiffrés sont remplis. ELIGIBLE_SOUS_RESERVE est réservé
        // au regroupement de membres « AUTRE » (ascendants, collatéraux), pour
        // lequel des conditions complémentaires d'appréciation par l'OFII existent
        // au-delà des seuils chiffrés. Conjoint et enfant mineur → ELIGIBLE direct.
        return TYPE_AUTRE.equals(typeRegroupement)
                ? VERDICT_ELIGIBLE_SOUS_RESERVE
                : VERDICT_ELIGIBLE;
    }

    private static void validateInputs(int dureeSejourRegulierMois,
                                       double ressourcesMensuellesNettes,
                                       int tailleLogementM2,
                                       int nombrePersonnesFoyer,
                                       String typeRegroupement,
                                       int membresFamilleARegrouper) {
        if (dureeSejourRegulierMois < 0) {
            throw new IllegalArgumentException("dureeSejourRegulierMois ne peut pas être négatif");
        }
        if (ressourcesMensuellesNettes <= 0) {
            throw new IllegalArgumentException("ressourcesMensuellesNettes doit être strictement positif");
        }
        if (tailleLogementM2 <= 0) {
            throw new IllegalArgumentException("tailleLogementM2 doit être strictement positif");
        }
        if (nombrePersonnesFoyer < 1) {
            throw new IllegalArgumentException("nombrePersonnesFoyer doit être ≥ 1");
        }
        if (typeRegroupement == null || !TYPES_VALIDES.contains(typeRegroupement)) {
            throw new IllegalArgumentException(
                    "typeRegroupement inconnu — valeurs attendues : " + TYPES_VALIDES);
        }
        if (membresFamilleARegrouper < 1 || membresFamilleARegrouper > 6) {
            throw new IllegalArgumentException("membresFamilleARegrouper doit être compris entre 1 et 6");
        }
    }

    private static double roundCents(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
