package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;

/**
 * F-180 SF-180-01 — DTOs du rapport d'audit dashboard exposés par
 * {@code DashboardAuditController}. Contrat figé dans
 * {@code docs/features/F-180/SF-180-01-backend-audit-service.md}.
 */
public final class DashboardAuditDtos {

    private DashboardAuditDtos() {
    }

    /**
     * Rapport complet — un run d'audit désérialisé.
     *
     * @param ranAt          instant du run, jamais null
     * @param crashedMappers panel 🔴 — mappers ayant crashé sur 168h (peut être vide)
     * @param dormantTiles   panel 🟡 — tables de résultat décisionnel à 0 row
     * @param activeTiles    panel 🟢 — tables à ≥ 1 row, triées par rowCount desc
     */
    public record DashboardAuditReport(
            Instant ranAt,
            List<CrashedMapper> crashedMappers,
            List<TileTableCount> dormantTiles,
            List<TileTableCount> activeTiles
    ) {
    }

    /**
     * Un mapper {@link DashboardTile} ayant crashé — agrégé par toolId.
     *
     * @param toolId              identifiant TOOL_REGISTRY du mapper
     * @param crashCount          nombre de crashes sur la fenêtre 168h
     * @param lastExceptionClass  classe de la dernière exception observée
     * @param lastExceptionMessage message de la dernière exception (tronqué 2000)
     * @param lastOccurredAt      instant du dernier crash
     */
    public record CrashedMapper(
            String toolId,
            long crashCount,
            String lastExceptionClass,
            String lastExceptionMessage,
            Instant lastOccurredAt
    ) {
    }

    /**
     * Comptage de rows d'une table de résultat décisionnel.
     *
     * @param tableName nom de la table {@code *_analyses}
     * @param rowCount  nombre de rows (0 → dormante, ≥ 1 → active)
     */
    public record TileTableCount(
            String tableName,
            long rowCount
    ) {
    }
}
