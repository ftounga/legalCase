package fr.ailegalcase.casefile;

/**
 * SF-218-11 : cause de la rupture du contrat d'un VRP statutaire.
 *
 * <p>Détermine l'éligibilité à l'indemnité de clientèle (art. L.7313-13 CT) :
 * la faute grave et la faute lourde du VRP, ainsi que la démission, excluent par
 * principe le droit à l'indemnité de clientèle. Le licenciement pour cause réelle
 * et sérieuse, le départ en retraite et la rupture conventionnelle n'excluent pas
 * (sous réserve de la condition de fond — clientèle développée). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 */
public enum VrpCauseRupture {
    LICENCIEMENT_CAUSE_REELLE,
    FAUTE_GRAVE,
    FAUTE_LOURDE,
    DEMISSION,
    DEPART_RETRAITE,
    RUPTURE_CONVENTIONNELLE
}
