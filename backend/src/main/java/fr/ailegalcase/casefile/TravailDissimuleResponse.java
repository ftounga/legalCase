package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TravailDissimuleResponse(
        UUID caseFileId,
        BigDecimal salaireMensuelReference,
        BigDecimal indemniteForfaitaire,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
