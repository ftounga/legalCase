package fr.ailegalcase.casefile;

/**
 * SF-216-23 : type d'avantage matrimonial / donation entre époux envisagé
 * (art. 1091-1100 Cciv + art. 1527 Cciv).
 *
 * <ul>
 *   <li>{@link #DONATION_BIEN_PRESENT} — donation entre époux portant sur un
 *       bien actuellement détenu par le donateur (immeuble, somme,
 *       portefeuille). Révocabilité limitée selon le régime matrimonial
 *       (art. 1096 Cciv).</li>
 *   <li>{@link #DONATION_BIENS_FUTURS} — donation portant sur les biens
 *       que le donateur laissera à son décès (« donation au dernier vivant »
 *       — art. 1094 et 1094-1 Cciv). Révocable ad nutum, soumise à la
 *       quotité disponible spéciale entre époux.</li>
 *   <li>{@link #AVANTAGE_MATRIMONIAL} — clause du contrat de mariage
 *       favorisant l'époux survivant (préciput, partage inégal de la
 *       communauté, etc.). Non-libéralité en principe — art. 1527 al. 1.</li>
 *   <li>{@link #ASSURANCE_VIE} — contrat d'assurance-vie souscrit au profit
 *       du conjoint. Hors succession en principe (art. L.132-12 Code des
 *       assurances) sauf primes manifestement exagérées.</li>
 *   <li>{@link #CLAUSE_ATTRIBUTION_INTEGRALE} — clause d'attribution
 *       intégrale de la communauté au conjoint survivant (art. 1524 Cciv).
 *       Avantage matrimonial : exposé à l'action en retranchement des
 *       enfants non communs (art. 1527 al. 2 Cciv).</li>
 * </ul>
 */
public enum AvantageMatrimonialTypeEnum {
    DONATION_BIEN_PRESENT,
    DONATION_BIENS_FUTURS,
    AVANTAGE_MATRIMONIAL,
    ASSURANCE_VIE,
    CLAUSE_ATTRIBUTION_INTEGRALE
}
