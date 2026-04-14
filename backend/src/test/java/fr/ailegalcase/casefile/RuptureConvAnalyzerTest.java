package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuptureConvAnalyzerTest {

    private static Map<String, String> allAnswers(String answer) {
        Map<String, String> m = new HashMap<>();
        m.put("RC_CONSENTEMENT", answer);
        m.put("RC_DELAI_RETRACTATION", answer);
        m.put("RC_HOMOLOGATION", answer);
        m.put("RC_ASSISTANCE", answer);
        m.put("RC_INDEMNITE", answer);
        m.put("RC_ENTRETIENS", answer);
        return m;
    }

    @Test
    void tousOui_scoreZeroEtValide() {
        RuptureConvAnalysisResult r = RuptureConvAnalyzer.analyze("FRANCE", allAnswers("OUI"));
        assertThat(r.scoreRisque()).isEqualTo(0);
        assertThat(r.verdict()).isEqualTo(RuptureConvAnalysisResult.VALIDE);
        assertThat(r.criteres()).hasSize(6);
    }

    @Test
    void tousNon_score100EtInvalide() {
        RuptureConvAnalysisResult r = RuptureConvAnalyzer.analyze("FRANCE", allAnswers("NON"));
        assertThat(r.scoreRisque()).isEqualTo(100);
        assertThat(r.verdict()).isEqualTo(RuptureConvAnalysisResult.INVALIDE);
    }

    @Test
    void tousInconnu_score50EtRisqueEleve() {
        // 6 critères, poids/2 chacun : 25/2 + 20/2 + 25/2 + 10/2 + 15/2 + 5/2 = 12+10+12+5+7+2 = 48
        RuptureConvAnalysisResult r = RuptureConvAnalyzer.analyze("FRANCE", allAnswers("INCONNU"));
        assertThat(r.scoreRisque()).isBetween(45, 55);
        assertThat(r.verdict()).isEqualTo(RuptureConvAnalysisResult.RISQUE_ELEVE);
    }

    @Test
    void unBloquantNon_verdictInvalideMemeSiScoreFaible() {
        // Seul RC_CONSENTEMENT (bloquant, poids 25) = NON, reste OUI → score 25, bloquant → INVALIDE
        Map<String, String> r = new HashMap<>(allAnswers("OUI"));
        r.put("RC_CONSENTEMENT", "NON");
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        assertThat(result.scoreRisque()).isEqualTo(25);
        assertThat(result.verdict()).isEqualTo(RuptureConvAnalysisResult.INVALIDE);
    }

    @Test
    void nonBloquantNon_scoreFaible_verdictValide() {
        // RC_ASSISTANCE (non-bloquant, poids 10) = NON, reste OUI → score 10, verdict VALIDE
        Map<String, String> r = new HashMap<>(allAnswers("OUI"));
        r.put("RC_ASSISTANCE", "NON");
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        assertThat(result.scoreRisque()).isEqualTo(10);
        assertThat(result.verdict()).isEqualTo(RuptureConvAnalysisResult.VALIDE);
    }

    @Test
    void scoreDansTrancheRisqueModere() {
        // 2 non-bloquants NON : RC_ASSISTANCE (10) + RC_ENTRETIENS (5) = 15
        Map<String, String> r = new HashMap<>(allAnswers("OUI"));
        r.put("RC_ASSISTANCE", "NON");
        r.put("RC_ENTRETIENS", "NON");
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        assertThat(result.scoreRisque()).isEqualTo(15);
        assertThat(result.verdict()).isEqualTo(RuptureConvAnalysisResult.RISQUE_MODERE);
    }

    @Test
    void paysBelgique_lanceException() {
        assertThatThrownBy(() -> RuptureConvAnalyzer.analyze("BELGIQUE", Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non supporté");
    }

    @Test
    void paysNull_lanceException() {
        assertThatThrownBy(() -> RuptureConvAnalyzer.analyze(null, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void paysLowerCase_accepteEtNormalise() {
        RuptureConvAnalysisResult r = RuptureConvAnalyzer.analyze("france", allAnswers("OUI"));
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @Test
    void reponsesNull_traiteCommeToutInconnu() {
        RuptureConvAnalysisResult r = RuptureConvAnalyzer.analyze("FRANCE", null);
        assertThat(r.scoreRisque()).isBetween(45, 55);
        assertThat(r.verdict()).isEqualTo(RuptureConvAnalysisResult.RISQUE_ELEVE);
        assertThat(r.criteres()).allMatch(c -> "INCONNU".equals(c.reponse()));
    }

    @Test
    void reponseNonReconnue_traiteCommeInconnu() {
        Map<String, String> r = new HashMap<>(allAnswers("OUI"));
        r.put("RC_CONSENTEMENT", "peut-être");
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        assertThat(result.criteres().stream()
                .filter(c -> "RC_CONSENTEMENT".equals(c.code()))
                .findFirst().orElseThrow().reponse()).isEqualTo("INCONNU");
    }

    @Test
    void critereInconnu_dansReponses_ignoreSansErreur() {
        Map<String, String> r = new HashMap<>(allAnswers("OUI"));
        r.put("RC_FANTAISIE", "OUI");
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        assertThat(result.criteres()).hasSize(6);
        assertThat(result.scoreRisque()).isEqualTo(0);
    }

    @Test
    void evaluationsPortentCommentairesAvecBaseJuridique() {
        Map<String, String> r = new HashMap<>(allAnswers("OUI"));
        r.put("RC_CONSENTEMENT", "NON");
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        RuptureConvAnalysisResult.CritereEvaluation ev = result.criteres().stream()
                .filter(c -> "RC_CONSENTEMENT".equals(c.code()))
                .findFirst().orElseThrow();
        assertThat(ev.commentaire()).contains("NON CONFORME (bloquant)");
        assertThat(ev.commentaire()).contains("L1237-11");
    }

    @Test
    void reponseOuiLowerCase_estNormalisee() {
        Map<String, String> r = new HashMap<>(allAnswers("oui"));
        RuptureConvAnalysisResult result = RuptureConvAnalyzer.analyze("FRANCE", r);
        assertThat(result.verdict()).isEqualTo(RuptureConvAnalysisResult.VALIDE);
    }
}
