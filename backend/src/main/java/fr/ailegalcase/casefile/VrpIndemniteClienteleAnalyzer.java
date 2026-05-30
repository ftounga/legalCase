package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-218-11 : analyseur de la rupture du contrat d'un VRP statutaire (voyageur
 * représentant placier, art. L.7311-1 et s. CT). Détermine :
 * <ul>
 *   <li>le <b>préavis VRP spécifique</b> (art. L.7313-9 CT) selon l'ancienneté :
 *       &lt; 1 an → 1 mois ; 1 à 2 ans → 2 mois ; &gt; 2 ans → 3 mois ;</li>
 *   <li>l'<b>éligibilité à l'indemnité de clientèle</b> (art. L.7313-13 CT) :
 *       due si la clientèle a été développée par le VRP et que la rupture n'est
 *       imputable ni à une faute grave/lourde ni à une démission ;</li>
 *   <li>une <b>estimation indicative</b> de l'indemnité de clientèle (fourchette
 *       1 à 2 années de commissions — usage jurisprudentiel le plus répandu) ;</li>
 *   <li>l'<b>indemnité légale de licenciement comparée</b> (art. R.1234-2 CT) ;</li>
 *   <li>l'<b>option la plus favorable</b> au titre de la règle de non-cumul
 *       (l'indemnité de clientèle ne se cumule pas avec l'indemnité légale, le
 *       VRP perçoit la plus élevée).</li>
 * </ul>
 * Outil <b>FRANCE UNIQUEMENT</b> (régime VRP du droit français).
 *
 * <p>Distinction (invariant CLAUDE.md — un outil = une situation métier) : cet
 * outil traite l'<b>indemnité de clientèle</b> propre au statut VRP, qui n'a pas
 * d'équivalent dans les autres outils décisionnels (indemnité légale de
 * licenciement générale, indemnité de rupture conventionnelle, etc.). La
 * requalification du statut VRP (litige sur la qualification) est une situation
 * distincte hors périmètre.
 */
public final class VrpIndemniteClienteleAnalyzer {

    /** Préavis VRP (art. L.7313-9 CT) : ancienneté &lt; 1 an. */
    public static final int PREAVIS_MOINS_1_AN_MOIS = 1;
    /** Préavis VRP (art. L.7313-9 CT) : ancienneté de 1 à 2 ans. */
    public static final int PREAVIS_1_A_2_ANS_MOIS = 2;
    /** Préavis VRP (art. L.7313-9 CT) : ancienneté &gt; 2 ans. */
    public static final int PREAVIS_PLUS_2_ANS_MOIS = 3;

