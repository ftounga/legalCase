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
 * F-195 SF-195-01 — UT du service {@link RisqueStatusService}.
 *
 * <p>Pattern miroir {@link PieceManquanteStatusServiceTest} (F-194).</p>
 *
 * <ul>
 *   <li>upsert idempotent (UNIQUE clé)</li>
 *   <li>normalisation libellé (trim/lowercase)</li>
 *   <li>validation entrée (statut, libellé, raison_ecarte)</li>
 *   <li>isolation workspace 404</li>
 *   <li>collectForEnrichment trichotomie (avec raisons d'écarté)</li>
 * </ul>
 */
class RisqueStatusServiceTest {

    private RisqueStatusRepository risqueStatusRepository;
    private CaseFileRepository caseFileRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;
    private RisqueStatusService service;

    private User user;
    private Workspace workspace;
    private WorkspaceMember member;
    private CaseFile caseFile;
    private UUID caseFileId;

    @BeforeEach
    void setUp() {
        risqueStatusRepository = mock(RisqueStatusRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);

        service = new RisqueStatusService(
                risqueStatusRepository, caseFileRepository,
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
    void upsertStatus_newRisque_createsEntry() {
        when(risqueStatusRepository.findByCaseFileIdAndRisqueLibelleNormalise(
                caseFileId, "harcèlement moral subi"))
                .thenReturn(Optional.empty());
        when(risqueStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RisqueStatus saved = service.upsertStatus(caseFileId,
                "Harcèlement moral subi", RisqueStatus.STATUT_VALIDE,
                null, null, mock(Principal.class));

        assertThat(saved.getStatut()).isEqualTo(RisqueStatus.STATUT_VALIDE);
        assertThat(saved.getRisqueLibelleNormalise()).isEqualTo("harcèlement moral subi");
        assertThat(saved.getRisqueLibelleOriginal()).isEqualTo("Harcèlement moral subi");
        assertThat(saved.getRaisonEcarte()).isNull();
        verify(risqueStatusRepository, times(1)).save(any());
    }

    @Test
    void upsertStatus_existingRisque_updatesEntry() {
        RisqueStatus existing = new RisqueStatus();
        existing.setCaseFile(caseFile);
        existing.setWorkspace(workspace);
        existing.setRisqueLibelleNormalise("harcèlement moral");
        existing.setRisqueLibelleOriginal("Harcèlement moral");
        existing.setStatut(RisqueStatus.STATUT_A_CREUSER);
        when(risqueStatusRepository.findByCaseFileIdAndRisqueLibelleNormalise(
                caseFileId, "harcèlement moral"))
                .thenReturn(Optional.of(existing));
        when(risqueStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RisqueStatus saved = service.upsertStatus(caseFileId,
                "Harcèlement moral", RisqueStatus.STATUT_VALIDE,
                null, null, mock(Principal.class));

        assertThat(saved.getStatut()).isEqualTo(RisqueStatus.STATUT_VALIDE);
        verify(risqueStatusRepository, times(1)).save(any());
    }

    @Test
    void upsertStatus_normalizationStrategy_trimAndLowercase() {
        when(risqueStatusRepository.findByCaseFileIdAndRisqueLibelleNormalise(any(), any()))
                .thenReturn(Optional.empty());
        when(risqueStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RisqueStatus s = service.upsertStatus(caseFileId,
                "  HARCELEMENT Moral  ", RisqueStatus.STATUT_VALIDE,
                null, null, mock(Principal.class));
        assertThat(s.getRisqueLibelleNormalise()).isEqualTo("harcelement moral");
        // Le libellé original conserve la casse mais est trimmé
        assertThat(s.getRisqueLibelleOriginal()).isEqualTo("HARCELEMENT Moral");
    }

    @Test
    void upsertStatus_invalidStatut_throws400() {
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Risque",
                "INVALIDE", null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Statut invalide");
        verify(risqueStatusRepository, never()).save(any());
    }

    @Test
    void upsertStatus_blankLibelle_throws400() {
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "   ",
                RisqueStatus.STATUT_VALIDE, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("requis");
    }

    @Test
    void upsertStatus_libelleTooLong_throws400() {
        String tooLong = "x".repeat(501);
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, tooLong,
                RisqueStatus.STATUT_VALIDE, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("trop long");
    }

    @Test
    void upsertStatus_raisonOnEcarteStatut_accepted() {
        when(risqueStatusRepository.findByCaseFileIdAndRisqueLibelleNormalise(any(), any()))
                .thenReturn(Optional.empty());
        when(risqueStatusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RisqueStatus saved = service.upsertStatus(caseFileId,
                "Clause non-concurrence abusive", RisqueStatus.STATUT_ECARTE,
                "Délai déjà expiré", null, mock(Principal.class));
        assertThat(saved.getRaisonEcarte()).isEqualTo("Délai déjà expiré");
    }

    @Test
    void upsertStatus_raisonOnNonEcarteStatut_throws400() {
        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Risque",
                RisqueStatus.STATUT_VALIDE, "raison interdite", null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("raisonEcarte");
    }

    @Test
    void upsertStatus_caseFileInOtherWorkspace_throws404() {
        Workspace other = new Workspace();
        setField(other, "id", UUID.randomUUID());
        caseFile.setWorkspace(other);

        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Risque",
                RisqueStatus.STATUT_VALIDE, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    @Test
    void upsertStatus_caseFileNotFound_throws404() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Risque",
                RisqueStatus.STATUT_VALIDE, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    @Test
    void upsertStatus_workspaceNotFound_throws404() {
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsertStatus(caseFileId, "Risque",
                RisqueStatus.STATUT_VALIDE, null, null, mock(Principal.class)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workspace not found");
    }

    @Test
    void collectForEnrichment_trichotomy_returnsThreeLists() {
        RisqueStatus s1 = new RisqueStatus();
        s1.setStatut(RisqueStatus.STATUT_VALIDE);
        s1.setRisqueLibelleOriginal("Harcèlement moral");
        RisqueStatus s2 = new RisqueStatus();
        s2.setStatut(RisqueStatus.STATUT_ECARTE);
        s2.setRisqueLibelleOriginal("Clause non-concurrence abusive");
        s2.setRaisonEcarte("Délai expiré");
        RisqueStatus s3 = new RisqueStatus();
        s3.setStatut(RisqueStatus.STATUT_A_CREUSER);
        s3.setRisqueLibelleOriginal("Discrimination");

        when(risqueStatusRepository.findByCaseFileId(caseFileId))
                .thenReturn(List.of(s1, s2, s3));

        RisqueStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(caseFileId);

        assertThat(snapshot.valides()).containsExactly("Harcèlement moral");
        assertThat(snapshot.ecartes()).hasSize(1);
        assertThat(snapshot.ecartes().get(0).libelle()).isEqualTo("Clause non-concurrence abusive");
        assertThat(snapshot.ecartes().get(0).raison()).isEqualTo("Délai expiré");
        assertThat(snapshot.aCreuser()).containsExactly("Discrimination");
    }

    @Test
    void collectForEnrichment_empty_returnsEmptySnapshot() {
        when(risqueStatusRepository.findByCaseFileId(caseFileId)).thenReturn(List.of());

        RisqueStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(caseFileId);

        assertThat(snapshot.valides()).isEmpty();
        assertThat(snapshot.ecartes()).isEmpty();
        assertThat(snapshot.aCreuser()).isEmpty();
    }

    @Test
    void collectForEnrichment_repoThrows_failsOpenWithEmpty() {
        when(risqueStatusRepository.findByCaseFileId(caseFileId))
                .thenThrow(new RuntimeException("DB down"));

        RisqueStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(caseFileId);

        assertThat(snapshot.valides()).isEmpty();
        assertThat(snapshot.ecartes()).isEmpty();
        assertThat(snapshot.aCreuser()).isEmpty();
    }

    @Test
    void collectForEnrichment_nullCaseFileId_returnsEmpty() {
        RisqueStatusService.EnrichmentSnapshot snapshot =
                service.collectForEnrichment(null);
        assertThat(snapshot.valides()).isEmpty();
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
