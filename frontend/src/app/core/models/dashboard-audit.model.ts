/**
 * F-180 SF-180-02 — Modèles du rapport d'audit dashboard tiles F-167.
 * Contrat importé de SF-180-01 (`docs/features/F-180/SF-180-01-backend-audit-service.md`).
 */

/** Panel 🔴 — un mapper DashboardTile ayant crashé en production, agrégé par toolId. */
export interface CrashedMapper {
  toolId: string;
  crashCount: number;
  lastExceptionClass: string;
  lastExceptionMessage: string;
  lastOccurredAt: string;
}

/** Panels 🟡 / 🟢 — comptage de rows d'une table de résultat décisionnel. */
export interface TileTableCount {
  tableName: string;
  rowCount: number;
}

/** Rapport complet d'un run d'audit dashboard. */
export interface DashboardAuditReport {
  ranAt: string;
  crashedMappers: CrashedMapper[];
  dormantTiles: TileTableCount[];
  activeTiles: TileTableCount[];
}
