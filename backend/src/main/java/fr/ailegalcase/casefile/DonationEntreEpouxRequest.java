package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-216-23 : body POST /api/v1/case-files/{id}/donation-entre-epoux.
 *
 * <p>Outil single-country FRANCE — art. 1091-1100 Cciv (donation entre
 * époux) + art. 265 al. 2 Cciv (révocation en cas de divorce) + art. 1527
 * al. 2 Cciv (action en retranchement enfants non communs) + art. 912-928
 * Cciv (réserve héréditaire).</p>
 *
 * <p>Le service rejette : valeurs négatives, absence de
 * {@code avantageMatrimonialType}, country != FRANCE.</p>
 *
 * @param avantageMatrimonialType    type d'avantage matrimonial / donation
 *                                   envisagé. Requis.
 * @param dateContrat                date du contrat de mariage ou de l'acte
 *                                   de donation. Optionnel.
 * @param regimeMatrimonial          régime matrimonial applicable au couple.
 *                                   Optionnel mais conditionne le verdict
 *                                   sur la révocabilité.
 * @param mariageDissous             true si le divorce est prononcé ou
 *                                   en cours (déclenche la révocation de
 *                                   plein droit — art. 265 al. 2 Cciv).
 * @param revocabiliteDetectee       true si une révocation expresse ou
 *                                   tacite (art. 1096 al. 2) a été
 *                                   documentée dans les pièces.
 * @param bienDonneType              type principal du bien donné (informatif).
 * @param valeurBienDonneEur         valeur du bien donné en euros, utilisée
 *                                   pour mémoire. Non négative.
 * @param enfantsNonCommunsDetected  true si le couple a des enfants non
 *                                   communs (déclenche l'action en
 *                                   retranchement art. 1527 al. 2 quand la
 *                                   clause d'attribution intégrale est
 *                                   stipulée).
 */
public record DonationEntreEpouxRequest(
        AvantageMatrimonialTypeEnum avantageMatrimonialType,
        LocalDate dateContrat,
        RegimeMatrimonialEnum regimeMatrimonial,
        Boolean mariageDissous,
        Boolean revocabiliteDetectee,
        BienDonneTypeEnum bienDonneType,
        Integer valeurBienDonneEur,
        Boolean enfantsNonCommunsDetected
) {}
