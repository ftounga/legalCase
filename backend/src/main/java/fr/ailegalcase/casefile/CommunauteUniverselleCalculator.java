package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * SF-FA-16-01 : calculateur d'analyse du régime conventionnel de
 * communauté universelle (FR — art. 1526 Cciv + 1527 al. 2 Cciv).
 *
 * <p>La communauté universelle est le 4ᵉ régime matrimonial français
 * (régimes par défaut : COMMUNAUTE_LEGALE depuis 1966 ;
 * conventionnels : SEPARATION_BIENS, PARTICIPATION_ACQUETS,
 * COMMUNAUTE_UNIVERSELLE). Tous les biens — présents et à venir,
 * propres ou communs — entrent dans la communauté.</p>
 *
 * <p>L'outil supporte 2 dispositifs :</p>
 * <ul>
 *   <li><strong>VALIDITE_CONVENTION</strong> — validité du contrat de mariage
 *       (art. 1394 Cciv : forme notariée obligatoire ; 1527 al. 2 : action en
 *       retranchement si enfants non communs et CAI).</li>
 *   <li><strong>LIQUIDATION_DECES</strong> — liquidation suite décès :
 *       avec clause d'attribution intégrale (CAI) le conjoint prend 100 %,
 *       sans CAI partage 50/50 + dévolution successorale sur la moitié du défunt.</li>
 * </ul>
 *
 * <p>Outil <strong>single-country FRANCE</strong> — le régime BE relève d'un
 * autre cadre juridique et fait l'objet d'une feature jumelle au backlog
 * si pertinent.</p>
 */
public final class CommunauteUniverselleCalculator {

    /** Dispositif d'analyse demandé (validité ou liquidation). */
    public enum DispositifAnalyse {
        /** Vérification de la validité du contrat de mariage. */
        VALIDITE_CONVENTION,
        /** Liquidation suite décès (avec ou sans CAI). */
        LIQUIDATION_DECES
    }

    /** Verdict de validité du contrat / de la liquidation. */
    public enum VerdictValidite {
        /** Régime/contrat valide — pas de risque identifié. */
        VALIDE,
        /** Régime/contrat contestable — un ou plusieurs vices/risques. */
        CONTESTABLE,
        /** Régime/contrat NUL — préalable de forme non rempli. */
        NUL
    }

    /** Pourcentage d'attribution conjoint avec CAI (clause d'attribution intégrale). */
    private static final int PCT_ATTRIBUTION_CAI = 100;

    /** Pourcentage d'attribution conjoint sans CAI (partage 50/50). */
    private static final int PCT_ATTRIBUTION_SANS_CAI = 50;

    /** Pourcentage d'attribution si contrat NUL (le régime tombe en défaut → indéterminé). */
    private static final int PCT_ATTRIBUTION_NUL = 0;

    /** Base juridique consolidée. */
    private static final String BASE_JURIDIQUE =
            "Art. 1526 Cciv + 1527 al. 2 Cciv (action en retranchement) + 1394 Cciv (forme notariée)";

    private CommunauteUniverselleCalculator() {}

