package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-17-01 : calculateur de recevabilité d'une procédure de partage
 * judiciaire (FR — art. 840 et s. Cciv + 1364 et s. CPC).
 *
 * <p>La procédure intervient quand les co-indivisaires (succession ou post-divorce)
 * n'arrivent pas à s'accorder sur la liquidation. 3 étapes possibles :</p>
 * <ul>
 *   <li><strong>PROCES_VERBAL_DIFFICULTES</strong> — le notaire dresse un PV constatant
 *       le désaccord (art. 1366 CPC) — préalable obligatoire.</li>
 *   <li><strong>HOMOLOGATION_AMIABLE_PARTIELLE</strong> — accord partiel : le juge
 *       homologue les points convenus + tranche les points litigieux.</li>
 *   <li><strong>PARTAGE_JUDICIAIRE_INTEGRAL</strong> — désaccord total : le tribunal
 *       procède au partage avec expertise notariale (art. 1364 CPC) + tirage au sort
 *       des lots ou licitation (vente aux enchères) si bien indivisible.</li>
 * </ul>
 *
 * <p>Critères d'éligibilité ELEVEE :</p>
 * <ul>
 *   <li>{@code pvDifficultesEtabli = true} (art. 1366 CPC)</li>
 *   <li>{@code tentativeAmiableEpuiseuee = true} (échec voie amiable)</li>
 *   <li>{@code typeBienIndivision} parmi les types prévus</li>
 *   <li>{@code nombreCoindivisaires ≥ 2}</li>
 *   <li>{@code desaccordMotive = true} (motif documenté)</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — l'équivalent BE
 * (CJ art. 1207 et s., juge de paix) suit un régime distinct et fait l'objet
 * d'une feature jumelle au backlog.</p>
 */
public final class PartageJudiciaireCalculator {

    /** Étape de la procédure de partage judiciaire. */
    public enum EtapeProcedure {
        /** PV de difficultés dressé par le notaire (art. 1366 CPC). */
        PROCES_VERBAL_DIFFICULTES,
        /** Accord partiel — le juge homologue + tranche les points litigieux. */
        HOMOLOGATION_AMIABLE_PARTIELLE,
        /** Désaccord total — partage judiciaire intégral (art. 1364 CPC). */
        PARTAGE_JUDICIAIRE_INTEGRAL
    }

    /** Type de bien en indivision. */
    public enum TypeBienIndivision {
        /** Immeuble divisible matériellement (terrain, lot copropriété, etc.). */
        IMMEUBLE_DIVISIBLE,
        /** Immeuble indivisible (maison unique) — risque de licitation. */
        IMMEUBLE_INDIVISIBLE,
        /** Meubles divers (mobilier, véhicules, comptes bancaires, etc.). */
        MEUBLES_DIVERS,
        /** Patrimoine mixte (immeuble + meubles). */
        MIXTE
    }

    /** Verdict de recevabilité de la procédure. */
    public enum VerdictRecevabilite {
        ELEVEE,
        MOYENNE,
        FAIBLE
    }

    /** Provision notariale basse (% valeur biens). */
    private static final double PROVISION_NOTARIALE_BASSE_PCT = 0.02;

    /** Provision notariale haute (% valeur biens). */
    private static final double PROVISION_NOTARIALE_HAUTE_PCT = 0.05;

    /** Durée minimale procédure (mois). */
    private static final int DUREE_MIN_MOIS = 6;

    /** Durée standard procédure (mois). */
    private static final int DUREE_STD_MOIS = 12;

    /** Durée maximale procédure (mois). */
    private static final int DUREE_MAX_MOIS = 18;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 840 et s. + 1361 Cciv + 1364 et s. + 1366 + 1377 CPC";

    private PartageJudiciaireCalculator() {}

