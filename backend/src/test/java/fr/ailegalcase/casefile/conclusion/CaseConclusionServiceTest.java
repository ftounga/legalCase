package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.StrategicOptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * F-98 / SF-98-01 — tests unitaires du worker de génération : succès → {@code DONE},
 * exception de l'appel IA → {@code FAILED}.
 */
class CaseConclusionServiceTest {

    private final CaseConclusionRepository caseConclusionRepository = mock(CaseConclusionRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final StrategicOptionRepository strategicOptionRepository = mock(StrategicOptionRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentPieceRepository documentPieceRepository = mock(DocumentPieceRepository.class);
    private final CaseFileDashboardService caseFileDashboardService = mock(CaseFileDashboardService.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final CaseConclusionPromptBuilder promptBuilder =
            new CaseConclusionPromptBuilder(new ObjectMapper());

    private final CaseConclusionService service = new CaseConclusionService(
            caseConclusionRepository, caseAnalysisRepository, strategicOptionRepository,
            documentRepository, documentPieceRepository, caseFileDashboardService,
            promptBuilder, anthropicService);

    @BeforeEach
    void wireSelf() {
        // En prod, `self` est le proxy transactionnel ; en test, on l'auto-référence.
        ReflectionTestUtils.setField(service, "self", service);
    }

    @Test
    void generate_success_marksDoneWithContentAndTokens() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        when(caseConclusionRepository.findById(conclusionId)).thenReturn(Optional.of(conclusion));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                any(), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte généré", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        assertThat(conclusion.getContent()).contains("PAR CES MOTIFS");
        assertThat(conclusion.getModelUsed()).isEqualTo("claude-sonnet-4-6");
        assertThat(conclusion.getPromptTokens()).isEqualTo(1200);
        assertThat(conclusion.getCompletionTokens()).isEqualTo(3400);
        assertThat(conclusion.getGeneratedAt()).isNotNull();
        assertThat(conclusion.getErrorMessage()).isNull();
    }

    @Test
    void generate_aiException_marksFailedWithErrorMessage() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        when(caseConclusionRepository.findById(conclusionId)).thenReturn(Optional.of(conclusion));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                any(), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic 529 overloaded"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.FAILED);
        assertThat(conclusion.getErrorMessage()).contains("529 overloaded");
        assertThat(conclusion.getContent()).isNull();
    }

    @Test
    void generate_emptyAiResponse_marksFailed() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        when(caseConclusionRepository.findById(conclusionId)).thenReturn(Optional.of(conclusion));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                any(), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("  ", "claude-sonnet-4-6", 10, 0, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.FAILED);
        assertThat(conclusion.getErrorMessage()).isNotBlank();
    }

    @Test
    void generate_nonPendingRow_isIgnored() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        conclusion.setStatus(CaseConclusionStatus.DONE); // déjà traité (double message)
        when(caseConclusionRepository.findById(conclusionId)).thenReturn(Optional.of(conclusion));

        service.generate(conclusionId);

        verify(anthropicService, never()).analyzeWithSystemCache(any(), any(), anyInt());
    }

    private CaseConclusion pendingConclusion(UUID id) {
        Workspace workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setCountry("FRANCE");

        CaseFile caseFile = new CaseFile();
        caseFile.setId(UUID.randomUUID());
        caseFile.setTitle("Dossier Dupont c/ SARL Martin");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");

        CaseConclusion conclusion = new CaseConclusion();
        conclusion.setId(id);
        conclusion.setCaseFile(caseFile);
        conclusion.setWorkspace(workspace);
        conclusion.setStatus(CaseConclusionStatus.PENDING);
        conclusion.setJurisdictionCode("CPH");
        conclusion.setStageCode("FOND");
        conclusion.setPositionCode("DEMANDEUR");
        return conclusion;
    }
}
