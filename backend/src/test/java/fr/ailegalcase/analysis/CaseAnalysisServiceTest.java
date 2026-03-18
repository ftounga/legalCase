package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CaseAnalysisServiceTest {

    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);

    private final CaseAnalysisService service = new CaseAnalysisService(
            documentAnalysisRepository, caseAnalysisRepository, caseFileRepository,
            anthropicService, analysisJobRepository);

    // U-01 : analyses de documents valides → CaseAnalysis DONE + job DONE
    @Test
    void consumeCaseAnalysis_validDocumentAnalyses_persistsDoneAnalysisAndJob() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        DocumentAnalysis da0 = documentAnalysis("{\"faits\":[\"fait1\"]}", Instant.now().minusSeconds(10));
        DocumentAnalysis da1 = documentAnalysis("{\"faits\":[\"fait2\"]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da0, da1));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{\"faits\":[\"synthese\"]}", "claude-sonnet-4-6", 300, 150));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[\"synthese\"]}");
        assertThat(captor.getValue().getModelUsed()).isEqualTo("claude-sonnet-4-6");
        assertThat(captor.getValue().getPromptTokens()).isEqualTo(300);
        assertThat(captor.getValue().getCompletionTokens()).isEqualTo(150);

        // Job mis à jour : DONE, processedItems=1
        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(2)).save(jobCaptor.capture()); // PROCESSING + DONE
        AnalysisJob finalJob = jobCaptor.getValue();
        assertThat(finalJob.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(finalJob.getProcessedItems()).isEqualTo(1);
    }

    // U-02 : erreur Anthropic → CaseAnalysis FAILED + job FAILED
    @Test
    void consumeCaseAnalysis_anthropicError_persistsFailedAnalysisAndJob() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenThrow(new RuntimeException("API error"));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(2)).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(jobCaptor.getValue().getErrorMessage()).isNotNull();
    }

    // U-03 : aucune document_analysis DONE → aucune CaseAnalysis créée
    @Test
    void consumeCaseAnalysis_noDocumentAnalyses_noCaseAnalysisCreated() {
        UUID caseFileId = UUID.randomUUID();
        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of());

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        verifyNoInteractions(caseAnalysisRepository, anthropicService, caseFileRepository, analysisJobRepository);
    }

    // U-04 : le prompt agrège les documents dans l'ordre chronologique
    @Test
    void consumeCaseAnalysis_promptContainsDocumentsInChronologicalOrder() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        Instant earlier = Instant.now().minusSeconds(60);
        Instant later = Instant.now();

        DocumentAnalysis daLater = documentAnalysis("{\"faits\":[\"B\"]}", later);
        DocumentAnalysis daEarlier = documentAnalysis("{\"faits\":[\"A\"]}", earlier);

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(daLater, daEarlier)); // ordre inversé intentionnel
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeChunk(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("Document 0")).isLessThan(prompt.indexOf("Document 1"));
        assertThat(prompt).contains("{\"faits\":[\"A\"]}");
        assertThat(prompt.indexOf("{\"faits\":[\"A\"]}")).isLessThan(prompt.indexOf("{\"faits\":[\"B\"]}"));
    }

    // Helper
    private DocumentAnalysis documentAnalysis(String result, Instant createdAt) {
        DocumentAnalysis da = new DocumentAnalysis();
        da.setDocument(new Document());
        da.setAnalysisResult(result);
        da.setAnalysisStatus(AnalysisStatus.DONE);
        da.setCreatedAt(createdAt);
        return da;
    }
}
