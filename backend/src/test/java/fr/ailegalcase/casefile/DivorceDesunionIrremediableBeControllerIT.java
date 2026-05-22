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
class DivorceDesunionIrremediableBeControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authDtBe;
    private CaseFile famBeCf;
    private CaseFile famFrCf;
    private CaseFile dtBeCf;

    /** 8 mois avant aujourd'hui — délai consentue OK, unilatérale KO. */
    private final LocalDate separationIlYa8Mois = LocalDate.now().minusMonths(8);

    /** 14 mois avant aujourd'hui — délai consentue + unilatérale OK. */
    private final LocalDate separationIlYa14Mois = LocalDate.now().minusMonths(14);

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE workspace DROIT_FAMILLE (cas nominal)
        User uBe = save(new User(), u -> { u.setEmail("desunion-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-desunion-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEF " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CBEF " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-desunion-be-" + ts, "desunion-be-" + ts + "@ex.com");

        // FR workspace DROIT_FAMILLE (gate country → rejet)
        User uFr = save(new User(), u -> { u.setEmail("desunion-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-desunion-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRF " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFRF " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-desunion-fr-" + ts, "desunion-fr-" + ts + "@ex.com");

        // BE workspace DROIT_DU_TRAVAIL (gate legal_domain → rejet)
        User uDtBe = save(new User(), u -> { u.setEmail("desunion-dt-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDtBe, "g-desunion-dt-be-" + ts);
        Workspace wsDtBe = saveWs(uDtBe, "WSBET " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDtBe, wsDtBe);
        dtBeCf = saveCf(uDtBe, wsDtBe, "CBET " + ts, "DROIT_DU_TRAVAIL");
        authDtBe = buildAuth("g-desunion-dt-be-" + ts, "desunion-dt-be-" + ts + "@ex.com");
    }

    private Map<String, Object> body(LocalDate dateSeparation,
                                     Boolean consentue,
                                     Boolean preuvesSep,
                                     Boolean preuvesDoc,
                                     Boolean reconciliation,
                                     LocalDate dateAssignation) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateSeparation", dateSeparation != null ? dateSeparation.toString() : null);
        m.put("separationConsentue", consentue);
        m.put("preuvesSeparation", preuvesSep);
        m.put("preuvesDocumentaires", preuvesDoc);
        m.put("tentativesReconciliation", reconciliation);
        m.put("dateAssignation", dateAssignation != null ? dateAssignation.toString() : null);
        return m;
    }

    private Map<String, Object> bodyConsentueElevee() {
        // 14 mois ≥ 6, consentue, preuves OK, doc OK, pas reconciliation → 100
        return body(separationIlYa14Mois, true, true, true, false, null);
    }

    private Map<String, Object> bodyUnilateraleElevee() {
        // 14 mois ≥ 12, unilatérale, preuves OK, doc OK, pas reconciliation → 100
        return body(separationIlYa14Mois, false, true, true, false, null);
    }

    @Test
    void POST_be_consentue_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConsentueElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.seuilSeparationMois").value(6))
                .andExpect(jsonPath("$.delaiObjectifOk").value(true))
                .andExpect(jsonPath("$.conditionsReunies").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("229")))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("Code civil belge")));
    }

    @Test
    void POST_be_unilaterale_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyUnilateraleElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seuilSeparationMois").value(12))
                .andExpect(jsonPath("$.delaiObjectifOk").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabilite").value("ELEVEE"));
    }

    @Test
    void POST_be_unilateraleSeparationCourte_moyenne() throws Exception {
        // 8 mois unilatérale → délai KO (seuil 12), preuves OK, doc OK, pas réconciliation
        // 0 + 30 + 15 + 15 = 60 → MOYENNE
        Map<String, Object> body = body(separationIlYa8Mois, false, true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seuilSeparationMois").value(12))
                .andExpect(jsonPath("$.delaiObjectifOk").value(false))
                .andExpect(jsonPath("$.scoreGlobal").value(60))
                .andExpect(jsonPath("$.verdictProbabilite").value("MOYENNE"));
    }

    @Test
    void POST_be_separationTresCourte_faible() throws Exception {
        Map<String, Object> body = body(LocalDate.now().minusMonths(1),
                false, false, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(0))
                .andExpect(jsonPath("$.verdictProbabilite").value("FAIBLE"))
                .andExpect(jsonPath("$.conditionsReunies").value(false));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConsentueElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authDtBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConsentueElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe → dossier FR → isolation 404
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConsentueElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_futureDateSeparation_returns400() throws Exception {
        Map<String, Object> body = body(LocalDate.now().plusDays(2),
                true, true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nullDateSeparation_returns400() throws Exception {
        Map<String, Object> body = body(null, true, true, true, false, null);
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConsentueElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(100));

        // Deuxième appel : dégrade → upsert (pas duplicate)
        Map<String, Object> next = body(separationIlYa8Mois, false, true, false, true, null);
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                // 0 + 30 + 0 + 0 = 30 → FAIBLE
                .andExpect(jsonPath("$.scoreGlobal").value(30))
                .andExpect(jsonPath("$.verdictProbabilite").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConsentueElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabilite").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famBeCf.getId() + "/desunion-irremediable-be")
                        .with(authentication(authBe)))
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
