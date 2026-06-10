package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * F-262 SF-262-01 — tests unitaires du service de détection des chefs de demande.
 * Mocke {@link DecisionToolVisibilityService} (applicabilité) et
 * {@link CaseFileDashboardService} (statut addressed).
 */
@ExtendWith(MockitoExtension.class)
class HeadsOfClaimServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private DecisionToolVisibilityService visibilityService;
    @Mock private CaseFileDashboardService dashboardService;

    private HeadsOfClaimService service;

    private static final UUID WS_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID OTHER_WS_ID = UUID.fromString("00000000-0000-0000-0000-000000000099");
    private static final UUID CF_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    private static final String TOOL_COMPARATEUR = "F-DT-09-comparateur-indemnites";
    private static final String TOOL_PREAVIS = "F-DT-25-indemnite-preavis";

    @BeforeEach
    void setUp() {
        service = new HeadsOfClaimService(
                caseFileRepository, workspaceMemberRepository, currentUserResolver,
                visibilityService, dashboardService);
    }

    // ────────────────────────── nominal travail FR ──────────────────────────

    @Test
    void travailFr_outilVisibleEtCalcule_chefApplicableEtAddressed() {
        mockUserAndCaseFile("DROIT_DU_TRAVAIL", "FRANCE", WS_ID);
        // comparateur visible (alwaysOn) + calculé ; préavis visible mais non calculé.
        when(visibilityService.resolveVisibleTools(any(), any(), any()))
                .thenReturn(new VisibleToolSetResponse(
                        List.of(TOOL_COMPARATEUR), List.of(TOOL_PREAVIS), List.of()));
        when(dashboardService.assembleDecisionToolTiles(CF_ID))
                .thenReturn(List.of(tile(TOOL_COMPARATEUR)));

        HeadsOfClaimResponse r = service.detect(CF_ID, null, null);

        var comparateur = r.heads().stream()
                .filter(h -> TOOL_COMPARATEUR.equals(h.toolId())).findFirst().orElseThrow();
        assertThat(comparateur.category()).isEqualTo("INDEMNITAIRE_OUTILLE");
        assertThat(comparateur.addressed()).isTrue();

        var preavis = r.heads().stream()
                .filter(h -> TOOL_PREAVIS.equals(h.toolId())).findFirst().orElseThrow();
        assertThat(preavis.addressed()).isFalse();
    }

    @Test
    void travailFr_outilNonVisible_chefExclu() {
        mockUserAndCaseFile("DROIT_DU_TRAVAIL", "FRANCE", WS_ID);
        // Aucun outil visible.
        when(visibilityService.resolveVisibleTools(any(), any(), any()))
                .thenReturn(new VisibleToolSetResponse(List.of(), List.of(), List.of()));
        when(dashboardService.assembleDecisionToolTiles(CF_ID)).thenReturn(List.of());

        HeadsOfClaimResponse r = service.detect(CF_ID, null, null);

        // Aucun chef outillé ne doit apparaître.
        assertThat(r.heads()).noneMatch(h -> "INDEMNITAIRE_OUTILLE".equals(h.category()));
    }

    @Test
    void travailFr_chefsTransverses_toujoursApplicablesEtNonAddressed() {
        mockUserAndCaseFile("DROIT_DU_TRAVAIL", "FRANCE", WS_ID);
        when(visibilityService.resolveVisibleTools(any(), any(), any()))
                .thenReturn(new VisibleToolSetResponse(List.of(), List.of(), List.of()));
        when(dashboardService.assembleDecisionToolTiles(CF_ID)).thenReturn(List.of());

        HeadsOfClaimResponse r = service.detect(CF_ID, null, null);

        var transverses = r.heads().stream()
                .filter(h -> "TRANSVERSE".equals(h.category())).toList();
        // Les 5 chefs transverses sont toujours présents, addressed=false, toolId=null.
        assertThat(transverses).hasSize(5);
        assertThat(transverses).allMatch(h -> !h.addressed());
        assertThat(transverses).allMatch(h -> h.toolId() == null);
        assertThat(transverses).extracting(HeadsOfClaimResponse.HeadItem::code)
                .contains("article-700-cpc", "depens", "interets-taux-legal",
                        "capitalisation-interets", "execution-provisoire");
    }

    // ────────────────────────── domaine non couvert ──────────────────────────

    @Test
    void domaineNonTravailFr_listeVide() {
        mockUserAndCaseFile("DROIT_DES_ETRANGERS", "FRANCE", WS_ID);
        // Pas de résolution visibilité/dashboard attendue (court-circuit catalogue vide).

        HeadsOfClaimResponse r = service.detect(CF_ID, null, null);

        assertThat(r.heads()).isEmpty();
    }

    @Test
    void travailBe_listeVide() {
        mockUserAndCaseFile("DROIT_DU_TRAVAIL", "BELGIQUE", WS_ID);

        HeadsOfClaimResponse r = service.detect(CF_ID, null, null);

        assertThat(r.heads()).isEmpty();
    }

    // ────────────────────────── isolation workspace ──────────────────────────

    @Test
    void dossierAutreWorkspace_404() {
        // Dossier appartient à OTHER_WS_ID, utilisateur rattaché à WS_ID.
        mockUserAndCaseFile("DROIT_DU_TRAVAIL", "FRANCE", OTHER_WS_ID);

        assertThatThrownBy(() -> service.detect(CF_ID, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void dossierIntrouvable_404() {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.detect(CF_ID, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    // ────────────────────────────── helpers ─────────────────────────────────

    private void mockUserAndCaseFile(String legalDomain, String country, UUID caseFileWorkspaceId) {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);

        Workspace cfWs = new Workspace();
        cfWs.setId(caseFileWorkspaceId);
        cfWs.setLegalDomain(legalDomain);
        cfWs.setCountry(country);

        CaseFile cf = new CaseFile();
        cf.setId(CF_ID);
        cf.setWorkspace(cfWs);
        cf.setLegalDomain(legalDomain);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.of(cf));

        // L'utilisateur est rattaché au workspace WS_ID (peut différer de celui du dossier).
        Workspace userWs = new Workspace();
        userWs.setId(WS_ID);
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(userWs);
        lenient().when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(member));
    }

    private static DashboardTile tile(String toolId) {
        return new DashboardTile(toolId, "INDEMNITES", "label", "verdict", null, "OK");
    }
}
