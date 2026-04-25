package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-21-01 : résultat de l'analyse "Séparation de corps + conversion en divorce"
 * (art. 296 et s. + 306 Code civil + Loi n° 2004-439 du 26/05/2004).
 * Outil single-country FRANCE + DROIT_FAMILLE.
 */
public record SeparationCorpsResult(
        String modeProcedure,
        LocalDate dateJugementSeparationCorps,
        LocalDate dateRequeteConversion,
        int dureeSeparationAnnees,
        boolean consentementMutuelConversion,
        boolean patrimoineCommun,
        int enfantsMineurs,
        boolean demandeReconciliationFormulee,
        boolean dureeSeparationOk,
        boolean delaiConversion2AnsAtteint,
        boolean conversionAutomatiquePossible,
        int scoreEligibiliteConversion,
        String verdictConversion,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
