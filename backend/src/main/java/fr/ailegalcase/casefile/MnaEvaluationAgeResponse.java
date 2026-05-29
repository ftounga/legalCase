package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-214-27 : réponse de l'analyse de la procédure d'évaluation d'âge MNA
 * (F-IM-38-mna-evaluation-age-fr, FRANCE uniquement).
 */
public record MnaEvaluationAgeResponse(
        UUID caseFileId,
        String country,
        LocalDate dateNaissanceDeclaree,
        int ageDeclare,
        boolean evaluationASERefusee,
        LocalDate dateRefusASE,
        LocalDate dateEcheanceSaisineJE,
        boolean examenOsseuxOrdonne,
        String resultatExamenOsseux,
        MnaEvaluationAgeStatut statut,
        List<String> procedureASE,
        List<String> contestationExamenOsseux,
        List<String> droitsAttaches,
        String baseJuridique
) {}
