package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-215-01 : body de la requête POST {@code /api/v1/case-files/{id}/single-permit-be-analysis}.
 * Champs requis — la validation est portée par {@link SinglePermitBeService} / {@link SinglePermitBeCalculator}.
 */
public record SinglePermitBeRequest(
        LocalDate dateDebutPermit,
        LocalDate dateFinPermit,
        SinglePermitBeRegionEnum regionInstruction,
        SinglePermitBeTypeActiviteEnum typeActivite,
        SinglePermitBeMotifEnum motifDemande
) {}
