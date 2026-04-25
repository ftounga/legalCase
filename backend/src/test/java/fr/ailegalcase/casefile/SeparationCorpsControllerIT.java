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
import java.time.LocalDate;
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
class SeparationCorpsControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("sepcrp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-sepcrp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFAS " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFAS " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-sepcrp-fr-" + ts, "sepcrp-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE (gate FRANCE → 400)
        User uBe = save(new User(), u -> { u.setEmail("sepcrp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-sepcrp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBES " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CBES " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-sepcrp-be-" + ts, "sepcrp-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate domain → 400)
        User uDt = save(new User(), u -> { u.setEmail("sepcrp-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-sepcrp-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-sepcrp-dt-" + ts, "sepcrp-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String mode,
                                     LocalDate dateJugement,
                                     LocalDate dateConversion,
                                     Integer duree,
                                     Boolean consentement,
                                     Boolean patrimoine,
                                     Integer enfants,
                                     Boolean reconciliation) {
        Map<String, Object> m = new HashMap<>();
        m.put("modeProcedure", mode);
        m.put("dateJugementSeparationCorps", dateJugement != null ? dateJugement.toString() : null);
        m.put("dateRequeteConversion", dateConversion != null ? dateConversion.toString() : null);
        m.put("dureeSeparationAnnees", duree);
        m.put("consentementMutuelConversion", consentement);
        m.put("patrimoineCommun", patrimoine);
        m.put("enfantsMineurs", enfants);
        m.put("demandeReconciliationFormulee", reconciliation);
        return m;
    }

    private Map<String, Object> bodyNominalPossible() {
        return body("CONSENTEMENT_MUTUEL", LocalDate.now().minusYears(3), null,
                3, true, true, 0, false);
    }

    @Test
    void POST_fr_consentementMutuel_possible() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalPossible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dureeSeparationOk").value(true))
                .andExpect(jsonPath("$.delaiConversion2AnsAtteint").value(true))
                .andExpect(jsonPath("$.conversionAutomatiquePossible").value(true))
                .andExpect(jsonPath("$.scoreEligibiliteConversion").value(100))
                .andExpect(jsonPath("$.verdictConversion").value("POSSIBLE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("296")))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("306")));
    }

    @Test
    void POST_fr_modeFaute_possible() throws Exception {
        Map<String, Object> body = body("FAUTE", LocalDate.now().minusYears(2), null,
                2, false, false, 0, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictConversion").value("POSSIBLE"));
    }

    @Test
    void POST_reconciliation_bloque() throws Exception {
        Map<String, Object> body = body("CONSENTEMENT_MUTUEL", LocalDate.now().minusYears(3), null,
                3, true, false, 0, true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictConversion").value("RECONCILIATION_BLOQUE"))
                .andExpect(jsonPath("$.conversionAutomatiquePossible").value(false));
    }

    @Test
    void POST_delaiInferieur2Ans_prematuree() throws Exception {
        Map<String, Object> body = body("CONSENTEMENT_MUTUEL", LocalDate.now().minusMonths(6), null,
                1, true, false, 0, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictConversion").value("PREMATUREE"))
                .andExpect(jsonPath("$.dureeSeparationOk").value(false));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/separation-corps")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalPossible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/separation-corps")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalPossible())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalPossible())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_missingMode_returns400() throws Exception {
        Map<String, Object> body = body(null, LocalDate.now().minusYears(3), null,
                3, true, false, 0, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_missingDuree_returns400() throws Exception {
        Map<String, Object> body = body("CONSENTEMENT_MUTUEL", LocalDate.now().minusYears(3), null,
                null, true, false, 0, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_modeInconnu_returns400() throws Exception {
        Map<String, Object> body = body("DIVORCE_PAR_LASSITUDE", LocalDate.now().minusYears(3), null,
                3, true, false, 0, false);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalPossible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreEligibiliteConversion").value(100));

        Map<String, Object> next = body("CONSENTEMENT_MUTUEL", LocalDate.now().minusYears(3), null,
                3, true, false, 0, true);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictConversion").value("RECONCILIATION_BLOQUE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalPossible())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreEligibiliteConversion").value(100))
                .andExpect(jsonPath("$.verdictConversion").value("POSSIBLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/separation-corps")
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