    /**
     * Évalue la validité ou la liquidation d'un régime de communauté universelle.
     *
     * @param dispositifAnalyse        dispositif demandé (obligatoire)
     * @param contratNotarie           contrat de mariage notarié (obligatoire — art. 1394)
     * @param inscriptionEtatCivil     mention sur l'acte de mariage (VALIDITE)
     * @param consentementLibreDesEpoux absence de vice du consentement (VALIDITE)
     * @param respectReserveHereditaire respect réserve héréditaire enfants (VALIDITE)
     * @param clauseAttributionIntegrale présence de la CAI (LIQUIDATION)
     * @param enfantsNonCommuns        existence d'enfants d'un précédent lit (LIQUIDATION)
     * @param valeurCommunauteEur      valeur estimée de la communauté (LIQUIDATION, ≥ 0)
     * @param country                  pays du workspace (FRANCE attendu)
     * @return résultat structuré
     * @throws IllegalArgumentException si paramètre invalide ou pays non supporté
     */
    public static CommunauteUniverselleResult compute(DispositifAnalyse dispositifAnalyse,
                                                       Boolean contratNotarie,
                                                       Boolean inscriptionEtatCivil,
                                                       Boolean consentementLibreDesEpoux,
                                                       Boolean respectReserveHereditaire,
                                                       Boolean clauseAttributionIntegrale,
                                                       Boolean enfantsNonCommuns,
                                                       Double valeurCommunauteEur,
                                                       String country) {
        if (dispositifAnalyse == null) {
            throw new IllegalArgumentException("Dispositif d'analyse requis");
        }
        if (contratNotarie == null) {
            throw new IllegalArgumentException("Contrat notarié (art. 1394 Cciv) requis");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("Pays du workspace requis");
        }
        String countryNormalized = country.trim().toUpperCase(Locale.ROOT);
        if (!"FRANCE".equals(countryNormalized)) {
            throw new IllegalArgumentException(
                    "Outil non disponible pour le pays " + country
                            + " — la communauté universelle BE relève d'un cadre"
                            + " juridique distinct (Code civil belge) et sera traitée"
                            + " dans une feature jumelle dédiée si pertinent.");
        }

        if (dispositifAnalyse == DispositifAnalyse.VALIDITE_CONVENTION) {
            if (inscriptionEtatCivil == null) {
                throw new IllegalArgumentException(
                        "Inscription état civil requise pour le dispositif VALIDITE_CONVENTION");
            }
            if (consentementLibreDesEpoux == null) {
                throw new IllegalArgumentException(
                        "Consentement libre des époux requis pour le dispositif VALIDITE_CONVENTION");
            }
            if (respectReserveHereditaire == null) {
                throw new IllegalArgumentException(
                        "Respect réserve héréditaire requis pour le dispositif VALIDITE_CONVENTION");
            }
        }

        if (dispositifAnalyse == DispositifAnalyse.LIQUIDATION_DECES) {
            if (clauseAttributionIntegrale == null) {
                throw new IllegalArgumentException(
                        "Clause d'attribution intégrale requise pour le dispositif LIQUIDATION_DECES");
            }
            if (enfantsNonCommuns == null) {
                throw new IllegalArgumentException(
                        "Existence d'enfants non communs requise pour le dispositif LIQUIDATION_DECES");
            }
            if (valeurCommunauteEur == null) {
                throw new IllegalArgumentException(
                        "Valeur de la communauté requise pour le dispositif LIQUIDATION_DECES");
            }
            if (valeurCommunauteEur < 0) {
                throw new IllegalArgumentException("Valeur de la communauté doit être ≥ 0");
            }
        }

        // ===== Calcul du verdict =====
        VerdictValidite verdict;
        int score;

        if (!contratNotarie) {
            // Préalable absolu (art. 1394 Cciv) — sans contrat notarié pas de communauté universelle
            verdict = VerdictValidite.NUL;
            score = 0;
        } else if (dispositifAnalyse == DispositifAnalyse.VALIDITE_CONVENTION) {
            // Notes :
            //  - contratNotarie=false → NUL (déjà traité au-dessus, art. 1394).
            //  - vice du consentement, défaut de publicité, atteinte à la réserve
            //    héréditaire → CONTESTABLE (nullité relative ou opposabilité).
            //  - respectReserveHereditaire seul ne rend pas le contrat NUL.
            score = 100;
            if (!consentementLibreDesEpoux) {
                score -= 50;
            }
            if (Boolean.FALSE.equals(inscriptionEtatCivil)) {
                score -= 20;
            }
            if (Boolean.FALSE.equals(respectReserveHereditaire)) {
                score -= 20;
            }
            verdict = (score >= 90) ? VerdictValidite.VALIDE : VerdictValidite.CONTESTABLE;
        } else {
            // LIQUIDATION_DECES — contrat notarié supposé valide → VALIDE par défaut,
            // CONTESTABLE si combinaison CAI + enfants non communs (action en retranchement)
            score = 100;
            boolean hasCai = Boolean.TRUE.equals(clauseAttributionIntegrale);
            boolean hasEnfantsNonCommuns = Boolean.TRUE.equals(enfantsNonCommuns);
            if (hasCai && hasEnfantsNonCommuns) {
                score -= 30; // risque significatif d'action en retranchement
                verdict = VerdictValidite.CONTESTABLE;
            } else {
                verdict = VerdictValidite.VALIDE;
            }
        }

        // ===== Calcul de l'attribution =====
        int partAttributionPct;
        double valeurAttribution;
        boolean actionRetranchementPossible = false;

        if (verdict == VerdictValidite.NUL) {
            partAttributionPct = PCT_ATTRIBUTION_NUL;
            valeurAttribution = 0.0;
        } else if (dispositifAnalyse == DispositifAnalyse.LIQUIDATION_DECES) {
            boolean hasCai = Boolean.TRUE.equals(clauseAttributionIntegrale);
            partAttributionPct = hasCai ? PCT_ATTRIBUTION_CAI : PCT_ATTRIBUTION_SANS_CAI;
            valeurAttribution = (valeurCommunauteEur != null ? valeurCommunauteEur : 0.0)
                    * partAttributionPct / 100.0;
            actionRetranchementPossible = hasCai && Boolean.TRUE.equals(enfantsNonCommuns);
        } else {
            // VALIDITE_CONVENTION — pas d'attribution calculée à ce stade (régime en cours de mariage)
            partAttributionPct = 0;
            valeurAttribution = 0.0;
        }

        // ===== Risques identifiés =====
        List<String> risques = buildRisques(dispositifAnalyse, contratNotarie,
                inscriptionEtatCivil, consentementLibreDesEpoux,
                respectReserveHereditaire, clauseAttributionIntegrale,
                enfantsNonCommuns, verdict, actionRetranchementPossible);

        // ===== Formule =====
        String formule = String.format(Locale.ROOT,
                "Dispositif=%s + contratNotarie=%s + verdict=%s → score %d, "
                        + "attribution conjoint %d%% (%.2f €), retranchement=%s",
                dispositifAnalyse.name(),
                contratNotarie,
                verdict.name(),
                score,
                partAttributionPct,
                valeurAttribution,
                actionRetranchementPossible);

        // ===== Messages =====
        List<String> messages = buildMessages(dispositifAnalyse, contratNotarie,
                inscriptionEtatCivil, consentementLibreDesEpoux,
                respectReserveHereditaire, clauseAttributionIntegrale,
                enfantsNonCommuns, valeurCommunauteEur, verdict,
                actionRetranchementPossible, partAttributionPct, valeurAttribution);

        return new CommunauteUniverselleResult(
                dispositifAnalyse,
                contratNotarie,
                inscriptionEtatCivil,
                consentementLibreDesEpoux,
                respectReserveHereditaire,
                clauseAttributionIntegrale,
                enfantsNonCommuns,
                valeurCommunauteEur,
                countryNormalized,
                verdict,
                score,
                actionRetranchementPossible,
                partAttributionPct,
                valeurAttribution,
                risques,
                BASE_JURIDIQUE,
                formule,
                messages
        );
    }

