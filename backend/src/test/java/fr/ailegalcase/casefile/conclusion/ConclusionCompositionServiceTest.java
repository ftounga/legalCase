package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileDashboardService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.casefile.DashboardTile;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-288 / SF-288-01 — tests unitaires de {@link ConclusionCompositionService} :
 * GET sans / avec exclusion, PUT puis GET (item exclu → included=false), dimension
 * inconnue au PUT → 400, isolation workspace.
 */
class ConclusionCompositionServiceTest {

    private final ConclusionCompositionExclusionRepository exclusionRepository =
            mock(ConclusionCompositionExclusionRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final WorkspaceMemberRepository workspaceMemberRepository =
            mock(WorkspaceMemberRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final CaseFileDashboardService dashboardService = mock(CaseFileDashboardService.class);
    private final AdverseMoyenPersistenceService adverseMoyenPersistenceService =
            mock(AdverseMoyenPersistenceService.class);

    private final ConclusionCompositionService service = new ConclusionCompositionService(
            exclusionRepository, caseFileRepository, workspaceMemberRepository,
            currentUserResolver, dashboardService, adverseMoyenPersistenceService);

    private final Principal principal = () -> "google";
    private UUID caseFileId;
    private User user;
    private Workspace workspace;
    private final List<ConclusionCompositionExclusion> store = new ArrayList<>();

    @BeforeEach
    void setUp() {
        caseFileId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        workspace = new Workspace();
        workspace.setId(workspaceId);

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        caseFile.setTitle("Dossier Dupont");
        caseFile.setWorkspace(workspace);

        user = new User();

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setPrimary(true);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId))
                .thenReturn(Optional.of(caseFile));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(member));

        when(dashboardService.assembleDecisionToolTiles(caseFileId)).thenReturn(List.of(
                new DashboardTile("F-DT-08", "VALIDITE", "Validité du licenciement",
                        "Sans cause", "2/7", "ALERT"),
                new DashboardTile("F-DT-09", "INDEMNITES", "Comparateur d'indemnités",
                        "18 000 €", null, "OK")));

