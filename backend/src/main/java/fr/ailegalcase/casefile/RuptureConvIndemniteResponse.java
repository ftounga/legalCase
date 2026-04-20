package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RuptureConvIndemniteResponse(
        UUID caseFileId,
        int ancienneteAnnees,
        BigDecimal salaireMensuel,
        BigDecimal indemniteLegaleMinimum,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