    private static List<String> buildRisques(DispositifAnalyse dispositif,
                                             Boolean contratNotarie,
                                             Boolean inscriptionEtatCivil,
                                             Boolean consentementLibre,
                                             Boolean reserveHereditaire,
                                             Boolean cai,
                                             Boolean enfantsNonCommuns,
                                             VerdictValidite verdict,
                                             boolean actionRetranchement) {
        List<String> r = new ArrayList<>();
        if (Boolean.FALSE.equals(contratNotarie)) {
            r.add("Contrat de mariage non notarié — nullité absolue du régime (art. 1394 Cciv).");
        }
        if (dispositif == DispositifAnalyse.VALIDITE_CONVENTION) {
            if (Boolean.FALSE.equals(inscriptionEtatCivil)) {
                r.add("Mention non portée sur l'acte de mariage — défaut d'opposabilité aux tiers (art. 1397-1).");
            }
            if (Boolean.FALSE.equals(consentementLibre)) {
                r.add("Vice du consentement — nullité relative possible (art. 1109 et s. Cciv).");
            }
            if (Boolean.FALSE.equals(reserveHereditaire)) {
                r.add("Réserve héréditaire des enfants potentiellement atteinte — action en retranchement encourue (art. 1527 al. 2).");
            }
        }
        if (dispositif == DispositifAnalyse.LIQUIDATION_DECES) {
            if (actionRetranchement) {
                r.add("Action en retranchement possible des enfants non communs (art. 1527 al. 2 Cciv) — "
                        + "ils peuvent réclamer la quotité dont ils auraient hérité dans un régime légal.");
            }
            if (Boolean.TRUE.equals(cai) && !Boolean.TRUE.equals(enfantsNonCommuns)) {
                r.add("Clause d'attribution intégrale + enfants tous communs — pas de risque de retranchement, "
                        + "mais le partage successoral est différé jusqu'au décès du conjoint survivant.");
            }
        }
        if (verdict == VerdictValidite.NUL) {
            r.add("Régime de communauté universelle inopérant — repli sur le régime légal (communauté réduite aux acquêts).");
        }
        return r;
    }

