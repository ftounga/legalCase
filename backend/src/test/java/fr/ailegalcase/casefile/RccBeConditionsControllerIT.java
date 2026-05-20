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
 * SF-207-06 : tests d'intégration de l'endpoint RCC BE conditions d'éligibilité.
 *
 * <p>Couvre les cas mini-spec : POST BE 200 (nominal ELIGIBLE GENERAL),
 * POST FR 404 (gate BE strict — isolation pays), POST caseFile autre
 * workspace 404 (isolation workspace), GET après POST, GET sans POST 404,
 * validation 400 (Bean Validation sur dateNaissance / carrière hors plage),
 * date naissance futur 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RccBeConditionsControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/rcc-be-conditions";

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
    private CaseFile beCaseFile;
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("rcc-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rcc-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE RCC " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE RCC " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-rcc-be-" + ts, "rcc-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("rcc-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rcc-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR RCC " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR RCC " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rcc-fr-" + ts, "rcc-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("rcc-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-rcc-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 RCC " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 RCC " + ts, "DROIT_DU_TRAVAIL");
    }

    /**
     * Body éligible régime GENERAL : âge 60 + carrière 40 à fin 2026.
     * Naissance 1965-01-01 → âge ~ 61 ans à 2026-12-31 → toujours ≥ 60.
     */
    private Map<String, Object> bodyEligibleGeneral() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateNaissance", "1965-01-01");
        m.put("anneesCarriereProfessionnelle", 41);
        m.put("metierLourd", false);
        m.put("longueCarriere", false);
        m.put("entrepriseEnDifficulte", false);
        m.put("dateLicenciementEnvisagee", "2026-12-31");
        return m;
    }

    @Test
    void POST_workspaceBe_eligibleGeneral_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleGeneral())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.regimeApplicable").value("GENERAL"))
                .andExpect(jsonPath("$.regimesEligibles").isArray())
                .andExpect(jsonPath("$.regimesEligibles[0]").value("GENERAL"))
                .andExpect(jsonPath("$.ageALaDateLicenciement").value(61))
                .andExpect(jsonPath("$.anneesCarriereCalculees").value(41))
                .andExpect(jsonPath("$.conditionsManquantes").isArray())
                .andExpect(jsonPath("$.conditionsManquantes.length()").value(0))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("CCT n°17")));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleGeneral())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleGeneral())))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleGeneral())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.regimeApplicable").value("GENERAL"))
                .andExpect(jsonPath("$.dateNaissance").value("1965-01-01"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateNaissanceManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleGeneral();
        body.remove("dateNaissance");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_carriereHorsPlage_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleGeneral();
        body.put("anneesCarriereProfessionnelle", 80);                 // > 60 → @Max
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceFuture_returns400() throws Exception {
        Map<String, Object> body = bodyEligibleGeneral();
        body.put("dateNaissance", "2099-01-01");                       // futur → 400
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateLicenciementAvantNaissance_returns400() throws Exception {
        Map<String, Object> body = bodyEligibleGeneral();
        body.put("dateNaissance", "2000-01-01");
        body.put("dateLicenciementEnvisagee", "1999-01-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_metiersLourds_returnsRegimeApplicableCorrect() throws Exception {
        Map<String, Object> body = new HashMap<>();
        // Naissance 1968 → âge ~ 58 fin 2026 → eligible METIERS_LOURDS (58/35/flag)
        body.put("dateNaissance", "1968-06-01");
        body.put("anneesCarriereProfessionnelle", 36);
        body.put("metierLourd", true);
        body.put("longueCarriere", false);
        body.put("entrepriseEnDifficulte", false);
        body.put("dateLicenciementEnvisagee", "2026-12-31");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.regimeApplicable").value("METIERS_LOURDS"));
    }

    @Test
    void POST_nonEligible_returnsConditionsManquantes() throws Exception {
        Map<String, Object> body = new HashMap<>();
        // âge 40 + carrière 10, aucun flag → NON_ELIGIBLE
        body.put("dateNaissance", "1986-06-01");
        body.put("anneesCarriereProfessionnelle", 10);
        body.put("metierLourd", false);
        body.put("longueCarriere", false);
        body.put("entrepriseEnDifficulte", false);
        body.put("dateLicenciementEnvisagee", "2026-12-31");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_ELIGIBLE"))
                .andExpect(jsonPath("$.regimeApplicable").doesNotExist())
                .andExpect(jsonPath("$.conditionsManquantes").isNotEmpty());
    }

    // ---- helpers (alignés sur RefereTribunalTravailBeControllerIT) ----

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
