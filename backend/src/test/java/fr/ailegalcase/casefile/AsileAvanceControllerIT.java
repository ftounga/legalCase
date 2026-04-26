package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicService;
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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AsileAvanceControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authDt;
    private CaseFile immFrCf;
    private CaseFile immBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_IMMIGRATION
        User uFr = save(new User(), u -> { u.setEmail("asi-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-asi-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-ASI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-ASI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-asi-fr-" + ts, "asi-fr-" + ts + "@ex.com");

        // BE DROIT_IMMIGRATION (gate country FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("asi-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-asi-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-ASI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-ASI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-asi-be-" + ts, "asi-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL (gate domaine → 400)
        User uDt = save(new User(), u -> { u.setEmail("asi-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-asi-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-ASI " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-ASI " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-asi-dt-" + ts, "asi-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyDublinNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("dispositifAsile", "DUBLIN_III");
        m.put("empreintesEurodacAutresEm", true);
        m.put("demandeurEnFuite", false);
        return m;
    }

    @Test
    void POST_fr_dublinTransfert_returnsRecevableTransfert() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDublinNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositifAsile").value("DUBLIN_III"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE_TRANSFERT"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(6.0))
                .andExpect(jsonPath("$.documentsRequis").isArray())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("604/2013")));
    }

    @Test
    void POST_fr_accelereePaysSur_returnsApplicable() throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("dispositifAsile", "PROCEDURE_ACCELEREE");
        b.put("paysOrigineDansListeSurs", true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("ACCELEREE_APPLICABLE"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(1.5))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.531-24")));
    }

    @Test
    void POST_fr_reexamenSansElementsNouveaux_returnsIrrecevable() throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("dispositifAsile", "REEXAMEN");
        b.put("dateDecisionAnterieure", "2024-06-01");
        b.put("elementsNouveaux", false);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("IRRECEVABLE"));
    }

    @Test
    void POST_fr_apatridieNominal_returnsRecevable() throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("dispositifAsile", "APATRIDIE");
        b.put("motifsExclusion", false);
        b.put("presenceReguliere", true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE_APATRIDIE"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(12.0));
    }

    @Test
    void POST_fr_protectionSubsidiaireNominal_returnsRecevable() throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("dispositifAsile", "PROTECTION_SUBSIDIAIRE");
        b.put("motifsExclusion", false);
        b.put("traitementsGravesEtablis", true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE_PROTECTION_SUBSIDIAIRE"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(18.0));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDublinNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDublinNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDublinNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dispositifInconnu_returns400() throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("dispositifAsile", "INCONNU");
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDublinNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE_TRANSFERT"));

        Map<String, Object> next = new HashMap<>();
        next.put("dispositifAsile", "APATRIDIE");
        next.put("motifsExclusion", false);
        next.put("presenceReguliere", true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispositifAsile").value("APATRIDIE"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE_APATRIDIE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyDublinNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositifAsile").value("DUBLIN_III"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("RECEVABLE_TRANSFERT"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/asile-avance-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private User save(User u, java.util.function.Consumer<User> init) {
        init.accept(u);
        return userRepository.save(u);
    }

    private void saveAuth(User user, String providerUserId) {
        AuthAccount a = new AuthAccount();
        a.setUser(user); a.setProvider("GOOGLE"); a.setProviderUserId(providerUserId);
        authAccountRepository.save(a);
    }

    private Workspace saveWs(User owner, String name, String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setName(name); ws.setSlug(name.toLowerCase().replace(' ', '-'));
        ws.setOwner(owner); ws.setLegalDomain(legalDomain); ws.setCountry(country);
        ws.setPlanCode("STARTER"); ws.setStatus("ACTIVE");
        return workspaceRepository.save(ws);
    }

    private void saveMember(User user, Workspace ws) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(ws); m.setUser(user); m.setMemberRole("OWNER"); m.setPrimary(true);
        workspaceMemberRepository.save(m);
    }

    private CaseFile saveCf(User user, Workspace ws, String title, String domain) {
        CaseFile cf = new CaseFile();
        cf.setTitle(title); cf.setWorkspace(ws); cf.setCreatedBy(user);
        cf.setLegalDomain(domain); cf.setStatus("OPEN");
        return caseFileRepository.save(cf);
    }

    private OAuth2AuthenticationToken buildAuth(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
