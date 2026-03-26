package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;

class CaseFileStatsServiceTest {

    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final UsageEventRepository usageEventRepository = mock(UsageEventRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);
    private final WorkspaceMemberRepository workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
    private final OidcUser oidcUser = mock(OidcUser.class);

    private final CaseFileStatsService service = new CaseFileStatsService(
            caseFileRepository, documentRepository, caseAnalysisRepository,
            usageEventRepository, currentUserResolver, workspaceMemberRepository);

    // U-01 : cas nominal — retourne les 3 métriques agrégées
    @Test
    void getStats_nominal_returnsAggregatedMetrics() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);

        setupWorkspaceResolution(workspace);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(3L);
        when(caseAnalysisRepository.countByCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE)).thenReturn(2L);
        when(usageEventRepository.sumTokensByCaseFileId(caseFileId)).thenReturn(12540L);

        CaseFileStatsResponse response = service.getStats(caseFileId, oidcUser, "GOOGLE", null);

        assertThat(response.documentCount()).isEqualTo(3);
        assertThat(response.analysisCount()).isEqualTo(2);
        assertThat(response.totalTokens()).isEqualTo(12540);
    }

    // U-02 : pas d'usage events → totalTokens = 0
    @Test
    void getStats_noUsageEvents_totalTokensIsZero() {
        UUID caseFileId = UUID.randomUUID();
        Workspace workspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, workspace);

        setupWorkspaceResolution(workspace);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(caseAnalysisRepository.countByCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE)).thenReturn(0L);
        when(usageEventRepository.sumTokensByCaseFileId(caseFileId)).thenReturn(0L);

        CaseFileStatsResponse response = service.getStats(caseFileId, oidcUser, "GOOGLE", null);

        assertThat(response.totalTokens()).isEqualTo(0);
        assertThat(response.analysisCount()).isEqualTo(0);
    }

    // U-03 : dossier inexistant → 404
    @Test
    void getStats_unknownCaseFile_throws404() {
        UUID caseFileId = UUID.randomUUID();
        setupWorkspaceResolution(workspace());
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStats(caseFileId, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // U-04 : dossier appartenant à un autre workspace → 404 opaque
    @Test
    void getStats_differentWorkspace_throws404() {
        UUID caseFileId = UUID.randomUUID();
        Workspace userWorkspace = workspace();
        Workspace otherWorkspace = workspace();
        CaseFile caseFile = caseFile(caseFileId, otherWorkspace);

        setupWorkspaceResolution(userWorkspace);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)).thenReturn(Optional.of(caseFile));

        assertThatThrownBy(() -> service.getStats(caseFileId, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(NOT_FOUND));
    }

    // Helpers
    private Workspace workspace() {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        return w;
    }

    private CaseFile caseFile(UUID id, Workspace workspace) {
        CaseFile cf = new CaseFile();
        cf.setId(id);
        cf.setWorkspace(workspace);
        return cf;
    }

    private void setupWorkspaceResolution(Workspace workspace) {
        User user = new User();
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
    }
}