        // Persistance simulée en mémoire, dimension-aware (DECISION_TOOL + ADVERSE_MOYEN).
        when(exclusionRepository.findByCaseFileIdAndDimension(eq(caseFileId), any()))
                .thenAnswer(inv -> {
                    String dim = inv.getArgument(1);
                    List<ConclusionCompositionExclusion> matching = new ArrayList<>();
                    for (ConclusionCompositionExclusion e : store) {
                        if (e.getDimension().equals(dim)) {
                            matching.add(e);
                        }
                    }
                    return matching;
                });
        when(exclusionRepository.save(any())).thenAnswer(inv -> {
            store.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        // deleteByCaseFileIdAndDimension retire UNIQUEMENT les lignes de la dimension visée.
        org.mockito.Mockito.doAnswer(inv -> {
            String dim = inv.getArgument(1);
            store.removeIf(e -> e.getDimension().equals(dim));
            return null;
        }).when(exclusionRepository).deleteByCaseFileIdAndDimension(eq(caseFileId), any());

        // Par défaut : aucun moyen adverse persisté (dimension ADVERSE_MOYEN omise).
        when(adverseMoyenPersistenceService.findPersisted(any())).thenReturn(List.of());
    }

    @Test
    void getComposition_noExclusion_allIncluded() {
        CompositionResponse response = service.getComposition(caseFileId, null, principal);

        assertThat(response.dimensions()).hasSize(1);
        CompositionResponse.Dimension dim = response.dimensions().get(0);
        assertThat(dim.key()).isEqualTo("DECISION_TOOL");
        assertThat(dim.items()).hasSize(2);
        assertThat(dim.items()).allMatch(CompositionResponse.Item::included);
    }

    @Test
    void putThenGet_excludedItemReturnsIncludedFalse_othersTrue() {
        CompositionUpdateRequest request = new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-08")));

        CompositionResponse afterPut = service.putComposition(caseFileId, request, null, principal);

        CompositionResponse.Item dt08 = itemByKey(afterPut, "F-DT-08");
        CompositionResponse.Item dt09 = itemByKey(afterPut, "F-DT-09");
        assertThat(dt08.included()).isFalse();
        assertThat(dt09.included()).isTrue();

        // GET reflète la persistance.
        CompositionResponse afterGet = service.getComposition(caseFileId, null, principal);
        assertThat(itemByKey(afterGet, "F-DT-08").included()).isFalse();
        assertThat(itemByKey(afterGet, "F-DT-09").included()).isTrue();
    }

    @Test
    void put_replacesPreviousExclusions() {
        // 1er PUT exclut F-DT-08.
        service.putComposition(caseFileId, new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-08"))),
                null, principal);
        // 2e PUT porte de nouveau DECISION_TOOL (cette fois en réinclut F-DT-09 seul → F-DT-08 ré-inclus).
        CompositionResponse response = service.putComposition(
                caseFileId, new CompositionUpdateRequest(List.of(
                        new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-09"))),
                null, principal);

        assertThat(itemByKey(response, "F-DT-08").included()).isTrue();
        assertThat(itemByKey(response, "F-DT-09").included()).isFalse();
        // delete appelé à chaque PUT portant DECISION_TOOL (remplacement de cette dimension).
        verify(exclusionRepository, times(2))
                .deleteByCaseFileIdAndDimension(caseFileId, "DECISION_TOOL");
    }

    @Test
    void put_emptyBody_touchesNoDimension() {
        // Body vide → aucune dimension portée → aucun delete (le PUT n'efface rien).
        service.putComposition(caseFileId, new CompositionUpdateRequest(List.of()), null, principal);

        verify(exclusionRepository, org.mockito.Mockito.never())
                .deleteByCaseFileIdAndDimension(eq(caseFileId), any());
    }

    @Test
    void put_unknownDimension_throws400() {
        CompositionUpdateRequest request = new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("UNKNOWN_DIM", "x")));

        assertThatThrownBy(() -> service.putComposition(caseFileId, request, null, principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    @Test
    void put_unknownItemKey_tolerated() {
        // toolId non calculé → persisté mais sans effet sur les items calculés (no-op).
        CompositionUpdateRequest request = new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-INEXISTANT")));

        CompositionResponse response = service.putComposition(caseFileId, request, null, principal);
        assertThat(response.dimensions().get(0).items()).allMatch(CompositionResponse.Item::included);
    }

    @Test
    void getComposition_foreignWorkspace_throws404() {
        Workspace other = new Workspace();
        other.setId(UUID.randomUUID());
        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(other);
        otherMember.setUser(user);
        otherMember.setPrimary(true);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(otherMember));

        // 404 non-leak (cohérent avec la famille /conclusions), pas 403.
        assertThatThrownBy(() -> service.getComposition(caseFileId, null, principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void getComposition_caseFileNotFound_throws404() {
        when(caseFileRepository.findByIdAndDeletedAtIsNull(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getComposition(UUID.randomUUID(), null, principal))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ── SF-288-03 — dimension ADVERSE_MOYEN ─────────────────────────────────────

    @Test
    void getComposition_withMoyens_returnsTwoDimensions() {
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(eq(caseFileId)))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen("Le licenciement repose sur une faute grave.",
                                List.of("art. L. 1234-1"), List.of("Lettre")))));

        CompositionResponse response = service.getComposition(caseFileId, null, principal);

        assertThat(response.dimensions()).hasSize(2);
        assertThat(response.dimensions().get(0).key()).isEqualTo("DECISION_TOOL");
        CompositionResponse.Dimension moyens = response.dimensions().get(1);
        assertThat(moyens.key()).isEqualTo("ADVERSE_MOYEN");
        assertThat(moyens.label()).isEqualTo("Moyens adverses");
        assertThat(moyens.items()).hasSize(1);
        assertThat(moyens.items().get(0).key()).isEqualTo(moyenId.toString());
        assertThat(moyens.items().get(0).included()).isTrue();
    }

    @Test
    void getComposition_noMoyen_omitsAdverseMoyenDimension() {
        // findPersisted renvoie vide (stub par défaut) → dimension ADVERSE_MOYEN omise.
        CompositionResponse response = service.getComposition(caseFileId, null, principal);

        assertThat(response.dimensions()).hasSize(1);
        assertThat(response.dimensions().get(0).key()).isEqualTo("DECISION_TOOL");
    }

    @Test
    void getComposition_longThese_isTruncatedTo80WithEllipsis() {
        String longThese = "A".repeat(200);
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(eq(caseFileId)))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen(longThese, List.of(), List.of()))));

        CompositionResponse response = service.getComposition(caseFileId, null, principal);

        String label = response.dimensions().get(1).items().get(0).label();
        assertThat(label).hasSize(81); // 80 caractères + « … »
        assertThat(label).endsWith("…");
    }