    /**
     * Évalue la recevabilité d'une procédure de partage judiciaire.
     *
     * @param etapeProcedure          étape de la procédure (obligatoire)
     * @param typeBienIndivision      type de bien (obligatoire)
     * @param nombreCoindivisaires    nombre de co-indivisaires (≥ 2 obligatoire)
     * @param valeurEstimeeBiensEur   valeur estimée des biens en euros (≥ 0)
     * @param pvDifficultesEtabli     PV de difficultés dressé (art. 1366)
     * @param tentativeAmiableEpuiseuee tentative amiable épuisée
     * @param desaccordMotive         désaccord motivé documenté
     * @param country                 pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static PartageJudiciaireResult compute(EtapeProcedure etapeProcedure,
                                                  TypeBienIndivision typeBienIndivision,
                                                  Integer nombreCoindivisaires,
                                                  Double valeurEstimeeBiensEur,
                                                  Boolean pvDifficultesEtabli,
                                                  Boolean tentativeAmiableEpuiseuee,
                                                  Boolean desaccordMotive,
                                                  String country) {
        if (etapeProcedure == null) {
            throw new IllegalArgumentException("Étape de procédure requise");
        }
        if (typeBienIndivision == null) {
            throw new IllegalArgumentException("Type de bien en indivision requis");
        }
        if (nombreCoindivisaires == null) {
            throw new IllegalArgumentException("Nombre de co-indivisaires requis");
        }
        if (nombreCoindivisaires < 2) {
            throw new IllegalArgumentException(
                    "Le nombre de co-indivisaires doit être ≥ 2 (procédure sans objet sinon)");
        }
        if (valeurEstimeeBiensEur == null) {
            throw new IllegalArgumentException("Valeur estimée des biens requise");
        }
        if (valeurEstimeeBiensEur < 0) {
            throw new IllegalArgumentException("Valeur estimée des biens doit être ≥ 0");
        }
        if (pvDifficultesEtabli == null) {
            throw new IllegalArgumentException("PV de difficultés (art. 1366) requis");
        }
        if (tentativeAmiableEpuiseuee == null) {
            throw new IllegalArgumentException("Tentative amiable (épuisée ou non) requise");
        }
        if (desaccordMotive == null) {
            throw new IllegalArgumentException("Désaccord motivé (oui/non) requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — l'équivalent BE (CJ art. 1207 et s., juge de paix)"
                            + " sera traité dans une feature jumelle dédiée.");
        }

        // Évaluation du score (0 à 100)
        int score = 0;
        if (pvDifficultesEtabli) {
            score += 35; // préalable obligatoire art. 1366
        }
        if (tentativeAmiableEpuiseuee) {
            score += 25; // intérêt à agir
        }
        if (desaccordMotive) {
            score += 20; // motif documenté
        }
        if (nombreCoindivisaires >= 2) {
            score += 10;
        }
        // Bonus si bien divisible (procédure plus simple)
        boolean bienIndivisible = typeBienIndivision == TypeBienIndivision.IMMEUBLE_INDIVISIBLE;
        if (!bienIndivisible) {
            score += 10;
        }

        // Verdict
        VerdictRecevabilite verdict;
        if (!pvDifficultesEtabli || !tentativeAmiableEpuiseuee) {
            verdict = VerdictRecevabilite.FAIBLE;
        } else if (bienIndivisible) {
            verdict = VerdictRecevabilite.MOYENNE;
        } else if (score >= 80) {
            verdict = VerdictRecevabilite.ELEVEE;
        } else {
            verdict = VerdictRecevabilite.MOYENNE;
        }

        // Risque de licitation : uniquement si bien immobilier indivisible
        boolean risqueLicitation = bienIndivisible;

        // Durée procédure
        int dureeMois;
        if (verdict == VerdictRecevabilite.FAIBLE) {
            dureeMois = DUREE_STD_MOIS;
        } else if (bienIndivisible) {
            // Expertise + licitation potentielle = +6 mois
            dureeMois = DUREE_MAX_MOIS;
        } else if (typeBienIndivision == TypeBienIndivision.MIXTE) {
            dureeMois = (DUREE_STD_MOIS + DUREE_MAX_MOIS) / 2; // 15
        } else {
            dureeMois = DUREE_MIN_MOIS + 3; // 9 mois pour cas standard divisible
        }

        // Frais estimés (provision notariale 2-5% des biens)
        double pctProvision = bienIndivisible
                ? PROVISION_NOTARIALE_HAUTE_PCT
                : PROVISION_NOTARIALE_BASSE_PCT
                        + (PROVISION_NOTARIALE_HAUTE_PCT - PROVISION_NOTARIALE_BASSE_PCT) / 2;
        double fraisEstimes = valeurEstimeeBiensEur * pctProvision;

        String formule = String.format(Locale.ROOT,
                "Étape %s + bien %s + %d co-indivisaires + PV=%s + amiable épuisé=%s "
                        + "+ désaccord motivé=%s → score %d → verdict %s "
                        + "→ durée %d mois, frais estimés %.2f €, licitation=%s",
                etapeProcedure.name(), typeBienIndivision.name(), nombreCoindivisaires,
                pvDifficultesEtabli, tentativeAmiableEpuiseuee, desaccordMotive,
                score, verdict.name(), dureeMois, fraisEstimes, risqueLicitation);

        List<String> messages = buildMessages(etapeProcedure, typeBienIndivision,
                pvDifficultesEtabli, tentativeAmiableEpuiseuee, desaccordMotive,
                verdict, risqueLicitation, dureeMois, fraisEstimes);

        return new PartageJudiciaireResult(
                etapeProcedure,
                typeBienIndivision,
                nombreCoindivisaires,
                valeurEstimeeBiensEur,
                pvDifficultesEtabli,
                tentativeAmiableEpuiseuee,
                desaccordMotive,
                countryNormalized,
                verdict,
                score,
                dureeMois,
                fraisEstimes,
                risqueLicitation,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static List<String> buildMessages(EtapeProcedure etape,
                                              TypeBienIndivision typeBien,
                                              boolean pvEtabli,
                                              boolean amiableEpuise,
                                              boolean desaccordMotive,
                                              VerdictRecevabilite verdict,
                                              boolean risqueLicitation,
                                              int dureeMois,
                                              double fraisEstimes) {
        List<String> msgs = new ArrayList<>();

        msgs.add("Étape de procédure : " + libelleEtape(etape));

        if (!pvEtabli) {
            msgs.add("PV de difficultés NON établi (art. 1366 CPC) — préalable obligatoire "
                    + "manquant. Faire dresser un PV de difficultés par le notaire avant "
                    + "toute saisine du tribunal.");
        } else {
            msgs.add("PV de difficultés établi (art. 1366 CPC) — préalable validé.");
        }

        if (!amiableEpuise) {
            msgs.add("Tentative amiable NON documentée comme épuisée — risque de "
                    + "défaut d'intérêt à agir. Constituer un dossier prouvant les "
                    + "tentatives amiables (correspondances, RDV, propositions).");
        } else {
            msgs.add("Tentative amiable épuisée — l'intérêt à agir est constitué.");
        }

        if (!desaccordMotive) {
            msgs.add("Désaccord NON motivé — un simple « on ne s'entend pas » ne suffit pas. "
                    + "Identifier précisément les points litigieux (évaluation, lots, soultes).");
        }

        switch (typeBien) {
            case IMMEUBLE_INDIVISIBLE -> msgs.add("Bien immobilier INDIVISIBLE — "
                    + "tirage au sort des lots impossible. Risque élevé de LICITATION "
                    + "(vente aux enchères judiciaires) si désaccord persiste. "
                    + "Allongement procédure : +6 mois pour expertise et publicité de licitation.");
            case IMMEUBLE_DIVISIBLE -> msgs.add("Bien immobilier divisible — "
                    + "tirage au sort des lots possible (art. 1377 CPC).");
            case MEUBLES_DIVERS -> msgs.add("Meubles divers — partage par tirage au sort "
                    + "ou attribution préférentielle (art. 831 et s. Cciv) selon la nature.");
            case MIXTE -> msgs.add("Patrimoine mixte (immeuble + meubles) — analyse du "
                    + "caractère divisible bien par bien.");
        }

        switch (verdict) {
            case ELEVEE -> msgs.add("VERDICT ELEVEE — la procédure de partage judiciaire "
                    + "est recevable et bien préparée. Saisine du TGI possible (art. 1361 CPC).");
            case MOYENNE -> msgs.add("VERDICT MOYENNE — la procédure est recevable mais "
                    + "comporte des facteurs d'allongement (bien indivisible ou critères "
                    + "partiels). Préparer la défense face au risque de licitation.");
            case FAIBLE -> msgs.add("VERDICT FAIBLE — préalables non remplis. "
                    + "Compléter la documentation (PV de difficultés, preuves "
                    + "tentative amiable) avant toute saisine.");
        }

        if (risqueLicitation) {
            msgs.add("⚠ Risque de LICITATION — bien indivisible : le tribunal peut "
                    + "ordonner la vente aux enchères publiques si aucun co-indivisaire "
                    + "n'est candidat à l'attribution. Préparer une demande "
                    + "d'attribution préférentielle (art. 831, 832-1 Cciv) si applicable.");
        }

        msgs.add(String.format(Locale.ROOT,
                "Durée estimée de la procédure : %d mois (mise en état + expertise notariale "
                        + "+ jugement / éventuelle licitation).", dureeMois));

        msgs.add(String.format(Locale.ROOT,
                "Frais estimés (provision notaire + frais expertise) : %.2f € "
                        + "(typiquement 2-5%% de la valeur des biens).", fraisEstimes));

        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }

    private static String libelleEtape(EtapeProcedure e) {
        return switch (e) {
            case PROCES_VERBAL_DIFFICULTES -> "PV de difficultés (art. 1366 CPC)";
            case HOMOLOGATION_AMIABLE_PARTIELLE -> "Homologation amiable partielle";
            case PARTAGE_JUDICIAIRE_INTEGRAL -> "Partage judiciaire intégral (art. 1364 CPC)";
        };
    }
}
