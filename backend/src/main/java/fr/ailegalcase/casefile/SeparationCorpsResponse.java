package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SeparationCorpsResponse(
        UUID caseFileId,
        String modeProcedure,
        LocalDate dateJugementSeparationCorps,
        LocalDate dateRequeteConversion,
        int dureeSeparationAnnees,
        boolean consentementMutuelConversion,
        boolean patrimoineCommun,
        int enfantsMineurs,
        boolean demandeReconciliationFormulee,
        String country,
        boolean dureeSeparationOk,
        boolean delaiConversion2AnsAtteint,
        boolean conversionAutomatiquePossible,
        int scoreEligibiliteConversion,
        String verdictConversion,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
