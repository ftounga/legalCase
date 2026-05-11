package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-211-01 : résultat de l'analyse du divorce par consentement mutuel (DC) belge.
 * CJ art. 1287-1304 + Loi 27/04/2007 + CC art. 229 §1.
 * Outil <b>single-country BE</b>.
 */
public record DivorceDcBeResult(
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
