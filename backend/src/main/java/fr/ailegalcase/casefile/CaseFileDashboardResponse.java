package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Résumé décisionnel transversal du dossier — agrège tous les outils métier.
 */
public record CaseFileDashboardResponse(
        UUID caseFileId,
        String legalDomain,

        // Score de risque IA global
        Integer riskScore,
        String riskLevel,

        // Droit du travail
        LicenciementSummary licenciement,
        IndemniteSummary indemnites,
        AncienneteSummary anciennete,

        // Immigration
        TitleDecisionSummary titleDecision,
        WorkRightSummary workRight,
        RecoursSummary recours,

        // Famille
        PartageSummary partage,
        GardeSummary garde,
        DivorceSummary divorce,

        // F-167 SF-167-01 — Liste générique de tiles décisionnelles (étend
        // progressivement la couverture du dashboard à tous les outils
        // décisionnels). Coexiste avec les 9 records typés ci-dessus pendant
        // la transition ; SF-167-05 fusionnera et supprimera ces records.
        List<DashboardTile> tiles
) {
    public record LicenciementSummary(int scoreRisque, String verdict, int criteresNonConformes, int criteresTotal) {}
    public record IndemniteSummary(String country, BigDecimal fourchetteBasse, BigDecimal fourhetteHaute, String baremeSource) {}
    public record AncienneteSummary(int annees, int mois, int congesTotalJours, int ecartsDetectes) {}
    public record TitleDecisionSummary(int nbRecommandations, String premierTitreLabel) {}
    public record WorkRightSummary(String droitTravail, String titreLabel) {}
    public record RecoursSummary(String recoursLabel, String dateLimite, boolean dateLimiteDepassee) {}
    public record PartageSummary(BigDecimal soulte, BigDecimal coutTotal) {}
    public record GardeSummary(String gardeLabel, int joursParentA, int joursParentB) {}
    public record DivorceSummary(int etapesCompletees, int etapesTotal, int piecesPresentes, int piecesTotal, int progressionPct) {}
}
