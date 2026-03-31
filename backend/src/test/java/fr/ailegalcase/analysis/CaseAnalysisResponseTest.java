package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class CaseAnalysisResponseTest {

    // U-01 : parsing nominal — tous les champs présents
    @Test
    void from_nominalJson_parsesAllFields() {
        CaseAnalysis analysis = analysis("""
                {
                  "timeline": [{"date": "2024-01-15", "evenement": "Embauche"}],
                  "faits": ["fait1", "fait2"],
                  "points_juridiques": ["point1"],
                  "risques": ["risque1"],
                  "questions_ouvertes": ["question1"]
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.status()).isEqualTo("DONE");
        assertThat(response.timeline()).hasSize(1);
        assertThat(response.timeline().get(0).date()).isEqualTo("2024-01-15");
        assertThat(response.timeline().get(0).evenement()).isEqualTo("Embauche");
        assertThat(response.faits()).containsExactly("fait1", "fait2");
        assertThat(response.pointsJuridiques()).containsExactly("point1");
        assertThat(response.risques()).containsExactly("risque1");
        assertThat(response.questionsOuvertes()).containsExactly("question1");
        assertThat(response.modelUsed()).isEqualTo("claude-sonnet-4-6");
    }

    // U-02 : champ manquant dans le JSON → liste vide
    @Test
    void from_missingField_returnsEmptyList() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait1"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).containsExactly("fait1");
        assertThat(response.timeline()).isEmpty();
        assertThat(response.pointsJuridiques()).isEmpty();
        assertThat(response.risques()).isEmpty();
        assertThat(response.questionsOuvertes()).isEmpty();
    }

    // U-03 : analysis_result null → toutes les listes vides
    @Test
    void from_nullAnalysisResult_returnsEmptyLists() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.timeline()).isEmpty();
        assertThat(response.faits()).isEmpty();
        assertThat(response.pointsJuridiques()).isEmpty();
        assertThat(response.risques()).isEmpty();
        assertThat(response.questionsOuvertes()).isEmpty();
    }

    // U-04 : JSON malformé → toutes les listes vides (pas d'exception)
    @Test
    void from_malformedJson_returnsEmptyListsWithoutException() {
        CaseAnalysis analysis = analysis("not valid json {{{");

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.timeline()).isEmpty();
        assertThat(response.faits()).isEmpty();
    }

    // U-05 : populateCounts — JSON nominal → 5 compteurs corrects
    @Test
    void populateCounts_nominalJson_setsAllCounts() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["f1", "f2", "f3"],
                  "points_juridiques": ["p1", "p2"],
                  "risques": ["r1"],
                  "questions_ouvertes": ["q1", "q2", "q3", "q4"],
                  "timeline": [{"date":"2024-01-01","evenement":"e1"},{"date":"2024-02-01","evenement":"e2"}]
                }
                """);

        CaseAnalysisResponse.populateCounts(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getFaitsCount()).isEqualTo(3);
        assertThat(analysis.getPointsJuridiquesCount()).isEqualTo(2);
        assertThat(analysis.getRisquesCount()).isEqualTo(1);
        assertThat(analysis.getQuestionsOuvertesCount()).isEqualTo(4);
        assertThat(analysis.getTimelineCount()).isEqualTo(2);
    }

    // U-06 : populateCounts — JSON malformé → compteurs restent null (fail-open)
    @Test
    void populateCounts_malformedJson_countsRemainNull() {
        CaseAnalysis analysis = analysis("not valid json");

        CaseAnalysisResponse.populateCounts(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getFaitsCount()).isNull();
        assertThat(analysis.getPointsJuridiquesCount()).isNull();
        assertThat(analysis.getRisquesCount()).isNull();
    }

    // U-07 : populateCounts — null → aucune exception, compteurs restent null
    @Test
    void populateCounts_nullResult_countsRemainNull() {
        CaseAnalysis analysis = analysis(null);

        CaseAnalysisResponse.populateCounts(analysis, null);

        assertThat(analysis.getFaitsCount()).isNull();
    }

    private CaseAnalysis analysis(String result) {
        CaseAnalysis a = new CaseAnalysis();
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setVersion(1);
        a.setAnalysisResult(result);
        a.setModelUsed("claude-sonnet-4-6");
        a.setUpdatedAt(Instant.now());
        return a;
    }
}
