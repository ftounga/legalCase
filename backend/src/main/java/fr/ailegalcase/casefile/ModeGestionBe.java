package fr.ailegalcase.casefile;

/**
 * SF-217-01 : mode de gestion applicable à un bien sous régime de communauté
 * légale belge.
 */
public enum ModeGestionBe {

    /** Chaque époux gère seul (CC Livre 3, art. 3.65). */
    GESTION_CONCURRENTE,

    /** L'époux propriétaire / exerçant gère seul son bien propre ou son outil de travail. */
    GESTION_EXCLUSIVE,

    /** Actes graves sur biens communs : accord des deux époux requis. */
    COGESTION
}
