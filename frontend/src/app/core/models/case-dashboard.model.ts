/**
 * F-167 SF-167-01 — DTO d'une tile générique du dashboard agrégé.
 *
 * Mappe le record backend `fr.ailegalcase.casefile.DashboardTile` (champs
 * sérialisés tels quels par Jackson).
 *
 * <p>F-167 SF-167-05 — Les 9 records typés legacy (`licenciement`,
 * `indemnites`, `anciennete`, `titleDecision`, `workRight`, `recours`,
 * `partage`, `garde`, `divorce`) ont été supprimés. La liste {@code tiles}
 * couvre désormais l'ensemble des outils décisionnels exécutés sur le
 * dossier.</p>
 */
export interface DashboardTile {
  toolId: string;
  theme: 'INDEMNITES' | 'VALIDITE' | 'DELAIS' | 'DOCUMENTS' | 'DIAGNOSTIC';
  label: string;
  primaryValue: string;
  secondaryValue?: string | null;
  alertLevel?: 'OK' | 'WARNING' | 'ALERT' | null;
}

export interface DashboardResponse {
  caseFileId: string;
  legalDomain: string;
  riskScore: number | null;
  riskLevel: string | null;

  // F-167 SF-167-01 / SF-167-05 — Liste générique de tiles décisionnelles.
  tiles?: DashboardTile[];
}
