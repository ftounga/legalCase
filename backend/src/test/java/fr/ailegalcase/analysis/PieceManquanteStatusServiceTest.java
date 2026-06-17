package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-194 SF-194-01 — UT du service {@link PieceManquanteStatusService}.
 *
 * <ul>
 *   <li>upsert idempotent (UNIQUE clé)</li>
 *   <li>normalisation libellé (trim/lowercase)</li>
 *   <li>validation entrée (statut, libellé, raison_non_app)</li>
 *   <li>isolation workspace 404</li>
 *   <li>collectForEnrichment trichotomie</li>
 * </ul>
 */
class PieceManquanteStatusServiceTest {

    private PieceManquanteStatusRepository pieceManquanteStatusRepository;
    private CaseFileRepository caseFileRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private PieceManquanteStatusService service;

    private User user;
    private Workspace workspace;
    private WorkspaceMember member;
    private CaseFile caseFile;
    private UUID caseFileId;

    @BeforeEach
    void setUp() {
        pieceManquanteStatusRepository = mock(PieceManquanteStatusRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);

        service = new PieceManquanteStatusService(
                pieceManquanteStatusRepository, caseFileRepository,
                workspaceMemberRepository, currentUserResolver);

        user = new User();
        workspace = new Workspace();
        setField(workspace, "id", UUID.randomUUID());
        member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFileId = UUID.randomUUID();
        setField(caseFile, "id", caseFileId);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
    }

