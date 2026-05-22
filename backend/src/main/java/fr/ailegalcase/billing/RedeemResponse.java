package fr.ailegalcase.billing;

import java.time.Instant;

/**
 * F-255 SF-255-01 — résultat d'une redemption TRIAL_EXTENSION réussie.
 *
 * @param newExpiresAt  nouvelle date d'expiration de l'essai après extension
 * @param addedDays     nombre de jours effectivement appliqués
 * @param partnerLabel  étiquette partenaire (affichée au user en confirmation)
 */
public record RedeemResponse(
        Instant newExpiresAt,
        int addedDays,
        String partnerLabel
) {}
