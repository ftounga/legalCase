package fr.ailegalcase.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface CreditPurchaseRepository extends JpaRepository<CreditPurchase, UUID> {

    Optional<CreditPurchase> findByStripeSessionId(String stripeSessionId);

    @Query("""
            SELECT COALESCE(SUM(cp.tokensBought), 0)
            FROM CreditPurchase cp
            WHERE cp.workspaceId = :workspaceId
            """)
    long sumTokensBoughtByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