    @Test
    void upsertStatus_newPiece_createsEntry() {
        when(pieceManquanteStatusRepository.findByCaseFileIdAndPieceLibelleNormalise(
                caseFileId, "contrat de travail original"))
                .thenReturn(Optional.empty());
        when(pieceManquanteStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PieceManquanteStatus saved = service.upsertStatus(caseFileId,
                "Contrat de travail original", PieceManquanteStatus.STATUT_A_DEMANDER,
                null, "client", null, mock(Principal.class));

        assertThat(saved.getStatut()).isEqualTo(PieceManquanteStatus.STATUT_A_DEMANDER);
        assertThat(saved.getPieceLibelleNormalise()).isEqualTo("contrat de travail original");
        assertThat(saved.getPieceLibelleOriginal()).isEqualTo("Contrat de travail original");
        assertThat(saved.getDestinataire()).isEqualTo("client");
        assertThat(saved.getRaisonNonApp()).isNull();
        verify(pieceManquanteStatusRepository, times(1)).save(any());
    }

    // SF-194-04 — lien document persisté pour une pièce OBTENUE.
    @Test
    void upsertStatus_obtenueWithDocument_persistsDocumentId() {
        UUID docId = UUID.randomUUID();
        when(pieceManquanteStatusRepository.findByCaseFileIdAndPieceLibelleNormalise(
                caseFileId, "bulletins de paie"))
                .thenReturn(Optional.empty());
        when(pieceManquanteStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PieceManquanteStatus saved = service.upsertStatus(caseFileId,
                "Bulletins de paie", PieceManquanteStatus.STATUT_OBTENUE,
                null, null, docId, null, mock(Principal.class));

        assertThat(saved.getStatut()).isEqualTo(PieceManquanteStatus.STATUT_OBTENUE);
        assertThat(saved.getObtainedDocumentId()).isEqualTo(docId);
    }

    // SF-194-04 — un lien document n'est conservé que pour OBTENUE (neutralisé sinon).
    @Test
    void upsertStatus_nonObtenue_clearsDocumentLink() {
        UUID docId = UUID.randomUUID();
        when(pieceManquanteStatusRepository.findByCaseFileIdAndPieceLibelleNormalise(
                caseFileId, "contrat"))
                .thenReturn(Optional.empty());
        when(pieceManquanteStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PieceManquanteStatus saved = service.upsertStatus(caseFileId,
                "Contrat", PieceManquanteStatus.STATUT_A_DEMANDER,
                null, "client", docId, null, mock(Principal.class));

        assertThat(saved.getObtainedDocumentId()).isNull();
    }

    @Test
    void upsertStatus_existingPiece_updatesEntry() {
        PieceManquanteStatus existing = new PieceManquanteStatus();
        existing.setCaseFile(caseFile);
        existing.setWorkspace(workspace);
        existing.setPieceLibelleNormalise("contrat de travail original");
        existing.setPieceLibelleOriginal("Contrat de travail original");
        existing.setStatut(PieceManquanteStatus.STATUT_A_DEMANDER);
        when(pieceManquanteStatusRepository.findByCaseFileIdAndPieceLibelleNormalise(
                caseFileId, "contrat de travail original"))
                .thenReturn(Optional.of(existing));
        when(pieceManquanteStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PieceManquanteStatus saved = service.upsertStatus(caseFileId,
                "Contrat de travail original", PieceManquanteStatus.STATUT_OBTENUE,
                null, null, null, mock(Principal.class));

        assertThat(saved.getStatut()).isEqualTo(PieceManquanteStatus.STATUT_OBTENUE);
        // Le repository n'est pas re-créé, mais save est bien appelé pour persister
        verify(pieceManquanteStatusRepository, times(1)).save(any());
    }

    @Test
    void upsertStatus_normalizationStrategy_trimAndLowercase() {
        when(pieceManquanteStatusRepository.findByCaseFileIdAndPieceLibelleNormalise(any(), any()))
                .thenReturn(Optional.empty());
        when(pieceManquanteStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PieceManquanteStatus s = service.upsertStatus(caseFileId,
                "  Contrat DE Travail Original  ", PieceManquanteStatus.STATUT_A_DEMANDER,
                null, null, null, mock(Principal.class));
        assertThat(s.getPieceLibelleNormalise()).isEqualTo("contrat de travail original");
        // Le libellé original conserve la casse mais est trimmé
        assertThat(s.getPieceLibelleOriginal()).isEqualTo("Contrat DE Travail Original");
    }

    @Test
    void upsertStatus_invalidStatut_throws400() {
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Contrat",
                "INVALIDE", null, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Statut invalide");
        verify(pieceManquanteStatusRepository, never()).save(any());
    }

    @Test
    void upsertStatus_blankLibelle_throws400() {
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "   ",
                PieceManquanteStatus.STATUT_A_DEMANDER, null, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("requis");
    }

    @Test
    void upsertStatus_libelleTooLong_throws400() {
        String tooLong = "x".repeat(501);
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, tooLong,
                PieceManquanteStatus.STATUT_A_DEMANDER, null, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("trop long");
    }

    @Test
    void upsertStatus_raisonOnNonApplicableStatut_accepted() {
        when(pieceManquanteStatusRepository.findByCaseFileIdAndPieceLibelleNormalise(any(), any()))
                .thenReturn(Optional.empty());
        when(pieceManquanteStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PieceManquanteStatus saved = service.upsertStatus(caseFileId,
                "Convention BTP", PieceManquanteStatus.STATUT_NON_APPLICABLE,
                "Hors champ d'application", null, null, mock(Principal.class));
        assertThat(saved.getRaisonNonApp()).isEqualTo("Hors champ d'application");
    }

    @Test
    void upsertStatus_raisonOnDemanderStatut_throws400() {
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Contrat",
                PieceManquanteStatus.STATUT_A_DEMANDER, "raison interdite", null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("raisonNonApp");
    }

    @Test
    void upsertStatus_caseFileInOtherWorkspace_throws404() {
        Workspace other = new Workspace();
        setField(other, "id", UUID.randomUUID());
        caseFile.setWorkspace(other);

        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Contrat",
                PieceManquanteStatus.STATUT_A_DEMANDER, null, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    @Test
    void upsertStatus_caseFileNotFound_throws404() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Contrat",
                PieceManquanteStatus.STATUT_A_DEMANDER, null, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    @Test
    void upsertStatus_workspaceNotFound_throws404() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Contrat",
                PieceManquanteStatus.STATUT_A_DEMANDER, null, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workspace not found");
    }

    @Test
    void collectForEnrichment_trichotomy_returnsThreeLists() {
        PieceManquanteStatus s1 = new PieceManquanteStatus();
        s1.setStatut(PieceManquanteStatus.STATUT_OBTENUE);
        s1.setPieceLibelleOriginal("Contrat de travail");
        PieceManquanteStatus s2 = new PieceManquanteStatus();
        s2.setStatut(PieceManquanteStatus.STATUT_NON_APPLICABLE);
        s2.setPieceLibelleOriginal("Convention BTP");
        PieceManquanteStatus s3 = new PieceManquanteStatus();
        s3.setStatut(PieceManquanteStatus.STATUT_A_DEMANDER);
        s3.setPieceLibelleOriginal("Lettre de licenciement");

        when(pieceManquanteStatusRepository.findByCaseFileId(caseFileId))
                .thenReturn(List.of(s1, s2, s3));

        PieceManquanteStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(caseFileId);

        assertThat(snapshot.obtenues()).containsExactly("Contrat de travail");
        assertThat(snapshot.nonApplicables()).containsExactly("Convention BTP");
        assertThat(snapshot.aDemander()).containsExactly("Lettre de licenciement");
    }

    @Test
    void collectForEnrichment_empty_returnsEmptySnapshot() {
        when(pieceManquanteStatusRepository.findByCaseFileId(caseFileId)).thenReturn(List.of());

        PieceManquanteStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(caseFileId);

        assertThat(snapshot.obtenues()).isEmpty();
        assertThat(snapshot.nonApplicables()).isEmpty();
        assertThat(snapshot.aDemander()).isEmpty();
    }

    @Test
    void collectForEnrichment_repoThrows_failsOpenWithEmpty() {
        when(pieceManquanteStatusRepository.findByCaseFileId(caseFileId))
                .thenThrow(new RuntimeException("DB down"));

        PieceManquanteStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(caseFileId);

        assertThat(snapshot.obtenues()).isEmpty();
        assertThat(snapshot.nonApplicables()).isEmpty();
        assertThat(snapshot.aDemander()).isEmpty();
    }

    @Test
    void collectForEnrichment_nullCaseFileId_returnsEmpty() {
        PieceManquanteStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(null);
        assertThat(snapshot.obtenues()).isEmpty();
    }

    private static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
