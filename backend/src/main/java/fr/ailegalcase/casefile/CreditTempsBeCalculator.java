package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SF-DT-29-01 : calculateur d'éligibilité au crédit-temps belge.
 *
 * <p>3 régimes prévus par la <b>CCT 103</b> (CNT, en vigueur depuis 2012)
 * et l'<b>AR 29/10/1997</b> :
 * <ul>
 *   <li>{@code AVEC_MOTIF} (CCT 103 art. 4) : 4 motifs reconnus
 *       (soins enfant &lt; 8 ans, soins parent malade, formation, autre)</li>
 *   <li>{@code SANS_MOTIF} (CCT 103 art. 3) : quota employeur, ETP &ge; 10</li>
 *   <li>{@code FIN_CARRIERE} (AR 29/10/1997 / CCT 103 art. 8) : âge &ge; 55</li>
 * </ul>
 *
 * <p>Outil <b>single-country BE</b>. L'équivalent FR (CET, art. L.3151-1 et s.)
 * est un mécanisme distinct, hors scope V8.
 */
public final class CreditTempsBeCalculator {

    public static final Set<String> REGIMES = Set.of("AVEC_MOTIF", "SANS_MOTIF", "FIN_CARRIERE");
    public static final Set<String> MOTIFS = Set.of(
            "SOINS_ENFANT_LT_8_ANS", "SOINS_PARENT_MALADE", "FORMATION", "AUTRE");
    public static final Set<String> DUREE_REDUCTION_TYPES = Set.of("CINQUIEME", "MOITIE", "TEMPS_PLEIN");

    /** Ancienneté minimale CCT 103 (24 mois). */
    public static final int ANCIENNETE_MIN_MOIS = 24;

    /** Seuil ETP minimal pour SANS_MOTIF. */
    public static final int ETP_MIN_SANS_MOTIF = 10;

    /** Plancher d'âge pour FIN_CARRIERE (métiers lourds). */
    public static final int AGE_MIN_FIN_CARRIERE = 55;

    /** Âge maximum demandeur. */
    public static final int AGE_MAX_DEMANDEUR = 75;

    /** Plafond légal FIN_CARRIERE (10 ans). */
    public static final int FIN_CARRIERE_DUREE_MAX_MOIS = 120;

    // Indemnités forfaitaires AVEC_MOTIF (€/mois)
    public static final BigDecimal INDEMNITE_AVEC_MOTIF_CINQUIEME = new BigDecimal("150.00");
    public static final BigDecimal INDEMNITE_AVEC_MOTIF_MOITIE = new BigDecimal("400.00");
    public static final BigDecimal INDEMNITE_AVEC_MOTIF_TEMPS_PLEIN = new BigDecimal("700.00");

    /** Coefficient d'abattement SANS_MOTIF (~70 % du barème AVEC_MOTIF). */
    public static final BigDecimal COEFF_SANS_MOTIF = new BigDecimal("0.70");

    // Indemnités forfaitaires FIN_CARRIERE (€/mois)
    public static final BigDecimal INDEMNITE_FIN_CARRIERE_CINQUIEME = new BigDecimal("200.00");
    public static final BigDecimal INDEMNITE_FIN_CARRIERE_MOITIE = new BigDecimal("500.00");

    private static final String BASE_JURIDIQUE = "CCT 103 (CNT) + AR 29/10/1997";

    private CreditTempsBeCalculator() {
    }

