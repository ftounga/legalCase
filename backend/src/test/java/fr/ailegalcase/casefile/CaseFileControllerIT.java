package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class CaseFileControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;

    private OAuth2AuthenticationToken auth;

    @BeforeEach
    void setUp() {
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setEmail("casefile-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-cf-sub");
        authAccountRepository.save(account);

        Workspace workspace = new Workspace();
        workspace.setName("casefile-test@example.com");
        workspace.setSlug("cf-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        auth = buildGoogleAuth("google-cf-sub", "casefile-test@example.com");
    }

    // I-01 : POST avec payload valide → 201
    @Test
    void create_validPayload_returns201() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Licenciement Dupont","description":"Test"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Licenciement Dupont"))
                .andExpect(jsonPath("$.legalDomain").value("DROIT_DU_TRAVAIL"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    // I-02 : title absent → 400
    @Test
    void create_missingTitle_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title is required"));
    }

    // I-04 : sans auth → 401
    @Test
    void create_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Test"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // I-08 : GET /{id} → 200 avec le dossier
    @Test
    void getById_existingCaseFile_returns200() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dossier GetById"}
                                """)
                        .with(authentication(auth)))
                .andReturn().getResponse().getContentAsString();

        String id = new com.fasterxml.jackson.databind.ObjectMapper().readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/v1/case-files/" + id)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Dossier GetById"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // I-09 : GET /{id} inconnu → 404
    @Test
    void getById_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + java.util.UUID.randomUUID())
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-10 : GET /{id} sans auth → 401
    @Test
    void getById_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + java.util.UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : GET liste vide → 200 + page vide
    @Test
    void list_emptyWorkspace_returns200WithEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/case-files")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // I-06 : GET liste avec dossiers → retourne les dossiers du workspace
    @Test
    void list_withCaseFiles_returnsItems() throws Exception {
        mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dossier A","description":"Desc"}
                                """)
                        .with(authentication(auth)));

        mockMvc.perform(get("/api/v1/case-files")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Dossier A"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"));
    }

    // I-07 : GET sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files"))
                .andExpect(status().isUnauthorized());
    }

    // I-11 : GET /{id}/stats → 200 avec métriques à zéro (dossier vide)
    @Test
    void getStats_emptyCaseFile_returns200WithZeroMetrics() throws Exception {
        String createResponse = mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dossier Stats"}
                                """)
                        .with(authentication(auth)))
                .andReturn().getResponse().getContentAsString();

        String id = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/v1/case-files/" + id + "/stats")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentCount").value(0))
                .andExpect(jsonPath("$.analysisCount").value(0))
                .andExpect(jsonPath("$.totalTokens").value(0));
    }

    // I-12 : GET /{id}/stats → 404 si dossier inexistant
    @Test
    void getStats_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + java.util.UUID.randomUUID() + "/stats")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-13 : GET /{id}/stats → 401 sans auth
    @Test
    void getStats_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + java.util.UUID.randomUUID() + "/stats"))
                .andExpect(status().isUnauthorized());
    }

    // I-14 : GET /{id}/stats → 404 si dossier appartenant à un autre workspace (isolation)
    @Test
    void getStats_differentWorkspace_returns404() throws Exception {
        // Créer un second utilisateur dans un workspace différent
        User otherUser = new User();
        otherUser.setEmail("other-stats@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-stats-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-stats@example.com");
        otherWorkspace.setSlug("stats-other-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-other-stats-sub", "other-stats@example.com");

        // Créer un dossier dans le workspace du second utilisateur
        String createResponse = mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dossier Autre"}
                                """)
                        .with(authentication(otherAuth)))
                .andReturn().getResponse().getContentAsString();

        String otherId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        // Le premier utilisateur essaie d'accéder aux stats du dossier de l'autre → 404
        mockMvc.perform(get("/api/v1/case-files/" + otherId + "/stats")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

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
