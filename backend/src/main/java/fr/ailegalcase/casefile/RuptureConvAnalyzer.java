package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analyse la validité d'une rupture conventionnelle à partir des réponses
 * aux 6 critères FR (art. L1237-11 et suivants).
 * Réponses attendues : "OUI" (conforme), "NON" (non-conforme), "INCONNU".
 * Toute valeur inconnue est traitée comme "INCONNU" (fail-open).
 */
public final class RuptureConvAnalyzer {

    private RuptureConvAnalyzer() {}

    /**
     * SF-139-01 : pays supportés (rupture conventionnelle FR only).
     * Remplace le static helper {@code RuptureConvCritereReferentiel.isCountryValid}
     * après suppression de la classe.
     */
    public static boolean isCountryValid(String country) {
        return "FRANCE".equalsIgnoreCase(country);
    }

    public static RuptureConvAnalysisResult analyze(String country, Map<String, String> reponses,
                                                     List<RuptureConvCritere> criteres) {
        Map<String, String> safeReponses = reponses == null ? Map.of() : reponses;
        List<RuptureConvAnalysisResult.CritereEvaluation> evaluations = new ArrayList<>();
        int totalRisque = 0;
        boolean hasBloquant = false;

        for (RuptureConvCritere critere : criteres) {
            String raw = safeReponses.get(critere.code());
            String reponse = normalize(raw);
            int points;
            String commentaire;

            switch (reponse) {
                case "OUI" -> {
                    points = 0;
                    commentaire = "Conforme — " + critere.baseJuridique();
                }
                case "NON" -> {
                    points = critere.poids();
                    commentaire = critere.bloquant()
                            ? "NON CONFORME (bloquant) — " + critere.baseJuridique()
                            : "Non conforme — " + critere.baseJuridique();
                    if (critere.bloquant()) hasBloquant = true;
                }
                default -> {
                    points = critere.poids() / 2;
                    commentaire = "À vérifier — " + critere.baseJuridique();
                    reponse = "INCONNU";
                }
            }

            totalRisque += points;
            evaluations.add(new RuptureConvAnalysisResult.CritereEvaluation(
                    critere.code(), critere.label(), reponse, points, critere.bloquant(), commentaire
            ));
        }

        int score = Math.min(100, totalRisque);

        String verdict;
        if (hasBloquant || score >= 70) {
            verdict = RuptureConvAnalysisResult.INVALIDE;
        } else if (score >= 40) {
            verdict = RuptureConvAnalysisResult.RISQUE_ELEVE;
        } else if (score >= 15) {
            verdict = RuptureConvAnalysisResult.RISQUE_MODERE;
        } else {
            verdict = RuptureConvAnalysisResult.VALIDE;
        }

        return new RuptureConvAnalysisResult(country, score, verdict, evaluations);
    }

    private static String normalize(String raw) {
        if (raw == null) return "INCONNU";
        String up = raw.trim().toUpperCase();
        return switch (up) {
            case "OUI", "NON", "INCONNU" -> up;
            default -> "INCONNU";
        };
    }
}
