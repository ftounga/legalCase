package fr.ailegalcase.casefile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Analyse la validité d'un licenciement à partir des réponses aux critères.
 * Réponses attendues : "OUI" (conforme), "NON" (non conforme), "INCONNU".
 */
public final class LicenciementAnalyzer {

    private LicenciementAnalyzer() {}

    /**
     * SF-139-01 : pays supportés (FR + BE). Remplace le static helper
     * {@code LicenciementCritereReferentiel.isCountryValid} après suppression.
     */
    public static boolean isCountryValid(String country) {
        return "FRANCE".equalsIgnoreCase(country) || "BELGIQUE".equalsIgnoreCase(country);
    }

    public static LicenciementAnalysisResult analyze(String country, Map<String, String> reponses, List<LicenciementCritere> criteres) {
        List<LicenciementAnalysisResult.CritereEvaluation> evaluations = new ArrayList<>();
        int totalRisque = 0;
        boolean hasBloquant = false;

        for (LicenciementCritere critere : criteres) {
            String reponse = reponses.getOrDefault(critere.code(), "INCONNU");
            int points = 0;
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
                }
            }

            totalRisque += points;
            evaluations.add(new LicenciementAnalysisResult.CritereEvaluation(
                    critere.code(), critere.label(), reponse, points, critere.bloquant(), commentaire
            ));
        }

        int totalPoids = criteres.stream().mapToInt(LicenciementCritere::poids).sum();
        int score = totalPoids > 0 ? Math.min(100, (totalRisque * 100) / totalPoids) : 0;

        String verdict;
        if (hasBloquant || score >= 70) {
            verdict = LicenciementAnalysisResult.INVALIDE;
        } else if (score >= 40) {
            verdict = LicenciementAnalysisResult.RISQUE_ELEVE;
        } else if (score >= 15) {
            verdict = LicenciementAnalysisResult.RISQUE_MODERE;
        } else {
            verdict = LicenciementAnalysisResult.VALIDE;
        }

        return new LicenciementAnalysisResult(country, score, verdict, evaluations);
    }
}
