package fr.ailegalcase.casefile;

/**
 * SF-218-07 : verdict de l'analyse de la saisie sur rémunération (quotité
 * saisissable). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>SAISISSABLE : une part de la rémunération est saisissable selon le
 *       barème progressif par tranches (art. R. 3252-2 CT). La quotité
 *       saisissable mensuelle est strictement positive.</li>
 *   <li>INSAISISSABLE : la rémunération est inférieure ou égale à la fraction
 *       absolument insaisissable (montant forfaitaire RSA — art. L. 3252-3 CT) ;
 *       aucune somme ne peut être saisie (quotité nulle).</li>
 *   <li>ALIMENTAIRE_PAIEMENT_DIRECT : la créance est alimentaire. Le créancier
 *       d'aliments peut recourir au paiement direct (loi du 2 janvier 1973) qui
 *       prime sur le barème classique ; seule la fraction insaisissable (RSA)
 *       reste réservée au débiteur.</li>
 * </ul>
 */
public enum SaisieRemunerationVerdict {
    SAISISSABLE,
    INSAISISSABLE,
    ALIMENTAIRE_PAIEMENT_DIRECT
}
