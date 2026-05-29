package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-214-27 : résultat de l'analyse de la procédure d'évaluation d'âge MNA
 * (F-IM-38-mna-evaluation-age-fr, FRANCE uniquement).
 */
public record MnaEvaluationAgeResult(
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
