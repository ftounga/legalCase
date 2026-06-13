package fr.ailegalcase.casefile.conclusion;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.StrategicOptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.stylelearning.StyleCorpusDocument;
import fr.ailegalcase.stylelearning.StyleCorpusDocumentStatus;
import fr.ailegalcase.stylelearning.StyleCorpusRepository;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * F-98 / SF-98-01 — tests unitaires du worker de génération : succès → {@code DONE},
 * exception de l'appel IA → {@code FAILED}.
 */
class CaseConclusionServiceTest {

    private final CaseConclusionRepository caseConclusionRepository = mock(CaseConclusionRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final StrategicOptionRepository strategicOptionRepository = mock(StrategicOptionRepository.class);
    private final DocumentPieceRepository documentPieceRepository = mock(DocumentPieceRepository.class);
    private final CaseFileDashboardService caseFileDashboardService = mock(CaseFileDashboardService.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final StyleCorpusRepository styleCorpusRepository = mock(StyleCorpusRepository.class);
    private final fr.ailegalcase.casefile.jurisprudence.JurisprudenceCitationRepository
            jurisprudenceCitationRepository =
            mock(fr.ailegalcase.casefile.jurisprudence.JurisprudenceCitationRepository.class);
    private final fr.ailegalcase.analysis.JurisprudenceCheckRepository jurisprudenceCheckRepository =
            mock(fr.ailegalcase.analysis.JurisprudenceCheckRepository.class);
    private final fr.ailegalcase.jurisprudencemapping.ConclusionsJurisprudenceContext
            toolJurisprudenceContext =
            mock(fr.ailegalcase.jurisprudencemapping.ConclusionsJurisprudenceContext.class);
    private final AdverseMoyenPersistenceService adverseMoyenPersistenceService =
            mock(AdverseMoyenPersistenceService.class);
    private final ConclusionPromptRegistry promptRegistry = new ConclusionPromptRegistry(List.of(
            new CphFondDemandeurPromptProvider(), new CphFondDefendeurPromptProvider()));
    private final CaseConclusionPromptBuilder promptBuilder =
            new CaseConclusionPromptBuilder(new ObjectMapper(), promptRegistry);

    private final ConclusionStreamPublisher streamPublisher = mock(ConclusionStreamPublisher.class);
    private final ConclusionCompositionExclusionRepository compositionExclusionRepository =
            mock(ConclusionCompositionExclusionRepository.class);

    private final CaseConclusionService service = new CaseConclusionService(
            caseConclusionRepository, caseAnalysisRepository, strategicOptionRepository,
            documentPieceRepository, caseFileDashboardService,
            promptBuilder, anthropicService, styleCorpusRepository, jurisprudenceCitationRepository,
            jurisprudenceCheckRepository, toolJurisprudenceContext,
            adverseMoyenPersistenceService,
            streamPublisher, compositionExclusionRepository);

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
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
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
        // sans corpus de style stubé → génération générique
        assertThat(conclusion.isStyleApplied()).isFalse();
    }

    // ── SF-287-01 — streaming + barre de progression ──────────────────────────

    @Test
    void generate_streaming_emitsProgressThenDone() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        conclusion.setVersionNumber(4);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Le worker passe un callback de fragment : on simule deux deltas streamés.
        when(anthropicService.analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(), any(), anyInt(),
                any(java.util.function.Consumer.class)))
                .thenAnswer(inv -> {
                    java.util.function.Consumer<String> onDelta = inv.getArgument(4);
                    onDelta.accept("## I. FAITS\n");
                    onDelta.accept("Exposé des faits…");
                    return new AnthropicResult("## I. FAITS\nExposé des faits…",
                            "claude-sonnet-4-6", 1200, 3400, "end_turn");
                });

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        // Le 1er fragment publie un progress (throttle laisse passer le premier).
        verify(streamPublisher, atLeastOnce()).publishProgress(
                eq(conclusion.getCaseFile().getId()), anyString(), anyInt(), eq(8000));
        // Fin de flux → done avec le numéro de version de la ligne.
        verify(streamPublisher).publishDone(conclusion.getCaseFile().getId(), 4);
        verify(streamPublisher, never()).publishFailed(any(), any());
    }

    @Test
    void generate_streaming_onException_emitsFailed() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenThrow(new RuntimeException("Anthropic 529 overloaded"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.FAILED);
        verify(streamPublisher).publishFailed(eq(conclusion.getCaseFile().getId()), anyString());
        verify(streamPublisher, never()).publishDone(any(), anyInt());
    }

    @Test
    void generate_defendeurCell_usesDefendeurSystemPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        conclusion.setPositionCode("DEFENDEUR"); // cellule SF-98-02
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — débouter", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), contains("avocat du défendeur (employeur)"), any(), anyInt(), any());
    }

    @Test
    void generate_aiException_marksFailedWithErrorMessage() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        when(caseConclusionRepository.findById(conclusionId)).thenReturn(Optional.of(conclusion));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                any(), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
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
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
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

        verify(anthropicService, never()).analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any());
    }

    // ── SF-98-47 — style mimicking ───────────────────────────────────────────

    @Test
    void generate_withActiveStyleSignatures_marksStyleApplied() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID workspaceId = conclusion.getWorkspace().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(
                workspaceId, StyleCorpusDocumentStatus.DONE))
                .thenReturn(List.of(styleDocument("Phrases courtes, registre assertif.")));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        assertThat(conclusion.isStyleApplied()).isTrue();
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), contains("Adopte le style rédactionnel suivant"), any(), anyInt(), any());
    }

    // F-260 / SF-260-01 : loadNumberedPieces lit le piece_number PERSISTANT.
    // Une pièce de numéro 7 doit apparaître « Pièce n° 7 » dans le prompt (pas
    // renumérotée 1 à la volée). Source unique partagée avec les fiches.
    @Test
    void generate_usesPersistentPieceNumberInPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        fr.ailegalcase.document.DocumentPiece piece = new fr.ailegalcase.document.DocumentPiece();
        piece.setType(fr.ailegalcase.document.DocumentPieceType.CONTRAT);
        piece.setLabel("CDI Dupont");
        piece.setPieceNumber(7);
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any()))
                .thenReturn(List.of(piece));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(), contains("Pièce n° 7 — CDI Dupont"), anyInt(), any());
    }

    // ── SF-98-57 — bordereau de pièces communiquées ──────────────────────────

    @Test
    void generate_withPieces_appendsBordereauAtEndOfContent() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any()))
                .thenReturn(List.of(
                        documentPiece(2, "Lettre de licenciement",
                                fr.ailegalcase.document.DocumentPieceType.LETTRE),
                        documentPiece(1, "CDI Dupont",
                                fr.ailegalcase.document.DocumentPieceType.CONTRAT)));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — débouter", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        // le corps LLM précède l'annexe, puis le bordereau clôt l'acte (trié par numéro)
        assertThat(conclusion.getContent())
                .startsWith("PAR CES MOTIFS — débouter")
                .endsWith("1. CDI Dupont (Contrat)\n2. Lettre de licenciement");
        assertThat(conclusion.getContent()).contains("## BORDEREAU DE PIÈCES COMMUNIQUÉES");
    }

    @Test
    void generate_noPiece_doesNotAppendBordereau() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion); // 0 pièce stubée
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        assertThat(conclusion.getContent())
                .isEqualTo("PAR CES MOTIFS — texte")
                .doesNotContain("BORDEREAU");
    }

    @Test
    void generate_bordereauNumbersMatchPromptPieceNumbers() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any()))
                .thenReturn(List.of(documentPiece(7, "CDI Dupont",
                        fr.ailegalcase.document.DocumentPieceType.CONTRAT)));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        // même source (loadNumberedPieces) → « Pièce n° 7 » au prompt == « 7. … » au bordereau
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(), contains("Pièce n° 7 — CDI Dupont"), anyInt(), any());
        assertThat(conclusion.getContent()).contains("7. CDI Dupont (Contrat)");
    }

    // SF-260-02 : filet de lecture. Des piece_number en collision (1, 1, 2) hérités
    // d'une race d'écriture passée → loadNumberedPieces renumérote positionnellement
    // 1..N (ordre de la requête conservé), donc plus aucun doublon au prompt ni au bordereau.
    @Test
    void generate_collidingPieceNumbers_renumberedPositionally() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // collision : deux pièces au n° 1 puis une au n° 2 (ordre déjà trié par la requête)
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any()))
                .thenReturn(List.of(
                        documentPiece(1, "CDI Dupont",
                                fr.ailegalcase.document.DocumentPieceType.CONTRAT),
                        documentPiece(1, "Lettre de licenciement",
                                fr.ailegalcase.document.DocumentPieceType.LETTRE),
                        documentPiece(2, "Bulletin de paie",
                                fr.ailegalcase.document.DocumentPieceType.BULLETIN_PAIE)));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        // bordereau renuméroté 1, 2, 3 — aucun doublon, ordre préservé
        String bordereau = conclusion.getContent().substring(
                conclusion.getContent().indexOf("## BORDEREAU"));
        assertThat(bordereau)
                .contains("1. CDI Dupont")
                .contains("2. Lettre de licenciement")
                .contains("3. Bulletin de paie");
        // prompt : numéros distincts également (même source loadNumberedPieces)
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("Pièce n° 3 — Bulletin de paie"), anyInt(), any());
    }

    @Test
    void generate_bordereau_antiJargon_noRawEnumTokenWhenLabelPresent() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any()))
                .thenReturn(List.of(documentPiece(1, "Avenant 2023",
                        fr.ailegalcase.document.DocumentPieceType.CONTRAT)));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        // section bordereau : libellé lisible, jamais le token d'enum brut
        String bordereau = conclusion.getContent().substring(
                conclusion.getContent().indexOf("## BORDEREAU"));
        assertThat(bordereau).contains("Avenant 2023 (Contrat)").doesNotContain("CONTRAT");
    }

    @Test
    void generate_failed_doesNotAppendBordereau() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any()))
                .thenReturn(List.of(documentPiece(1, "CDI Dupont",
                        fr.ailegalcase.document.DocumentPieceType.CONTRAT)));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenThrow(new RuntimeException("Anthropic 529 overloaded"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.FAILED);
        assertThat(conclusion.getContent()).isNull();
    }

    @Test
    void generate_noActiveStyleSignature_marksStyleNotApplied() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        assertThat(conclusion.isStyleApplied()).isFalse();
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), argThat(s -> !s.contains("Adopte le style rédactionnel suivant")), any(), anyInt(), any());
    }

    @Test
    void generate_styleCorpusReadFails_failsOpenWithoutStyle() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenThrow(new RuntimeException("style_corpus_documents indisponible"));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        // fail-open : génération générique aboutie, style_applied = false
        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        assertThat(conclusion.isStyleApplied()).isFalse();
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), argThat(s -> !s.contains("Adopte le style rédactionnel suivant")), any(), anyInt(), any());
    }

    // ── SF-261-05 — moyens adverses persistés (lus, non ré-extraits) ─────────

    @Test
    void generate_withPersistedAdverseMoyens_injectsThemIntoPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());

        // Le service de persistance retourne le set persisté (avec id stable).
        when(adverseMoyenPersistenceService.loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE"))
                .thenReturn(List.of(new AdverseMoyenWithId(
                        UUID.randomUUID(),
                        new AdverseMoyen(
                                "Le licenciement repose sur une faute grave.",
                                List.of("art. L. 1234-1 C. trav."),
                                List.of("Lettre de licenciement")))));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        verify(adverseMoyenPersistenceService).loadOrExtract(caseFileId, "DROIT_DU_TRAVAIL", "FRANCE");
        // le moyen persisté alimente la section du prompt (intrant inchangé)
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("=== MOYENS ADVERSES À RÉFUTER ==="), anyInt(), any());
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("Le licenciement repose sur une faute grave."), anyInt(), any());
    }

    @Test
    void generate_noPersistedAdverseMoyens_noSectionInPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // aucun moyen (loadOrExtract renvoie liste vide par défaut du mock)
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(m -> !m.contains("MOYENS ADVERSES À RÉFUTER")), anyInt(), any());
    }

    // ── F-271 / SF-271-02 — conclusions récapitulatives (texte de l'avocat préservé) ──

    @Test
    void generate_withPreviousDoneVersion_injectsPreservationBaseIntoPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Dernière version DONE avec content édité par l'avocat = texte à préserver.
        CaseConclusion previous = new CaseConclusion();
        previous.setId(UUID.randomUUID());
        previous.setStatus(CaseConclusionStatus.DONE);
        previous.setContent("## PAR CES MOTIFS\nCondamner à 18 000 € (édition avocat).");
        when(caseConclusionRepository.findFirstByCaseFileIdAndStatusOrderByVersionNumberDesc(
                caseFileId, CaseConclusionStatus.DONE)).thenReturn(Optional.of(previous));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — récapitulatif", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("TEXTE ACTUEL À PRÉSERVER"), anyInt(), any());
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("Condamner à 18 000 € (édition avocat)."), anyInt(), any());
    }

    @Test
    void generate_firstVersion_noPreservationBaseInPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Aucune version DONE antérieure (findFirst...DONE renvoie empty par défaut).
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — from scratch", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(userMessage -> !userMessage.contains("TEXTE ACTUEL À PRÉSERVER")), anyInt(), any());
    }

    @Test
    void generate_previousDoneVersionWithBlankContent_noPreservationBaseInPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        CaseConclusion previous = new CaseConclusion();
        previous.setId(UUID.randomUUID());
        previous.setStatus(CaseConclusionStatus.DONE);
        previous.setContent("   ");
        when(caseConclusionRepository.findFirstByCaseFileIdAndStatusOrderByVersionNumberDesc(
                caseFileId, CaseConclusionStatus.DONE)).thenReturn(Optional.of(previous));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(userMessage -> !userMessage.contains("TEXTE ACTUEL À PRÉSERVER")), anyInt(), any());
    }

    // ── SF-271-02 — bordereau retiré de la base + flag fromScratch ──

    @Test
    void generate_withPreviousDoneVersion_stripsBordereauFromInjectedBase() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Version DONE précédente = corps de l'acte SUIVI du bordereau (assemblé par
        // finalize). La base réinjectée doit garder le corps et COUPER le bordereau.
        CaseConclusion previous = new CaseConclusion();
        previous.setId(UUID.randomUUID());
        previous.setStatus(CaseConclusionStatus.DONE);
        previous.setContent("## PAR CES MOTIFS\nCondamner à 18 000 €."
                + "\n\n## BORDEREAU DE PIÈCES COMMUNIQUÉES\n\n1. Contrat de travail");
        when(caseConclusionRepository.findFirstByCaseFileIdAndStatusOrderByVersionNumberDesc(
                caseFileId, CaseConclusionStatus.DONE)).thenReturn(Optional.of(previous));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — récapitulatif", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        // Le corps est conservé dans la base injectée.
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("Condamner à 18 000 €."), anyInt(), any());
        // Le bordereau (et sa pièce) est ABSENT de la base réinjectée → pas de doublon.
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(m -> !m.contains("BORDEREAU DE PIÈCES COMMUNIQUÉES")
                        && !m.contains("1. Contrat de travail")), anyInt(), any());
    }

    @Test
    void generate_fromScratch_ignoresPreviousDoneVersion() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        // « Régénérer de zéro » : le flag est posé au déclenchement.
        conclusion.setGeneratedFromScratch(true);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Même s'il existe une version DONE précédente, elle ne doit PAS être injectée.
        CaseConclusion previous = new CaseConclusion();
        previous.setId(UUID.randomUUID());
        previous.setStatus(CaseConclusionStatus.DONE);
        previous.setContent("## PAR CES MOTIFS\nCondamner à 18 000 € (édition avocat).");
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — from scratch", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        // En from-scratch, loadPreviousRecapContent n'est même pas appelé.
        verify(caseConclusionRepository, never())
                .findFirstByCaseFileIdAndStatusOrderByVersionNumberDesc(
                        any(), eq(CaseConclusionStatus.DONE));
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(m -> !m.contains("TEXTE ACTUEL À PRÉSERVER")
                        && !m.contains("édition avocat")), anyInt(), any());
    }

    // ── F-288 / SF-288-01 — filtre de composition (outils décisionnels exclus) ──

    @Test
    void generate_excludedTool_isAbsentFromTilesInjectedIntoPrompt() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Deux outils calculés : F-DT-08 (exclu) et F-DT-09 (conservé).
        when(caseFileDashboardService.assembleDecisionToolTiles(caseFileId)).thenReturn(List.of(
                new fr.ailegalcase.casefile.DashboardTile("F-DT-08-licenciement-validity",
                        "VALIDITE", "Validité du licenciement", "Sans cause", "2/7", "ALERT"),
                new fr.ailegalcase.casefile.DashboardTile("F-DT-09-comparateur-indemnites",
                        "INDEMNITES", "Comparateur d'indemnités", "18 000 €", null, "OK")));
        when(compositionExclusionRepository.findByCaseFileIdAndDimension(
                caseFileId, "DECISION_TOOL"))
                .thenReturn(List.of(exclusion(caseFileId, "F-DT-08-licenciement-validity")));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        // L'outil exclu ne figure pas au prompt ; l'outil conservé oui.
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(m -> m.contains("Comparateur d'indemnités")
                        && !m.contains("Validité du licenciement")), anyInt(), any());
    }

    @Test
    void generate_excludedTool_filtersItsDerivedJurisprudence() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        // Jurisprudence dérivée de deux outils : l'un exclu, l'autre conservé.
        when(toolJurisprudenceContext.collectForCaseFile(caseFileId)).thenReturn(List.of(
                new fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool(
                        "F-DT-08-licenciement-validity", "DEFAULT",
                        List.of(citation("Cass. soc. 25 sept. 1991, n° 90-42.000"))),
                new fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool(
                        "F-DT-09-comparateur-indemnites", "DEFAULT",
                        List.of(citation("Cass. soc. 1 fév. 2017, n° 15-12.345")))));
        when(compositionExclusionRepository.findByCaseFileIdAndDimension(
                caseFileId, "DECISION_TOOL"))
                .thenReturn(List.of(exclusion(caseFileId, "F-DT-08-licenciement-validity")));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        // L'arrêt dérivé de l'outil exclu disparaît du prompt ; celui de l'outil conservé reste.
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                argThat(m -> m.contains("90-42.000") == false
                        && m.contains("15-12.345")), anyInt(), any());
    }

    @Test
    void generate_noExclusion_tilesUnchanged_nonRegression() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(caseFileId)).thenReturn(List.of(
                new fr.ailegalcase.casefile.DashboardTile("F-DT-08-licenciement-validity",
                        "VALIDITE", "Validité du licenciement", "Sans cause", "2/7", "ALERT")));
        // Aucune exclusion persistée (repository renvoie liste vide par défaut).
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        // Sans exclusion, l'outil calculé est injecté comme aujourd'hui.
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("Validité du licenciement"), anyInt(), any());
    }

    @Test
    void generate_exclusionReadFails_failsOpen_noFilter() {
        UUID conclusionId = UUID.randomUUID();
        CaseConclusion conclusion = pendingConclusion(conclusionId);
        UUID caseFileId = conclusion.getCaseFile().getId();
        stubGenerationStubs(conclusionId, conclusion);
        when(styleCorpusRepository.findByWorkspaceIdAndActiveTrueAndStatus(any(), any()))
                .thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(caseFileId)).thenReturn(List.of(
                new fr.ailegalcase.casefile.DashboardTile("F-DT-08-licenciement-validity",
                        "VALIDITE", "Validité du licenciement", "Sans cause", "2/7", "ALERT")));
        // La lecture des exclusions échoue → fail-open : aucun filtre, l'outil reste injecté.
        when(compositionExclusionRepository.findByCaseFileIdAndDimension(caseFileId, "DECISION_TOOL"))
                .thenThrow(new RuntimeException("conclusion_composition_exclusion indisponible"));
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any()))
                .thenReturn(new AnthropicResult("PAR CES MOTIFS — texte", "claude-sonnet-4-6",
                        1200, 3400, "end_turn"));

        service.generate(conclusionId);

        assertThat(conclusion.getStatus()).isEqualTo(CaseConclusionStatus.DONE);
        verify(anthropicService).analyzeWithSystemCacheStreaming(
                any(AiCallContext.class), any(),
                contains("Validité du licenciement"), anyInt(), any());
    }

    private ConclusionCompositionExclusion exclusion(UUID caseFileId, String itemKey) {
        ConclusionCompositionExclusion e = new ConclusionCompositionExclusion();
        e.setCaseFileId(caseFileId);
        e.setDimension("DECISION_TOOL");
        e.setItemKey(itemKey);
        return e;
    }

    private fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationResponse citation(String arretRef) {
        return new fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationResponse(
                UUID.randomUUID(), arretRef, "Cour de cassation", null, null, null, null, null, null);
    }

    /** Stubs communs aux scénarios de génération aboutie (hors corpus de style et IA). */
    private void stubGenerationStubs(UUID conclusionId, CaseConclusion conclusion) {
        when(caseConclusionRepository.findById(conclusionId)).thenReturn(Optional.of(conclusion));
        when(caseConclusionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                any(), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());
        when(documentPieceRepository.findByCaseFileIdOrderByPieceNumber(any())).thenReturn(List.of());
        when(caseFileDashboardService.assembleDecisionToolTiles(any())).thenReturn(List.of());
        // SF-261-05 — par défaut aucun moyen adverse persisté (set vide) ; les scénarios
        // qui en ont besoin surchargent loadOrExtract avec leurs moyens.
        when(adverseMoyenPersistenceService.loadOrExtract(any(), any(), any())).thenReturn(List.of());
    }

    private fr.ailegalcase.document.DocumentPiece documentPiece(
            int pieceNumber, String label, fr.ailegalcase.document.DocumentPieceType type) {
        fr.ailegalcase.document.DocumentPiece piece = new fr.ailegalcase.document.DocumentPiece();
        piece.setPieceNumber(pieceNumber);
        piece.setLabel(label);
        piece.setType(type);
        return piece;
    }

    private StyleCorpusDocument styleDocument(String signature) {
        StyleCorpusDocument doc = new StyleCorpusDocument();
        doc.setStatus(StyleCorpusDocumentStatus.DONE);
        doc.setActive(true);
        doc.setStyleSignature(signature);
        return doc;
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
