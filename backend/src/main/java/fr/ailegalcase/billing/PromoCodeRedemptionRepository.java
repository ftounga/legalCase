package fr.ailegalcase.billing;

import org.springframework.data.jpa.repository.JpaRepository;

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
}
