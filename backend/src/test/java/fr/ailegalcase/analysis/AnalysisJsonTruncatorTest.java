package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisJsonTruncatorTest {

    // ─── helpers ────────────────────────────────────────────────────────────

    private static AnalysisLimitsProperties.LevelLimits docLimits(int faits, int pj, int risques, int qo) {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(faits); l.setPointsJuridiques(pj); l.setRisques(risques); l.setQuestionsOuvertes(qo);
        return l;
    }

    private static AnalysisLimitsProperties.LevelLimits dossierLimits(int faits, int pj, int risques, int qo, int timeline) {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(faits); l.setPointsJuridiques(pj); l.setRisques(risques);
        l.setQuestionsOuvertes(qo); l.setTimeline(timeline);
        return l;
    }

    private static final AnalysisLimitsProperties.LevelLimits DEFAULT_DOC         = docLimits(5, 3, 3, 3);
    private static final AnalysisLimitsProperties.LevelLimits DEFAULT_DOSSIER      = dossierLimits(7, 5, 5, 5, 5);
    private static final AnalysisLimitsProperties.LevelLimits IMMIGRATION_DOC      = docLimits(7, 4, 4, 3);
    private static final AnalysisLimitsProperties.LevelLimits IMMIGRATION_DOSSIER  = dossierLimits(10, 6, 6, 5, 8);

    // --- truncateDocumentAnalysis ---

    // U-01 : faits > 5 → tronqué à 5
    @Test
    void truncateDocumentAnalysis_faitsExceedLimit_truncatedTo5() {
        String json = """
                {"faits":["f1","f2","f3","f4","f5","f6","f7"],
                 "points_juridiques":["p1"],
                 "risques":["r1"],
                 "questions_ouvertes":["q1"]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).contains("f5").doesNotContain("f6").doesNotContain("f7");
    }

    // U-02 : faits < 5 → tous conservés
    @Test
    void truncateDocumentAnalysis_faitsUnderLimit_allConserved() {
        String json = """
                {"faits":["f1","f2"],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).contains("f1").contains("f2");
    }

    // U-03 : points_juridiques > 3 → tronqué à 3
    @Test
    void truncateDocumentAnalysis_pointsJuridiquesExceedLimit_truncatedTo3() {
        String json = """
                {"faits":[],
                 "points_juridiques":["p1","p2","p3","p4","p5"],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).contains("p3").doesNotContain("p4").doesNotContain("p5");
    }

    // U-04 : risques > 3 → tronqué à 3
    @Test
    void truncateDocumentAnalysis_risquesExceedLimit_truncatedTo3() {
        String json = """
                {"faits":[],
                 "points_juridiques":[],
                 "risques":["r1","r2","r3","r4"],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).contains("r3").doesNotContain("r4");
    }

    // U-05 : questions_ouvertes > 3 → tronqué à 3
    @Test
    void truncateDocumentAnalysis_questionsOuvertesExceedLimit_truncatedTo3() {
        String json = """
                {"faits":[],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":["q1","q2","q3","q4"]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).contains("q3").doesNotContain("q4");
    }

    // U-06 : pas de champ timeline au niveau document → non affecté
    @Test
    void truncateDocumentAnalysis_noTimelineField_unaffected() {
        String json = """
                {"faits":["f1"],
                 "points_juridiques":[],
                 "risques":[],
                 "questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).doesNotContain("timeline");
    }

    // U-12 : immigration doc — faits limité à 7
    @Test
    void truncateDocumentAnalysis_immigration_faitsLimitIs7() {
        String json = """
                {"faits":["f1","f2","f3","f4","f5","f6","f7","f8"],
                 "points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, IMMIGRATION_DOC);
        assertThat(result).contains("f7").doesNotContain("f8");
    }

    // --- truncateCaseAnalysis ---

    // U-07 : faits > 7 → tronqué à 7
    @Test
    void truncateCaseAnalysis_faitsExceedLimit_truncatedTo7() {
        String json = """
                {"timeline":[],
                 "faits":["f1","f2","f3","f4","f5","f6","f7","f8","f9"],
                 "points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateCaseAnalysis(json, DEFAULT_DOSSIER);
        assertThat(result).contains("f7").doesNotContain("f8").doesNotContain("f9");
    }

    // U-08 : timeline > 5 → tronqué à 5
    @Test
    void truncateCaseAnalysis_timelineExceedsLimit_truncatedTo5() {
        String json = """
                {"timeline":[
                    {"date":"2024-01-01","evenement":"e1"},{"date":"2024-02-01","evenement":"e2"},
                    {"date":"2024-03-01","evenement":"e3"},{"date":"2024-04-01","evenement":"e4"},
                    {"date":"2024-05-01","evenement":"e5"},{"date":"2024-06-01","evenement":"e6"}
                 ],
                 "faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateCaseAnalysis(json, DEFAULT_DOSSIER);
        assertThat(result).contains("e5").doesNotContain("e6");
    }

    // U-13 : immigration dossier — timeline limité à 8
    @Test
    void truncateCaseAnalysis_immigration_timelineLimitIs8() {
        String json = """
                {"timeline":[
                    {"date":"2024-01-01","evenement":"e1"},{"date":"2024-02-01","evenement":"e2"},
                    {"date":"2024-03-01","evenement":"e3"},{"date":"2024-04-01","evenement":"e4"},
                    {"date":"2024-05-01","evenement":"e5"},{"date":"2024-06-01","evenement":"e6"},
                    {"date":"2024-07-01","evenement":"e7"},{"date":"2024-08-01","evenement":"e8"},
                    {"date":"2024-09-01","evenement":"e9"}
                 ],
                 "faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateCaseAnalysis(json, IMMIGRATION_DOSSIER);
        assertThat(result).contains("e8").doesNotContain("e9");
    }

    // U-14 : immigration dossier — faits limité à 10
    @Test
    void truncateCaseAnalysis_immigration_faitsLimitIs10() {
        String json = """
                {"timeline":[],
                 "faits":["f1","f2","f3","f4","f5","f6","f7","f8","f9","f10","f11"],
                 "points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        String result = AnalysisJsonTruncator.truncateCaseAnalysis(json, IMMIGRATION_DOSSIER);
        assertThat(result).contains("f10").doesNotContain("f11");
    }

    // U-09 : JSON invalide → retourné tel quel sans exception
    @Test
    void truncate_invalidJson_returnedAsIs() {
        String broken = "{\"faits\":[\"f1\",\"f2\"";
        assertThat(AnalysisJsonTruncator.truncateDocumentAnalysis(broken, DEFAULT_DOC)).isEqualTo(broken);
    }

    // U-10 : JSON null → retourné null
    @Test
    void truncate_null_returnsNull() {
        assertThat(AnalysisJsonTruncator.truncateDocumentAnalysis(null, DEFAULT_DOC)).isNull();
    }

    // U-11 : markdown code block → strippé puis tronqué
    @Test
    void truncate_withMarkdownCodeBlock_strippedAndTruncated() {
        String json = "```json\n{\"faits\":[\"f1\",\"f2\",\"f3\",\"f4\",\"f5\",\"f6\"]," +
                "\"points_juridiques\":[],\"risques\":[],\"questions_ouvertes\":[]}\n```";
        String result = AnalysisJsonTruncator.truncateDocumentAnalysis(json, DEFAULT_DOC);
        assertThat(result).doesNotContain("```").doesNotContain("f6").contains("f5");
    }
}
