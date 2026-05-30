package fr.ailegalcase.casefile;

/**
 * SF-218-15 : barème de l'indemnité de congédiement du journaliste professionnel
 * et seuil de compétence de la commission arbitrale paritaire — art. L.7112-3 et
 * L.7112-4 du Code du travail. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Indemnité de congédiement (art. L.7112-3) : un mois de salaire (dernier
 * salaire mensuel) par année ou fraction d'année d'ancienneté ; toute année
 * commencée compte pour une année entière. L'indemnité de congédiement de droit
 * est plafonnée à 15 mensualités : au-delà de 15 ans d'ancienneté, la fixation
 * de l'indemnité relève de la compétence exclusive de la commission arbitrale
 * paritaire (art. L.7112-4).
 *
 * <p><b>CONSTANTES À ACTUALISER</b> : la quotité (1 mois/année), le plafond
 * (15 mois) et le seuil de saisine de la commission arbitrale (15 ans) sont
 * fixés par le Code du travail ; vérifier à chaque évolution législative.
 */
public final class BaremeJournalisteStatut {

    /** Quotité de l'indemnité de congédiement : 1 mois de salaire par année (art. L.7112-3). À ACTUALISER. */
    public static final int INDEMNITE_MOIS_PAR_ANNEE = 1;

    /**
     * Plafond de l'indemnité de congédiement de droit, en mensualités (15 mois).
     * Au-delà, compétence exclusive de la commission arbitrale (art. L.7112-4). À ACTUALISER.
     */
    public static final int PLAFOND_INDEMNITE_MOIS = 15;

    /**
     * Seuil d'ancienneté (années) au-delà duquel la fixation de l'indemnité
     * relève de la commission arbitrale paritaire (art. L.7112-4). À ACTUALISER.
     */
    public static final int SEUIL_COMMISSION_ARBITRALE_ANNEES = 15;

    private BaremeJournalisteStatut() {
    }
}
