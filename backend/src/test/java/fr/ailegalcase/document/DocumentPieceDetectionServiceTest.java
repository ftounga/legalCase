package fr.ailegalcase.document;

import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentPieceDetectionServiceTest {

    @Mock private DocumentExtractionRepository extractionRepository;
    @Mock private DocumentPieceRepository pieceRepository;
    @Mock private AnthropicService anthropicService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private DocumentPieceDetectionService service;

    private static final UUID EXTRACTION_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000022");

    @BeforeEach
    void setUp() {
        service = new DocumentPieceDetectionService(
                extractionRepository, pieceRepository, anthropicService, new SyncTaskExecutor(), eventPublisher);
    }

    // U-01 : Haiku retourne 2 pièces → 2 entrées persistées dans le bon ordre
    @Test
    void detect_twoPieces_persistsBoth() {
        mockExtraction();
        String haikuResponse = """
                [
                  {"type":"CONTRAT","label":"CDI Dupont","pageStart":1,"pageEnd":3,"orderIndex":0},
                  {"type":"ATTESTATION","label":"Attestation collègue","pageStart":4,"pageEnd":4,"orderIndex":1}
                ]
                """;
        when(anthropicService.analyze(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(haikuResponse, "haiku", 100, 50));

        service.detect(EXTRACTION_ID, "un long texte...");

        ArgumentCaptor<DocumentPiece> captor = ArgumentCaptor.forClass(DocumentPiece.class);
        verify(pieceRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<DocumentPiece> saved = captor.getAllValues();
        assertThat(saved).extracting(DocumentPiece::getType)
                .containsExactly(DocumentPieceType.CONTRAT, DocumentPieceType.ATTESTATION);
        assertThat(saved).extracting(DocumentPiece::getLabel)
                .containsExactly("CDI Dupont", "Attestation collègue");
        assertThat(saved.get(0).getPageStart()).isEqualTo(1);
        assertThat(saved.get(0).getPageEnd()).isEqualTo(3);
        assertThat(saved.get(1).getOrderIndex()).isEqualTo(1);
    }

    // U-02 : Haiku jette une exception → fallback 1 pièce AUTRE
    @Test
    void detect_haikuException_fallbackToAutre() {
        mockExtraction();
        when(anthropicService.analyze(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("haiku down"));

        service.detect(EXTRACTION_ID, "un long texte...");

        ArgumentCaptor<DocumentPiece> captor = ArgumentCaptor.forClass(DocumentPiece.class);
        verify(pieceRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(DocumentPieceType.AUTRE);
        assertThat(captor.getValue().getLabel()).isEqualTo("Document complet");
    }

    // U-03 : JSON invalide → fallback 1 pièce AUTRE
    @Test
    void detect_invalidJson_fallbackToAutre() {
        mockExtraction();
        when(anthropicService.analyze(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("pas du JSON du tout", "haiku", 10, 5));

        service.detect(EXTRACTION_ID, "un long texte...");

        ArgumentCaptor<DocumentPiece> captor = ArgumentCaptor.forClass(DocumentPiece.class);
        verify(pieceRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(DocumentPieceType.AUTRE);
    }

    // U-04 : Idempotence — les pièces existantes sont supprimées avant insert
    @Test
    void detect_existingPieces_deletedBeforeInsert() {
        mockExtraction();
        when(anthropicService.analyze(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("[]", "haiku", 10, 5));

        service.detect(EXTRACTION_ID, "texte");

        verify(pieceRepository).deleteByDocumentId(DOC_ID);
    }

    // U-05 : parseur robuste — accepte du texte autour du JSON array
    @Test
    void parseResponse_surroundingText_stillParses() throws Exception {
        String raw = "Voici les pièces identifiées :\n" +
                "[{\"type\":\"SMS\",\"label\":\"SMS\",\"pageStart\":1,\"pageEnd\":1,\"orderIndex\":0}]\n" +
                "fin.";
        List<DocumentPieceDetectionService.ParsedPiece> parsed =
                DocumentPieceDetectionService.parseResponse(raw);
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).type()).isEqualTo(DocumentPieceType.SMS);
    }

    // U-06 : type inconnu dans la réponse → fallback AUTRE (enum fromStringOrDefault)
    @Test
    void parseResponse_unknownType_fallsBackToAutre() throws Exception {
        String raw = "[{\"type\":\"PUZZLE\",\"label\":\"?\",\"pageStart\":1,\"pageEnd\":1,\"orderIndex\":0}]";
        List<DocumentPieceDetectionService.ParsedPiece> parsed =
                DocumentPieceDetectionService.parseResponse(raw);
        assertThat(parsed).hasSize(1);
        assertThat(parsed.get(0).type()).isEqualTo(DocumentPieceType.AUTRE);
    }

    // U-07 : extraction sans document → skip silencieux (pas de save)
    @Test
    void detect_missingDocument_skipsSilently() {
        when(extractionRepository.findById(EXTRACTION_ID)).thenReturn(Optional.empty());

        service.detect(EXTRACTION_ID, "texte");

        verify(pieceRepository, org.mockito.Mockito.never()).save(any());
    }

    // SF-145-03 U-08 : prompt contient les règles de regroupement et exemples few-shot
    @Test
    void systemPrompt_containsGroupingRulesAndExamples() {
        String prompt = DocumentPieceDetectionService.SYSTEM_PROMPT_TEMPLATE;
        assertThat(prompt).contains("regroupe les pages");
        assertThat(prompt).contains("RÈGLE CENTRALE");
        assertThat(prompt).contains("EXEMPLE 1");
        assertThat(prompt).contains("EXEMPLE 2");
        assertThat(prompt).contains("UNE SEULE entrée");
        assertThat(prompt).contains("préfère regrouper");
    }

    // SF-145-03 U-09 : le service utilise Sonnet (analyze), pas Haiku (analyzeFast)
    @Test
    void detect_usesSonnetNotHaiku() {
        mockExtraction();
        when(anthropicService.analyze(any(AiCallContext.class), anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("[]", "sonnet", 100, 50));

        service.detect(EXTRACTION_ID, "texte");

        verify(anthropicService).analyze(any(AiCallContext.class), anyString(), anyString(), anyInt());
        verify(anthropicService, org.mockito.Mockito.never())
                .analyzeFast(any(AiCallContext.class), anyString(), anyString(), anyInt());
    }

    private void mockExtraction() {
        Workspace ws = new Workspace();
        ws.setId(UUID.randomUUID());
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        cf.setWorkspace(ws);
        Document doc = new Document();
        doc.setId(DOC_ID);
        doc.setCaseFile(cf);
        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(EXTRACTION_ID);
        extraction.setDocument(doc);
        when(extractionRepository.findById(EXTRACTION_ID)).thenReturn(Optional.of(extraction));
    }

    // SF-145-09 U-10 : applicableFor(DROIT_DU_TRAVAIL) = commons + types travail
    @Test
    void applicableFor_droitDuTravail_containsContratAndBulletinPaie() {
        java.util.Set<DocumentPieceType> types =
                DocumentPieceType.applicableFor("DROIT_DU_TRAVAIL");
        assertThat(types).contains(DocumentPieceType.CONTRAT, DocumentPieceType.BULLETIN_PAIE);
        assertThat(types).contains(DocumentPieceType.PHOTO, DocumentPieceType.SMS); // commons
        assertThat(types).doesNotContain(DocumentPieceType.TITRE_DE_SEJOUR);
        assertThat(types).doesNotContain(DocumentPieceType.JUGEMENT_DIVORCE);
    }

    // SF-145-09 U-11 : applicableFor(DROIT_IMMIGRATION) = commons + types immigration
    @Test
    void applicableFor_immigration_containsTitreDeSejour() {
        java.util.Set<DocumentPieceType> types =
                DocumentPieceType.applicableFor("DROIT_IMMIGRATION");
        assertThat(types).contains(DocumentPieceType.TITRE_DE_SEJOUR,
                DocumentPieceType.PASSEPORT, DocumentPieceType.RECEPISSE_PREFECTURE,
                DocumentPieceType.DECISION_OQTF);
        assertThat(types).contains(DocumentPieceType.ATTESTATION); // common
        assertThat(types).doesNotContain(DocumentPieceType.BULLETIN_PAIE);
        assertThat(types).doesNotContain(DocumentPieceType.JUGEMENT_DIVORCE);
        // SF-145-10 : ACTE_MARIAGE désormais disponible en immigration
        // (L.423-1 conjoint de Français, L.434-7 regroupement familial).
        assertThat(types).contains(DocumentPieceType.ACTE_MARIAGE);
        // SF-145-10 : BAIL_LOCATION commun aux 3 domaines
        assertThat(types).contains(DocumentPieceType.BAIL_LOCATION);
    }

    // SF-145-09 U-12 : applicableFor(DROIT_FAMILLE)
    @Test
    void applicableFor_famille_containsJugementDivorce() {
        java.util.Set<DocumentPieceType> types =
                DocumentPieceType.applicableFor("DROIT_FAMILLE");
        assertThat(types).contains(DocumentPieceType.JUGEMENT_DIVORCE,
                DocumentPieceType.ACTE_MARIAGE, DocumentPieceType.LIVRET_FAMILLE);
        assertThat(types).contains(DocumentPieceType.PIECE_IDENTITE); // common
        assertThat(types).doesNotContain(DocumentPieceType.BULLETIN_PAIE);
        assertThat(types).doesNotContain(DocumentPieceType.TITRE_DE_SEJOUR);
        // SF-145-10 : BAIL_LOCATION commun
        assertThat(types).contains(DocumentPieceType.BAIL_LOCATION);
    }

    // SF-145-09 U-13 : legalDomain null ou inconnu → tous les types (fallback permissif)
    @Test
    void applicableFor_nullOrUnknown_returnsAllTypes() {
        assertThat(DocumentPieceType.applicableFor(null))
                .hasSameSizeAs(java.util.EnumSet.allOf(DocumentPieceType.class));
        assertThat(DocumentPieceType.applicableFor("UNKNOWN_DOMAIN"))
                .hasSameSizeAs(java.util.EnumSet.allOf(DocumentPieceType.class));
    }

    // SF-145-09 U-14 : buildSystemPrompt injecte la liste filtrée
    @Test
    void buildSystemPrompt_immigration_containsTitreSejourNotBulletin() {
        String prompt = DocumentPieceDetectionService.buildSystemPrompt("DROIT_IMMIGRATION");
        // La section "Types autorisés (liste exacte)" doit mentionner les types immigration
        assertThat(prompt).contains("TITRE_DE_SEJOUR");
        assertThat(prompt).contains("RECEPISSE_PREFECTURE");
        assertThat(prompt).contains("DECISION_OQTF");
        // Placeholder bien substitué
        assertThat(prompt).doesNotContain("{{TYPES_LIST}}");
        // La ligne dédiée "Types autorisés" ne mentionne PAS les types hors-domaine.
        // Remarque : BULLETIN_PAIE peut apparaître dans la section statique "Tailles
        // typiques" qui sert d'indication générale — on teste donc la ligne précise.
        String typesLine = extractTypesLine(prompt);
        assertThat(typesLine).doesNotContain("BULLETIN_PAIE");
        assertThat(typesLine).doesNotContain("JUGEMENT_DIVORCE");
        assertThat(typesLine).contains("TITRE_DE_SEJOUR");
    }

    private static String extractTypesLine(String prompt) {
        int i = prompt.indexOf("Types autorisés (liste exacte)");
        assertThat(i).isGreaterThanOrEqualTo(0);
        // Récupère les 2 lignes suivantes (label + liste)
        String rest = prompt.substring(i);
        int end = rest.indexOf("\n\n");
        return end > 0 ? rest.substring(0, end) : rest;
    }
}
