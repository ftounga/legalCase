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
class AtMpControllerIT {

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
    private OAuth2AuthenticationToken authIm;
    private CaseFile dtFrCf;
    private CaseFile dtBeCf;
    private CaseFile imFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_DU_TRAVAIL — workspace nominal
        User uFr = save(new User(), u -> { u.setEmail("atmp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-atmp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRT-ATMP " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        dtFrCf = saveCf(uFr, wsFr, "CFRT-ATMP " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-atmp-fr-" + ts, "atmp-fr-" + ts + "@ex.com");

        // BE DROIT_DU_TRAVAIL — gate country FRANCE → 400
        User uBe = save(new User(), u -> { u.setEmail("atmp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-atmp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBET-ATMP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        dtBeCf = saveCf(uBe, wsBe, "CBET-ATMP " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-atmp-be-" + ts, "atmp-be-" + ts + "@ex.com");

        // FR DROIT_IMMIGRATION — gate domaine → 400
        User uIm = save(new User(), u -> { u.setEmail("atmp-im-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uIm, "g-atmp-im-" + ts);
        Workspace wsIm = saveWs(uIm, "WSFRI-ATMP " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uIm, wsIm);
        imFrCf = saveCf(uIm, wsIm, "CFRI-ATMP " + ts, "DROIT_IMMIGRATION");
        authIm = buildAuth("g-atmp-im-" + ts, "atmp-im-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyAtNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("dispositif", "RECONNAISSANCE_AT");
        m.put("dateAccident", "2026-03-15");
        m.put("lieuTravail", true);
        m.put("declarationEmployeurDansLes48h", true);
        m.put("certificatMedicalInitial", true);
        return m;
    }

    private Map<String, Object> bodyMpHorsTableau() {
        Map<String, Object> m = new HashMap<>();
        m.put("dispositif", "RECONNAISSANCE_MP");
        m.put("numeroTableau", "HORS_TABLEAU");
        m.put("delaiPriseEnChargeRespecte", true);
        m.put("certificatMedicalInitial", true);
        m.put("dateExposition", "2025-06-01");
        return m;
    }

    private Map<String, Object> bodyIppNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("dispositif", "CONTESTATION_TAUX_IPP");
        m.put("tauxFixeParCpam", 8);
        m.put("tauxRevendique", 25);
        m.put("expertiseMedicaleProduite", true);
        m.put("datePremierAvisCpam", "2026-03-01");
        return m;
    }

    @Test
    void POST_atNominalFr_returnsELEVEE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAtNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositif").value("RECONNAISSANCE_AT"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiInstructionJours").value(90))
                .andExpect(jsonPath("$.competence").value("CPAM"))
                .andExpect(jsonPath("$.expertiseRequise").value(false))
                .andExpect(jsonPath("$.documentsRequis").isArray())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.411-1")));
    }

    @Test
    void POST_ippNominalFr_returnsELEVEE_competenceCMRA() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIppNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispositif").value("CONTESTATION_TAUX_IPP"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiInstructionJours").value(120))
                .andExpect(jsonPath("$.competence").value("CMRA"))
                .andExpect(jsonPath("$.expertiseRequise").value(true))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.434-2")));
    }

    @Test
    void POST_mpHorsTableauFr_returnsMOYENNE_competenceCRRMP() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyMpHorsTableau())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispositif").value("RECONNAISSANCE_MP"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("MOYENNE"))
                .andExpect(jsonPath("$.competence").value("CRRMP"))
                .andExpect(jsonPath("$.expertiseRequise").value(true));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/at-mp-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAtNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_immigrationDomain_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + imFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authIm)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAtNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAtNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dispositifInvalide_returns400() throws Exception {
        Map<String, Object> b = new HashMap<>();
        b.put("dispositif", "AUTRE_DISP");
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAtNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositif").value("RECONNAISSANCE_AT"))
                .andExpect(jsonPath("$.lieuTravail").value(true))
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyAtNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"));

        // Bascule vers IPP — l'analyse doit être remplacée (1:1 par dossier)
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/at-mp-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIppNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dispositif").value("CONTESTATION_TAUX_IPP"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"));
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
