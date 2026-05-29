package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-17 : réponse de l'analyse de la procédure d'introduction de la demande
 * d'asile à l'OFPRA (GUDA / ADA, art. R. 521-1 et s. CESEDA). Outil single-country FR.
 */
public record OfpraIntroductionResponse(
        UUID caseFileId,
        LocalDate dateArriveeEnFrance,
        boolean passageGudaEffectue,
        LocalDate datePassageGuda,
        boolean adaRequise,
        String paysOrigine,
        LocalDate dateEcheanceIntroduction,
        long joursRestantsIntroduction,
        OfpraIntroductionStatut statutDelai,
        boolean procedureAccelereeRisque,
        List<String> etapesAPrendre,
        List<String> piecesRequises,
        String recommandation,
        String country,
        String baseJuridique
) {}