    /** Indemnité légale (art. R.1234-2 CT) : 1/4 de mois par an pour les 10 premières années. */
    private static final BigDecimal QUART = new BigDecimal("0.25");
    /** Indemnité légale (art. R.1234-2 CT) : 1/3 de mois par an au-delà de 10 ans. */
    private static final BigDecimal TIERS = BigDecimal.ONE.divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP);
    private static final int SEUIL_INDEMNITE_LEGALE_ANNEES = 10;

    private static final BigDecimal MULTIPLICATEUR_CLIENTELE_MIN = BigDecimal.ONE;
    private static final BigDecimal MULTIPLICATEUR_CLIENTELE_MAX = new BigDecimal("2");

    private static final String BASE_JURIDIQUE =
            "L. 7311-1 et s. CT (statut légal du VRP) ; L. 7313-13 CT (indemnité de "
                    + "clientèle) ; L. 7313-9 CT (préavis VRP : 1 / 2 / 3 mois selon "
                    + "ancienneté) ; L. 7313-11 CT (commissions de retour sur "
                    + "échantillonnages) ; R. 1234-2 CT (indemnité légale de licenciement "
                    + "comparée) ; non-cumul indemnité de clientèle / indemnité légale : "
                    + "jurisprudence constante (Cass. soc.), le VRP perçoit la plus "
                    + "favorable (à vérifier par avocat)";

    private static final String MOTIF_FAUTE_GRAVE =
            "Faute grave du VRP : la rupture est imputable à une faute grave, qui prive "
                    + "le VRP de l'indemnité de clientèle (art. L. 7313-13 CT — indemnité "
                    + "due « sauf faute grave »).";
    private static final String MOTIF_FAUTE_LOURDE =
            "Faute lourde du VRP : la rupture est imputable à une faute lourde, qui prive "
                    + "a fortiori le VRP de l'indemnité de clientèle (art. L. 7313-13 CT).";
    private static final String MOTIF_DEMISSION =
            "Démission du VRP : la rupture est à l'initiative du VRP ; l'indemnité de "
                    + "clientèle suppose en principe une rupture à l'initiative de "
                    + "l'employeur ou non imputable au VRP (art. L. 7313-13 CT — à vérifier "
                    + "par avocat selon les circonstances).";
    private static final String MOTIF_CLIENTELE_NON_DEVELOPPEE =
            "Clientèle non développée par le VRP : la condition de fond de l'indemnité de "
                    + "clientèle n'est pas remplie ; l'indemnité répare le préjudice tiré de "
                    + "la perte de la clientèle créée, apportée ou développée par le VRP "
                    + "(art. L. 7313-13 CT).";

    private VrpIndemniteClienteleAnalyzer() {
    }

    /**
     * Analyse la rupture du contrat VRP.
     *
     * @param dateEntree début du contrat VRP (requis).
     * @param dateRupture date de notification de la rupture (requise, ≥ dateEntree).
     * @param causeRupture cause de la rupture (requise).
     * @param typeVrp type de VRP — défaut {@code EXCLUSIF}.
     * @param commissionsAnnuellesMoyennes moyenne annuelle des commissions des 3
     *        dernières années (assiette de l'indemnité de clientèle ; requise, ≥ 0).
     * @param salaireMensuelMoyen salaire mensuel moyen (pour l'indemnité légale
     *        comparée ; requis, ≥ 0).
     * @param clienteleDeveloppee true si le VRP a créé / apporté / développé la
     *        clientèle (condition de fond L. 7313-13 CT).
     */
    public static VrpIndemniteClienteleResult analyze(LocalDate dateEntree,
                                                      LocalDate dateRupture,
                                                      VrpCauseRupture causeRupture,
                                                      VrpTypeVrp typeVrp,
                                                      BigDecimal commissionsAnnuellesMoyennes,
                                                      BigDecimal salaireMensuelMoyen,
                                                      boolean clienteleDeveloppee) {
        validate(dateEntree, dateRupture, causeRupture, commissionsAnnuellesMoyennes, salaireMensuelMoyen);

        VrpTypeVrp type = typeVrp != null ? typeVrp : VrpTypeVrp.EXCLUSIF;

        long ancienneteMois = ChronoUnit.MONTHS.between(dateEntree, dateRupture);
        BigDecimal ancienneteAnnees = BigDecimal.valueOf(ChronoUnit.DAYS.between(dateEntree, dateRupture))
                .divide(new BigDecimal("365.25"), 6, RoundingMode.HALF_UP);

        int dureePreavisMois = computePreavis(ancienneteMois);

        VrpEligibiliteClientele eligibilite = computeEligibilite(causeRupture, clienteleDeveloppee);
        String motifNonDue = eligibilite == VrpEligibiliteClientele.NON_DUE
                ? computeMotifNonDue(causeRupture, clienteleDeveloppee) : null;

        // estimation indicative — évaluation souveraine du juge (préjudice réel)
        BigDecimal indemniteClienteleMin;
        BigDecimal indemniteClienteleMax;
        if (eligibilite == VrpEligibiliteClientele.DUE) {
            indemniteClienteleMin = scale(commissionsAnnuellesMoyennes.multiply(MULTIPLICATEUR_CLIENTELE_MIN));
            indemniteClienteleMax = scale(commissionsAnnuellesMoyennes.multiply(MULTIPLICATEUR_CLIENTELE_MAX));
        } else {
            indemniteClienteleMin = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            indemniteClienteleMax = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal indemniteLegaleLicenciement =
                computeIndemniteLegale(ancienneteAnnees, salaireMensuelMoyen);

        VrpOptionRecommandee optionRecommandee =
                indemniteClienteleMax.compareTo(indemniteLegaleLicenciement) >= 0
                        ? VrpOptionRecommandee.INDEMNITE_CLIENTELE
                        : VrpOptionRecommandee.INDEMNITE_LEGALE;

        return new VrpIndemniteClienteleResult(
                dateEntree,
                dateRupture,
                causeRupture,
                type,
                scale(commissionsAnnuellesMoyennes),
                scale(salaireMensuelMoyen),
                clienteleDeveloppee,
                ancienneteMois,
                dureePreavisMois,
                eligibilite,
                motifNonDue,
                indemniteClienteleMin,
                indemniteClienteleMax,
                indemniteLegaleLicenciement,
                optionRecommandee,
                BASE_JURIDIQUE);
    }

    /** Préavis VRP (art. L. 7313-9 CT) selon l'ancienneté en mois à la rupture. */
    private static int computePreavis(long ancienneteMois) {
        if (ancienneteMois < 12) {
            return PREAVIS_MOINS_1_AN_MOIS;
        }
        if (ancienneteMois <= 24) {
            return PREAVIS_1_A_2_ANS_MOIS;
        }
        return PREAVIS_PLUS_2_ANS_MOIS;
    }

    private static VrpEligibiliteClientele computeEligibilite(VrpCauseRupture causeRupture,
                                                              boolean clienteleDeveloppee) {
        if (!clienteleDeveloppee) {
            return VrpEligibiliteClientele.NON_DUE;
        }
        return switch (causeRupture) {
            case FAUTE_GRAVE, FAUTE_LOURDE, DEMISSION -> VrpEligibiliteClientele.NON_DUE;
            case LICENCIEMENT_CAUSE_REELLE, DEPART_RETRAITE, RUPTURE_CONVENTIONNELLE ->
                    VrpEligibiliteClientele.DUE;
        };
    }

    private static String computeMotifNonDue(VrpCauseRupture causeRupture, boolean clienteleDeveloppee) {
        return switch (causeRupture) {
            case FAUTE_GRAVE -> MOTIF_FAUTE_GRAVE;
            case FAUTE_LOURDE -> MOTIF_FAUTE_LOURDE;
            case DEMISSION -> MOTIF_DEMISSION;
            default -> clienteleDeveloppee ? null : MOTIF_CLIENTELE_NON_DEVELOPPEE;
        };
    }

    /**
     * Indemnité légale de licenciement (art. R. 1234-2 CT) : ancienneté en années
     * × (1/4 de salaire mensuel pour les 10 premières années + 1/3 au-delà).
     */
    private static BigDecimal computeIndemniteLegale(BigDecimal ancienneteAnnees, BigDecimal salaireMensuelMoyen) {
        BigDecimal seuil = new BigDecimal(SEUIL_INDEMNITE_LEGALE_ANNEES);
        BigDecimal anneesJusquauSeuil = ancienneteAnnees.min(seuil);
        BigDecimal anneesAuDela = ancienneteAnnees.subtract(seuil).max(BigDecimal.ZERO);

        BigDecimal moisIndemnite = anneesJusquauSeuil.multiply(QUART)
                .add(anneesAuDela.multiply(TIERS));
        return scale(moisIndemnite.multiply(salaireMensuelMoyen));
    }

    private static BigDecimal scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static void validate(LocalDate dateEntree,
                                 LocalDate dateRupture,
                                 VrpCauseRupture causeRupture,
                                 BigDecimal commissionsAnnuellesMoyennes,
                                 BigDecimal salaireMensuelMoyen) {
        if (dateEntree == null) {
            throw new IllegalArgumentException("dateEntree est requise");
        }
        if (dateRupture == null) {
            throw new IllegalArgumentException("dateRupture est requise");
        }
        if (dateRupture.isBefore(dateEntree)) {
            throw new IllegalArgumentException("dateRupture ne peut pas être antérieure à dateEntree");
        }
        if (causeRupture == null) {
            throw new IllegalArgumentException("causeRupture est requise");
        }
        if (commissionsAnnuellesMoyennes == null
                || commissionsAnnuellesMoyennes.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "commissionsAnnuellesMoyennes est requise et ne peut pas être négative");
        }
        if (salaireMensuelMoyen == null || salaireMensuelMoyen.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "salaireMensuelMoyen est requis et ne peut pas être négatif");
        }
    }
}
