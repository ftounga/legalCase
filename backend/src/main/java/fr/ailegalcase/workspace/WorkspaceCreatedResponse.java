package fr.ailegalcase.workspace;

import java.util.UUID;

/**
 * SF-156-01 : réponse à {@code POST /api/v1/workspaces}.
 *
 * <p>Renvoie l'identifiant du workspace créé en {@code PENDING_PAYMENT} et
 * l'URL Stripe Checkout vers laquelle l'avocat doit être redirigé pour
 * activer son abonnement.
 *
 * <p>{@code stripeCheckoutUrl} est {@code null} si Stripe est désactivé
 * (mode dev / test sans clé) — le workspace est alors créé directement en
 * {@code ACTIVE} pour ne pas bloquer le dev local.
 */
public record WorkspaceCreatedResponse(
        UUID workspaceId,
        String name,
        String status,
        String planCode,
        String legalDomain,
        String country,
        String stripeCheckoutUrl
) {}
