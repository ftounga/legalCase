package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-215-01 : payload de réponse HTTP pour {@code /api/v1/case-files/{id}/single-permit-be-analysis}.
 */
public record SinglePermitBeResponse(
        UUID caseFileId,
        LocalDate dateDebutPermit,
        LocalDate dateFinPermit,
        SinglePermitBeRegionEnum regionInstruction,
        SinglePermitBeTypeActiviteEnum typeActivite,
        SinglePermitBeMotifEnum motifDemande,
        String country,
        LocalDate dateLimiteDemande,
        long joursAvantExpiration,
        SinglePermitBeStatutRenouvellement statutRenouvellement,
        String regionCompetente,
        List<String> etapesProchaines,
        String baseJuridique
) {}
