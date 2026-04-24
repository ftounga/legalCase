package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record IndemnitePrecariteCddResponse(
        UUID caseFileId,
        BigDecimal totalSalairesBruts,
        int tauxPrecarite,
        String casExclusion,
        BigDecimal indemnitePrecarite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
