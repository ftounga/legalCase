package fr.ailegalcase.casefile;

/**
 * SF-218-13 : barème du préavis et de l'indemnité de licenciement / de rupture
 * d'un salarié du particulier employeur (CESU) ou d'un assistant maternel —
 * CCN des salariés du particulier employeur (IDCC 3239, 2021), CCN des
 * assistants maternels du particulier employeur, art. L. 7221-1 et s. CT,
 * art. L. 423-1 et s. CASF. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Préavis (selon l'ancienneté à la date de rupture, barème conventionnel) :
 * <ul>
 *   <li>ancienneté &lt; 6 mois → 1 semaine ;</li>
 *   <li>6 mois ≤ ancienneté &lt; 2 ans → 1 mois ;</li>
 *   <li>ancienneté ≥ 2 ans → 2 mois.</li>
 * </ul>
 *
 * <p>Indemnité de licenciement conventionnelle (CCN salariés du particulier
 * employeur) : 1/4 de mois de salaire moyen par année d'ancienneté pour les
 * 10 premières années, 1/3 au-delà (aligné R. 1234-2 CT). Seuil d'ouverture du
 * droit : 8 mois d'ancienneté.
 *
 * <p>Indemnité de rupture des assistants maternels : 1/80 du total des salaires
 * nets perçus depuis le 1er jour du contrat (formule conventionnelle).
 *
 * <p><b>CONSTANTES À ACTUALISER ANNUELLEMENT</b> : les seuils d'ancienneté, les
 * quotités de préavis et le coefficient d'indemnité sont fixés par les CCN ;
 * les barèmes de salaires conventionnels sont révisés chaque année par avenant
 * salaires. Vérifier et mettre à jour à chaque avenant.
 */
public final class BaremeParticulierEmployeurCesu {

    /** Seuil d'ancienneté ouvrant droit à l'indemnité de licenciement (mois). À ACTUALISER. */
    public static final int SEUIL_INDEMNITE_MOIS = 8;

    /** Seuil d'ancienneté (mois) au-delà duquel le préavis passe de 1 semaine à 1 mois. À ACTUALISER. */
    public static final int SEUIL_PREAVIS_6_MOIS = 6;

    /** Seuil d'ancienneté (mois) au-delà duquel le préavis passe de 1 mois à 2 mois (2 ans). À ACTUALISER. */
    public static final int SEUIL_PREAVIS_2_ANS_MOIS = 24;

    /** Durée de préavis en jours pour une ancienneté &lt; 6 mois (1 semaine). À ACTUALISER. */
    public static final int PREAVIS_JOURS_MOINS_6_MOIS = 7;

    /** Durée de préavis en jours pour une ancienneté de 6 mois à &lt; 2 ans (1 mois). À ACTUALISER. */
    public static final int PREAVIS_JOURS_1_MOIS = 30;

    /** Durée de préavis en jours pour une ancienneté ≥ 2 ans (2 mois). À ACTUALISER. */
    public static final int PREAVIS_JOURS_2_MOIS = 60;

    /** Seuil (années) de bascule du coefficient d'indemnité conventionnelle PE (10 ans). À ACTUALISER. */
    public static final int INDEMNITE_PE_SEUIL_ANNEES = 10;

    /** Coefficient d'indemnité conventionnelle PE pour les 10 premières années (1/4 de mois/an). À ACTUALISER. */
    public static final double INDEMNITE_PE_COEFF_10_PREMIERES = 0.25;

    /** Coefficient d'indemnité conventionnelle PE au-delà de 10 ans (1/3 de mois/an). À ACTUALISER. */
    public static final double INDEMNITE_PE_COEFF_AU_DELA = 1.0 / 3.0;

    /** Diviseur de l'indemnité de rupture assistant maternel (1/80 des salaires nets perçus). À ACTUALISER. */
    public static final double INDEMNITE_ASSMAT_DIVISEUR = 80.0;

    /** Nombre moyen de jours par mois servant à estimer l'ancienneté en mois. */
    public static final double JOURS_PAR_MOIS = 30.4375;

    private BaremeParticulierEmployeurCesu() {
    }
}
