package fr.ailegalcase.document.vision;

import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentPiece;
import fr.ailegalcase.document.DocumentPieceRepository;
import fr.ailegalcase.document.DocumentPieceType;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class VisionEnrichmentServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentExtractionRepository extractionRepository;
    @Mock private DocumentPieceRepository pieceRepository;
    @Mock private AnthropicService anthropicService;
    @Mock private StorageService storageService;

    private VisionProperties props;
    private VisionEnrichmentService service;

    @BeforeEach
    void setUp() {
        props = new VisionProperties();
        props.setEnabled(true);
        props.setMinOcrCharsPerPage(40);
        props.setTriggerTypesByDomain(Map.of(
                "DROIT_DU_TRAVAIL", Set.of("SMS", "ATTESTATION", "PHOTO")
        ));
        props.setSkipTypesByDomain(Map.of(
                "DROIT_DU_TRAVAIL", Set.of("CONTRAT", "BULLETIN_PAIE")
        ));
        service = new VisionEnrichmentService(
                documentRepository, extractionRepository, pieceRepository,
                anthropicService, storageService, props, new SyncTaskExecutor());
    }

    private DocumentPiece piece(DocumentPieceType type, int pageStart, int pageEnd) {
        DocumentPiece p = new DocumentPiece();
        p.setType(type);
        p.setPageStart(pageStart);
        p.setPageEnd(pageEnd);
        p.setOrderIndex(0);
        return p;
    }

    @Test
    void U01_signal1_ocrPauvre_forceVisionMemeSiBlacklisté() {
        DocumentPiece contract = piece(DocumentPieceType.CONTRAT, 1, 1);
        Map<Integer, String> textByPage = Map.of(1, "short"); // < 40 chars
        assertThat(service.shouldEnrichPiece(contract, "DROIT_DU_TRAVAIL", textByPage)).isTrue();
    }

    @Test
    void U02_signal2_whitelist_ocrOK_vision() {
        DocumentPiece sms = piece(DocumentPieceType.SMS, 1, 1);
        String richText = "a".repeat(500);
        Map<Integer, String> textByPage = Map.of(1, richText);
        assertThat(service.shouldEnrichPiece(sms, "DROIT_DU_TRAVAIL", textByPage)).isTrue();
    }

    @Test
    void U03_blacklist_ocrOK_skip() {
        DocumentPiece contract = piece(DocumentPieceType.CONTRAT, 1, 2);
        String richText = "a".repeat(500);
        Map<Integer, String> textByPage = Map.of(1, richText, 2, richText);
        assertThat(service.shouldEnrichPiece(contract, "DROIT_DU_TRAVAIL", textByPage)).isFalse();
    }

    @Test
    void U04_niWhitelist_niBlacklist_ocrOK_skipParDefaut() {
        DocumentPiece autre = piece(DocumentPieceType.AUTRE, 1, 1);
        String richText = "a".repeat(500);
        Map<Integer, String> textByPage = Map.of(1, richText);
        assertThat(service.shouldEnrichPiece(autre, "DROIT_DU_TRAVAIL", textByPage)).isFalse();
    }

    @Test
    void U05_featureFlagOff_toujoursFalse() {
        props.setEnabled(false);
        DocumentPiece sms = piece(DocumentPieceType.SMS, 1, 1);
        Map<Integer, String> textByPage = Map.of(1, "short");
        assertThat(service.shouldEnrichPiece(sms, "DROIT_DU_TRAVAIL", textByPage)).isFalse();
    }

    @Test
    void U06_domaineInconnu_pasDeWhitelist_skipSaufSignal1() {
        DocumentPiece sms = piece(DocumentPieceType.SMS, 1, 1);
        Map<Integer, String> okText = Map.of(1, "a".repeat(500));
        assertThat(service.shouldEnrichPiece(sms, "DROIT_INCONNU", okText)).isFalse();

        Map<Integer, String> poorText = Map.of(1, "x");
        assertThat(service.shouldEnrichPiece(sms, "DROIT_INCONNU", poorText)).isTrue();
    }

    @Test
    void U10_multiPage_uneSeulePauvre_declenche() {
        DocumentPiece sms = piece(DocumentPieceType.CONTRAT, 1, 3);
        Map<Integer, String> textByPage = Map.of(
                1, "a".repeat(500),
                2, "page trop courte", // < 40 chars
                3, "a".repeat(500)
        );
        assertThat(service.shouldEnrichPiece(sms, "DROIT_DU_TRAVAIL", textByPage)).isTrue();
    }
}
