package fr.ailegalcase.immigration;

/**
 * F-150 SF-150-01 : codes normalisés d'événements factuels ouvrant un nouveau
 * droit de séjour en France (CESEDA). Extrait par l'IA depuis les pièces et
 * enrichi par {@link ImmigrationTriggerEventReferential} avec base légale +
 * titre de séjour cible.
 *
 * <p>Scope V1 : France uniquement. La Belgique (code des étrangers, arrêtés
 * royaux) est hors périmètre de cette SF.
 */
public enum ImmigrationTriggerEventCode {
    MARIAGE_RESSORTISSANT_FR,
    PACS_RESSORTISSANT_FR,
    NAISSANCE_ENFANT_FR,
    DOCTORAT_OBTENU,
    CDI_OBTENU_SALARIE,
    ENTREE_LEGALE_10ANS,
    VIOLENCES_CONJUGALES_CONSTATEES,
    DEMANDE_ASILE_ACCORDEE_OFPRA,
    ENFANT_NE_FR_13ANS_PRESENCE,
    REGROUPEMENT_FAMILIAL_AUTORISE
}
