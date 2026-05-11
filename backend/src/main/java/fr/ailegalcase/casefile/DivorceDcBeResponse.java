package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DivorceDcBeResponse(
        UUID caseFileId,
        String country,
        LocalDate dateSignatureConvention,
        LocalDate dateAudienceHomologation,
        long delaiReflexionJours,
        boolean delaiReflexionRespecte,
        boolean conventionLogement,
        boolean conventionBiens,
        boolean conventionGardeEnfants,
        boolean conventionContributions,
        boolean conventionComplete,
        boolean enfantsMineursCommuns,
        boolean epouxConsentent,
        String verdict,
        List<String> motifsIrrecevabilite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
