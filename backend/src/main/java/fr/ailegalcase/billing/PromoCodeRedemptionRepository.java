package fr.ailegalcase.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRedemptionRepository extends JpaRepository<PromoCodeRedemption, UUID> {

    /**
     * Vérifie l'invariant anti-abus « 1 TRIAL_EXTENSION par workspace à vie ».
     * Doublé par un index unique partiel PostgreSQL (migration 300), mais
     * exécuté en SELECT préalable côté Java pour produire l'erreur applicative
     * 409 WORKSPACE_ALREADY_REDEEMED_TRIAL_EXTENSION sans dépendre d'une
     * exception SQL (et pour rester portable H2 dans les tests).
     */
    boolean existsByWorkspaceIdAndType(UUID workspaceId, PromoCodeType type);

    long countByPromoCodeId(UUID promoCodeId);

    /**
     * SF-255-04 — idempotence webhook : si Stripe ré-émet le même event
     * {@code customer.discount.created}, on retrouve la redemption précédente
     * et on no-op. Doublé par une contrainte UNIQUE (nullable) DB
     * (migration 302) comme défense en profondeur.
     */
    Optional<PromoCodeRedemption> findBySourceEventId(String sourceEventId);
}
