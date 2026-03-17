package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ExtendWith(MockitoExtension.class)
class CaseFileServiceTest {

    @Mock private CaseFileRepository caseFileRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private OidcUser oidcUser;

    private CaseFileService service;

    @BeforeEach
    void setUp() {
        service = new CaseFileService(caseFileRepository, authAccountRepository, workspaceMemberRepository);
    }

    private void mockUserAndWorkspace() {
        User user = new User();
        Workspace workspace = new Workspace();
        workspace.setName("test");
        workspace.setSlug("slug");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");

        AuthAccount account = new AuthAccount();
        account.setUser(user);

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        when(oidcUser.getSubject()).thenReturn("sub-123");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-123"))
                .thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findFirstByUser(user)).thenReturn(Optional.of(member));
        when(caseFileRepository.save(any(CaseFile.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // U-01 : création valide → CaseFileResponse retourné
    @Test
    void create_validRequest_returnsCaseFileResponse() {
        mockUserAndWorkspace();
        CaseFileRequest request = new CaseFileRequest("Licenciement Dupont", "EMPLOYMENT_LAW", "Description");

        CaseFileResponse response = service.create(request, oidcUser, "GOOGLE");

        assertThat(response.title()).isEqualTo("Licenciement Dupont");
        assertThat(response.legalDomain()).isEqualTo("EMPLOYMENT_LAW");
        assertThat(response.status()).isEqualTo("OPEN");
    }

    // U-02 : legalDomain invalide → 400
    @Test
    void create_invalidLegalDomain_throws400() {
        CaseFileRequest request = new CaseFileRequest("Title", "IMMIGRATION_LAW", null);

        assertThatThrownBy(() -> service.create(request, oidcUser, "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(BAD_REQUEST));
    }

    // U-03 : title trimmé avant persistance
    @Test
    void create_titleWithSpaces_isTrimmed() {
        mockUserAndWorkspace();
        CaseFileRequest request = new CaseFileRequest("  Titre avec espaces  ", "EMPLOYMENT_LAW", null);

        CaseFileResponse response = service.create(request, oidcUser, "GOOGLE");

        assertThat(response.title()).isEqualTo("Titre avec espaces");
    }
}
