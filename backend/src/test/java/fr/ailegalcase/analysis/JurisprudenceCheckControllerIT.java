package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.audit.AuditLogRepository;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-179 SF-179-01 — tests d'intégration de {@link JurisprudenceCheckController}.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class JurisprudenceCheckControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;
    @Autowired private CaseAnalysisRepository caseAnalysisRepository;
    @Autowired private JurisprudenceCheckRepository jurisprudenceCheckRepository;
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
        jurisprudenceCheckRepository.deleteAll();
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
        user.setEmail("jurisp-test@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-jurisp-sub");
        authAccountRepository.save(account);

        workspace = new Workspace();
        workspace.setName("jurisp-test@example.com");
        workspace.setSlug("jurisp-slug-" + System.currentTimeMillis());
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
        caseFile.setTitle("Dossier Test Jurisprudence");
        caseFile.setLegalDomain("DROIT_DU_TRAVAIL");
        caseFile.setStatus("OPEN");
        caseFileRepository.save(caseFile);

        analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(1);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.DONE);
        caseAnalysisRepository.save(analysis);

        auth = buildGoogleAuth("google-jurisp-sub", "jurisp-test@example.com");
    }

    // I-01 : GET liste vide → 200, checks []
    @Test
    void list_noChecks_returns200EmptyList() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/jurisprudence-checks", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checks.length()").value(0));
    }

    // I-02 : GET avec checks → 200, liste avec statuts
    @Test
    void list_withChecks_returns200WithStatuts() throws Exception {
        saveCheck("conclusions_adverses.pdf", "Cass. soc. 25 sept. 2013, n° 12-17.516",
                JurisprudenceCheckStatus.SUSPECT, "Position alleguee incoherente.",
                "L'adversaire pretend que cet arret fonde la nullite.");
        saveCheck("conclusions_adverses.pdf", "CE 30 juin 2017, n° 398445",
                JurisprudenceCheckStatus.VERIFIED, "Existence et position confirmees.", null);

        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/jurisprudence-checks", caseFile.getId())
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checks.length()").value(2))
                .andExpect(jsonPath("$.checks[?(@.statut == 'SUSPECT')]").exists())
                .andExpect(jsonPath("$.checks[?(@.statut == 'VERIFIED')]").exists())
                // SF-98-56 — le DTO expose désormais markedAdverse (false par défaut).
                .andExpect(jsonPath("$.checks[0].markedAdverse").value(false));
    }

    // ── SF-98-56 — PATCH marquage adverse ────────────────────────────────────

    // I-05 : PATCH markedAdverse:true sur un SUSPECT → 200, persistance vérifiée
    @Test
    void markAdverse_suspectTrue_returns200AndPersists() throws Exception {
        JurisprudenceCheck check = saveCheck("conclusions_adverses.pdf",
                "Cass. soc. 25 sept. 2013, n° 12-17.516",
                JurisprudenceCheckStatus.SUSPECT, "Position alleguee incoherente.",
                "L'adversaire pretend que cet arret fonde la nullite.");

        mockMvc.perform(patch(
                        "/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking",
                        caseFile.getId(), check.getId())
                        .with(authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("{\"markedAdverse\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedAdverse").value(true))
                .andExpect(jsonPath("$.statut").value("SUSPECT"));

        assertThat(jurisprudenceCheckRepository.findById(check.getId()).orElseThrow().isMarkedAdverse())
                .isTrue();
    }

    // I-06 : PATCH markedAdverse:false sur un NOT_FOUND → 200, démarquage
    @Test
    void markAdverse_notFoundFalse_returns200AndUnmarks() throws Exception {
        JurisprudenceCheck check = saveCheck("conclusions_adverses.pdf",
                "Cass. soc. 1 jan. 2099, n° 99-99.999",
                JurisprudenceCheckStatus.NOT_FOUND, "Reference introuvable.", null);
        check.setMarkedAdverse(true);
        jurisprudenceCheckRepository.save(check);

        mockMvc.perform(patch(
                        "/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking",
                        caseFile.getId(), check.getId())
                        .with(authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("{\"markedAdverse\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedAdverse").value(false));

        assertThat(jurisprudenceCheckRepository.findById(check.getId()).orElseThrow().isMarkedAdverse())
                .isFalse();
    }

    // I-07 : PATCH body sans markedAdverse → 400
    @Test
    void markAdverse_missingField_returns400() throws Exception {
        JurisprudenceCheck check = saveCheck("conclusions_adverses.pdf",
                "Cass. soc. 25 sept. 2013, n° 12-17.516",
                JurisprudenceCheckStatus.SUSPECT, "Position alleguee incoherente.", null);

        mockMvc.perform(patch(
                        "/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking",
                        caseFile.getId(), check.getId())
                        .with(authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // I-08 : PATCH sur un statut non réfutable (VERIFIED) → 422
    @Test
    void markAdverse_nonRefutableStatus_returns422() throws Exception {
        JurisprudenceCheck check = saveCheck("conclusions_adverses.pdf",
                "CE 30 juin 2017, n° 398445",
                JurisprudenceCheckStatus.VERIFIED, "Existence et position confirmees.", null);

        mockMvc.perform(patch(
                        "/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking",
                        caseFile.getId(), check.getId())
                        .with(authentication(auth))
                        .contentType(APPLICATION_JSON)
                        .content("{\"markedAdverse\": true}"))
                .andExpect(status().isUnprocessableEntity());
    }

    // I-09 : PATCH sur un check d'un autre workspace → 404 (isolation, pas de fuite)
    @Test
    void markAdverse_otherWorkspace_returns404() throws Exception {
        JurisprudenceCheck check = saveCheck("conclusions_adverses.pdf",
                "Cass. soc. 25 sept. 2013, n° 12-17.516",
                JurisprudenceCheckStatus.SUSPECT, "Position alleguee incoherente.", null);

        User otherUser = new User();
        otherUser.setEmail("other-mark@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-mark-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-mark@example.com");
        otherWorkspace.setSlug("other-mark-slug-" + System.currentTimeMillis());
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

        OAuth2AuthenticationToken otherAuth =
                buildGoogleAuth("google-other-mark-sub", "other-mark@example.com");

        mockMvc.perform(patch(
                        "/api/v1/case-files/{caseFileId}/jurisprudence-checks/{checkId}/adverse-marking",
                        caseFile.getId(), check.getId())
                        .with(authentication(otherAuth))
                        .contentType(APPLICATION_JSON)
                        .content("{\"markedAdverse\": true}"))
                .andExpect(status().isNotFound());

        // Le marquage n'a pas eu lieu (camouflage : aucune mutation cross-workspace).
        assertThat(jurisprudenceCheckRepository.findById(check.getId()).orElseThrow().isMarkedAdverse())
                .isFalse();
    }

    // I-03 : GET sans auth → 401
    @Test
    void list_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/jurisprudence-checks", caseFile.getId()))
                .andExpect(status().isUnauthorized());
    }

    // I-04 : GET dossier d'un autre workspace → 404 (isolation workspace)
    @Test
    void list_otherWorkspace_returns404() throws Exception {
        User otherUser = new User();
        otherUser.setEmail("other-jurisp@example.com");
        otherUser.setStatus("ACTIVE");
        userRepository.save(otherUser);

        AuthAccount otherAccount = new AuthAccount();
        otherAccount.setUser(otherUser);
        otherAccount.setProvider("GOOGLE");
        otherAccount.setProviderUserId("google-other-jurisp-sub");
        authAccountRepository.save(otherAccount);

        Workspace otherWorkspace = new Workspace();
        otherWorkspace.setName("other-jurisp@example.com");
        otherWorkspace.setSlug("other-jurisp-slug-" + System.currentTimeMillis());
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

        OAuth2AuthenticationToken otherAuth =
                buildGoogleAuth("google-other-jurisp-sub", "other-jurisp@example.com");

        mockMvc.perform(get("/api/v1/case-files/{caseFileId}/jurisprudence-checks", caseFile.getId())
                        .with(authentication(otherAuth)))
                .andExpect(status().isNotFound());
    }

    // I-10 : le repository ne renvoie que les checks marqués ET éligibles (SUSPECT/NOT_FOUND)
    @Test
    void repository_findMarkedRefutable_returnsOnlyEligibleMarked() {
        JurisprudenceCheck suspectMarked = saveCheck("a.pdf", "ref-suspect-marked",
                JurisprudenceCheckStatus.SUSPECT, "x", null);
        suspectMarked.setMarkedAdverse(true);
        jurisprudenceCheckRepository.save(suspectMarked);

        JurisprudenceCheck notFoundMarked = saveCheck("a.pdf", "ref-notfound-marked",
                JurisprudenceCheckStatus.NOT_FOUND, "x", null);
        notFoundMarked.setMarkedAdverse(true);
        jurisprudenceCheckRepository.save(notFoundMarked);

        // marqué mais statut non réfutable → exclu (ne devrait pas exister via l'API, défense en profondeur)
        JurisprudenceCheck verifiedMarked = saveCheck("a.pdf", "ref-verified-marked",
                JurisprudenceCheckStatus.VERIFIED, "x", null);
        verifiedMarked.setMarkedAdverse(true);
        jurisprudenceCheckRepository.save(verifiedMarked);

        // éligible mais non marqué → exclu
        saveCheck("a.pdf", "ref-suspect-unmarked", JurisprudenceCheckStatus.SUSPECT, "x", null);

        List<JurisprudenceCheck> result = jurisprudenceCheckRepository
                .findByCaseFileIdAndStatutInAndMarkedAdverseTrue(
                        caseFile.getId(),
                        List.of(JurisprudenceCheckStatus.SUSPECT, JurisprudenceCheckStatus.NOT_FOUND));

        assertThat(result).extracting(JurisprudenceCheck::getReference)
                .containsExactlyInAnyOrder("ref-suspect-marked", "ref-notfound-marked");
    }

    private JurisprudenceCheck saveCheck(String documentName, String reference,
                                         JurisprudenceCheckStatus statut,
                                         String explication, String positionAlleguee) {
        JurisprudenceCheck check = new JurisprudenceCheck();
        check.setCaseFile(caseFile);
        check.setCaseAnalysis(analysis);
        check.setWorkspace(workspace);
        check.setDocumentName(documentName);
        check.setReference(reference);
        check.setStatut(statut);
        check.setExplication(explication);
        check.setPositionAlleguee(positionAlleguee);
        check.setClaudeConfidence("HIGH");
        check.setWebSearchUsed(false);
        return jurisprudenceCheckRepository.save(check);
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
