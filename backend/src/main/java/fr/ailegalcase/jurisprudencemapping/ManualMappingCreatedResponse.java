package fr.ailegalcase.jurisprudencemapping;

import java.time.LocalDate;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-15 — réponse 201 Created du POST /mappings.
 */
public record ManualMappingCreatedResponse(
        UUID id,
        String toolId,
        String brancheCalculId,
        String arretRef,
        String juridiction,
        LocalDate dateArret,
        String numeroPourvoi,
        String lienLegifrance) {

    public static ManualMappingCreatedResponse from(ToolJurisprudenceMapping m) {
        return new ManualMappingCreatedResponse(
                m.getId(),
                m.getToolId(),
                m.getBrancheCalculId(),
                m.getArretRef(),
                m.getJuridiction(),
                m.getDateArret(),
                m.getNumeroPourvoi(),
                m.getLienLegifrance());
    }
}
