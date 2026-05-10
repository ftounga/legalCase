package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-210-01 : réponse de l'API médiation familiale pré-saisine.
 */
public record MediationFamilialePreSaisineResponse(
        UUID caseFileId,
        String motifSaisine,
        boolean mediationTentee,
        LocalDate dateMediation,
        String exceptionApplicable,
        String exceptionDetail,
        String verdict,
        boolean motifInScope,
        boolean dispenseApplicable,
        List<String> piecesAJoindre,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
