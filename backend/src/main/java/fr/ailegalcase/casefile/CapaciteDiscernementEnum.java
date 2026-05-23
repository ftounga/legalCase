package fr.ailegalcase.casefile;

/**
 * SF-216-13 : capacité de discernement du mineur appréciée par le juge
 * (art. 388-1 Cciv).
 *
 * <p>Pas de seuil d'âge légal fixe en droit français — appréciation
 * <i>in concreto</i> du discernement, fonction de l'âge, de la maturité
 * et de la nature de l'affaire (Cass. 1ère civ., 22/11/2005, n°03-17.911).
 * Seuil indicatif jurisprudentiel : généralement à partir de 7-8 ans, voire
 * plus tôt selon les circonstances. Avant 5-6 ans, discernement
 * hautement improbable.</p>
 */
public enum CapaciteDiscernementEnum {
    CERTAINE,
    PROBABLE,
    DOUTEUSE,
    INCONNUE
}
