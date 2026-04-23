package fr.ailegalcase.chat;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.UsageEventService;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentExtractionRepository documentExtractionRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private AnthropicService anthropicService;
    @Mock private UsageEventService usageEventService;
    @Mock private PlanLimitService planLimitService;
    @Mock private fr.ailegalcase.analysis.PiecesPromptContext piecesPromptContext;

    private ChatService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID USER_ID      = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @BeforeEach
    void setUp() {
        service = new ChatService(chatMessageRepository, caseFileRepository, caseAnalysisRepository,
                documentRepository, documentExtractionRepository,
                workspaceMemberRepository, currentUserResolver, anthropicService,
                usageEventService, planLimitService, piecesPromptContext);
        // Default : no pieces context injected (tests legacy restent valides).
        org.mockito.Mockito.lenient().when(piecesPromptContext.buildContextForCaseFile(org.mockito.ArgumentMatchers.any()))
                .thenReturn("");
    }

    private void mockContext(boolean budgetExceeded, boolean hasSynthesis) {
        User user = new User();
        user.setId(USER_ID);

        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        CaseFile caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        when(planLimitService.isChatMessageLimitReached(WORKSPACE_ID)).thenReturn(budgetExceeded);

        if (!budgetExceeded && hasSynthesis) {
            CaseAnalysis analysis = new CaseAnalysis();
            analysis.setAnalysisResult("{\"synthese\":\"test\"}");
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                    CASE_FILE_ID, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
            // SF-35-03 : par défaut aucun document (les tests dédiés SF-35-03 overrideront)
            when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_FILE_ID))
                    .thenReturn(java.util.List.of());
        } else if (!budgetExceeded) {
            when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                    CASE_FILE_ID, AnalysisStatus.DONE)).thenReturn(Optional.empty());
        }
    }

    // U-01 : limite non atteinte, synthèse présente → message créé, réponse retournée
    @Test
    void sendMessage_success_returnsResponse() {
        mockContext(false, true);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Réponse IA", "claude-haiku-4-5-20251001", 100, 50));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question test", false), null, "GOOGLE", null);

        assertThat(response.answer()).isEqualTo("Réponse IA");
        assertThat(response.modelUsed()).isEqualTo("claude-haiku-4-5-20251001");
        verify(usageEventService).record(eq(CASE_FILE_ID), eq(USER_ID), any(), eq(100), eq(50));
    }

    // U-02 : limite atteinte → 402
    @Test
    void sendMessage_limitReached_throws402() {
        mockContext(true, false);

        assertThatThrownBy(() -> service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", false), null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(PAYMENT_REQUIRED));

        verifyNoInteractions(anthropicService);
    }

    // U-03 : pas de synthèse → 424
    @Test
    void sendMessage_noSynthesis_throws424() {
        mockContext(false, false);

        assertThatThrownBy(() -> service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", false), null, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(FAILED_DEPENDENCY));

        verifyNoInteractions(anthropicService);
    }

    // U-04 : useEnriched=true + PRO → modèle Sonnet (analyze appelé, pas analyzeFast)
    @Test
    void sendMessage_useEnriched_pro_callsSonnet() {
        mockContext(false, true);
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(true);
        when(anthropicService.analyze(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Réponse Sonnet", "claude-sonnet-4-6", 200, 100));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Analyse approfondie", true), null, "GOOGLE", null);

        assertThat(response.useEnriched()).isTrue();
        verify(anthropicService).analyze(any(), any(), anyInt());
        verify(anthropicService, never()).analyzeFast(any(), any(), anyInt());
    }

    // U-05 : useEnriched=true + STARTER → fallback Haiku
    @Test
    void sendMessage_useEnriched_starter_fallbackToHaiku() {
        mockContext(false, true);
        when(planLimitService.isEnrichedAnalysisAllowedForWorkspace(WORKSPACE_ID)).thenReturn(false);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Réponse Haiku", "claude-haiku-4-5-20251001", 100, 50));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", true), null, "GOOGLE", null);

        assertThat(response.useEnriched()).isFalse();
        verify(anthropicService).analyzeFast(any(), any(), anyInt());
        verify(anthropicService, never()).analyze(any(), any(), anyInt());
    }

    // U-06 : sans souscription → autorisé (isChatMessageLimitReached retourne false)
    @Test
    void sendMessage_noSubscription_allowed() {
        mockContext(false, true);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("OK", "claude-haiku-4-5-20251001", 50, 25));
        when(chatMessageRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChatMessageResponse response = service.sendMessage(
                CASE_FILE_ID, new ChatMessageRequest("Question", false), null, "GOOGLE", null);

        assertThat(response.answer()).isEqualTo("OK");
    }

    // ────────────────────────────────────────────────────────────────────────
    // SF-35-03 : buildUserMessage — injection texte brut documents
    // ────────────────────────────────────────────────────────────────────────

    @Test
    void buildUserMessage_noDocuments_returnsSynthesisOnly() {
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_FILE_ID))
                .thenReturn(java.util.List.of());

        String result = service.buildUserMessage("La synthèse", CASE_FILE_ID, "Ma question");

        assertThat(result).isEqualTo("Dossier :\nLa synthèse\n\nQuestion : Ma question");
    }

    // F-148 hotfix : buildUserMessage inclut la section PIÈCES IDENTIFIÉES (+ Vision)
    @Test
    void buildUserMessage_injectsPiecesPromptContext_includingVisionDescriptions() {
        fr.ailegalcase.document.Document doc = buildDoc(UUID.randomUUID(), "dossier.pdf");
        fr.ailegalcase.document.DocumentExtraction ex = buildExtraction(doc,
                fr.ailegalcase.document.ExtractionStatus.DONE, "Contenu OCR.",
                "{\"extractor\":\"TEXTRACT\",\"pageCount\":3}");
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_FILE_ID))
                .thenReturn(java.util.List.of(doc));
        when(documentExtractionRepository.findByDocumentIdIn(java.util.List.of(doc.getId())))
                .thenReturn(java.util.List.of(ex));
        when(piecesPromptContext.buildContextForCaseFile(CASE_FILE_ID))
                .thenReturn("=== PIÈCES IDENTIFIÉES DANS LES DOCUMENTS ===\n"
                        + "Document : dossier.pdf\n"
                        + "  - SMS « Échanges Fatma/Anne » (p. 8) — [Vision : Bulles vertes à gauche.]\n"
                        + "===\n");

        String result = service.buildUserMessage("Synth", CASE_FILE_ID, "Qui parle dans les SMS ?");

        assertThat(result).contains("PIÈCES IDENTIFIÉES DANS LES DOCUMENTS");
        assertThat(result).contains("SMS « Échanges Fatma/Anne » (p. 8)");
        assertThat(result).contains("[Vision : Bulles vertes à gauche.]");
    }

    @Test
    void buildUserMessage_withDoneDocument_injectsExtractedText() {
        fr.ailegalcase.document.Document doc = buildDoc(UUID.randomUUID(), "P3-releve.pdf");
        fr.ailegalcase.document.DocumentExtraction ex = buildExtraction(doc,
                fr.ailegalcase.document.ExtractionStatus.DONE, "Virement de 804,05 EUR le 15 mars.",
                "{\"extractor\":\"TEXTRACT\",\"pageCount\":2}");
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_FILE_ID))
                .thenReturn(java.util.List.of(doc));
        when(documentExtractionRepository.findByDocumentIdIn(java.util.List.of(doc.getId())))
                .thenReturn(java.util.List.of(ex));

        String result = service.buildUserMessage("Synthèse", CASE_FILE_ID, "Est-ce 804 te parle ?");

        assertThat(result).contains("P3-releve.pdf");
        assertThat(result).contains("OCR");
        assertThat(result).contains("804,05 EUR");
        assertThat(result).contains("Question : Est-ce 804 te parle ?");
    }

    @Test
    void buildUserMessage_failedExtractions_excluded() {
        fr.ailegalcase.document.Document ok = buildDoc(UUID.randomUUID(), "ok.pdf");
        fr.ailegalcase.document.Document failed = buildDoc(UUID.randomUUID(), "corrompu.pdf");
        fr.ailegalcase.document.DocumentExtraction okEx = buildExtraction(ok,
                fr.ailegalcase.document.ExtractionStatus.DONE, "Texte OK.", null);
        fr.ailegalcase.document.DocumentExtraction failedEx = buildExtraction(failed,
                fr.ailegalcase.document.ExtractionStatus.FAILED, null, null);
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_FILE_ID))
                .thenReturn(java.util.List.of(ok, failed));
        when(documentExtractionRepository.findByDocumentIdIn(any()))
                .thenReturn(java.util.List.of(okEx, failedEx));

        String result = service.buildUserMessage("Synth", CASE_FILE_ID, "Q");

        assertThat(result).contains("ok.pdf");
        assertThat(result).doesNotContain("corrompu.pdf");
    }

    @Test
    void buildUserMessage_budgetExceeded_truncatesAndMentionsExcluded() {
        // 2 docs : le premier prend tout le budget, le second est exclu
        String hugeText = "A".repeat(ChatService.DOCUMENT_TEXT_BUDGET_CHARS + 50_000);
        fr.ailegalcase.document.Document d1 = buildDoc(UUID.randomUUID(), "big.pdf");
        fr.ailegalcase.document.Document d2 = buildDoc(UUID.randomUUID(), "small.pdf");
        fr.ailegalcase.document.DocumentExtraction ex1 = buildExtraction(d1,
                fr.ailegalcase.document.ExtractionStatus.DONE, hugeText, null);
        fr.ailegalcase.document.DocumentExtraction ex2 = buildExtraction(d2,
                fr.ailegalcase.document.ExtractionStatus.DONE, "Petit texte.", null);
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(CASE_FILE_ID))
                .thenReturn(java.util.List.of(d1, d2));
        when(documentExtractionRepository.findByDocumentIdIn(any()))
                .thenReturn(java.util.List.of(ex1, ex2));

        String result = service.buildUserMessage("Synth", CASE_FILE_ID, "Q");

        assertThat(result).contains("1 inclus, 1 exclus par limite de taille");
        assertThat(result).contains("[… tronqué]");
    }

    private fr.ailegalcase.document.Document buildDoc(UUID id, String name) {
        fr.ailegalcase.document.Document d = new fr.ailegalcase.document.Document();
        d.setId(id);
        d.setOriginalFilename(name);
        d.setContentType("application/pdf");
        d.setFileSize(1000L);
        d.setStorageKey("k/" + id);
        return d;
    }

    private fr.ailegalcase.document.DocumentExtraction buildExtraction(
            fr.ailegalcase.document.Document doc,
            fr.ailegalcase.document.ExtractionStatus status,
            String text, String metadata) {
        fr.ailegalcase.document.DocumentExtraction ex = new fr.ailegalcase.document.DocumentExtraction();
        ex.setDocument(doc);
        ex.setExtractionStatus(status);
        ex.setExtractedText(text);
        ex.setExtractionMetadata(metadata);
        return ex;
    }
}
