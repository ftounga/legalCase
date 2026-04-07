package fr.ailegalcase.casefile;

import java.util.List;

/**
 * Résultat de l'analyse de validité d'un licenciement.
 */
public record LicenciementAnalysisResult(
        String country,
        int scoreRisque,
        String verdict,
        List<CritereEvaluation> criteres
) {
    public static final String VALIDE = "VALIDE";
    public static final String RISQUE_MODERE = "RISQUE_MODERE";
    public static final String RISQUE_ELEVE = "RISQUE_ELEVE";
    public static final String INVALIDE = "INVALIDE";

    public record CritereEvaluation(
            String code,
            String label,
            String reponse,
            int pointsRisque,
            boolean bloquant,
            String commentaire
    ) {}
}
