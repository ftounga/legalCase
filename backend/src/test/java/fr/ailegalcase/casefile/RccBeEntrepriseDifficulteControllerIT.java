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
 * SF-219-03 : tests d'intégration end-to-end de l'outil
 * rcc-be-entreprise-difficulte — gate BE-only strict, isolation workspace,
 * persistance et upsert, validation Bean, hiérarchie verdicts (démission >
 * reconnaissance > âge > carrière > ancienneté), calcul indicatif
 * indemnité complémentaire.
 *
 * <p>Pattern miroir de {@link RccBeLongueCarriereControllerIT} (SF-219-02).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RccBeEntrepriseDifficulteControllerIT {

    private static final String PATH_SUFFIX =
            "/decision-tools/rcc-be-entreprise-difficulte";
    private static final String DATE_FIN = "2027-03-31";

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
                u -> { u.setEmail("redbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-redbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSREDBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CREDBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-redbe-" + ts, "redbe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject 404)
        User uFr = save(new User(),
                u -> { u.setEmail("redfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-redfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSREDFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CREDFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-redfr-" + ts, "redfr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject 400)
        User uOt = save(new User(),
                u -> { u.setEmail("redot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-redot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSREDOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CREDOT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-redot-" + ts, "redot-" + ts + "@ex.com");
    }

    // ── POST nominal : éligible ────────────────────────────────────────────

    @Test
    void POST_be_eligible_returns200_verdictEligible() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE"))
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.conditionReconnaissanceRemplie").value(true))
                .andExpect(jsonPath("$.conditionAgeRemplie").value(true))
                .andExpect(jsonPath("$.conditionCarriereRemplie").value(true))
                .andExpect(jsonPath("$.conditionAncienneteRemplie").value(true))
                .andExpect(jsonPath("$.conditionLicenciementRemplie").value(true))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("AR du 03/05/2007")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("CCT n° 17")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("reconnaissance")));
    }

    @Test
    void POST_be_eligibleAvecMontants_calculeIndemniteComplementaire() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationNetteMensuelleReference", 3200.00);
        body.put("allocationChomageMensuelleEstimee", 1800.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteComplementaireMensuelle").value(700.00))
                .andExpect(jsonPath("$.avertissement")
                        .value(org.hamcrest.Matchers.containsString("indicatif")));
    }

    // ── Inéligibilité par démission ────────────────────────────────────────

    @Test
    void POST_be_demission_verdictIneligibleDemission() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("licenciementEffectif", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_DEMISSION"))
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.indemniteComplementaireMensuelle").doesNotExist());
    }

    // ── Inéligibilité par reconnaissance absente ───────────────────────────

    @Test
    void POST_be_nonReconnue_verdictIneligibleReconnaissance() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("typeReconnaissance", "NON_RECONNUE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_RECONNAISSANCE_ABSENTE"))
                .andExpect(jsonPath("$.conditionReconnaissanceRemplie").value(false));
    }

    // ── Inéligibilité par âge ──────────────────────────────────────────────

    @Test
    void POST_be_ageBas_verdictIneligibleAge() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("ageFinContrat", 54);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_AGE_INSUFFISANT"))
                .andExpect(jsonPath("$.conditionAgeRemplie").value(false));
    }

    // ── Inéligibilité par carrière ─────────────────────────────────────────

    @Test
    void POST_be_carriereInsuffisante_verdictIneligibleCarriere() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesCarriereTotale", 9);
        body.put("anneesAncienneteSecteur", 5);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_CARRIERE_INSUFFISANTE"))
                .andExpect(jsonPath("$.conditionCarriereRemplie").value(false));
    }

    // ── Inéligibilité par ancienneté ───────────────────────────────────────

    @Test
    void POST_be_ancienneteInsuffisante_verdictIneligibleAnciennete() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesAncienneteSecteur", 4);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_ANCIENNETE_INSUFFISANTE"))
                .andExpect(jsonPath("$.conditionAncienneteRemplie").value(false));
    }

    // ── Gate BE-only ────────────────────────────────────────────────────────

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

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ──────────────────────────────────────────────────────

    @Test
    void POST_typeReconnaissanceManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("typeReconnaissance");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ageReduitPlanManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("ageReduitPlan");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ageManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("ageFinContrat");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_carriereManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("anneesCarriereTotale");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("anneesAncienneteSecteur");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFinManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateFinContrat");

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
    void POST_ageNegatif_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("ageFinContrat", -5);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationNegative_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationNetteMensuelleReference", -100);
        body.put("allocationChomageMensuelleEstimee", 1500);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ancienneteSuperieureCarriere_returns400_coherence() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesCarriereTotale", 10);
        body.put("anneesAncienneteSecteur", 12);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Upsert + GET ────────────────────────────────────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = baseBody();
        first.put("ageFinContrat", 55);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageFinContrat").value(55));

        Map<String, Object> second = baseBody();
        second.put("ageFinContrat", 60);
        second.put("anneesCarriereTotale", 20);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageFinContrat").value(60))
                .andExpect(jsonPath("$.anneesCarriereTotale").value(20));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationNetteMensuelleReference", 3000.00);
        body.put("allocationChomageMensuelleEstimee", 1500.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE"))
                .andExpect(jsonPath("$.typeReconnaissance").value("EN_DIFFICULTE"))
                .andExpect(jsonPath("$.ageReduitPlan").value(55))
                .andExpect(jsonPath("$.ageFinContrat").value(55))
                .andExpect(jsonPath("$.anneesCarriereTotale").value(10))
                .andExpect(jsonPath("$.anneesAncienneteSecteur").value(5))
                .andExpect(jsonPath("$.dateFinContrat").value(DATE_FIN))
                .andExpect(jsonPath("$.indemniteComplementaireMensuelle").value(750.00));
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
        body.put("typeReconnaissance", "EN_DIFFICULTE");
        body.put("ageReduitPlan", 55);
        body.put("ageFinContrat", 55);
        body.put("anneesCarriereTotale", 10);
        body.put("anneesAncienneteSecteur", 5);
        body.put("dateFinContrat", DATE_FIN);
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
