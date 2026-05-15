package fr.ailegalcase.casefile;

import java.util.UUID;

/**
 * F-243 / SF-243-01 — Réponse des endpoints B (lecture) et C (mise à jour).
 *
 * <p>Structure figée du contrat API : les 3 codes du stade procédural du dossier
 * accompagnés de leurs libellés humains. Tout champ non renseigné vaut {@code null}
 * (code et libellé associés).
 */
public record ProcedureStageResponse(
        UUID caseFileId,
        String jurisdiction,
        String jurisdictionLabel,
        String stage,
        String stageLabel,
        String position,
        String positionLabel
) {

    /**
     * Construit la réponse à partir d'un dossier, en résolvant les libellés via le
     * {@link ProcedureStageCatalog} pour le domaine et le pays donnés.
     */
    public static ProcedureStageResponse from(CaseFile caseFile, String domain, String country) {
        String jurisdiction = caseFile.getProcedureJurisdiction();
        String stage = caseFile.getProcedureStage();
        String position = caseFile.getProcedurePosition();
        return new ProcedureStageResponse(
                caseFile.getId(),
                jurisdiction,
                ProcedureStageCatalog.jurisdictionLabel(domain, country, jurisdiction),
                stage,
                ProcedureStageCatalog.stageLabel(domain, country, stage),
                position,
                ProcedureStageCatalog.positionLabel(domain, country, position)
        );
    }
}
