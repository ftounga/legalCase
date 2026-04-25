package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-15-01 : payload de réponse pour POST + GET récompenses.
 */
public record RecompensesResponse(
        UUID caseFileId,
        String regimeMatrimonial,
        List<RecompensesResult.RecompenseDetail> recompenses,
        BigDecimal totalRecompensesDuesParCommunauteEur,
        BigDecimal totalRecompensesDuesParEpouxEur,
        BigDecimal soldeNetPourEpouxEur,
        String baseJuridiqueGenerale,
        List<String> messages,
        String country
) {}
