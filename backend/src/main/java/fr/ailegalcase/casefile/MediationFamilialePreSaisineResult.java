package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-210-01 : résultat du calculateur de médiation familiale obligatoire
 * pré-saisine JAF (Cciv art. 373-2-10 al. 3).
 */
public record MediationFamilialePreSaisineResult(
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
        List<String> messages
) {}
