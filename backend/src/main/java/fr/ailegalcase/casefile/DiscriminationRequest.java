package fr.ailegalcase.casefile;

import java.math.BigDecimal;

public record DiscriminationRequest(
        BigDecimal salaireMensuelReference,
        String motifDiscrimination,
        String contexteActe
) {}
