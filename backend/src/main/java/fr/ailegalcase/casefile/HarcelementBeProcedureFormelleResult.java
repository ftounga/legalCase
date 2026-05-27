package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-213-07 : résultat brut produit par
 * {@link HarcelementBeProcedureFormelleProcedureChecker} — fonction pure.
 *
 * <p>N'inclut pas le {@code caseFileId} (ajouté ensuite par
 * {@link HarcelementBeProcedureFormelleService} dans
 * {@link HarcelementBeProcedureFormelleResponse}).</p>
 *
 * <p>{@code dateDebutProtectionRepresailles}, {@code dateFinProtectionRepresailles}
 * et {@code prochainDelaiFatal} sont {@code null} si non applicables à
 * l'étape considérée. {@code avertissement} est {@code null} si aucune
 * alerte n'est levée.</p>
 */
public record HarcelementBeProcedureFormelleResult(
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
