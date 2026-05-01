package fr.ailegalcase.casefile;

/**
 * F-167 SF-167-01 — Tile générique du dashboard agrégé. Représente le verdict
 * synthétique d'un outil décisionnel (issu d'une *Analysis persistée).
 *
 * <p>Cette structure est destinée à remplacer progressivement les 9 records typés
 * de {@link CaseFileDashboardResponse} (Licenciement/Indemnite/.../Divorce). Elle
 * coexiste avec eux pendant la transition (SF-167-01 → SF-167-04) ; la fusion
 * et la suppression des records typés sont prévues en SF-167-05.</p>
 *
 * @param toolId        Identifiant TOOL_REGISTRY frontend (ex. F-IM-11-changement-statut).
 * @param theme         Thème métier (INDEMNITES | VALIDITE | DELAIS | DOCUMENTS | DIAGNOSTIC).
 * @param label         Libellé court UI (≤ 60 chars).
 * @param primaryValue  Verdict humain lisible (≤ 80 chars).
 * @param secondaryValue Détail subordonné (≤ 100 chars), nullable.
 * @param alertLevel    OK | WARNING | ALERT, nullable (pour outils sans verdict tranché).
 */
public record DashboardTile(
        String toolId,
        String theme,
        String label,
        String primaryValue,
        String secondaryValue,
        String alertLevel
) {}
