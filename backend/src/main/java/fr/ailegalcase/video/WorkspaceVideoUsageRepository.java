package fr.ailegalcase.video;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * SF-231-03 — Spring Data repository pour {@link WorkspaceVideoUsage}.
 *
 * <p>La méthode {@link #sumMinutesByWorkspaceAndMonth} est l'oracle du quota
 * mensuel : le total renvoyé est comparé au plafond plan
 * ({@code PlanLimitService.getMonthlyVideoMinutes(planCode)}) avant d'autoriser
 * un nouvel upload vidéo.
 *
 * <p>L'isolation workspace est strictement respectée par le filtre
 * {@code WHERE workspace_id = :workspaceId}. Aucune requête transversale.
 */
@Repository
public interface WorkspaceVideoUsageRepository extends JpaRepository<WorkspaceVideoUsage, UUID> {

    /**
     * Somme des minutes consommées par un workspace pour un mois donné.
     *
     * @param workspaceId workspace cible (isolation stricte)
     * @param yearMonth   format ISO `YYYY-MM` (ex: `2026-05`)
     * @return total des minutes (0 si aucune ligne — COALESCE).
     */
    @Query("""
            SELECT COALESCE(SUM(v.minutesConsumed), 0)
              FROM WorkspaceVideoUsage v
             WHERE v.workspaceId = :workspaceId
               AND v.yearMonth   = :yearMonth
            """)
    int sumMinutesByWorkspaceAndMonth(@Param("workspaceId") UUID workspaceId,
                                      @Param("yearMonth") String yearMonth);
}
