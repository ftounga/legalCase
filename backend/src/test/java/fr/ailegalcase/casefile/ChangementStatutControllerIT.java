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

import java.math.BigDecimal;
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
class ChangementStatutControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("chgst-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-chgst-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-CS " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-CS " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-chgst-fr-" + ts, "chgst-fr-" + ts + "@ex.com");

        // BE DROIT_IMMIGRATION (gate country FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("chgst-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-chgst-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-CS " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-CS " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-chgst-be-" + ts, "chgst-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL (gate domaine → 400)
        User uDt = save(new User(), u -> { u.setEmail("chgst-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-chgst-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-CS " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-CS " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-chgst-dt-" + ts, "chgst-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String src, String dst, Integer duree, Boolean justif,
                                     BigDecimal remu, Boolean casierVierge) {
        Map<String, Object> m = new HashMap<>();
        m.put("titreActuel", src);
        m.put("titreEnvisage", dst);
        m.put("dureeRestanteSurTitreActuelMois", duree);
        m.put("documentJustificatifFourni", justif);
        m.put("remunerationContratEur", remu);
        m.put("casierJudiciaireVierge", casierVierge);
        return m;
    }

    private Map<String, Object> bodyEtudiantSalarieNominal() {
        return body("ETUDIANT", "SALARIE", 10, true, new BigDecimal("2800.00"), true);
    }

    @Test
    void POST_fr_etudiantSalarie_returnsELEVEE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEtudiantSalarieNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.titreActuel").value("ETUDIANT"))
                .andExpect(jsonPath("$.titreEnvisage").value("SALARIE"))
                .andExpect(jsonPath("$.verdictTransition").value("ELEVEE"))
                .andExpect(jsonPath("$.documentsRequis").isArray())
                .andExpect(jsonPath("$.delaiInstructionMois").value(3))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.421-1")));
    }

    @Test
    void POST_fr_dureeSousSeuil_returnsFAIBLE() throws Exception {
        Map<String, Object> b = body("ETUDIANT", "SALARIE", 1, true,
                new BigDecimal("2800.00"), true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("FAIBLE"));
    }

    @Test
    void POST_fr_remunerationLimite_returnsMOYENNE() throws Exception {
        Map<String, Object> b = body("ETUDIANT", "SALARIE", 10, true,
                new BigDecimal("2200.00"), true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("MOYENNE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEtudiantSalarieNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEtudiantSalarieNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr → dossier BE → isolation 404
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEtudiantSalarieNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_transitionInvalide_returns400() throws Exception {
        Map<String, Object> b = body("SALARIE", "VPF", 10, true, null, true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dureeRestanteNull_returns400() throws Exception {
        Map<String, Object> b = body("ETUDIANT", "SALARIE", null, true,
                new BigDecimal("2800.00"), true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEtudiantSalarieNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("ELEVEE"));

        Map<String, Object> next = body("ETUDIANT", "SALARIE", 10, false,
                new BigDecimal("2800.00"), true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEtudiantSalarieNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.titreActuel").value("ETUDIANT"))
                .andExpect(jsonPath("$.verdictTransition").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_vpfSalarie_returnsELEVEE() throws Exception {
        Map<String, Object> b = body("VPF", "SALARIE", 10, true,
                new BigDecimal("2800.00"), true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("ELEVEE"));
    }

    @Test
    void POST_etudiantAps_returnsELEVEE_delai2() throws Exception {
        Map<String, Object> b = body("ETUDIANT", "APS", 10, true, null, true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/changement-statut-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTransition").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiInstructionMois").value(2));
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
