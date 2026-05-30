package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * SF-218-13 : analyseur de la rupture du contrat d'un salarié du particulier
 * employeur (CESU : garde d'enfants, employé de maison) ou d'un assistant
 * maternel — préavis conventionnel et indemnité de licenciement / de rupture.
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Trois situations métier distinctes (invariant CLAUDE.md — un outil = une
 * situation) :
 * <ul>
 *   <li><b>Salarié du particulier employeur</b> (CCN 2021) : préavis par
 *       tranche d'ancienneté ; indemnité de licenciement = 1/4 de mois par
 *       année pour les 10 premières années + 1/3 au-delà, due à partir de 8
 *       mois d'ancienneté ;</li>
 *   <li><b>Assistant maternel</b> (CCN assistants maternels) : préavis par
 *       tranche d'ancienneté ; indemnité de rupture = 1/80 du total des
 *       salaires nets perçus depuis le 1er jour du contrat, due à partir de 8
 *       mois d'accueil — le retrait de l'enfant ouvre droit à cette
 *       indemnité ;</li>
 *   <li><b>Faute grave</b> : prive de l'indemnité de licenciement / de rupture
 *       quelle que soit la catégorie.</li>
 * </ul>
 *
 * <p>Sources : CCN des salariés du particulier employeur (IDCC 3239, 2021) ;
 * CCN des assistants maternels du particulier employeur ; art. L. 7221-1 et s.
 * Code du travail (employés de maison) ; art. L. 423-1 et s. CASF (assistant
 * maternel, retrait de l'enfant).
 */
public final class ParticulierEmployeurCesuAnalyzer {

    static final String BASE_JURIDIQUE =
            "CCN des salariés du particulier employeur (IDCC 3239, 2021) — préavis et "
                    + "indemnité de licenciement ; CCN des assistants maternels du "
                    + "particulier employeur — indemnité de rupture (1/80 des salaires nets) ; "
                    + "art. L. 7221-1 et s. Code du travail (employés de maison) ; art. L. 423-1 "
                    + "et s. Code de l'action sociale et des familles (assistant maternel, retrait "
                    + "de l'enfant) (à vérifier par avocat)";

    private ParticulierEmployeurCesuAnalyzer() {
    }

    /**
     * Calcule le préavis, l'éligibilité et le montant de l'indemnité, et le
     * verdict global.
     *
     * @param dateEntree début du contrat (requis).
     * @param dateRupture date de notification de la rupture (requis, ≥ dateEntree).
     * @param categorieEmploye catégorie pilotant la CCN (requis).
     * @param causeRupture cause de la rupture (requis).
     * @param salaireMensuelMoyen assiette de l'indemnité (€, strictement positif).
     */
    public static ParticulierEmployeurCesuResult analyze(LocalDate dateEntree,
                                                         LocalDate dateRupture,
                                                         ParticulierEmployeurCesuCategorie categorieEmploye,
                                                         ParticulierEmployeurCesuCause causeRupture,
                                                         BigDecimal salaireMensuelMoyen) {
        validate(dateEntree, dateRupture, categorieEmploye, causeRupture, salaireMensuelMoyen);

        int ancienneteMois = (int) ChronoUnit.MONTHS.between(dateEntree, dateRupture);

        int preavisJours = preavisJours(ancienneteMois);
        String preavisLibelle = preavisLibelle(ancienneteMois);

        // Éligibilité à l'indemnité.
        ParticulierEmployeurCesuEligibilite eligibilite;
        String motifNonDue = null;
        if (causeRupture == ParticulierEmployeurCesuCause.FAUTE_GRAVE) {
            eligibilite = ParticulierEmployeurCesuEligibilite.NON_DUE;
            motifNonDue = "Licenciement pour faute grave — l'indemnité de licenciement n'est pas due";
        } else if (ancienneteMois < BaremeParticulierEmployeurCesu.SEUIL_INDEMNITE_MOIS) {
            eligibilite = ParticulierEmployeurCesuEligibilite.NON_DUE;
            motifNonDue = "Ancienneté inférieure au seuil conventionnel de "
                    + BaremeParticulierEmployeurCesu.SEUIL_INDEMNITE_MOIS + " mois";
        } else {
            eligibilite = ParticulierEmployeurCesuEligibilite.DUE;
        }

        // Montant et méthode de l'indemnité.
        BigDecimal indemnite;
        ParticulierEmployeurCesuMethode methode;
        if (eligibilite == ParticulierEmployeurCesuEligibilite.NON_DUE) {
            indemnite = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            methode = ParticulierEmployeurCesuMethode.AUCUNE;
        } else if (categorieEmploye == ParticulierEmployeurCesuCategorie.ASSISTANT_MATERNEL) {
            indemnite = indemniteRuptureAssmat(salaireMensuelMoyen, ancienneteMois);
            methode = ParticulierEmployeurCesuMethode.INDEMNITE_RUPTURE_ASSMAT;
        } else {
            indemnite = indemniteConventionnellePe(salaireMensuelMoyen, ancienneteMois);
            methode = ParticulierEmployeurCesuMethode.CONVENTIONNEL_PE;
        }

        // Verdict global.
        ParticulierEmployeurCesuVerdict verdict;
        if (eligibilite == ParticulierEmployeurCesuEligibilite.NON_DUE) {
            verdict = ParticulierEmployeurCesuVerdict.INDEMNITE_NON_DUE;
        } else {
            // Une indemnité est due et/ou un préavis est à formaliser : rupture à sécuriser.
            verdict = ParticulierEmployeurCesuVerdict.RUPTURE_A_SECURISER;
        }

        return new ParticulierEmployeurCesuResult(
                dateEntree,
                dateRupture,
                categorieEmploye,
                causeRupture,
                salaireMensuelMoyen.setScale(2, RoundingMode.HALF_UP),
                ancienneteMois,
                preavisJours,
                preavisLibelle,
                eligibilite,
                motifNonDue,
                indemnite,
                methode,
                verdict,
                BASE_JURIDIQUE);
    }

    private static int preavisJours(int ancienneteMois) {
        if (ancienneteMois < BaremeParticulierEmployeurCesu.SEUIL_PREAVIS_6_MOIS) {
            return BaremeParticulierEmployeurCesu.PREAVIS_JOURS_MOINS_6_MOIS;
        } else if (ancienneteMois < BaremeParticulierEmployeurCesu.SEUIL_PREAVIS_2_ANS_MOIS) {
            return BaremeParticulierEmployeurCesu.PREAVIS_JOURS_1_MOIS;
        } else {
            return BaremeParticulierEmployeurCesu.PREAVIS_JOURS_2_MOIS;
        }
    }

    private static String preavisLibelle(int ancienneteMois) {
        if (ancienneteMois < BaremeParticulierEmployeurCesu.SEUIL_PREAVIS_6_MOIS) {
            return "1 semaine";
        } else if (ancienneteMois < BaremeParticulierEmployeurCesu.SEUIL_PREAVIS_2_ANS_MOIS) {
            return "1 mois";
        } else {
            return "2 mois";
        }
    }

    /**
     * Indemnité de licenciement conventionnelle CCN salariés du particulier
     * employeur : 1/4 de mois de salaire moyen par année d'ancienneté pour les
     * 10 premières années, 1/3 au-delà.
     * <p>// barème CCN à actualiser annuellement (avenant salaires)
     */
    private static BigDecimal indemniteConventionnellePe(BigDecimal salaireMensuelMoyen, int ancienneteMois) {
        double anciennete = ancienneteMois / 12.0;
        double seuil = BaremeParticulierEmployeurCesu.INDEMNITE_PE_SEUIL_ANNEES;
        double anneesBasses = Math.min(anciennete, seuil);
        double anneesHautes = Math.max(0.0, anciennete - seuil);
        double coeff = anneesBasses * BaremeParticulierEmployeurCesu.INDEMNITE_PE_COEFF_10_PREMIERES
                + anneesHautes * BaremeParticulierEmployeurCesu.INDEMNITE_PE_COEFF_AU_DELA;
        return salaireMensuelMoyen
                .multiply(BigDecimal.valueOf(coeff))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Indemnité de rupture des assistants maternels : 1/80 du total des salaires
     * nets perçus depuis le 1er jour du contrat. Le total est estimé à partir du
     * salaire mensuel moyen et de l'ancienneté en mois.
     * <p>// barème CCN à actualiser annuellement (avenant salaires)
     */
    private static BigDecimal indemniteRuptureAssmat(BigDecimal salaireMensuelMoyen, int ancienneteMois) {
        BigDecimal totalSalaires = salaireMensuelMoyen.multiply(BigDecimal.valueOf(ancienneteMois));
        return totalSalaires
                .divide(BigDecimal.valueOf(BaremeParticulierEmployeurCesu.INDEMNITE_ASSMAT_DIVISEUR),
                        2, RoundingMode.HALF_UP);
    }

    private static void validate(LocalDate dateEntree,
                                 LocalDate dateRupture,
                                 ParticulierEmployeurCesuCategorie categorieEmploye,
                                 ParticulierEmployeurCesuCause causeRupture,
                                 BigDecimal salaireMensuelMoyen) {
        if (dateEntree == null) {
            throw new IllegalArgumentException("dateEntree est requise");
        }
        if (dateRupture == null) {
            throw new IllegalArgumentException("dateRupture est requise");
        }
        if (categorieEmploye == null) {
            throw new IllegalArgumentException("categorieEmploye est requise");
        }
        if (causeRupture == null) {
            throw new IllegalArgumentException("causeRupture est requise");
        }
        if (dateRupture.isBefore(dateEntree)) {
            throw new IllegalArgumentException("dateRupture ne peut pas être antérieure à dateEntree");
        }
        if (salaireMensuelMoyen == null || salaireMensuelMoyen.signum() <= 0) {
            throw new IllegalArgumentException("salaireMensuelMoyen doit être strictement positif");
        }
        if (causeRupture == ParticulierEmployeurCesuCause.RETRAIT_ENFANT
                && categorieEmploye != ParticulierEmployeurCesuCategorie.ASSISTANT_MATERNEL) {
            throw new IllegalArgumentException(
                    "RETRAIT_ENFANT est réservé à la catégorie ASSISTANT_MATERNEL");
        }
    }
}
