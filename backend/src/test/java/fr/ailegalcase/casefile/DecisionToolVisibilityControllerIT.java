package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.AnalysisType;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
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
import org.springframework.boot.test.mock.mockito.MockBean;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DecisionToolVisibilityControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authTravailFr;
    private OAuth2AuthenticationToken authOtherWs;
    private OAuth2AuthenticationToken authImmFr;
    private OAuth2AuthenticationToken authTravailBe;
    private CaseFile travailCf;
    private CaseFile otherCf;
    private CaseFile immCf;
    private CaseFile deletedCf;
    private CaseFile travailBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace 1: DROIT_DU_TRAVAIL / FRANCE (primary for authTravailFr)
        User u1 = new User();
        u1.setEmail("dtv-" + ts + "@ex.com");
        u1.setStatus("ACTIVE");
        u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount();
        a1.setUser(u1);
        a1.setProvider("GOOGLE");
        a1.setProviderUserId("g-dtv-" + ts);
        authAccountRepository.save(a1);
        Workspace ws1 = new Workspace();
        ws1.setName("DTV " + ts);
        ws1.setSlug("ws-dtv-" + ts);
        ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_DU_TRAVAIL");
        ws1.setCountry("FRANCE");
        ws1.setPlanCode("STARTER");
        ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember();
        m1.setWorkspace(ws1);
        m1.setUser(u1);
        m1.setMemberRole("OWNER");
        m1.setPrimary(true);
        workspaceMemberRepository.save(m1);
        travailCf = new CaseFile();
        travailCf.setTitle("DTV " + ts);
        travailCf.setWorkspace(ws1);
        travailCf.setCreatedBy(u1);
        travailCf.setLegalDomain("DROIT_DU_TRAVAIL");
        travailCf.setStatus("OPEN");
        travailCf = caseFileRepository.save(travailCf);
        // Soft-deleted case file in same workspace for the 404 DELETED scenario
        deletedCf = new CaseFile();
        deletedCf.setTitle("Deleted " + ts);
        deletedCf.setWorkspace(ws1);
        deletedCf.setCreatedBy(u1);
        deletedCf.setLegalDomain("DROIT_DU_TRAVAIL");
        deletedCf.setStatus("OPEN");
        deletedCf.setDeletedAt(Instant.now());
        deletedCf = caseFileRepository.save(deletedCf);
        authTravailFr = buildAuth("g-dtv-" + ts, "dtv-" + ts + "@ex.com");

        // Workspace 2: DROIT_IMMIGRATION / FRANCE (for authOtherWs, and owns immCf)
        User u2 = new User();
        u2.setEmail("dtv-o-" + ts + "@ex.com");
        u2.setStatus("ACTIVE");
        u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount();
        a2.setUser(u2);
        a2.setProvider("GOOGLE");
        a2.setProviderUserId("g-dtv-o-" + ts);
        authAccountRepository.save(a2);
        Workspace ws2 = new Workspace();
        ws2.setName("OTHER " + ts);
        ws2.setSlug("ws-dtv-o-" + ts);
        ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_IMMIGRATION");
        ws2.setCountry("FRANCE");
        ws2.setPlanCode("STARTER");
        ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember();
        m2.setWorkspace(ws2);
        m2.setUser(u2);
        m2.setMemberRole("OWNER");
        m2.setPrimary(true);
        workspaceMemberRepository.save(m2);
        immCf = new CaseFile();
        immCf.setTitle("IMM " + ts);
        immCf.setWorkspace(ws2);
        immCf.setCreatedBy(u2);
        immCf.setLegalDomain("DROIT_IMMIGRATION");
        immCf.setStatus("OPEN");
        immCf = caseFileRepository.save(immCf);
        // "otherCf" = case file of ws2 used to test cross-workspace access
        otherCf = immCf;
        authImmFr = buildAuth("g-dtv-o-" + ts, "dtv-o-" + ts + "@ex.com");
        authOtherWs = authImmFr;

        // Workspace 3 : DROIT_DU_TRAVAIL / BELGIQUE (for SF-IA-04-03 RUPTURE_AMIABLE test)
        User u3 = new User();
        u3.setEmail("dtv-be-" + ts + "@ex.com");
        u3.setStatus("ACTIVE");
        u3 = userRepository.save(u3);
        AuthAccount a3 = new AuthAccount();
        a3.setUser(u3);
        a3.setProvider("GOOGLE");
        a3.setProviderUserId("g-dtv-be-" + ts);
        authAccountRepository.save(a3);
        Workspace ws3 = new Workspace();
        ws3.setName("DTV BE " + ts);
        ws3.setSlug("ws-dtv-be-" + ts);
        ws3.setOwner(u3);
        ws3.setLegalDomain("DROIT_DU_TRAVAIL");
        ws3.setCountry("BELGIQUE");
        ws3.setPlanCode("STARTER");
        ws3.setStatus("ACTIVE");
        ws3 = workspaceRepository.save(ws3);
        WorkspaceMember m3 = new WorkspaceMember();
        m3.setWorkspace(ws3);
        m3.setUser(u3);
        m3.setMemberRole("OWNER");
        m3.setPrimary(true);
        workspaceMemberRepository.save(m3);
        travailBeCf = new CaseFile();
        travailBeCf.setTitle("DTV BE " + ts);
        travailBeCf.setWorkspace(ws3);
        travailBeCf.setCreatedBy(u3);
        travailBeCf.setLegalDomain("DROIT_DU_TRAVAIL");
        travailBeCf.setStatus("OPEN");
        travailBeCf = caseFileRepository.save(travailBeCf);
        authTravailBe = buildAuth("g-dtv-be-" + ts, "dtv-be-" + ts + "@ex.com");
    }

    // ──────────────────────────── Nominal ───────────────────────────────────

    @Test
    void GET_travailFr_sansAnalyse_returns200_avecAlwaysOn_et_catalog() throws Exception {
        // Après SF-IA-04-03 migration 106 : F-DT-08, F-DT-09 sont ALWAYS_ON (rétrocompat).
        // F-DT-10, F-132 restent CONTEXTUAL → visibles dans catalog sans analyse.
        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alwaysOn").isArray())
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-03-prescription-litige")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-08-licenciement-validity")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-09-comparateur-indemnites")))
                .andExpect(jsonPath("$.contextual").isEmpty())
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity")))
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.hasItem("F-132-rupture-conv-indemnite")));
    }

    @Test
    void GET_travailFr_avecAnalyseRuptureConv_returns200_contextualContains_FDT10_F132() throws Exception {
        persistAnalysis(travailCf, """
                { "compensation_data": { "type_rupture": "RUPTURE_CONVENTIONNELLE" } }
                """);

        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextual", org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity")))
                .andExpect(jsonPath("$.contextual", org.hamcrest.Matchers.hasItem("F-132-rupture-conv-indemnite")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-08-licenciement-validity")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-09-comparateur-indemnites")));
    }

    @Test
    void GET_travailFr_avecAnalyseLicenciement_returns200_FDT08_FDT09_alwaysOn() throws Exception {
        // Après migration 106 : F-DT-08 et F-DT-09 sont ALWAYS_ON, pas CONTEXTUAL.
        persistAnalysis(travailCf, """
                { "compensation_data": { "type_rupture": "LICENCIEMENT" } }
                """);

        mockMvc.perform(get("/api/v1/case-files/" + travailCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-08-licenciement-validity")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-DT-09-comparateur-indemnites")))
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.hasItem("F-DT-10-rupture-conv-validity")))
                .andExpect(jsonPath("$.catalog", org.hamcrest.Matchers.hasItem("F-132-rupture-conv-indemnite")));
    }

    @Test
    void GET_immigrationFr_sansAnalyse_returns200_alwaysOnContientToutLesOutilsImm() throws Exception {
        // Après migration 106 : F-IM-01 et F-IM-06 sont ALWAYS_ON (rétrocompat), plus dans catalog.
        mockMvc.perform(get("/api/v1/case-files/" + immCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authImmFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-IM-01-checklist-pieces")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-IM-05-arbre-decisionnel-titre")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-IM-06-recours")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-IM-07-droit-au-travail")));
    }

    @Test
    void SF_IA_04_03_travailBe_avecAnalyseRuptureAmiable_returns200_contextualContains_F132_rupture_amiable_info() throws Exception {
        persistAnalysis(travailBeCf, """
                { "compensation_data": { "type_rupture": "RUPTURE_AMIABLE" } }
                """);

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authTravailBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contextual", org.hamcrest.Matchers.hasItem("F-132-rupture-amiable-info")));
    }

    @Test
    void SF_IA_04_03_familleFr_sansAnalyse_returns200_outilsFamilleAlwaysOn() throws Exception {
        // F-FA-05, F-FA-06, F-FA-07 passent ALWAYS_ON par migration 106 — rétrocompat.
        // Utilise immCf workspace DROIT_IMMIGRATION pour créer un nouveau ws DROIT_FAMILLE.
        long ts = System.nanoTime();
        User u4 = new User();
        u4.setEmail("dtv-fa-" + ts + "@ex.com");
        u4.setStatus("ACTIVE");
        u4 = userRepository.save(u4);
        AuthAccount a4 = new AuthAccount();
        a4.setUser(u4);
        a4.setProvider("GOOGLE");
        a4.setProviderUserId("g-dtv-fa-" + ts);
        authAccountRepository.save(a4);
        Workspace wsF = new Workspace();
        wsF.setName("DTV FA " + ts);
        wsF.setSlug("ws-dtv-fa-" + ts);
        wsF.setOwner(u4);
        wsF.setLegalDomain("DROIT_FAMILLE");
        wsF.setCountry("FRANCE");
        wsF.setPlanCode("STARTER");
        wsF.setStatus("ACTIVE");
        wsF = workspaceRepository.save(wsF);
        WorkspaceMember mF = new WorkspaceMember();
        mF.setWorkspace(wsF);
        mF.setUser(u4);
        mF.setMemberRole("OWNER");
        mF.setPrimary(true);
        workspaceMemberRepository.save(mF);
        CaseFile famCf = new CaseFile();
        famCf.setTitle("FA " + ts);
        famCf.setWorkspace(wsF);
        famCf.setCreatedBy(u4);
        famCf.setLegalDomain("DROIT_FAMILLE");
        famCf.setStatus("OPEN");
        famCf = caseFileRepository.save(famCf);
        OAuth2AuthenticationToken authFaFr = buildAuth("g-dtv-fa-" + ts, "dtv-fa-" + ts + "@ex.com");

        mockMvc.perform(get("/api/v1/case-files/" + famCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authFaFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-FA-05-partage-immobilier")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-FA-06-calendrier-garde")))
                .andExpect(jsonPath("$.alwaysOn", org.hamcrest.Matchers.hasItem("F-FA-07-checklist-divorce")));
    }

    // ────────────────────────────── Erreurs ─────────────────────────────────

    @Test
    void GET_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + otherCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_deletedCaseFile_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + deletedCf.getId() + "/decision-tools-visibility")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_caseFileInexistant_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/00000000-0000-0000-0000-000000099999/decision-tools-visibility")
                        .with(authentication(authTravailFr)))
                .andExpect(status().isNotFound());
    }

    // ─────────────────────────── Helpers ────────────────────────────────────

    private void persistAnalysis(CaseFile cf, String json) {
        CaseAnalysis ca = new CaseAnalysis();
        ca.setCaseFile(cf);
        ca.setVersion(1);
        ca.setAnalysisType(AnalysisType.STANDARD);
        ca.setAnalysisStatus(AnalysisStatus.DONE);
        ca.setAnalysisResult(json);
        caseAnalysisRepository.save(ca);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
