package fr.ailegalcase.casefile;

import java.util.List;

/**
 * F-243 / SF-243-01 — Réponse de l'endpoint A (référentiel des valeurs).
 *
 * <p>Structure figée du contrat API : domaine + pays demandés, et les 3 listes
 * (juridictions, stades, positions) du sous-référentiel applicable.
 */
public record ProcedureStageOptionsResponse(
        String domain,
        String country,
        List<JurisdictionOption> jurisdictions,
        List<StageOption> stages,
        List<PositionOption> positions
) {

    /** Une juridiction du référentiel. */
    public record JurisdictionOption(String code, String label) {
    }

    /** Un stade du référentiel, avec le code de sa juridiction parente. */
    public record StageOption(String code, String label, String jurisdictionCode) {
    }

    /** Une position du référentiel, avec les codes des stades pour lesquels elle est valide. */
    public record PositionOption(String code, String label, List<String> stageCodes) {
    }

    /** Construit la réponse depuis un sous-référentiel du {@link ProcedureStageCatalog}. */
    public static ProcedureStageOptionsResponse from(String domain, String country,
                                                     ProcedureStageCatalog.CatalogEntry entry) {
        return new ProcedureStageOptionsResponse(
                domain,
                country,
                entry.jurisdictions().stream()
                        .map(j -> new JurisdictionOption(j.code(), j.label()))
                        .toList(),
                entry.stages().stream()
                        .map(s -> new StageOption(s.code(), s.label(), s.jurisdictionCode()))
                        .toList(),
                entry.positions().stream()
                        .map(p -> new PositionOption(p.code(), p.label(), p.stageCodes()))
                        .toList()
        );
    }
}
