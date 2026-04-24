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
class DivorceFauteControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFrFa;
    private OAuth2AuthenticationToken authBeFa;
    private OAuth2AuthenticationToken authFrDt;
    private CaseFile faFrCf;
    private CaseFile faBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_FAMILLE → cible de l'outil
        User uFr = save(new User(), u -> { u.setEmail("df-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-df-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRF " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-df-fr-" + ts, "df-fr-" + ts + "@ex.com");

        // BE DROIT_FAMILLE → gate country FRANCE → rejet
        User uBe = save(new User(), u -> { u.setEmail("df-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-df-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEF " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEF " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-df-be-" + ts, "df-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL → gate legal_domain → rejet
        User uDt = save(new User(), u -> { u.setEmail("df-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-df-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT " + ts, "DROIT_DU_TRAVAIL");
        authFrDt = buildAuth("g-df-dt-" + ts, "df-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(List<String> fautes,
                                     Boolean preuves,
                                     Boolean tortsAdverse,
                                     Integer dureeMariageAnnees,
                                     String revDem,
                                     String revDef) {
        Map<String, Object> m = new HashMap<>();
        m.put("fautesInvoquees", fautes);
        m.put("preuvesDocumentaires", preuves);
        m.put("tortsAdverseInvoques", tortsAdverse);
        m.put("dureeMariageAnnees", dureeMariageAnnees);
        m.put("revenusAnnuelsDemandeurEur", revDem);
        m.put("revenusAnnuelsDefendeurEur", revDef);
        m.put("dateDepotAssignation", null);
        return m;
    }

    @Test
    void POST_fr_nominal_elevee_tortsExclusifs() throws Exception {
        // 3 fautes + preuves + pas torts partagés → score 100 ELEVEE + DI plein
        Map<String, Object> body = body(
                List.of("ADULTERE", "VIOLENCES", "OUTRAGES"),
                true, false, 10, "60000", "30000");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.nombreFautesInvoquees").value(3))
                .andExpect(jsonPath("$.solidariteeFautesOk").value(true))
                .andExpect(jsonPath("$.risqueTortsPartages").value(false))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabiliteDivorceFaute").value("ELEVEE"))
                .andExpect(jsonPath("$.verdictTortsEstimes").value("EXCLUSIF_DEFENDEUR"))
                .andExpect(jsonPath("$.damagesInteretsArt266FourchetteMin").value(3000.00))
                .andExpect(jsonPath("$.damagesInteretsArt266FourchetteMax").value(30000.00))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("242")));
    }

    @Test
    void POST_fr_tortsPartages_divise2() throws Exception {
        Map<String, Object> body = body(
                List.of("ADULTERE", "VIOLENCES"),
                true, true, 10, "0", "0");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTortsEstimes").value("PARTAGES"))
                .andExpect(jsonPath("$.damagesInteretsArt266FourchetteMin").value(1000.00))
                .andExpect(jsonPath("$.damagesInteretsArt266FourchetteMax").value(10000.00));
    }

    @Test
    void POST_fr_sansPreuves_impredictible_diZero() throws Exception {
        Map<String, Object> body = body(
                List.of("ADULTERE"),
                false, false, 5, "0", "0");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictTortsEstimes").value("IMPREDICTIBLE"))
                .andExpect(jsonPath("$.damagesInteretsArt266FourchetteMax").value(0));
    }

    @Test
    void POST_fr_prestationCompensatoire_calculee() throws Exception {
        // écart 30000 × 0.30 × 8 = 72000 ; ±15% → [61200 ; 82800]
        Map<String, Object> body = body(
                List.of("ADULTERE"),
                true, false, 10, "60000", "30000");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prestationCompensatoireFourchetteMin").value(61200.00))
                .andExpect(jsonPath("$.prestationCompensatoireFourchetteMax").value(82800.00));
    }

    @Test
    void POST_fr_fauteInconnue_returns400() throws Exception {
        Map<String, Object> body = body(
                List.of("INSULTES_GRAVES"),
                true, false, 5, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_missingRequiredFields_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fautesInvoquees", List.of("ADULTERE"));
        // preuvesDocumentaires manquant
        body.put("tortsAdverseInvoques", false);
        body.put("dureeMariageAnnees", 5);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        Map<String, Object> body = body(List.of("ADULTERE"), true, false, 10, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/divorce-faute")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        Map<String, Object> body = body(List.of("ADULTERE"), true, false, 10, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = body(List.of("ADULTERE"), true, false, 10, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(List.of("ADULTERE", "VIOLENCES", "OUTRAGES"),
                                        true, false, 10, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(100));

        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(List.of("ADULTERE"), false, true, 3, null, null))))
                .andExpect(status().isOk())
                // 15 + 0 + 0 = 15 FAIBLE
                .andExpect(jsonPath("$.scoreGlobal").value(15))
                .andExpect(jsonPath("$.verdictProbabiliteDivorceFaute").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(List.of("ADULTERE", "VIOLENCES", "OUTRAGES"),
                                        true, false, 10, null, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictTortsEstimes").value("EXCLUSIF_DEFENDEUR"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/divorce-faute")
                        .with(authentication(authFrFa)))
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
