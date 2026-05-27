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

/**
 * SF-219-01 : tests d'intégration end-to-end de l'outil
 * rcc-be-metiers-lourds — gate BE-only strict, isolation workspace,
 * gate {@code DROIT_DU_TRAVAIL}, persistance et upsert, validation Bean,
 * alternative 5/10 ou 7/15.
 *
 * <p>Pattern miroir de {@link LicenciementBeProtectionDelegueeControllerIT}
 * (SF-213-08).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RccBeMetiersLourdsControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/rcc-be-metiers-lourds";

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
    private OAuth2AuthenticationToken authOther;
    private CaseFile travailBeCf;
    private CaseFile travailFrCf;
    private CaseFile immBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // BE workspace DROIT_DU_TRAVAIL
        User uBe = save(new User(),
                u -> { u.setEmail("rcc-ml-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rcc-ml-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSRCCBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CRCCBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-rcc-ml-be-" + ts, "rcc-ml-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject 404)
        User uFr = save(new User(),
                u -> { u.setEmail("rcc-ml-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rcc-ml-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSRCCFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CRCCFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rcc-ml-fr-" + ts, "rcc-ml-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject 400)
        User uOt = save(new User(),
                u -> { u.setEmail("rcc-ml-ot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-rcc-ml-ot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSRCCOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CRCCOT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-rcc-ml-ot-" + ts, "rcc-ml-ot-" + ts + "@ex.com");
    }

    // ── POST nominal : toutes conditions OK → ELIGIBLE ────────────────────

    @Test
    void POST_be_toutesConditionsOK_returns200_verdictEligible() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.raisonIneligibilite").doesNotExist())
                .andExpect(jsonPath("$.synthese").value(
                        org.hamcrest.Matchers.containsString("éligible")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("CCT n°17")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("AR du 03/05/2007")))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("vérifier annuellement")));
    }

    // ── POST INELIGIBLE — démission ────────────────────────────────────────

    @Test
    void POST_be_demission_returns200_verdictIneligibleDemission() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("licenciementEffectif", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.raisonIneligibilite").value("DEMISSION_NON_ELIGIBLE"));
    }

    // ── POST INELIGIBLE — âge ──────────────────────────────────────────────

    @Test
    void POST_be_age57_returns200_verdictIneligibleAge() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("ageFinContrat", 57);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.raisonIneligibilite").value("AGE_INSUFFISANT"));
    }

    // ── POST INELIGIBLE — carrière ─────────────────────────────────────────

    @Test
    void POST_be_carriere34_returns200_verdictIneligibleCarriere() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesCarriereTotal", 34);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.raisonIneligibilite").value("CARRIERE_INSUFFISANTE"));
    }

    // ── POST INELIGIBLE — durée métier lourd ───────────────────────────────

    @Test
    void POST_be_dureeMetierLourdInsuffisante_returns200_verdictIneligibleDuree() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesMetierLourdRecent10", 4); // < 5
        body.put("anneesMetierLourdRecent15", 6); // < 7

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE"))
                .andExpect(jsonPath("$.raisonIneligibilite")
                        .value("DUREE_METIER_LOURD_INSUFFISANTE"));
    }

    // ── POST ELIGIBLE via alternative 7/15 ─────────────────────────────────

    @Test
    void POST_be_alternative7sur15_returns200_eligible() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesMetierLourdRecent10", 4); // < 5 mais
        body.put("anneesMetierLourdRecent15", 8); // >= 7

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));
    }

    // ── Gate BE-only ───────────────────────────────────────────────────────

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = baseBody();

        // user BE essaie d'accéder à un case file FR auquel il n'appartient pas
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ─────────────────────────────────────────────────────

    @Test
    void POST_typeMetierLourdManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("typeMetierLourd");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_licenciementEffectifManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("licenciementEffectif");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_age10_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("ageFinContrat", 10); // < 18

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_carriereNegative_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesCarriereTotal", -1);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Upsert + GET ───────────────────────────────────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = baseBody();
        first.put("ageFinContrat", 58);
        first.put("anneesCarriereTotal", 35);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageFinContrat").value(58))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));

        Map<String, Object> second = baseBody();
        second.put("ageFinContrat", 65);
        second.put("anneesCarriereTotal", 45);
        second.put("typeMetierLourd", "INTERRUPTIONS_HORAIRES_VARIABLES");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageFinContrat").value(65))
                .andExpect(jsonPath("$.typeMetierLourd")
                        .value("INTERRUPTIONS_HORAIRES_VARIABLES"));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageFinContrat").value(60))
                .andExpect(jsonPath("$.anneesCarriereTotal").value(38))
                .andExpect(jsonPath("$.typeMetierLourd").value("TRAVAIL_PENIBLE"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404_isolationBE() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    // ── helpers ────────────────────────────────────────────────────────────

    private Map<String, Object> baseBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ageFinContrat", 60);
        body.put("anneesCarriereTotal", 38);
        body.put("anneesMetierLourdRecent10", 6);
        body.put("anneesMetierLourdRecent15", 8);
        body.put("typeMetierLourd", "TRAVAIL_PENIBLE");
        body.put("licenciementEffectif", true);
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
