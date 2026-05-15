package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-243 / SF-243-01 — Tests d'intégration des 3 endpoints du stade procédural.
 *
 * <p>Couvre les codes 200/400/422/403/404/401 du contrat API et l'isolation workspace.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class ProcedureStageControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private CaseFileRepository caseFileRepository;

    private OAuth2AuthenticationToken auth;
    private UUID caseFileId;

    @BeforeEach
    void setUp() {
        caseFileRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        authAccountRepository.deleteAll();
        userRepository.deleteAll();

        User user = createUser("ps-test@example.com");
        createAuthAccount(user, "google-ps-sub");
        Workspace workspace = createWorkspace("ps@example.com", user, "DROIT_DU_TRAVAIL", "FRANCE");
        createMember(workspace, user);
        auth = buildGoogleAuth("google-ps-sub", "ps-test@example.com");

        CaseFile cf = new CaseFile();
        cf.setWorkspace(workspace);
        cf.setCreatedBy(user);
        cf.setTitle("Dossier stade procédural");
        cf.setLegalDomain("DROIT_DU_TRAVAIL");
        cf.setStatus("OPEN");
        caseFileId = caseFileRepository.save(cf).getId();
    }

    // =========================================================
    // Endpoint A — GET /api/v1/procedure-stage/options
    // =========================================================

    // I-01 : options → 200 pour chacun des 6 couples domaine/pays.
    @Test
    void getOptions_allSixCombinations_return200() throws Exception {
        String[] domains = {"DROIT_DU_TRAVAIL", "DROIT_IMMIGRATION", "DROIT_FAMILLE"};
        String[] countries = {"FRANCE", "BELGIQUE"};
        for (String domain : domains) {
            for (String country : countries) {
                mockMvc.perform(get("/api/v1/procedure-stage/options")
                                .param("domain", domain).param("country", country)
                                .with(authentication(auth)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.domain").value(domain))
                        .andExpect(jsonPath("$.country").value(country))
                        .andExpect(jsonPath("$.jurisdictions").isNotEmpty())
                        .andExpect(jsonPath("$.stages").isNotEmpty())
                        .andExpect(jsonPath("$.positions").isNotEmpty());
            }
        }
    }

    // I-02 : options DROIT_DU_TRAVAIL/FRANCE → structure du référentiel.
    @Test
    void getOptions_travailFrance_returnsExpectedCodes() throws Exception {
        mockMvc.perform(get("/api/v1/procedure-stage/options")
                        .param("domain", "DROIT_DU_TRAVAIL").param("country", "FRANCE")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jurisdictions[0].code").value("CPH"))
                .andExpect(jsonPath("$.jurisdictions[0].label").value("Conseil de prud'hommes"))
                .andExpect(jsonPath("$.stages[?(@.code=='FOND')].jurisdictionCode")
                        .value(org.hamcrest.Matchers.hasItem("CPH")))
                .andExpect(jsonPath("$.positions[?(@.code=='DEMANDEUR')].stageCodes[0]")
                        .value(org.hamcrest.Matchers.hasItem("BCO")));
    }

    // I-03 : options domaine invalide → 400.
    @Test
    void getOptions_invalidDomain_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/procedure-stage/options")
                        .param("domain", "DROIT_PENAL").param("country", "FRANCE")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-04 : options pays invalide → 400.
    @Test
    void getOptions_invalidCountry_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/procedure-stage/options")
                        .param("domain", "DROIT_DU_TRAVAIL").param("country", "SUISSE")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // I-05 : options sans auth → 401.
    @Test
    void getOptions_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/procedure-stage/options")
                        .param("domain", "DROIT_DU_TRAVAIL").param("country", "FRANCE"))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================
    // Endpoint B — GET /api/v1/case-files/{id}/procedure-stage
    // =========================================================

    // I-06 : lecture d'un dossier non renseigné → 200, champs null.
    @Test
    void getProcedureStage_emptyCaseFile_returns200WithNulls() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.caseFileId").value(caseFileId.toString()))
                .andExpect(jsonPath("$.jurisdiction").doesNotExist())
                .andExpect(jsonPath("$.stage").doesNotExist())
                .andExpect(jsonPath("$.position").doesNotExist());
    }

    // I-07 : lecture après mise à jour → 200, champs + labels renseignés.
    @Test
    void getProcedureStage_afterUpdate_returns200WithValues() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH","stage":"FOND","position":"DEMANDEUR"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jurisdiction").value("CPH"))
                .andExpect(jsonPath("$.jurisdictionLabel").value("Conseil de prud'hommes"))
                .andExpect(jsonPath("$.stage").value("FOND"))
                .andExpect(jsonPath("$.stageLabel").value("Bureau de jugement (fond)"))
                .andExpect(jsonPath("$.position").value("DEMANDEUR"))
                .andExpect(jsonPath("$.positionLabel").value("Demandeur (salarié)"));
    }

    // I-08 : lecture dossier inexistant → 404.
    @Test
    void getProcedureStage_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + UUID.randomUUID() + "/procedure-stage")
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-09 : lecture sans auth → 401.
    @Test
    void getProcedureStage_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileId + "/procedure-stage"))
                .andExpect(status().isUnauthorized());
    }

    // I-10 : lecture d'un dossier d'un autre workspace → 403 (isolation workspace).
    @Test
    void getProcedureStage_differentWorkspace_returns403() throws Exception {
        UUID otherCaseFileId = createOtherWorkspaceCaseFile();
        mockMvc.perform(get("/api/v1/case-files/" + otherCaseFileId + "/procedure-stage")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // =========================================================
    // Endpoint C — PATCH /api/v1/case-files/{id}/procedure-stage
    // =========================================================

    // I-11 : PATCH combinaison valide → 200.
    @Test
    void updateProcedureStage_validCombination_returns200() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH","stage":"REFERE","position":"DEFENDEUR"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jurisdiction").value("CPH"))
                .andExpect(jsonPath("$.stage").value("REFERE"))
                .andExpect(jsonPath("$.position").value("DEFENDEUR"));
    }

    // I-12 : PATCH stade hors juridiction → 422.
    @Test
    void updateProcedureStage_stageNotUnderJurisdiction_returns422() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH","stage":"APPEL"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isUnprocessableEntity());
    }

    // I-13 : PATCH position hors stade → 422.
    @Test
    void updateProcedureStage_positionNotValidForStage_returns422() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH","stage":"FOND","position":"APPELANT"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isUnprocessableEntity());
    }

    // I-14 : PATCH valeur hors domaine du dossier → 422.
    @Test
    void updateProcedureStage_valueNotInCaseDomain_returns422() throws Exception {
        // JAF appartient au droit de la famille ; le dossier est en droit du travail.
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"JAF"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isUnprocessableEntity());
    }

    // I-15 : PATCH jurisdiction:null → cascade efface stage + position.
    @Test
    void updateProcedureStage_clearJurisdiction_cascades() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH","stage":"FOND","position":"DEMANDEUR"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":null,"stage":"FOND","position":"DEMANDEUR"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jurisdiction").doesNotExist())
                .andExpect(jsonPath("$.stage").doesNotExist())
                .andExpect(jsonPath("$.position").doesNotExist());

        CaseFile reloaded = caseFileRepository.findById(caseFileId).orElseThrow();
        assertThat(reloaded.getProcedureJurisdiction()).isNull();
        assertThat(reloaded.getProcedureStage()).isNull();
        assertThat(reloaded.getProcedurePosition()).isNull();
    }

    // I-16 : PATCH stage:null → cascade efface position.
    @Test
    void updateProcedureStage_clearStage_cascadesPosition() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH","stage":null,"position":"DEMANDEUR"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jurisdiction").value("CPH"))
                .andExpect(jsonPath("$.stage").doesNotExist())
                .andExpect(jsonPath("$.position").doesNotExist());
    }

    // I-17 : PATCH dossier inexistant → 404.
    @Test
    void updateProcedureStage_unknownCaseFile_returns404() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + UUID.randomUUID() + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isNotFound());
    }

    // I-18 : PATCH sans auth → 401.
    @Test
    void updateProcedureStage_withoutAuth_returns401() throws Exception {
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // I-19 : PATCH dossier d'un autre workspace → 403 (isolation workspace).
    @Test
    void updateProcedureStage_differentWorkspace_returns403() throws Exception {
        UUID otherCaseFileId = createOtherWorkspaceCaseFile();
        mockMvc.perform(patch("/api/v1/case-files/" + otherCaseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"jurisdiction":"CPH"}
                                """)
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // I-20 : PATCH champ trop long → 400 (paramètre malformé).
    @Test
    void updateProcedureStage_fieldTooLong_returns400() throws Exception {
        String tooLong = "X".repeat(51);
        mockMvc.perform(patch("/api/v1/case-files/" + caseFileId + "/procedure-stage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jurisdiction\":\"" + tooLong + "\"}")
                        .with(authentication(auth)))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // Helpers
    // =========================================================

    /** Crée un dossier dans un workspace distinct, pour les tests d'isolation. */
    private UUID createOtherWorkspaceCaseFile() {
        User otherUser = createUser("ps-other@example.com");
        createAuthAccount(otherUser, "google-ps-other-sub");
        Workspace otherWorkspace = createWorkspace("ps-other@example.com", otherUser,
                "DROIT_DU_TRAVAIL", "FRANCE");
        createMember(otherWorkspace, otherUser);

        CaseFile cf = new CaseFile();
        cf.setWorkspace(otherWorkspace);
        cf.setCreatedBy(otherUser);
        cf.setTitle("Dossier autre workspace");
        cf.setLegalDomain("DROIT_DU_TRAVAIL");
        cf.setStatus("OPEN");
        return caseFileRepository.save(cf).getId();
    }

    private User createUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    private void createAuthAccount(User user, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        authAccountRepository.save(account);
    }

    private Workspace createWorkspace(String name, User owner, String domain, String country) {
        Workspace workspace = new Workspace();
        workspace.setName(name);
        workspace.setSlug("ps-slug-" + UUID.randomUUID());
        workspace.setOwner(owner);
        workspace.setLegalDomain(domain);
        workspace.setCountry(country);
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");
        return workspaceRepository.save(workspace);
    }

    private void createMember(Workspace workspace, User user) {
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(workspace);
        member.setUser(user);
        member.setMemberRole("OWNER");
        member.setPrimary(true);
        workspaceMemberRepository.save(member);
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
