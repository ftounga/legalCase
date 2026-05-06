package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-192 SF-192-01 — IT du contrôleur GET /retained-pistes-alignment.
 *
 * <ul>
 *   <li>Pas d'analyse DONE → 200 + []</li>
 *   <li>Analyse DONE pré-F-192 (alignment_json null) → 200 + []</li>
 *   <li>Analyse DONE avec alignment matérialisé → 200 + tableau</li>
 *   <li>Case file dans un autre workspace → 404 (camouflage)</li>
 *   <li>Sans authentification → 401</li>
 * </ul>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test",
        "spring.security.oauth2.client.registration.google.client-secret=test",
        "anthropic.api-key=test"})
@AutoConfigureMockMvc
class RetainedPisteAlignmentControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired CaseAnalysisRepository caseAnalysisRepository;
    @Autowired ObjectMapper objectMapper;

    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authUser1;
    private OAuth2AuthenticationToken authUser2;
    private CaseFile caseFileWs1;
    private CaseFile caseFileWs2;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User u1 = new User(); u1.setEmail("rpa-" + ts + "@ex.com"); u1.setStatus("ACTIVE");
        u1 = userRepository.save(u1);
        AuthAccount a1 = new AuthAccount(); a1.setUser(u1); a1.setProvider("GOOGLE");
        a1.setProviderUserId("g-rpa-" + ts);
        authAccountRepository.save(a1);
        Workspace ws1 = new Workspace();
        ws1.setName("W1-" + ts); ws1.setSlug("ws-rpa-1-" + ts); ws1.setOwner(u1);
        ws1.setLegalDomain("DROIT_IMMIGRATION"); ws1.setCountry("FRANCE");
        ws1.setPlanCode("STARTER"); ws1.setStatus("ACTIVE");
        ws1 = workspaceRepository.save(ws1);
        WorkspaceMember m1 = new WorkspaceMember(); m1.setWorkspace(ws1); m1.setUser(u1);
        m1.setMemberRole("OWNER"); m1.setPrimary(true);
        workspaceMemberRepository.save(m1);
        caseFileWs1 = new CaseFile(); caseFileWs1.setTitle("CF1-" + ts); caseFileWs1.setWorkspace(ws1);
        caseFileWs1.setCreatedBy(u1); caseFileWs1.setLegalDomain("DROIT_IMMIGRATION");
        caseFileWs1.setStatus("OPEN");
        caseFileWs1 = caseFileRepository.save(caseFileWs1);
        authUser1 = buildAuth("g-rpa-" + ts, "rpa-" + ts + "@ex.com");

        User u2 = new User(); u2.setEmail("rpa2-" + ts + "@ex.com"); u2.setStatus("ACTIVE");
        u2 = userRepository.save(u2);
        AuthAccount a2 = new AuthAccount(); a2.setUser(u2); a2.setProvider("GOOGLE");
        a2.setProviderUserId("g-rpa2-" + ts);
        authAccountRepository.save(a2);
        Workspace ws2 = new Workspace();
        ws2.setName("W2-" + ts); ws2.setSlug("ws-rpa-2-" + ts); ws2.setOwner(u2);
        ws2.setLegalDomain("DROIT_IMMIGRATION"); ws2.setCountry("FRANCE");
        ws2.setPlanCode("STARTER"); ws2.setStatus("ACTIVE");
        ws2 = workspaceRepository.save(ws2);
        WorkspaceMember m2 = new WorkspaceMember(); m2.setWorkspace(ws2); m2.setUser(u2);
        m2.setMemberRole("OWNER"); m2.setPrimary(true);
        workspaceMemberRepository.save(m2);
        caseFileWs2 = new CaseFile(); caseFileWs2.setTitle("CF2-" + ts); caseFileWs2.setWorkspace(ws2);
        caseFileWs2.setCreatedBy(u2); caseFileWs2.setLegalDomain("DROIT_IMMIGRATION");
        caseFileWs2.setStatus("OPEN");
        caseFileWs2 = caseFileRepository.save(caseFileWs2);
        authUser2 = buildAuth("g-rpa2-" + ts, "rpa2-" + ts + "@ex.com");
    }

    @Test
    void GET_noAnalysis_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs1.getId() + "/retained-pistes-alignment")
                        .with(authentication(authUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void GET_legacyAnalysisWithoutAlignmentJson_returnsEmptyArray() throws Exception {
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFileWs1);
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult("{}");
        a.setRetainedPistesAlignmentJson(null);
        caseAnalysisRepository.save(a);

        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs1.getId() + "/retained-pistes-alignment")
                        .with(authentication(authUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void GET_withMaterializedAlignment_returnsList() throws Exception {
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(caseFileWs1);
        a.setVersion(1);
        a.setAnalysisType(AnalysisType.ENRICHED);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisResult("{}");
        UUID pisteId = UUID.randomUUID();
        a.setRetainedPistesAlignmentJson(
                "[{\"pisteId\":\"" + pisteId + "\","
                        + "\"texte\":\"Demander un VPF\","
                        + "\"baseJuridique\":\"L.421-14 CESEDA\","
                        + "\"horizonTemporel\":\"3-6 mois\","
                        + "\"conditions\":[],"
                        + "\"toolIdCible\":\"" + RetainedPisteToolMatcher.TOOL_TITRE + "\","
                        + "\"matchStatus\":\"DIVERGENT\"}]");
        caseAnalysisRepository.save(a);

        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs1.getId() + "/retained-pistes-alignment")
                        .with(authentication(authUser1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matchStatus").value("DIVERGENT"))
                .andExpect(jsonPath("$[0].toolIdCible").value(RetainedPisteToolMatcher.TOOL_TITRE));
    }

    @Test
    void GET_caseFileInOtherWorkspace_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + caseFileWs2.getId() + "/retained-pistes-alignment")
                        .with(authentication(authUser1)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_unauthenticated_returnsRedirectOrUnauthorized() throws Exception {
        // Spring Security par défaut redirige vers /oauth2/authorization (302) ou
        // renvoie 401/403 selon configuration. On accepte les 3.
        int sc = mockMvc.perform(get("/api/v1/case-files/" + caseFileWs1.getId() + "/retained-pistes-alignment"))
                .andReturn().getResponse().getStatus();
        assert sc == 401 || sc == 403 || sc == 302
                : "Expected 401/403/302 but got " + sc;
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> c = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken t = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), c);
        return new OAuth2AuthenticationToken(new DefaultOidcUser(List.of(), t), List.of(), "google");
    }
}
