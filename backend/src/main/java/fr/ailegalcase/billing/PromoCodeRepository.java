package fr.ailegalcase.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromoCodeRepository extends JpaRepository<PromoCode, UUID> {

    /**
     * Lookup case-insensitive — défense en profondeur : en pratique le service
     * normalise déjà le code en uppercase, mais ce contrat empêche un drift
     * si un appelant futur oublie la normalisation.
     */
    Optional<PromoCode> findByCodeIgnoreCase(String code);

    List<PromoCode> findAllByOrderByCreatedAtDesc();

    /**
     * Incrément atomique de {@code uses_count} sous garde-fou
     * {@code uses_count < max_uses}. Retourne 0 si le code est épuisé,
     * traduit en 409 {@code PROMO_CODE_EXHAUSTED} par le service. Pattern
     * choisi pour éviter SELECT FOR UPDATE et rester portable H2/PostgreSQL.
     */
    @Modifying
    @Query("UPDATE PromoCode p SET p.usesCount = p.usesCount + 1 "
            + "WHERE p.id = :id AND p.usesCount < p.maxUses")
    int incrementUsesCount(@Param("id") UUID id);
}
