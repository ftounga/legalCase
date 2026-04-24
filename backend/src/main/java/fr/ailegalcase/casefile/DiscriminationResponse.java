package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record DiscriminationResponse(
        UUID caseFileId,
        BigDecimal salaireMensuelReference,
        String motifDiscrimination,
        String contexteActe,
        String country,
        BigDecimal fourchetteMin,
        BigDecimal fourchetteMediane,
        BigDecimal fourchetteMax,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
