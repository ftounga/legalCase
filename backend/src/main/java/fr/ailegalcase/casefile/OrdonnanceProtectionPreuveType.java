package fr.ailegalcase.casefile;

/**
 * SF-FA-14-01 : types de preuves disponibles à l'appui d'une demande
 * d'ordonnance de protection (art. 515-9 Cciv). La diversification (≥ 3
 * types) est un facteur de score de vraisemblance.
 */
public enum OrdonnanceProtectionPreuveType {
    CONSTAT_HUISSIER,
    MAIN_COURANTE,
    CERTIFICAT_MEDICAL,
    TEMOIGNAGES,
    PHOTOS,
    PLAINTE_DEPOSEE,
    JUGEMENT_CORRECTIONNEL,
    AUTRE
}
