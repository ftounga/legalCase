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
