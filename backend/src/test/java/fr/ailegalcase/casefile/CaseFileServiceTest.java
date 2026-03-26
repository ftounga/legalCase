package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.PAYMENT_REQUIRED;

@ExtendWith(MockitoExtension.class)
class CaseFileServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private PlanLimitService planLimitService;
    @Mock private OidcUser oidcUser;

    private CaseFileService service;

    @BeforeEach
    void setUp() {
        service = new CaseFileService(caseFileRepository, currentUserResolver, workspaceMemberRepository, planLimitService);
    }

    private Workspace mockUserAndWorkspace() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        workspace.setName("test");
        workspace.setSlug("slug");
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
       workspace.setLegalDomain("DROIT_DU_TRAVAIL");


        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        lenient().when(caseFileRepository.save(any(CaseFile.class))).thenAnswer(inv -> inv.getArgument(0));
        return workspace;
    }

    // U-01 : création valide → CaseFileResponse retourné
    @Test
    void create_validRequest_returnsCaseFileResponse() {
        mockUserAndWorkspace();
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(any(UUID.class), eq("OPEN"))).thenReturn(0L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(any(UUID.class))).thenReturn(3);
        CaseFileRequest request = new CaseFileRequest("Licenciement Dupont", "Description");

        CaseFileResponse response = service.create(request, oidcUser, "GOOGLE", null);

        assertThat(response.title()).isEqualTo("Licenciement Dupont");
        assertThat(response.legalDomain()).isEqualTo("DROIT_DU_TRAVAIL");
        assertThat(response.status()).isEqualTo("OPEN");
    }

    // U-02 : workspace avec domaine non supporté → 400
    @Test
    void create_unsupportedWorkspaceDomain_throws400() {
        Workspace workspace = mockUserAndWorkspace();
        workspace.setLegalDomain("DROIT_IMMIGRATION");
        CaseFileRequest request = new CaseFileRequest("Title", null);

        assertThatThrownBy(() -> service.create(request, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(BAD_REQUEST));
    }

    // U-03 : title trimmé avant persistance
    @Test
    void create_titleWithSpaces_isTrimmed() {
        mockUserAndWorkspace();
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(any(UUID.class), eq("OPEN"))).thenReturn(0L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(any(UUID.class))).thenReturn(3);
        CaseFileRequest request = new CaseFileRequest("  Titre avec espaces  ", null);

        CaseFileResponse response = service.create(request, oidcUser, "GOOGLE", null);

        assertThat(response.title()).isEqualTo("Titre avec espaces");
    }

    // U-04 : quota OPEN atteint (Starter 3/3) → 402
    @Test
    void create_quotaReached_throws402() {
        mockUserAndWorkspace();
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(any(UUID.class), eq("OPEN"))).thenReturn(3L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(any(UUID.class))).thenReturn(3);
        CaseFileRequest request = new CaseFileRequest("Nouveau dossier", null);

        assertThatThrownBy(() -> service.create(request, oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(PAYMENT_REQUIRED));
    }

    // U-05 : quota non atteint (Starter 2/3) → création autorisée
    @Test
    void create_quotaNotReached_succeeds() {
        mockUserAndWorkspace();
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(any(UUID.class), eq("OPEN"))).thenReturn(2L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(any(UUID.class))).thenReturn(3);
        CaseFileRequest request = new CaseFileRequest("Dossier 3", null);

        CaseFileResponse response = service.create(request, oidcUser, "GOOGLE", null);

        assertThat(response).isNotNull();
    }

    // U-06 : pas de subscription → fail open, création autorisée
    @Test
    void create_noSubscription_failOpen() {
        mockUserAndWorkspace();
        when(caseFileRepository.countByWorkspace_IdAndStatusAndDeletedAtIsNull(any(UUID.class), eq("OPEN"))).thenReturn(10L);
        when(planLimitService.getMaxOpenCaseFilesForWorkspace(any(UUID.class))).thenReturn(Integer.MAX_VALUE);
        CaseFileRequest request = new CaseFileRequest("Dossier sans sub", null);

        CaseFileResponse response = service.create(request, oidcUser, "GOOGLE", null);

        assertThat(response).isNotNull();
    }
}
