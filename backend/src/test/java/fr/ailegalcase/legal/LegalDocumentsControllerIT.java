package fr.ailegalcase.legal;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.consent.UserConsentAcceptanceRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-240 SF-240-04 — Tests d'intégration de {@link LegalDocumentsController}.
 *
 * <ul>
 *   <li>LD-IT-01 : GET /api/v1/legal/dpa sans auth → 200, Content-Type pdf, Content-Disposition attachment</li>
 *   <li>LD-IT-02 : corps > 1 ko (PDF non vide)</li>
 *   <li>LD-IT-03 : avec auth → 200 + enregistrement DPA_DOWNLOAD en BD</li>
 *   <li>LD-IT-04 : sans workspace → 200 (tracking best-effort, pas de 500)</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class LegalDocumentsControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private UserConsentAcceptanceRepository acceptanceRepository;

    private OAuth2AuthenticationToken authWithWorkspace;
    private OAuth2AuthenticationToken authWithoutWorkspace;

    @BeforeEach
    void setUp() {
        acceptanceRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        // Utilisateur AVEC workspace primary
        User user = new User();
        user.setEmail("dpa-it@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-dpa-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("dpa-it@example.com");
        workspace.setSlug("dpa-ws-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        authWithWorkspace = buildGoogleAuth("google-dpa-sub", "dpa-it@example.com");

        // Utilisateur SANS workspace
        User freshUser = new User();
        freshUser.setEmail("dpa-fresh@example.com");
        freshUser.setStatus("ACTIVE");
        userRepository.save(freshUser);

        AuthAccount freshAccount = new AuthAccount();
        freshAccount.setUser(freshUser);
        freshAccount.setProvider("GOOGLE");
        freshAccount.setProviderUserId("google-dpa-fresh-sub");
        authAccountRepository.save(freshAccount);

        authWithoutWorkspace = buildGoogleAuth("google-dpa-fresh-sub", "dpa-fresh@example.com");
    }

    /** LD-IT-01 : endpoint public — 200, Content-Type PDF, Content-Disposition attachment sans auth. */
    @Test
    void downloadDpa_anonymous_returns200WithPdfHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/legal/dpa"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        org.hamcrest.Matchers.containsString("legalcase-dpa-v1.pdf")));
    }

    /** LD-IT-02 : corps > 1 ko (PDF non vide). */
    @Test
    void downloadDpa_anonymous_bodyExceedsOneKilobyte() throws Exception {
        byte[] body = mockMvc.perform(get("/api/v1/legal/dpa"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray();

        assertThat(body).hasSizeGreaterThan(1024);
    }

    /** LD-IT-03 : avec auth → 200 + enregistrement DPA_DOWNLOAD en BD. */
    @Test
    void downloadDpa_authenticated_tracks200AndPersistsDpaDownload() throws Exception {
        assertThat(acceptanceRepository.count()).isZero();

        mockMvc.perform(get("/api/v1/legal/dpa")
                        .with(authentication(authWithWorkspace)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"));

        assertThat(acceptanceRepository.count()).isEqualTo(1);
        assertThat(acceptanceRepository.findAll())
                .extracting(c -> c.getConsentType())
                .containsExactly("DPA_DOWNLOAD");
    }

    /** LD-IT-04 : avec auth sans workspace → 200 (best-effort, pas de 500). */
    @Test
    void downloadDpa_authenticatedNoWorkspace_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/legal/dpa")
                        .with(authentication(authWithoutWorkspace)))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/pdf"));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
