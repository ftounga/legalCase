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

/**
 * SF-DT-25-01 : tests d'intégration de l'endpoint indemnité compensatrice de préavis FR.
 * Couvre le nominal CCN/légale, l'exemption false, l'upsert, l'isolation workspace,
 * le reject domaine (immigration), le reject pays (BE), et GET avant/après POST.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class IndemnitePreavisControllerIT {

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
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailFrCf;
    private CaseFile travailBeCf;
    private CaseFile immFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR workspace DROIT_DU_TRAVAIL
        User uFr = save(new User(), u -> { u.setEmail("prea-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-prea-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-prea-fr-" + ts, "prea-fr-" + ts + "@ex.com");

        // BE workspace DROIT_DU_TRAVAIL (pour reject country)
        User uBe = save(new User(), u -> { u.setEmail("prea-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-prea-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-prea-be-" + ts, "prea-be-" + ts + "@ex.com");

        // FR workspace DROIT_IMMIGRATION (pour reject domain)
        User uOt = save(new User(), u -> { u.setEmail("prea-im-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-prea-im-" + ts);
        Workspace wsOt = saveWs(uOt, "WSIM " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uOt, wsOt);
        immFrCf = saveCf(uOt, wsOt, "CIM " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-prea-im-" + ts, "prea-im-" + ts + "@ex.com");
    }

    @Test
    void POST_nominal_ccnBanque_persists_and_returns200() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", 5);
        body.put("ancienneteMois", 60);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("conventionCollectiveCode", "IDCC_2120");
        body.put("fonction", "EMPLOYE");
        body.put("exemptionEmployeur", true);
        body.put("dateRupture", "2026-04-25");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisMois").value(3))
                .andExpect(jsonPath("$.sourceDuree").value("CCN"))
                .andExpect(jsonPath("$.montantIndemniteEur").value(7500.00))
                .andExpect(jsonPath("$.exemptionRetenue").value(true))
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.baseJuridique").value(org.hamcrest.Matchers.containsString("L.1234-1")))
                .andExpect(jsonPath("$.baseJuridique").value(org.hamcrest.Matchers.containsString("IDCC_2120")));
    }

    @Test
    void POST_sansCcn_returnsLegale() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", 5);
        body.put("ancienneteMois", 60);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("fonction", "EMPLOYE");
        body.put("exemptionEmployeur", true);
        body.put("dateRupture", "2026-04-25");
        // conventionCollectiveCode absent → durée légale

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisMois").value(2))
                .andExpect(jsonPath("$.sourceDuree").value("LEGALE"))
                .andExpect(jsonPath("$.montantIndemniteEur").value(5000.00));
    }

    @Test
    void POST_ccnInconnue_fallbackLegale() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", 5);
        body.put("ancienneteMois", 60);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("conventionCollectiveCode", "IDCC_9999");
        body.put("fonction", "EMPLOYE");
        body.put("exemptionEmployeur", true);
        body.put("dateRupture", "2026-04-25");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisMois").value(2))
                .andExpect(jsonPath("$.sourceDuree").value("LEGALE"));
    }

    @Test
    void POST_exemptionFalse_returnsZero() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", 5);
        body.put("ancienneteMois", 60);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("fonction", "EMPLOYE");
        body.put("exemptionEmployeur", false);
        body.put("dateRupture", "2026-04-25");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantIndemniteEur").value(0))
                .andExpect(jsonPath("$.exemptionRetenue").value(false));
    }

    @Test
    void POST_upsertReplacesExistingAnalysis() throws Exception {
        Map<String, Object> body1 = new HashMap<>();
        body1.put("ancienneteAnnees", 1);
        body1.put("ancienneteMois", 12);
        body1.put("salaireMensuelBrutEur", 2500.00);
        body1.put("fonction", "EMPLOYE");
        body1.put("exemptionEmployeur", true);
        body1.put("dateRupture", "2026-04-25");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisMois").value(1))
                .andExpect(jsonPath("$.montantIndemniteEur").value(2500.00));

        Map<String, Object> body2 = new HashMap<>();
        body2.put("ancienneteAnnees", 5);
        body2.put("ancienneteMois", 60);
        body2.put("salaireMensuelBrutEur", 3000.00);
        body2.put("fonction", "EMPLOYE");
        body2.put("exemptionEmployeur", true);
        body2.put("dateRupture", "2026-04-25");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisMois").value(2))
                .andExpect(jsonPath("$.montantIndemniteEur").value(6000.00));
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = validBody();
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400() throws Exception {
        Map<String, Object> body = validBody();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authOther)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_belgiumWorkspace_returns400() throws Exception {
        Map<String, Object> body = validBody();
        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + "/indemnite-preavis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireZero_returns400() throws Exception {
        Map<String, Object> body = validBody();
        body.put("salaireMensuelBrutEur", 0);
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersistedAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody())))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteMois").value(60))
                .andExpect(jsonPath("$.sourceDuree").value(org.hamcrest.Matchers.anyOf(
                        org.hamcrest.Matchers.is("LEGALE"),
                        org.hamcrest.Matchers.is("CCN"))));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + "/indemnite-preavis")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers ----

    private Map<String, Object> validBody() {
        Map<String, Object> body = new HashMap<>();
        body.put("ancienneteAnnees", 5);
        body.put("ancienneteMois", 60);
        body.put("salaireMensuelBrutEur", 2500.00);
        body.put("fonction", "EMPLOYE");
        body.put("exemptionEmployeur", true);
        body.put("dateRupture", "2026-04-25");
        return body;
    }

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
