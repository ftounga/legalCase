package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LicenciementAnalyzerTest {

    @Test
    void allOui_france_scoreZero_valide() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : TestLicenciementCriteres.FRANCE) {
            reponses.put(c.code(), "OUI");
        }
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses, TestLicenciementCriteres.FRANCE);

        assertThat(result.scoreRisque()).isZero();
        assertThat(result.verdict()).isEqualTo("VALIDE");
        assertThat(result.criteres()).allMatch(c -> c.pointsRisque() == 0);
    }

    @Test
    void allNon_france_invalide() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : TestLicenciementCriteres.FRANCE) {
            reponses.put(c.code(), "NON");
        }
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses, TestLicenciementCriteres.FRANCE);

        assertThat(result.scoreRisque()).isEqualTo(100);
        assertThat(result.verdict()).isEqualTo("INVALIDE");
    }

    @Test
    void bloquantNon_france_invalideRegardlessOfScore() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : TestLicenciementCriteres.FRANCE) {
            reponses.put(c.code(), "OUI");
        }
        reponses.put("FR_MOTIVATION", "NON");
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses, TestLicenciementCriteres.FRANCE);
        assertThat(result.verdict()).isEqualTo("INVALIDE");
    }

    @Test
    void allOui_belgique_valide() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : TestLicenciementCriteres.BELGIQUE) {
            reponses.put(c.code(), "OUI");
        }
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("BELGIQUE", reponses, TestLicenciementCriteres.BELGIQUE);

        assertThat(result.scoreRisque()).isZero();
        assertThat(result.verdict()).isEqualTo("VALIDE");
    }

    @Test
    void partialNon_belgique_risqueModereOuEleve() {
        Map<String, String> reponses = new HashMap<>();
        for (LicenciementCritere c : TestLicenciementCriteres.BELGIQUE) {
            reponses.put(c.code(), "OUI");
        }
        reponses.put("BE_FORMALITES", "NON");
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("BELGIQUE", reponses, TestLicenciementCriteres.BELGIQUE);
        assertThat(result.scoreRisque()).isGreaterThan(0);
        assertThat(result.verdict()).isIn("RISQUE_MODERE", "RISQUE_ELEVE", "VALIDE");
    }

    @Test
    void allInconnu_partialScore() {
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", Map.of(), TestLicenciementCriteres.FRANCE);

        assertThat(result.scoreRisque()).isGreaterThan(0);
        assertThat(result.scoreRisque()).isLessThan(100);
        assertThat(result.criteres()).allMatch(c -> "INCONNU".equals(c.reponse()));
    }

    @Test
    void isCountryValid_frOuBe() {
        assertThat(LicenciementAnalyzer.isCountryValid("FRANCE")).isTrue();
        assertThat(LicenciementAnalyzer.isCountryValid("BELGIQUE")).isTrue();
        assertThat(LicenciementAnalyzer.isCountryValid("ALLEMAGNE")).isFalse();
        assertThat(LicenciementAnalyzer.isCountryValid(null)).isFalse();
    }

    @Test
    void criteresReturnedMatchCountry() {
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", Map.of(), TestLicenciementCriteres.FRANCE);
        assertThat(result.country()).isEqualTo("FRANCE");
        assertThat(result.criteres()).hasSize(TestLicenciementCriteres.FRANCE.size());

        LicenciementAnalysisResult resultBe = LicenciementAnalyzer.analyze("BELGIQUE", Map.of(), TestLicenciementCriteres.BELGIQUE);
        assertThat(resultBe.country()).isEqualTo("BELGIQUE");
        assertThat(resultBe.criteres()).hasSize(TestLicenciementCriteres.BELGIQUE.size());
    }

    @Test
    void commentaire_containsBaseJuridique() {
        Map<String, String> reponses = Map.of("FR_MOTIVATION", "OUI");
        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze("FRANCE", reponses, TestLicenciementCriteres.FRANCE);

        assertThat(result.criteres()).anyMatch(c ->
                "FR_MOTIVATION".equals(c.code()) && c.commentaire().contains("L. 1232"));
    }
}
