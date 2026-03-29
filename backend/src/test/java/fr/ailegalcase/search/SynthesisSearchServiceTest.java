package fr.ailegalcase.search;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.casefile.CaseFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SynthesisSearchServiceTest {

    private final SynthesisSearchService service =
            new SynthesisSearchService(mock(SynthesisSearchRepository.class),
                    mock(fr.ailegalcase.shared.CurrentUserResolver.class),
                    mock(fr.ailegalcase.workspace.WorkspaceMemberRepository.class));

    private CaseAnalysis buildAnalysis(String resultJson) {
        CaseFile cf = new CaseFile();
        cf.setTitle("Dossier Test");
        cf.setLegalDomain("DROIT_DU_TRAVAIL");

        CaseAnalysis ca = new CaseAnalysis();
        ca.setCaseFile(cf);
        ca.setAnalysisType(AnalysisType.STANDARD);
        ca.setAnalysisStatus(AnalysisStatus.DONE);
        ca.setVersion(1);
        ca.setAnalysisResult(resultJson);
        return ca;
    }

    // U-01
    @Test
    void extractExcerpts_termInFaits_returnsExcerpt() {
        String json = """
                {"faits":["Le salarié a été licencié le 15 janvier."],"points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        List<String> excerpts = service.extractExcerpts(json, "licencié");
        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.get(0)).contains("licencié");
    }

    // U-02
    @Test
    void extractExcerpts_caseInsensitive_matches() {
        String json = """
                {"faits":["Risque de LICENCIEMENT abusif."],"points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        List<String> excerpts = service.extractExcerpts(json, "licenciement");
        assertThat(excerpts).hasSize(1);
    }

    // U-03
    @Test
    void extractExcerpts_maxThreeExcerpts() {
        String json = """
                {"faits":["licenciement 1","licenciement 2","licenciement 3","licenciement 4"],
                 "points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        List<String> excerpts = service.extractExcerpts(json, "licenciement");
        assertThat(excerpts).hasSize(3);
    }

    // U-04
    @Test
    void extractExcerpts_noMatch_returnsEmpty() {
        String json = """
                {"faits":["Aucun rapport avec le terme."],"points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        List<String> excerpts = service.extractExcerpts(json, "licenciement");
        assertThat(excerpts).isEmpty();
    }

    // U-05
    @Test
    void extractExcerpts_allFields_searchesInAll() {
        String json = """
                {"faits":[],"points_juridiques":["Article relatif au licenciement."],"risques":[],"questions_ouvertes":[]}
                """;
        List<String> excerpts = service.extractExcerpts(json, "licenciement");
        assertThat(excerpts).hasSize(1);
        assertThat(excerpts.get(0)).contains("licenciement");
    }

    // U-06
    @Test
    void extractExcerpts_nullResult_returnsEmpty() {
        List<String> excerpts = service.extractExcerpts(null, "licenciement");
        assertThat(excerpts).isEmpty();
    }

    // U-07
    @Test
    void extractExcerpts_malformedJson_returnsEmpty() {
        List<String> excerpts = service.extractExcerpts("{invalid json", "licenciement");
        assertThat(excerpts).isEmpty();
    }

    // U-08
    @Test
    void toResult_buildsSynthesisSearchResult() {
        String json = """
                {"faits":["Motif de licenciement contesté."],"points_juridiques":[],"risques":[],"questions_ouvertes":[]}
                """;
        CaseAnalysis ca = buildAnalysis(json);
        SynthesisSearchResult result = service.toResult(ca, "licenciement");

        assertThat(result.caseFileTitle()).isEqualTo("Dossier Test");
        assertThat(result.legalDomain()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(result.analysisType()).isEqualTo("STANDARD");
        assertThat(result.excerpts()).hasSize(1);
        assertThat(result.matchCount()).isEqualTo(1);
    }
}
