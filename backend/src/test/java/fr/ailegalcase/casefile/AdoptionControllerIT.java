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
import java.util.LinkedHashMap;
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
class AdoptionControllerIT {

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
    private CaseFile famFrCf;
    private CaseFile famBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("adopt-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-adopt-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSADF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFADF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-adopt-fr-" + ts, "adopt-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE (gate FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("adopt-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-adopt-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSADB " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFADB " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-adopt-be-" + ts, "adopt-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate domain → 400)
        User uDt = save(new User(), u -> { u.setEmail("adopt-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-adopt-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSADDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFADDT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-adopt-dt-" + ts, "adopt-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> nominalBodyPleniere() {
        Map<String, Object> body = new LinkedHashMap<>();
        // Couple marié 35 ans + enfant 5 ans, tous critères OK
        body.put("formeAdoption", "PLENIERE");
        body.put("ageAdoptant", 35);
        body.put("ageAdopte", 5);
        body.put("consentementParents", true);
        body.put("consentementAdopte", false);
        body.put("consentementConjointAdoptant", true);
        body.put("enquetes", true);
        body.put("placement6mois", true);
        body.put("pupilleEtat", false);
        body.put("adoptantMarie", true);
        return body;
    }

    private Map<String, Object> nominalBodySimple() {
        Map<String, Object> body = new LinkedHashMap<>();
        // Adoption simple d'un majeur, célibataire 40 ans
        body.put("formeAdoption", "SIMPLE");
        body.put("ageAdoptant", 40);
        body.put("ageAdopte", 20);
        body.put("consentementParents", false);
        body.put("consentementAdopte", true);
        body.put("consentementConjointAdoptant", false);
        body.put("enquetes", false);
        body.put("placement6mois", false);
        body.put("pupilleEtat", false);
        body.put("adoptantMarie", false);
        return body;
    }

    @Test
    void POST_fr_pleniereValide_returnsELEVEE_formePLENIERE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodyPleniere())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.formeRecommandee").value("PLENIERE"))
                .andExpect(jsonPath("$.differenceAgeAns").value(30))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("343")));
    }

    @Test
    void POST_fr_simpleValide_returnsELEVEE_formeSIMPLE() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodySimple())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.formeRecommandee").value("SIMPLE"));
    }

    @Test
    void POST_fr_basculePleniereSimple_quandPleniereImpossible() throws Exception {
        // Adopté 17 ans → trop âgé pour plénière, OK pour simple
        Map<String, Object> body = nominalBodyPleniere();
        body.put("ageAdopte", 17);
        body.put("consentementAdopte", true);
        // Pour SIMPLE : adopté 17 < 18 → consentement parents requis (déjà true)
        // diff=18 ≥15 ✓, marié+conjoint ✓, adopté 17≥13 + consent ✓
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.formeAdoption").value("PLENIERE"))
                .andExpect(jsonPath("$.formeRecommandee").value("SIMPLE"));
    }

    @Test
    void POST_fr_critereCardinalManquant_returnsFAIBLE() throws Exception {
        // Différence d'âge insuffisante en simple
        Map<String, Object> body = nominalBodySimple();
        body.put("ageAdoptant", 30);
        body.put("ageAdopte", 20); // diff = 10 < 15
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("FAIBLE"))
                .andExpect(jsonPath("$.formeRecommandee").value("AUCUNE"));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodyPleniere())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodyPleniere())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodyPleniere())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_missingFormeAdoption_returns400() throws Exception {
        Map<String, Object> body = nominalBodyPleniere();
        body.put("formeAdoption", null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodyPleniere())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"));

        Map<String, Object> next = nominalBodyPleniere();
        next.put("ageAdoptant", 22); // trop jeune
        next.put("placement6mois", false);
        next.put("consentementConjointAdoptant", false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictRecevabilite").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nominalBodyPleniere())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictRecevabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.formeRecommandee").value("PLENIERE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId()
                                + "/adoption-analysis")
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
