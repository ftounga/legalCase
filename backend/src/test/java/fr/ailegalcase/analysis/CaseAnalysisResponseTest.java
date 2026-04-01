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
        assertThat(response.faits()).extracting(AnalysisItem::texte).containsExactly("fait1", "fait2");
        assertThat(response.pointsJuridiques()).extracting(AnalysisItem::texte).containsExactly("point1");
        assertThat(response.risques()).extracting(AnalysisItem::texte).containsExactly("risque1");
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

        assertThat(response.faits()).extracting(AnalysisItem::texte).containsExactly("fait1");
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

    // U-08 : piecesManquantes — JSON avec liste → retourne la liste
    @Test
    void from_withPiecesManquantes_returnsList() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["fait1"],
                  "pieces_manquantes": ["Contrat de travail", "Bulletins de salaire"]
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantes()).containsExactly("Contrat de travail", "Bulletins de salaire");
    }

    // U-09 : piecesManquantes — champ absent du JSON → liste vide (fail-open)
    @Test
    void from_missingPiecesManquantes_returnsEmptyList() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait1"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantes()).isEmpty();
    }

    // U-10 : piecesManquantes — champ vide → liste vide
    @Test
    void from_emptyPiecesManquantes_returnsEmptyList() {
        CaseAnalysis analysis = analysis("""
                {
                  "faits": ["fait1"],
                  "pieces_manquantes": []
                }
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.piecesManquantes()).isEmpty();
    }

    // TC-01 : item string → AnalysisItem(texte, null, null) — rétrocompatibilité
    @Test
    void extractItemList_stringItem_returnsItemWithNullSource() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["fait ancien"]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).hasSize(1);
        assertThat(response.faits().get(0).texte()).isEqualTo("fait ancien");
        assertThat(response.faits().get(0).source()).isNull();
        assertThat(response.faits().get(0).extrait()).isNull();
    }

    // TC-02 : item objet complet {texte, source, extrait} → AnalysisItem complet
    @Test
    void extractItemList_objectItem_returnsCompleteAnalysisItem() {
        CaseAnalysis analysis = analysis("""
                {"faits": [{"texte": "Licenciement sans motif", "source": "Document 0", "extrait": "Il est mis fin au contrat"}]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).hasSize(1);
        assertThat(response.faits().get(0).texte()).isEqualTo("Licenciement sans motif");
        assertThat(response.faits().get(0).source()).isEqualTo("Document 0");
        assertThat(response.faits().get(0).extrait()).isEqualTo("Il est mis fin au contrat");
    }

    // TC-03 : item objet sans source → source null (fail-open)
    @Test
    void extractItemList_objectItemWithoutSource_returnsNullSource() {
        CaseAnalysis analysis = analysis("""
                {"faits": [{"texte": "Fait sans source"}]}
                """);

        CaseAnalysisResponse response = CaseAnalysisResponse.from(analysis);

        assertThat(response.faits()).hasSize(1);
        assertThat(response.faits().get(0).texte()).isEqualTo("Fait sans source");
        assertThat(response.faits().get(0).source()).isNull();
        assertThat(response.faits().get(0).extrait()).isNull();
    }

    // U-11 : populateRiskScore — JSON nominal → riskLevel="MOYEN", riskScore=55
    @Test
    void populateRiskScore_nominalJson_setsLevelAndScore() {
        CaseAnalysis analysis = analysis("""
                {"score_risque": {"niveau": "MOYEN", "valeur": 55}}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isEqualTo("MOYEN");
        assertThat(analysis.getRiskScore()).isEqualTo(55);
    }

    // U-12 : populateRiskScore — champ absent → null, null (fail-open)
    @Test
    void populateRiskScore_missingField_remainsNull() {
        CaseAnalysis analysis = analysis("""
                {"faits": ["f1"]}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isNull();
        assertThat(analysis.getRiskScore()).isNull();
    }

    // U-13 : populateRiskScore — niveau inconnu → riskLevel null (fail-open)
    @Test
    void populateRiskScore_unknownNiveau_riskLevelNull() {
        CaseAnalysis analysis = analysis("""
                {"score_risque": {"niveau": "CRITIQUE", "valeur": 90}}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isNull();
        assertThat(analysis.getRiskScore()).isEqualTo(90);
    }

    // U-14 : populateRiskScore — valeur hors [0-100] → riskScore null (fail-open)
    @Test
    void populateRiskScore_valueOutOfRange_riskScoreNull() {
        CaseAnalysis analysis = analysis("""
                {"score_risque": {"niveau": "ELEVE", "valeur": 150}}
                """);

        CaseAnalysisResponse.populateRiskScore(analysis, analysis.getAnalysisResult());

        assertThat(analysis.getRiskLevel()).isEqualTo("ELEVE");
        assertThat(analysis.getRiskScore()).isNull();
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
