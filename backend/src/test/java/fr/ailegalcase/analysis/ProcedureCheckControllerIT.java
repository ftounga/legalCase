package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.audit.AuditLogRepository;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ProcedureCheckControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private ProcedureCheckRepository procedureCheckRepository;
    @Autowired private SubscriptionRepository subscriptionRepository;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private OAuth2AuthenticationToken auth;
    private CaseFile caseFile;
    private CaseAnalysis analysis;
    private Workspace workspace;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");
        procedureCheckRepository.deleteAll();
        caseAnalysisRepository.deleteAll();
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        auditLogRepository.deleteAll();
        subscriptionRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        User user = new User();
        user.setEmail("proc-check-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-proc-check-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("proc-check-test@example.com");
        workspace.setSlug("proc-check-slug-" + System.currentTimeMillis());
        workspace.setOwner(user);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        workspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(workspace);

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setCreatedBy(user);
        caseFile.setTitle("Dossier Test Checklist");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(1);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        caseAnalysisRepository.save(analysis);

        auth = buildGoogleAuth("google-proc-check-sub", "proc-check-test@example.com");
    }

    // I-01 : GET liste vide → 200, []
    @Test
    void list_noChecks_returns200EmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks",
                        caseFile.getId(), analysis.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // I-02 : GET avec checks → 200, liste ordonnée par ordre
    @Test
    void list_withChecks_returns200OrderedList() throws Exception {
        saveCheck(analysis, workspace, 0, "Entretien préalable tenu", ProcedureCheckStatus.TO_CHECK);
        saveCheck(analysis, workspace, 1, "Lettre motivée envoyée", ProcedureCheckStatus.VERIFIED);

        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks",
                        caseFile.getId(), analysis.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ordre").value(0))
                .andExpect(jsonPath("$[0].description").value("Entretien préalable tenu"))
                .andExpect(jsonPath("$[0].statut").value("TO_CHECK"))
                .andExpect(jsonPath("$[1].ordre").value(1))
                .andExpect(jsonPath("$[1].statut").value("VERIFIED"));
    }

    // I-03 : PATCH statut VERIFIED → 200, statut mis à jour
    @Test
    void updateStatus_verified_returns200AndUpdatesStatut() throws Exception {
        ProcedureCheck check = saveCheck(analysis, workspace, 0, "Point A", ProcedureCheckStatus.TO_CHECK);

        mockMvc.perform(patch("/api/v1/procedure-checks/{checkId}", check.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("VERIFIED"));
    }

    // I-04 : PATCH statut invalide → 400
    @Test
    void updateStatus_invalidStatut_returns400() throws Exception {
        ProcedureCheck check = saveCheck(analysis, workspace, 0, "Point A", ProcedureCheckStatus.TO_CHECK);

        mockMvc.perform(patch("/api/v1/procedure-checks/{checkId}", check.getId())
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"INVALID_STATUS\"}"))
                .andExpect(status().isBadRequest());
    }

    // I-05 : PATCH avec workspace différent → 404 (isolation workspace)
    @Test
    void updateStatus_differentWorkspace_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-proc@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-proc-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-proc@example.com");
        otherWorkspace.setSlug("other-proc-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        // Check appartenant au workspace de "proc-check-test", tenté depuis "other-proc"
        ProcedureCheck check = saveCheck(analysis, workspace, 0, "Point A", ProcedureCheckStatus.TO_CHECK);

        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-other-proc-sub", "other-proc@example.com");

        mockMvc.perform(patch("/api/v1/procedure-checks/{checkId}", check.getId())
                        .with(authentication(otherAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"statut\": \"VERIFIED\"}"))
                .andExpect(status().isNotFound());
    }

    // I-06 : GET analyse appartenant à un autre workspace → 404
    @Test
    void list_otherWorkspaceAnalysis_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-list-proc@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-list-proc-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-list-proc@example.com");
        otherWorkspace.setSlug("other-list-proc-slug-" + System.currentTimeMillis());
        otherWorkspace.setOwner(otherUser);
        otherWorkspace.setPlanCode("STARTER");
        otherWorkspace.setStatus("ACTIVE");
        otherWorkspace.setLegalDomain("DROIT_DU_TRAVAIL");
        workspaceRepository.save(otherWorkspace);

        WorkspaceMember otherMember = new WorkspaceMember();
        otherMember.setWorkspace(otherWorkspace);
        otherMember.setUser(otherUser);
        otherMember.setMemberRole("OWNER");
        otherMember.setPrimary(true);
        workspaceMemberRepository.save(otherMember);

        OAuth2AuthenticationToken otherAuth = buildGoogleAuth("google-other-list-proc-sub", "other-list-proc@example.com");

        // Tente d'accéder au dossier du workspace d'origine depuis l'autre workspace
        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks",
                        caseFile.getId(), analysis.getId())
                        .with(authentication(otherAuth)))
                .andExpect(status().isNotFound());
    }

    // I-07 : GET sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks",
                        caseFile.getId(), analysis.getId()))
                .andExpect(status().isUnauthorized());
    }

    private ProcedureCheck saveCheck(CaseAnalysis ca, Workspace ws, int ordre,
                                     String description, ProcedureCheckStatus statut) {
        ProcedureCheck check = new ProcedureCheck();
        check.setCaseAnalysis(ca);
        check.setWorkspace(ws);
        check.setOrdre(ordre);
        check.setDescription(description);
        check.setStatut(statut);
        return procedureCheckRepository.save(check);
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