    public static CreditTempsBeResult compute(String regime,
                                              String motif,
                                              int ancienneteEntrepriseMois,
                                              int tailleEntrepriseEtp,
                                              String dureeReductionType,
                                              int ageDemandeurAnnees,
                                              LocalDate dateDemande) {
        validateInputs(regime, ancienneteEntrepriseMois, tailleEntrepriseEtp,
                dureeReductionType, ageDemandeurAnnees, dateDemande);

        List<String> criteresNonRemplis = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        int score = 100;

        // Critère commun : ancienneté (CCT 103, tous régimes)
        if (ancienneteEntrepriseMois < ANCIENNETE_MIN_MOIS) {
            criteresNonRemplis.add("Ancienneté ≥ 24 mois");
            messages.add("Ancienneté insuffisante : " + ancienneteEntrepriseMois
                    + " mois — requise ≥ 24 mois (CCT 103).");
            score -= 40;
        } else {
            messages.add("Ancienneté ≥ 24 mois confirmée (CCT 103).");
        }

        // Règles spécifiques par régime
        switch (regime) {
            case "AVEC_MOTIF" -> {
                if (motif == null || motif.isBlank()) {
                    criteresNonRemplis.add("Motif requis pour régime AVEC_MOTIF");
                    messages.add("Motif obligatoire pour le régime AVEC_MOTIF (CCT 103 art. 4).");
                    score -= 40;
                } else if (!MOTIFS.contains(motif)) {
                    criteresNonRemplis.add("Motif invalide : " + motif);
                    messages.add("Motif inconnu — valeurs acceptées : " + MOTIFS + ".");
                    score -= 40;
                } else {
                    messages.add("Motif " + motif + " reconnu (CCT 103 art. 4).");
                }
            }
            case "SANS_MOTIF" -> {
                if (tailleEntrepriseEtp < ETP_MIN_SANS_MOTIF) {
                    criteresNonRemplis.add("Taille entreprise ≥ 10 ETP");
                    messages.add("Taille entreprise insuffisante : " + tailleEntrepriseEtp
                            + " ETP — régime SANS_MOTIF réservé aux entreprises ≥ 10 ETP (CCT 103 art. 3).");
                    score -= 30;
                } else {
                    messages.add("Taille entreprise ≥ 10 ETP confirmée (CCT 103 art. 3).");
                }
            }
            case "FIN_CARRIERE" -> {
                if (ageDemandeurAnnees < AGE_MIN_FIN_CARRIERE) {
                    criteresNonRemplis.add("Âge ≥ 55 ans");
                    messages.add("Âge insuffisant : " + ageDemandeurAnnees
                            + " ans — régime FIN_CARRIERE requiert ≥ 55 ans (AR 29/10/1997, métiers lourds).");
                    score -= 50;
                } else {
                    messages.add("Âge ≥ 55 ans confirmé (AR 29/10/1997).");
                }
                if ("TEMPS_PLEIN".equals(dureeReductionType)) {
                    criteresNonRemplis.add("TEMPS_PLEIN non autorisé en FIN_CARRIERE");
                    messages.add("Le temps plein (suspension complète) n'est pas autorisé en fin de carrière "
                            + "— uniquement CINQUIEME ou MOITIE (CCT 103 art. 8).");
                    score -= 30;
                }
            }
            default -> throw new IllegalArgumentException("Régime inconnu : " + regime);
        }

        // Plancher / plafond du score
        if (score < 0) score = 0;
        if (score > 100) score = 100;

        boolean eligible = criteresNonRemplis.isEmpty();
        String verdict = verdictFromScore(score);

        BigDecimal indemnite = computeIndemnite(regime, dureeReductionType);
        int dureeMax = computeDureeMaxMois(regime, motif);

        String formule = buildFormule(regime, motif, dureeReductionType, indemnite, dureeMax);

        if (eligible) {
            messages.add("Éligibilité confirmée — indemnité ONEM mensuelle estimée : "
                    + formatMontant(indemnite) + " € pendant " + dureeMax + " mois maximum.");
        } else {
            messages.add("Éligibilité non remplie — " + criteresNonRemplis.size()
                    + " critère(s) bloquant(s) à corriger avant introduction de la demande à l'ONEM.");
        }
        messages.add("Outil informatif — l'indemnité ONEM exacte dépend du barème en vigueur "
                + "à la date de la demande (" + dateDemande + ") et doit être validée par le formulaire C61-CT.");

        return new CreditTempsBeResult(
                regime, eligible, score, verdict, criteresNonRemplis,
                indemnite, dureeMax, BASE_JURIDIQUE, formule, messages);
    }

