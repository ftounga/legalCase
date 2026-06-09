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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * F-260 / SF-260-01 : réordonnancement explicite des pièces (réassignation 1..N),
 * isolation workspace et validation de la liste fournie.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PieceOrderServiceTest {

    @Mock private DocumentPieceRepository pieceRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository memberRepository;
    @Mock private CurrentUserResolver currentUserResolver;

    private PieceOrderService service;

    private static final UUID WORKSPACE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID CASE_FILE_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PIECE_A = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID PIECE_B = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID PIECE_C = UUID.fromString("00000000-0000-0000-0000-0000000000cc");

    private User user;
    private Workspace workspace;
    private CaseFile caseFile;
    private DocumentPiece pieceA;
    private DocumentPiece pieceB;
    private DocumentPiece pieceC;

    @BeforeEach
    void setUp() {
        service = new PieceOrderService(
                pieceRepository, caseFileRepository, memberRepository, currentUserResolver);

        user = new User();
        workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);

        WorkspaceMember wm = new WorkspaceMember();
        wm.setWorkspace(workspace);
        wm.setUser(user);

        caseFile = new CaseFile();
        caseFile.setId(CASE_FILE_ID);
        caseFile.setWorkspace(workspace);

        Document document = new Document();
        document.setId(UUID.randomUUID());
        document.setCaseFile(caseFile);

        pieceA = piece(PIECE_A, document, 1);
        pieceB = piece(PIECE_B, document, 2);
        pieceC = piece(PIECE_C, document, 3);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(memberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(wm));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.of(caseFile));
        when(pieceRepository.findByCaseFileIdOrderByPieceNumber(CASE_FILE_ID))
                .thenReturn(List.of(pieceA, pieceB, pieceC));
        when(pieceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // F-260 U-01 : réordonnancement complet → piece_number réassigné 1..N dans l'ordre fourni.
    @Test
    void reorder_validList_reassigns1ToN() {
        List<DocumentPieceSummary> result = service.reorder(
                CASE_FILE_ID, List.of(PIECE_C, PIECE_A, PIECE_B), null, "google", null);

        assertThat(result).extracting(DocumentPieceSummary::id)
                .containsExactly(PIECE_C, PIECE_A, PIECE_B);
        assertThat(result).extracting(DocumentPieceSummary::pieceNumber)
                .containsExactly(1, 2, 3);
        assertThat(pieceC.getPieceNumber()).isEqualTo(1);
        assertThat(pieceA.getPieceNumber()).isEqualTo(2);
        assertThat(pieceB.getPieceNumber()).isEqualTo(3);
    }

    // F-260 U-02 : liste incomplète (manque une pièce) → 400.
    @Test
    void reorder_incompleteList_throws400() {
        assertThatThrownBy(() -> service.reorder(
                CASE_FILE_ID, List.of(PIECE_A, PIECE_B), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // F-260 U-03 : liste avec une pièce étrangère au dossier → 400.
    @Test
    void reorder_foreignPiece_throws400() {
        UUID foreign = UUID.randomUUID();
        assertThatThrownBy(() -> service.reorder(
                CASE_FILE_ID, List.of(PIECE_A, PIECE_B, foreign), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // F-260 U-04 : liste contenant un doublon → 400.
    @Test
    void reorder_duplicateId_throws400() {
        assertThatThrownBy(() -> service.reorder(
                CASE_FILE_ID, List.of(PIECE_A, PIECE_A, PIECE_B), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // F-260 U-05 : liste vide → 400.
    @Test
    void reorder_emptyList_throws400() {
        assertThatThrownBy(() -> service.reorder(
                CASE_FILE_ID, List.of(), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // F-260 U-06 : dossier d'un autre workspace → 404 (isolation workspace).
    @Test
    void reorder_crossWorkspace_throws404() {
        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setId(UUID.randomUUID());
        caseFile.setWorkspace(otherWorkspace);

        assertThatThrownBy(() -> service.reorder(
                CASE_FILE_ID, List.of(PIECE_A, PIECE_B, PIECE_C), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    // F-260 U-07 : dossier inexistant → 404.
    @Test
    void reorder_unknownCaseFile_throws404() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CASE_FILE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reorder(
                CASE_FILE_ID, List.of(PIECE_A, PIECE_B, PIECE_C), null, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static DocumentPiece piece(UUID id, Document document, int pieceNumber) {
        DocumentPiece p = new DocumentPiece();
        p.setId(id);
        p.setDocument(document);
        p.setType(DocumentPieceType.AUTRE);
        p.setPieceNumber(pieceNumber);
        return p;
    }
}
