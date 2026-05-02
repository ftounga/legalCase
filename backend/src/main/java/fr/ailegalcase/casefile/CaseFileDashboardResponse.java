package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * Résumé décisionnel transversal du dossier — agrège tous les outils métier
 * sous forme d'une liste générique de {@link DashboardTile}.
 *
 * <p>F-167 SF-167-05 — Les 9 records typés legacy
 * (`LicenciementSummary`, `IndemniteSummary`, `AncienneteSummary`,
 * `TitleDecisionSummary`, `WorkRightSummary`, `RecoursSummary`,
 * `PartageSummary`, `GardeSummary`, `DivorceSummary`) ont été supprimés ; les
 * 9 mappers `tileFromXxxAnalysis` produisent désormais des
 * {@link DashboardTile} équivalents (assemblés par
 * {@code CaseFileDashboardService.assembleTiles()}).</p>
 */
public record CaseFileDashboardResponse(
        UUID caseFileId,
        String legalDomain,

        // Score de risque IA global
        Integer riskScore,
        String riskLevel,

        // F-167 SF-167-01 — Liste générique de tiles décisionnelles. Couvre
        // l'ensemble des outils décisionnels exécutés sur le dossier (Travail
        // FR+BE, Famille FR+BE, Immigration FR+BE).
        List<DashboardTile> tiles
) {
}