    /** Calcule l'indemnité ONEM mensuelle estimée selon régime + type de réduction. */
    private static BigDecimal computeIndemnite(String regime, String dureeReductionType) {
        return switch (regime) {
            case "AVEC_MOTIF" -> indemniteAvecMotif(dureeReductionType);
            case "SANS_MOTIF" -> indemniteAvecMotif(dureeReductionType)
                    .multiply(COEFF_SANS_MOTIF)
                    .setScale(2, RoundingMode.HALF_UP);
            case "FIN_CARRIERE" -> indemniteFinCarriere(dureeReductionType);
            default -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private static BigDecimal indemniteAvecMotif(String dureeReductionType) {
        return switch (dureeReductionType) {
            case "CINQUIEME" -> INDEMNITE_AVEC_MOTIF_CINQUIEME;
            case "MOITIE" -> INDEMNITE_AVEC_MOTIF_MOITIE;
            case "TEMPS_PLEIN" -> INDEMNITE_AVEC_MOTIF_TEMPS_PLEIN;
            default -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }

    private static BigDecimal indemniteFinCarriere(String dureeReductionType) {
        return switch (dureeReductionType) {
            case "CINQUIEME" -> INDEMNITE_FIN_CARRIERE_CINQUIEME;
            case "MOITIE" -> INDEMNITE_FIN_CARRIERE_MOITIE;
            // TEMPS_PLEIN non autorisé en FIN_CARRIERE — indemnité 0 (critère bloquant)
            default -> BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        };
    }

    /** Plafond légal de durée selon régime + motif (mois). */
    private static int computeDureeMaxMois(String regime, String motif) {
        return switch (regime) {
            case "AVEC_MOTIF" -> dureeMaxAvecMotif(motif);
            case "SANS_MOTIF" -> 12;
            case "FIN_CARRIERE" -> FIN_CARRIERE_DUREE_MAX_MOIS;
            default -> 0;
        };
    }

    private static int dureeMaxAvecMotif(String motif) {
        if (motif == null) return 0;
        return switch (motif) {
            case "SOINS_ENFANT_LT_8_ANS" -> 51;
            case "SOINS_PARENT_MALADE" -> 12;
            case "FORMATION" -> 36;
            case "AUTRE" -> 36;
            default -> 0;
        };
    }

    private static String verdictFromScore(int score) {
        if (score >= 80) return "ELEVEE";
        if (score >= 60) return "MOYENNE";
        return "FAIBLE";
    }

    private static String buildFormule(String regime, String motif, String dureeReductionType,
                                       BigDecimal indemnite, int dureeMax) {
        String motifPart = "AVEC_MOTIF".equals(regime) && motif != null
                ? " (motif " + motif + ")"
                : "";
        return "Régime " + regime + motifPart
                + " | Réduction " + dureeReductionType
                + " | Indemnité ONEM ≈ " + formatMontant(indemnite) + " €/mois"
                + " | Durée max " + dureeMax + " mois";
    }

    private static void validateInputs(String regime,
                                       int ancienneteEntrepriseMois,
                                       int tailleEntrepriseEtp,
                                       String dureeReductionType,
                                       int ageDemandeurAnnees,
                                       LocalDate dateDemande) {
        if (regime == null || regime.isBlank()) {
            throw new IllegalArgumentException("regime est requis");
        }
        if (!REGIMES.contains(regime)) {
            throw new IllegalArgumentException("regime inconnu : " + regime
                    + ". Valeurs acceptées : " + REGIMES);
        }
        if (ancienneteEntrepriseMois < 0) {
            throw new IllegalArgumentException("ancienneteEntrepriseMois doit être ≥ 0");
        }
        if (tailleEntrepriseEtp < 0) {
            throw new IllegalArgumentException("tailleEntrepriseEtp doit être ≥ 0");
        }
        if (dureeReductionType == null || dureeReductionType.isBlank()) {
            throw new IllegalArgumentException("dureeReductionType est requis");
        }
        if (!DUREE_REDUCTION_TYPES.contains(dureeReductionType)) {
            throw new IllegalArgumentException("dureeReductionType inconnu : " + dureeReductionType
                    + ". Valeurs acceptées : " + DUREE_REDUCTION_TYPES);
        }
        if (ageDemandeurAnnees < 0 || ageDemandeurAnnees > AGE_MAX_DEMANDEUR) {
            throw new IllegalArgumentException("ageDemandeurAnnees doit être compris entre 0 et "
                    + AGE_MAX_DEMANDEUR);
        }
        if (dateDemande == null) {
            throw new IllegalArgumentException("dateDemande est requise");
        }
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
