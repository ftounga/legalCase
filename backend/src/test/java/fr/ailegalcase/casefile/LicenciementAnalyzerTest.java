package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LicenciementAnalyzerTest {

    @Test
    void allOui_france_scoreZero_valide() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : LicenciementCritereReferentiel.getByCountry("FRANCE")) {
            reponses.put(c.code(), "OUI");
        }
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses);

        assertThat(result.scoreRisque()).isZero();
        assertThat(result.verdict()).isEqualTo("VALIDE");
        assertThat(result.criteres()).allMatch(c -> c.pointsRisque() == 0);
    }

    @Test
    void allNon_france_invalide() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : LicenciementCritereReferentiel.getByCountry("FRANCE")) {
            reponses.put(c.code(), "NON");
        }
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses);

        assertThat(result.scoreRisque()).isEqualTo(100);
        assertThat(result.verdict()).isEqualTo("INVALIDE");
    }

    @Test
    void bloquantNon_france_invalideRegardlessOfScore() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : LicenciementCritereReferentiel.getByCountry("FRANCE")) {
            reponses.put(c.code(), "OUI");
        }
        // Set one bloquant to NON
        reponses.put("FR_MOTIVATION", "NON");

        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses);
        assertThat(result.verdict()).isEqualTo("INVALIDE");
    }

    @Test
    void allOui_belgique_valide() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : LicenciementCritereReferentiel.getByCountry("BELGIQUE")) {
            reponses.put(c.code(), "OUI");
        }
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("BELGIQUE", reponses);

        assertThat(result.scoreRisque()).isZero();
        assertThat(result.verdict()).isEqualTo("VALIDE");
    }

    @Test
    void partialNon_belgique_risqueModereOuEleve() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : LicenciementCritereReferentiel.getByCountry("BELGIQUE")) {
            reponses.put(c.code(), "OUI");
        }
        // Non-bloquant critères only
        reponses.put("BE_AUDITION", "NON");
        reponses.put("BE_INDEMNITE_MANIFESTE", "NON");

        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("BELGIQUE", reponses);
        assertThat(result.scoreRisque()).isGreaterThan(0);
        assertThat(result.verdict()).isIn("RISQUE_MODERE", "RISQUE_ELEVE");
    }

    @Test
    void allInconnu_partialScore() {
        Map<String, String> reponses = new HashMap<>();
        // Empty map → all default to INCONNU
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses);

        assertThat(result.scoreRisque()).isGreaterThan(0);
        assertThat(result.scoreRisque()).isLessThan(100);
        assertThat(result.criteres()).allMatch(c -> "INCONNU".equals(c.reponse()));
    }

    @Test
    void invalidCountry_throws() {
        assertThatThrownBy(() -> LicenciementAnalyzer.analyze("ALLEMAGNE", Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void criteresReturnedMatchCountry() {
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", Map.of());
        assertThat(result.country()).isEqualTo("FRANCE");
        assertThat(result.criteres()).hasSizeGreaterThanOrEqualTo(6);

        LicenciementAnalysisResult resultBe = LicenciementAnalyzer.analyze("BELGIQUE", Map.of());
        assertThat(resultBe.country()).isEqualTo("BELGIQUE");
        assertThat(resultBe.criteres()).hasSizeGreaterThanOrEqualTo(6);
    }

    @Test
    void commentaire_containsBaseJuridique() {
        Map<String, String> reponses = Map.of("FR_MOTIVATION", "OUI");
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses);

        assertThat(result.criteres()).anyMatch(c ->
                "FR_MOTIVATION".equals(c.code()) && c.commentaire().contains("Code du travail"));
    }
}
