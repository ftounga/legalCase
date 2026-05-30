package fr.ailegalcase.casefile;

/**
 * SF-218-03 : barème de la garantie AGS (Association pour la gestion du régime
 * de garantie des créances des salariés — L. 3253-6 à L. 3253-21 Code travail).
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Le plafond de garantie de l'AGS est exprimé en multiples du plafond mensuel
 * de la sécurité sociale (PMSS) : 6, 5 ou 4 fois le plafond mensuel selon
 * l'ancienneté du contrat de travail au regard de la date d'ouverture de la
 * procédure collective (art. D. 3253-5 Code travail).
 *
 * <p><b>CONSTANTE À ACTUALISER ANNUELLEMENT</b> : le plafond mensuel de la
 * sécurité sociale est revalorisé au 1er janvier de chaque année. Valeur 2026
 * documentée ci-dessous. Vérifier et mettre à jour {@link #AGS_PLAFOND_MENSUEL_SS}
 * à chaque revalorisation du PMSS.
 */
public final class AgsBareme {

    /**
     * Plafond mensuel de la sécurité sociale (PMSS) — <b>valeur 2026</b>.
     * <b>À ACTUALISER ANNUELLEMENT</b> (revalorisation au 1er janvier).
     * Valeur 2026 : 3 977 € (PMSS 2026).
     */
    public static final double AGS_PLAFOND_MENSUEL_SS = 3_977.0;

    /**
     * Coefficient de plafond AGS le plus élevé (6 × PMSS) — contrat conclu plus
     * de 2 ans avant la date d'ouverture de la procédure (art. D. 3253-5 CT).
     */
    public static final int COEFFICIENT_PLAFOND_6 = 6;

    /**
     * Coefficient de plafond AGS intermédiaire (5 × PMSS) — contrat conclu entre
     * 6 mois et 2 ans avant l'ouverture (art. D. 3253-5 CT).
     */
    public static final int COEFFICIENT_PLAFOND_5 = 5;

    /**
     * Coefficient de plafond AGS le plus bas (4 × PMSS) — contrat conclu moins de
     * 6 mois avant l'ouverture (art. D. 3253-5 CT).
     */
    public static final int COEFFICIENT_PLAFOND_4 = 4;

    private AgsBareme() {
    }

    /**
     * Détermine le coefficient de plafond AGS applicable selon l'ancienneté du
     * contrat (en mois) à la date d'ouverture de la procédure collective.
     *
     * @param ancienneteContratMois ancienneté du contrat en mois ; null ou
     *        négatif → coefficient maximal retenu par défaut (le plus protecteur).
     */
    public static int coefficientPlafond(Integer ancienneteContratMois) {
        if (ancienneteContratMois == null || ancienneteContratMois < 0) {
            return COEFFICIENT_PLAFOND_6;
        }
        if (ancienneteContratMois < 6) {
            return COEFFICIENT_PLAFOND_4;
        }
        if (ancienneteContratMois < 24) {
            return COEFFICIENT_PLAFOND_5;
        }
        return COEFFICIENT_PLAFOND_6;
    }

    /**
     * Plafond AGS en euros = coefficient × PMSS.
     */
    public static double plafondEuros(int coefficient) {
        return coefficient * AGS_PLAFOND_MENSUEL_SS;
    }
}
