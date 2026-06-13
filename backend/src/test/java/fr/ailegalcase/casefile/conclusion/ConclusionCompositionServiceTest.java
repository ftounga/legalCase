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

    private final ConclusionCompositionService service = new ConclusionCompositionService(
            exclusionRepository, caseFileRepository, workspaceMemberRepository,
            currentUserResolver, dashboardService);

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

        // Persistance simulée en mémoire.
        when(exclusionRepository.findByCaseFileIdAndDimension(eq(caseFileId), eq("DECISION_TOOL")))
                .thenAnswer(inv -> new ArrayList<>(store));
        when(exclusionRepository.save(any())).thenAnswer(inv -> {
            store.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        doAnswerDelete();
    }

    private void doAnswerDelete() {
        // deleteByCaseFileIdAndDimension vide le store.
        org.mockito.Mockito.doAnswer(inv -> {
            store.clear();
            return null;
        }).when(exclusionRepository).deleteByCaseFileIdAndDimension(eq(caseFileId), eq("DECISION_TOOL"));
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
        // 2e PUT (set vide) ré-inclut tout.
        CompositionResponse response = service.putComposition(
                caseFileId, new CompositionUpdateRequest(List.of()), null, principal);

        assertThat(response.dimensions().get(0).items()).allMatch(CompositionResponse.Item::included);
        // delete appelé à chaque PUT (remplacement).
        verify(exclusionRepository, times(2))
                .deleteByCaseFileIdAndDimension(caseFileId, "DECISION_TOOL");
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

    private static CompositionResponse.Item itemByKey(CompositionResponse response, String key) {
        return response.dimensions().get(0).items().stream()
                .filter(i -> i.key().equals(key))
                .findFirst()
                .orElseThrow();
    }
}