    private static List<String> buildMessages(DispositifAnalyse dispositif,
                                              Boolean contratNotarie,
                                              Boolean inscriptionEtatCivil,
                                              Boolean consentementLibre,
                                              Boolean reserveHereditaire,
                                              Boolean cai,
                                              Boolean enfantsNonCommuns,
                                              Double valeurCommunaute,
                                              VerdictValidite verdict,
                                              boolean actionRetranchement,
                                              int partAttributionPct,
                                              double valeurAttribution) {
        List<String> msgs = new ArrayList<>();

        msgs.add("Dispositif : " + libelleDispositif(dispositif));

        if (Boolean.TRUE.equals(contratNotarie)) {
            msgs.add("Contrat de mariage notarié (art. 1394 Cciv) — forme valide.");
        } else {
            msgs.add("Contrat de mariage NON notarié — sans acte authentique, le régime de communauté "
                    + "universelle est NUL (art. 1394 Cciv). Repli automatique sur le régime légal.");
        }

        if (dispositif == DispositifAnalyse.VALIDITE_CONVENTION) {
            if (Boolean.TRUE.equals(inscriptionEtatCivil)) {
                msgs.add("Mention portée sur l'acte de mariage — régime opposable aux tiers.");
            } else if (Boolean.FALSE.equals(inscriptionEtatCivil)) {
                msgs.add("Mention NON portée sur l'acte de mariage — régime inopposable aux tiers tant que "
                        + "la publicité n'est pas faite (art. 1397-1 Cciv).");
            }
            if (Boolean.TRUE.equals(consentementLibre)) {
                msgs.add("Consentement libre des époux confirmé.");
            } else if (Boolean.FALSE.equals(consentementLibre)) {
                msgs.add("Vice du consentement allégué — risque de nullité relative (art. 1109 et s. Cciv) "
                        + "à constater par le juge.");
            }
            if (Boolean.TRUE.equals(reserveHereditaire)) {
                msgs.add("Réserve héréditaire respectée — pas de risque immédiat d'action en retranchement.");
            } else if (Boolean.FALSE.equals(reserveHereditaire)) {
                msgs.add("Réserve héréditaire des enfants non communs potentiellement atteinte — "
                        + "vérifier l'art. 1527 al. 2 Cciv.");
            }
        }

        if (dispositif == DispositifAnalyse.LIQUIDATION_DECES) {
            if (Boolean.TRUE.equals(cai)) {
                msgs.add("Clause d'attribution intégrale (CAI) — le conjoint survivant prend 100 % "
                        + "de la communauté en franchise de droits de mutation.");
            } else {
                msgs.add("Pas de clause d'attribution intégrale — partage classique 50/50 + dévolution "
                        + "successorale sur la moitié du défunt.");
            }
            if (Boolean.TRUE.equals(enfantsNonCommuns)) {
                msgs.add("Présence d'enfants d'un précédent lit (enfants non communs) — surveiller "
                        + "l'action en retranchement (art. 1527 al. 2 Cciv).");
            }
            if (actionRetranchement) {
                msgs.add("⚠ Action en retranchement possible — les enfants non communs peuvent demander "
                        + "la part dont ils auraient hérité en l'absence de communauté universelle. "
                        + "Conseiller la rédaction d'un éventuel acte de renonciation anticipée.");
            }
            msgs.add(String.format(Locale.ROOT,
                    "Part attribuée au conjoint survivant : %d %% — soit %.2f € sur une communauté de %.2f €.",
                    partAttributionPct, valeurAttribution,
                    valeurCommunaute != null ? valeurCommunaute : 0.0));
        }

        switch (verdict) {
            case VALIDE -> msgs.add("VERDICT VALIDE — la convention de communauté universelle est valide "
                    + "et opposable. Aucun risque majeur identifié.");
            case CONTESTABLE -> msgs.add("VERDICT CONTESTABLE — la convention présente des facteurs de "
                    + "risque (vice du consentement, action en retranchement potentielle, etc.). "
                    + "Documenter les preuves et préparer la défense.");
            case NUL -> msgs.add("VERDICT NUL — le régime de communauté universelle est NUL faute de "
                    + "contrat notarié. Repli automatique sur le régime légal (communauté réduite aux acquêts).");
        }

        msgs.add("Base juridique : " + BASE_JURIDIQUE + ".");

        return msgs;
    }

    private static String libelleDispositif(DispositifAnalyse d) {
        return switch (d) {
            case VALIDITE_CONVENTION -> "Validité de la convention de communauté universelle";
            case LIQUIDATION_DECES -> "Liquidation suite décès d'un époux";
        };
    }
}
