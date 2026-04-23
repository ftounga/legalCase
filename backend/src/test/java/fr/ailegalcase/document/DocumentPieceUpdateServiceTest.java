package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentPieceUpdateServiceTest {

    @Mock private DocumentPieceRepository pieceRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private CurrentUserResolver currentUserResolver;

    private DocumentPieceUpdateService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID DOCUMENT_ID  = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PIECE_ID     = UUID.fromString("00000000-0000-0000-0000-000000000004");

    private User user;
    private Workspace workspace;
    private CaseFile caseFile;
    private Document document;
    private DocumentPiece piece;

    @BeforeEach
    void setUp() {
        service = new DocumentPieceUpdateService(
                pieceRepository, caseFileRepository, memberRepository, currentUserResolver);

        user = new User();
        workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        workspace.setLegalDomain("DROIT_IMMIGRATION");

        WorkspaceMember wm = new WorkspaceMember();
        wm.setWorkspace(workspace);
        wm.setUser(user);

        caseFile = new CaseFile();
        caseFile.setId(CASE_FILE_ID);
        caseFile.setWorkspace(workspace);

        document = new Document();
        document.setId(DOCUMENT_ID);
        document.setCaseFile(caseFile);

        piece = new DocumentPiece();
        piece.setId(PIECE_ID);
        piece.setDocument(document);
        piece.setType(DocumentPieceType.AUTRE);
        piece.setLabel("Ancien label");
        piece.setPageStart(1);
        piece.setPageEnd(1);
        piece.setOrderIndex(0);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(memberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(wm));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        when(pieceRepository.findById(PIECE_ID)).thenReturn(Optional.of(piece));
    }

    @Test
    void U01_updateType_ok_persisteEtRetourneSummary() {
        DocumentPieceSummary result = service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "ACTE_MARIAGE", "Acte n°127 Mairie 5ème",
                null, "LOCAL", null);

        assertThat(result.type()).isEqualTo("ACTE_MARIAGE");
        assertThat(result.label()).isEqualTo("Acte n°127 Mairie 5ème");
        assertThat(piece.getType()).isEqualTo(DocumentPieceType.ACTE_MARIAGE);
    }

    @Test
    void U02_typeInvalide_400() {
        assertThatThrownBy(() -> service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "PAS_UN_VRAI_TYPE", "label",
                null, "LOCAL", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Invalid piece type");
    }

    @Test
    void U03_typeNonApplicableAuDomaine_400() {
        // JUGEMENT_DIVORCE dispo en famille, pas en immigration
        assertThatThrownBy(() -> service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "JUGEMENT_DIVORCE", null,
                null, "LOCAL", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not applicable to domain DROIT_IMMIGRATION");
    }

    @Test
    void U04_BAIL_LOCATION_okDansImmigration_SF145_10() {
        DocumentPieceSummary result = service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "BAIL_LOCATION", "Bail Orsay",
                null, "LOCAL", null);

        assertThat(result.type()).isEqualTo("BAIL_LOCATION");
        assertThat(piece.getType()).isEqualTo(DocumentPieceType.BAIL_LOCATION);
    }

    @Test
    void U05_pieceDansAutreCaseFile_404() {
        CaseFile autreCaseFile = new CaseFile();
        autreCaseFile.setId(UUID.randomUUID());
        document.setCaseFile(autreCaseFile);

        assertThatThrownBy(() -> service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "ACTE_MARIAGE", null,
                null, "LOCAL", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Piece not found");
    }

    @Test
    void U06_caseFileAutreWorkspace_404_isolation() {
        Workspace autreWs = new Workspace();
        autreWs.setId(UUID.randomUUID());
        caseFile.setWorkspace(autreWs);

        assertThatThrownBy(() -> service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "ACTE_MARIAGE", null,
                null, "LOCAL", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    @Test
    void U07_labelVide_devientNull() {
        DocumentPieceSummary result = service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "ACTE_MARIAGE", "   ",
                null, "LOCAL", null);

        assertThat(result.label()).isNull();
    }

    @Test
    void U08_labelSuperieurA500car_tronque() {
        String longLabel = "x".repeat(700);
        DocumentPieceSummary result = service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "ACTE_MARIAGE", longLabel,
                null, "LOCAL", null);

        assertThat(result.label()).hasSize(500);
    }

    @Test
    void U09_typeEnMinuscules_normaliseeUppercase() {
        DocumentPieceSummary result = service.update(
                CASE_FILE_ID, DOCUMENT_ID, PIECE_ID,
                "  acte_mariage  ", null,
                null, "LOCAL", null);

        assertThat(result.type()).isEqualTo("ACTE_MARIAGE");
    }
}
