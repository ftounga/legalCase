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
class MesuresEloignementControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("mel-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mel-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI-MEL " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI-MEL " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-mel-fr-" + ts, "mel-fr-" + ts + "@ex.com");

        // BE DROIT_IMMIGRATION (gate country FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("mel-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-mel-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI-MEL " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI-MEL " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-mel-be-" + ts, "mel-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL (gate domaine → 400)
        User uDt = save(new User(), u -> { u.setEmail("mel-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-mel-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT-MEL " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT-MEL " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-mel-dt-" + ts, "mel-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dispositif, String motif,
                                     Boolean commission, Boolean urgence,
                                     Integer circularite, Integer presence,
                                     Boolean comportement) {
        Map<String, Object> m = new HashMap<>();
        m.put("dispositif", dispositif);
        m.put("motifMenace", motif);
        m.put("procedureCommissionRespectee", commission);
        m.put("urgenceAbsolueJustifiee", urgence);
        m.put("dureeCircularitePrecaire", circularite);
        m.put("dureePresenceIrreguliereMois", presence);
        m.put("comportementAggravant", comportement);
        return m;
    }

    private Map<String, Object> bodyExpulsionPrefectoraleNominal() {
        return body("EXPULSION_PREFECTORALE", "ORDRE_PUBLIC",
                true, false, null, null, false);
    }

    @Test
    void POST_fr_expulsionPrefectoraleConforme_returnsVALIDE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyExpulsionPrefectoraleNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositif").value("EXPULSION_PREFECTORALE"))
                .andExpect(jsonPath("$.verdictLegalite").value("VALIDE"))
                .andExpect(jsonPath("$.delaiRecoursJours").value(30))
                .andExpect(jsonPath("$.juridictionRecours").value("TA"))
                .andExpect(jsonPath("$.documentsRequis").isArray())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.631-1")));
    }

    @Test
    void POST_fr_expulsionMinisterielleSansUrgence_returnsCONTESTABLE() throws Exception {
        Map<String, Object> b = body("EXPULSION_MINISTERIELLE", "ORDRE_PUBLIC",
                true, false, null, null, false);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictLegalite").value("CONTESTABLE"))
                .andExpect(jsonPath("$.delaiRecoursJours").value(60))
                .andExpect(jsonPath("$.juridictionRecours").value("CE"));
    }

    @Test
    void POST_fr_irtfNominal_returnsVALIDE() throws Exception {
        Map<String, Object> b = body("IRTF", "ORDRE_PUBLIC",
                true, false, 6, 24, true);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictLegalite").value("VALIDE"))
                .andExpect(jsonPath("$.delaiRecoursJours").value(15))
                .andExpect(jsonPath("$.juridictionRecours").value("TA"));
    }

    @Test
    void POST_fr_iatTerrorisme_returnsVALIDE() throws Exception {
        Map<String, Object> b = body("IAT", "TERRORISME",
                true, false, null, null, false);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictLegalite").value("VALIDE"))
                .andExpect(jsonPath("$.juridictionRecours").value("CE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("L.222-1")));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyExpulsionPrefectoraleNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyExpulsionPrefectoraleNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyExpulsionPrefectoraleNominal())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dispositifInvalide_returns400() throws Exception {
        Map<String, Object> b = body("AUTRE_DISP", "ORDRE_PUBLIC",
                true, false, null, null, false);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifInvalide_returns400() throws Exception {
        Map<String, Object> b = body("EXPULSION_PREFECTORALE", "MOTIF_BIDON",
                true, false, null, null, false);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyExpulsionPrefectoraleNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictLegalite").value("VALIDE"));

        // Bascule motif AUTRE → NUL
        Map<String, Object> next = body("EXPULSION_PREFECTORALE", "AUTRE",
                true, false, null, null, false);
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictLegalite").value("NUL"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyExpulsionPrefectoraleNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dispositif").value("EXPULSION_PREFECTORALE"))
                .andExpect(jsonPath("$.motifMenace").value("ORDRE_PUBLIC"))
                .andExpect(jsonPath("$.verdictLegalite").value("VALIDE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immFrCf.getId() + "/mesures-eloignement-analysis")
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
