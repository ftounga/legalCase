package fr.ailegalcase.casefile;

import java.time.LocalDate;

public record DivorceDcBeRequest(
        LocalDate dateSignatureConvention,
        LocalDate dateAudienceHomologation,
        Boolean conventionLogement,
        Boolean conventionBiens,
        Boolean conventionGardeEnfants,
        Boolean conventionContributions,
        Boolean enfantsMineursCommuns,
        Boolean epouxConsentent
) {}
