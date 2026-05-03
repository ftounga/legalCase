package fr.ailegalcase.blog.cost;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface BlogUsageEventRepository extends JpaRepository<BlogUsageEvent, UUID> {

    @Query("SELECT COALESCE(SUM(e.costEur), 0) FROM BlogUsageEvent e " +
           "WHERE e.provider = :provider AND e.createdAt >= :since")
    BigDecimal sumCostByProviderSince(@Param("provider") IaProvider provider,
                                      @Param("since") Instant since);
}
