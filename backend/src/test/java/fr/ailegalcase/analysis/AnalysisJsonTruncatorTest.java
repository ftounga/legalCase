package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisJsonTruncatorTest {

    // --- truncateDocumentAnalysis ---

    // U-01 : faits > 5 → tronqué à 5
    @Test
    void truncateDocumentAnalysis_faitsExceedLimit_truncatedTo5() throws Exception {
        String json = """
                {"faits":["f1","f2","f3","f4","f5","f6","f7"],
                 "points_juridiques":["p1"],
                 "risques":["r1"],
                 "questions_ouvertes":["q1"]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).contains("f1").contains("f2").contains("f3").contains("f4").contains("f5");
        assertThat(result).doesNotContain("f6").doesNotContain("f7");
    }

    // U-02 : faits < 5 → tous conservés
    @Test
    void truncateDocumentAnalysis_faitsUnderLimit_allConserved() throws Exception {
        String json = """
                {"faits":["f1","f2"],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).contains("f1").contains("f2");
    }

    // U-03 : points_juridiques > 3 → tronqué à 3
    @Test
    void truncateDocumentAnalysis_pointsJuridiquesExceedLimit_truncatedTo3() throws Exception {
        String json = """
                {"faits":[],
                 "points_juridiques":["p1","p2","p3","p4","p5"],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).contains("p1").contains("p2").contains("p3");
        assertThat(result).doesNotContain("p4").doesNotContain("p5");
    }

    // U-04 : risques > 3 → tronqué à 3
    @Test
    void truncateDocumentAnalysis_risquesExceedLimit_truncatedTo3() throws Exception {
        String json = """
                {"faits":[],
                 "points_juridiques":[],
                 "risques":["r1","r2","r3","r4"],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).contains("r1").contains("r2").contains("r3");
        assertThat(result).doesNotContain("r4");
    }

    // U-05 : questions_ouvertes > 3 → tronqué à 3
    @Test
    void truncateDocumentAnalysis_questionsOuvertesExceedLimit_truncatedTo3() throws Exception {
        String json = """
                {"faits":[],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":["q1","q2","q3","q4"]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).contains("q1").contains("q2").contains("q3");
        assertThat(result).doesNotContain("q4");
    }

    // U-06 : timeline présente dans un document analysis → non tronquée (pas de champ timeline au niveau document)
    @Test
    void truncateDocumentAnalysis_noTimelineField_unaffected() throws Exception {
        String json = """
                {"faits":["f1"],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).doesNotContain("timeline");
    }

    // --- truncateCaseAnalysis ---

    // U-07 : faits > 7 → tronqué à 7
    @Test
    void truncateCaseAnalysis_faitsExceedLimit_truncatedTo7() throws Exception {
        String json = """
                {"timeline":[],
                 "faits":["f1","f2","f3","f4","f5","f6","f7","f8","f9"],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateCaseAnalysis(json);
        assertThat(result).contains("f7");
        assertThat(result).doesNotContain("f8").doesNotContain("f9");
    }

    // U-08 : timeline > 5 → tronqué à 5
    @Test
    void truncateCaseAnalysis_timelineExceedsLimit_truncatedTo5() throws Exception {
        String json = """
                {"timeline":[
                    {"date":"2024-01-01","evenement":"e1"},
                    {"date":"2024-02-01","evenement":"e2"},
                    {"date":"2024-03-01","evenement":"e3"},
                    {"date":"2024-04-01","evenement":"e4"},
                    {"date":"2024-05-01","evenement":"e5"},
                    {"date":"2024-06-01","evenement":"e6"}
                 ],
                 "faits":[],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateCaseAnalysis(json);
        assertThat(result).contains("e5");
        assertThat(result).doesNotContain("e6");
    }

    // U-09 : JSON invalide (tronqué max_tokens) → retourné tel quel sans exception
    @Test
    void truncate_invalidJson_returnedAsIs() {
        String broken = "{\"faits\":[\"f1\",\"f2\"";
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(broken);
        assertThat(result).isEqualTo(broken);
    }

    // U-10 : JSON null → retourné null
    @Test
    void truncate_null_returnsNull() {
        assertThat(AnalysisJsonTruncator.truncateDocumentAnalysis(null)).isNull();
    }

    // U-11 : JSON avec markdown code block → strippé puis tronqué
    @Test
    void truncate_withMarkdownCodeBlock_strippedAndTruncated() {
        String json = "```json\n{\"faits\":[\"f1\",\"f2\",\"f3\",\"f4\",\"f5\",\"f6\"]," +
                "\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}\n```";
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json);
        assertThat(result).doesNotContain("```");
        assertThat(result).doesNotContain("f6");
        assertThat(result).contains("f5");
    }

    private int countOccurrences(String text, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
