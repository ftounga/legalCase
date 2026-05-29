package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-27 : requête POST pour l'analyse de la procédure d'évaluation d'âge
 * MNA (F-IM-38-mna-evaluation-age-fr, FRANCE uniquement).
 */
public record MnaEvaluationAgeRequest(
        LocalDate dateNaissanceDeclaree,
        boolean evaluationASERefusee,
        LocalDate dateRefusASE,
        boolean examenOsseuxOrdonne,
        String resultatExamenOsseux
) {}