    @Test
    void putThenGet_adverseMoyenExcluded_includedFalse() {
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(eq(caseFileId)))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen("Faute grave.", List.of(), List.of()))));

        CompositionResponse afterPut = service.putComposition(caseFileId,
                new CompositionUpdateRequest(List.of(
                        new CompositionUpdateRequest.ExclusionEntry("ADVERSE_MOYEN", moyenId.toString()))),
                null, principal);

        CompositionResponse.Dimension moyens = afterPut.dimensions().get(1);
        assertThat(moyens.items().get(0).included()).isFalse();

        // GET reflète la persistance.
        CompositionResponse afterGet = service.getComposition(caseFileId, null, principal);
        assertThat(afterGet.dimensions().get(1).items().get(0).included()).isFalse();
    }

    @Test
    void put_decisionToolOnly_doesNotEraseAdverseMoyenExclusions() {
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(eq(caseFileId)))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen("Faute grave.", List.of(), List.of()))));

        // 1) exclure un moyen.
        service.putComposition(caseFileId, new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("ADVERSE_MOYEN", moyenId.toString()))),
                null, principal);
        // 2) PUT outils SEULS (ne porte que DECISION_TOOL).
        service.putComposition(caseFileId, new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-08"))),
                null, principal);

        // L'exclusion du moyen DOIT survivre.
        CompositionResponse response = service.getComposition(caseFileId, null, principal);
        assertThat(response.dimensions().get(1).items().get(0).included()).isFalse();
        assertThat(itemByKey(response, "F-DT-08").included()).isFalse();
        // delete ADVERSE_MOYEN appelé UNE seule fois (au 1er PUT), pas au PUT outils.
        verify(exclusionRepository, times(1))
                .deleteByCaseFileIdAndDimension(caseFileId, "ADVERSE_MOYEN");
    }

    @Test
    void put_adverseMoyenOnly_doesNotEraseDecisionToolExclusions() {
        UUID moyenId = UUID.randomUUID();
        when(adverseMoyenPersistenceService.findPersisted(eq(caseFileId)))
                .thenReturn(List.of(new AdverseMoyenWithId(moyenId,
                        new AdverseMoyen("Faute grave.", List.of(), List.of()))));

        // 1) exclure un outil.
        service.putComposition(caseFileId, new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("DECISION_TOOL", "F-DT-08"))),
                null, principal);
        // 2) PUT moyens SEULS.
        service.putComposition(caseFileId, new CompositionUpdateRequest(List.of(
                new CompositionUpdateRequest.ExclusionEntry("ADVERSE_MOYEN", moyenId.toString()))),
                null, principal);

        CompositionResponse response = service.getComposition(caseFileId, null, principal);
        assertThat(itemByKey(response, "F-DT-08").included()).isFalse();
        assertThat(response.dimensions().get(1).items().get(0).included()).isFalse();
        verify(exclusionRepository, times(1))
                .deleteByCaseFileIdAndDimension(caseFileId, "DECISION_TOOL");
    }

    private static CompositionResponse.Item itemByKey(CompositionResponse response, String key) {
        return response.dimensions().get(0).items().stream()
                .filter(i -> i.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
