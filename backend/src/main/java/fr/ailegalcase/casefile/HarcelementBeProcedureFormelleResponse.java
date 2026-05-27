package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-213-07 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/harcelement-be-procedure-formelle}.
 *
 * <p>Snapshot complet (inputs + outputs) — pattern uniforme avec les autres
 * outils décisionnels BE de la F-213 (miroir
 * {@link TransactionBeTravailResponse} SF-213-06).</p>
 */
public record HarcelementBeProcedureFormelleResponse(
        UUID caseFileId,

        HarcelementBeTypeEnum typeHarcelement,
        HarcelementBeEtapeEnum etapeProcedure,
        LocalDate dateDepotPlainte,
        boolean entreprisePossedeCPAP,
        HarcelementBeTailleEntrepriseEnum entrepriseTaille,
        boolean mesureDefavorableApres,
        Integer delaiDepuisDepotJours,

        List<HarcelementBeChecklistItemRecord> checklistItems,
        boolean representaillesPossibles,
        LocalDate dateDebutProtectionRepresailles,
        LocalDate dateFinProtectionRepresailles,
        LocalDate prochainDelaiFatal,
        String baseJuridique,
        String avertissement
) {
}
