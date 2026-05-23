package fr.ailegalcase.casefile;

/**
 * SF-216-23 : régime matrimonial principal applicable au couple.
 *
 * <p>Utilisé par l'outil "Donation entre époux" (F-FA-DONATION-ENTRE-EPOUX)
 * pour évaluer la révocabilité (art. 1096 Cciv) — la révocation est en
 * principe libre, sauf si l'avantage matrimonial est intégré au contrat de
 * mariage (auquel cas la révocation suit le régime du changement de
 * contrat).</p>
 *
 * <ul>
 *   <li>{@link #COMMUNAUTE_LEGALE} — communauté réduite aux acquêts
 *       (art. 1400 Cciv) — régime légal par défaut depuis 1966.</li>
 *   <li>{@link #COMMUNAUTE_UNIVERSELLE} — communauté universelle avec
 *       éventuelle clause d'attribution intégrale (art. 1524 Cciv).</li>
 *   <li>{@link #SEPARATION_DE_BIENS} — séparation pure (art. 1536 Cciv) —
 *       chaque époux conserve la propriété et la gestion de ses biens.</li>
 *   <li>{@link #PARTICIPATION_AUX_ACQUETS} — séparation pendant l'union,
 *       partage à la dissolution (art. 1569 Cciv).</li>
 *   <li>{@link #AUTRE} — régime étranger / autre régime conventionnel.</li>
 * </ul>
 */
public enum RegimeMatrimonialEnum {
    COMMUNAUTE_LEGALE,
    COMMUNAUTE_UNIVERSELLE,
    SEPARATION_DE_BIENS,
    PARTICIPATION_AUX_ACQUETS,
    AUTRE
}
