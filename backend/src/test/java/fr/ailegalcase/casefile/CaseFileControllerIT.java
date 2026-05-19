package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisJob;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.billing.Subscription;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
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
    @Autowired private AnalysisJobRepository analysisJobRepository;
    @Autowired private AuditLogRepository auditLogRepository;

    private OAuth2AuthenticationToken auth;

    @BeforeEach
    void setUp() {
        analysisJobRepository.deleteAll();
        auditLogRepository.deleteAll();
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

    // =========================================================
    // SF-86-01 — Modifier titre et description d'un dossier
    // =========================================================

    // I-30 : PATCH /{id} → 200 avec payload valide
    @Test
    void update_validPayload_returns200() throws Exception {
        String id = createCaseFile("Titre initial");

        mockMvc.perform(patch("/api/v1/case-files/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Titre modifié","description":"Nouvelle description"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titre modifié"))
                .andExpect(jsonPath("$.description").value("Nouvelle description"));
    }

    // I-31 : PATCH /{id} → 400 si titre vide
    @Test
    void update_emptyTitle_returns400() throws Exception {
        String id = createCaseFile("Titre initial");

        mockMvc.perform(patch("/api/v1/case-files/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"","description":"desc"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-32 : PATCH /{id} → 403 si dossier appartient à un autre workspace
    @Test
    void update_differentWorkspace_returns403() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-update@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-update-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-update@example.com");
        otherWorkspace.setSlug("update-other-slug-" + System.currentTimeMillis());
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

        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-other-update-sub", "other-update@example.com");

        // Créer un dossier dans l'autre workspace
        String createResponse = mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dossier autre workspace"}
                                """)
                        .with(authentication(otherAuth)))
                .andReturn().getResponse().getContentAsString();

        String otherId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        // Le premier user essaie de modifier le dossier de l'autre → 403
        mockMvc.perform(patch("/api/v1/case-files/" + otherId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Tentative modification"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // I-33 : PATCH /{id} → 404 si dossier inexistant
    @Test
    void update_unknownId_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Titre quelconque"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // SF-53-01 — Statut des dossiers
    // =========================================================

    // I-15 : PATCH /{id}/close → 200, status CLOSED
    @Test
    void close_openCaseFile_returns200AndStatusClosed() throws Exception {
        String id = createCaseFile("Dossier à clôturer");

        mockMvc.perform(patch("/api/v1/case-files/" + id + "/close")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    // I-16 : PATCH /{id}/reopen → 200, status OPEN
    @Test
    void reopen_closedCaseFile_returns200AndStatusOpen() throws Exception {
        String id = createCaseFile("Dossier à réouvrir");
        mockMvc.perform(patch("/api/v1/case-files/" + id + "/close").with(authentication(auth)));

        mockMvc.perform(patch("/api/v1/case-files/" + id + "/reopen")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    // I-17 : PATCH /{id}/reopen → 402 si quota atteint
    @Test
    void reopen_quotaFull_returns402() throws Exception {
        // Plan FREE = 2 dossiers ouverts max (cf. PlanLimitService.FREE_MAX_OPEN_CASE_FILES).
        Workspace workspace = workspaceMemberRepository.findAll().stream()
                .filter(WorkspaceMember::isPrimary).findFirst().orElseThrow().getWorkspace();
        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspace.getId());
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        sub.setStartedAt(Instant.now());
        subscriptionRepository.save(sub);

        // Créer + fermer 1 dossier
        String closedId = createCaseFile("Dossier fermé");
        mockMvc.perform(patch("/api/v1/case-files/" + closedId + "/close").with(authentication(auth)));

        // Remplir le quota (FREE = 2 dossiers ouverts) avec 2 autres dossiers
        createCaseFile("Open 1");
        createCaseFile("Open 2");

        mockMvc.perform(patch("/api/v1/case-files/" + closedId + "/reopen")
                        .with(authentication(auth)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.code").value("CASE_FILE_OPEN_LIMIT_EXCEEDED"));
    }

    // I-18 : PATCH /{id}/reopen → 403 si LAWYER
    @Test
    void reopen_byLawyer_returns403() throws Exception {
        String id = createCaseFile("Dossier test");
        mockMvc.perform(patch("/api/v1/case-files/" + id + "/close").with(authentication(auth)));

        OAuth2AuthenticationToken lawyerAuth = createMemberAuth("lawyer-reopen@example.com",
                "google-lawyer-reopen", "LAWYER");

        mockMvc.perform(patch("/api/v1/case-files/" + id + "/reopen")
                        .with(authentication(lawyerAuth)))
                .andExpect(status().isForbidden());
    }

    // I-19 : DELETE → 204, deleted_at non null en base
    @Test
    void delete_byCaseFileOwner_returns204AndSetsDeletedAt() throws Exception {
        String id = createCaseFile("Dossier à supprimer");

        mockMvc.perform(delete("/api/v1/case-files/" + id)
                        .with(authentication(auth)))
                .andExpect(status().isNoContent());

        CaseFile cf = caseFileRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(cf.getDeletedAt()).isNotNull();
    }

    // I-20 : DELETE → 403 si ADMIN
    @Test
    void delete_byAdmin_returns403() throws Exception {
        String id = createCaseFile("Dossier test");
        OAuth2AuthenticationToken adminAuth = createMemberAuth("admin-delete@example.com",
                "google-admin-delete", "ADMIN");

        mockMvc.perform(delete("/api/v1/case-files/" + id)
                        .with(authentication(adminAuth)))
                .andExpect(status().isForbidden());
    }

    // I-21 : DELETE → 409 si analyse en cours
    @Test
    void delete_withRunningAnalysis_returns409() throws Exception {
        String id = createCaseFile("Dossier analysé");

        AnalysisJob job = new AnalysisJob();
        job.setCaseFileId(UUID.fromString(id));
        job.setJobType(JobType.CASE_ANALYSIS);
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        job.setProcessedItems(0);
        analysisJobRepository.save(job);

        mockMvc.perform(delete("/api/v1/case-files/" + id)
                        .with(authentication(auth)))
                .andExpect(status().isConflict());
    }

    // I-22 : GET /case-files → dossier supprimé absent de la liste
    @Test
    void list_excludesDeletedCaseFiles() throws Exception {
        String id = createCaseFile("Dossier supprimé");
        createCaseFile("Dossier visible");
        mockMvc.perform(delete("/api/v1/case-files/" + id).with(authentication(auth)));

        mockMvc.perform(get("/api/v1/case-files").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Dossier visible"));
    }

    // I-23 : GET /{id} → 404 si dossier supprimé
    @Test
    void getById_deletedCaseFile_returns404() throws Exception {
        String id = createCaseFile("Dossier supprimé");
        mockMvc.perform(delete("/api/v1/case-files/" + id).with(authentication(auth)));

        mockMvc.perform(get("/api/v1/case-files/" + id).with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // =========================================================
    // SF-87-01 — Export ZIP
    // =========================================================

    // I-24 : GET /{id}/export → 200 + Content-Type application/zip
    @Test
    void export_existingCaseFile_returns200WithZipContentType() throws Exception {
        String id = createCaseFile("Dossier Export Test");

        mockMvc.perform(get("/api/v1/case-files/" + id + "/export")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/zip")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString(".zip")));
    }

    // I-25 : GET /{id}/export → 404 si dossier inexistant
    @Test
    void export_unknownId_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + java.util.UUID.randomUUID() + "/export")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-26 : GET /{id}/export → 403 si dossier appartient à un autre workspace
    @Test
    void export_differentWorkspace_returns403() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("export-other@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-export-other-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("export-other@example.com");
        otherWorkspace.setSlug("export-other-slug-" + System.currentTimeMillis());
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

        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-export-other-sub", "export-other@example.com");

        String createResponse = mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Dossier Autre Export"}
                                """)
                        .with(authentication(otherAuth)))
                .andReturn().getResponse().getContentAsString();

        String otherId = new com.fasterxml.jackson.databind.ObjectMapper()
                .readTree(createResponse).get("id").asText();

        mockMvc.perform(get("/api/v1/case-files/" + otherId + "/export")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // I-27 : GET /{id}/export → 401 sans auth
    @Test
    void export_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + java.util.UUID.randomUUID() + "/export"))
                .andExpect(status().isUnauthorized());
    }

    private String createCaseFile(String title) throws Exception {
        // andExpect(isCreated) : un quota plein renverrait 402 sans champ "id" — on
        // échoue ici explicitement plutôt que sur un NullPointerException opaque.
        String response = mockMvc.perform(post("/api/v1/case-files")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}")
                        .with(authentication(auth)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new com.fasterxml.jackson.databind.ObjectMapper().readTree(response).get("id").asText();
    }

    private OAuth2AuthenticationToken createMemberAuth(String email, String sub, String role) {
        User memberUser = new User();
        memberUser.setEmail(email);
        memberUser.setStatus("ACTIVE");
        userRepository.save(memberUser);

        AuthAccount account = new AuthAccount();
        account.setUser(memberUser);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);

        Workspace workspace = workspaceMemberRepository.findAll().stream()
                .filter(WorkspaceMember::isPrimary)
                .filter(m -> "OWNER".equals(m.getMemberRole()))
                .findFirst().orElseThrow().getWorkspace();

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(memberUser);
        member.setMemberRole(role);
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        return buildGoogleAuth(sub, email);
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
