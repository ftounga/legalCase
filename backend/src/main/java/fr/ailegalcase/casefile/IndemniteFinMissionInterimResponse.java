package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record IndemniteFinMissionInterimResponse(
        UUID caseFileId,
        BigDecimal totalRemunerationsBrutesEur,
        int dureeMissionJours,
        String motifExclusion,
        LocalDate dateFinMission,
        BigDecimal tauxApplique,
        BigDecimal montantIndemniteEur,
        boolean exclusionRetenue,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
