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
 * SF-219-04 : tests d'intégration end-to-end de l'outil
 * cumul-rcc-allocations — gate BE-only strict, isolation workspace,
 * persistance et upsert, validation Bean, hiérarchie verdicts
 * (pension > activité non déclarée > plafond), calcul du cumul + de la
 * réduction, régime de disponibilité.
 *
 * <p>Pattern miroir de {@link RccBeLongueCarriereControllerIT}
 * (SF-219-02).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CumulRccAllocationsControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/cumul-rcc-allocations";

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
                u -> { u.setEmail("crabe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-crabe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSCRABE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CCRABE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-crabe-" + ts, "crabe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject 404)
        User uFr = save(new User(),
                u -> { u.setEmail("crafr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-crafr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSCRAFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CCRAFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-crafr-" + ts, "crafr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject 400)
        User uOt = save(new User(),
                u -> { u.setEmail("craot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-craot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSCRAOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CCRAOT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-craot-" + ts, "craot-" + ts + "@ex.com");
    }

    // ── POST nominal : conforme ────────────────────────────────────────────

    @Test
    void POST_be_conforme_returns200_verdictConforme() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CUMUL_CONFORME"))
                .andExpect(jsonPath("$.conforme").value(true))
                .andExpect(jsonPath("$.plafondRespecte").value(true))
                .andExpect(jsonPath("$.cumulMensuelTotal").value(2500.00))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("CCT n° 17")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("AR du 25/11/1991")))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("indicatif")));
    }

    // ── Cas plafond dépassé ────────────────────────────────────────────────

    @Test
    void POST_be_plafondDepasse_returns200_verdictDepasseEtReduction() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("indemniteComplementaireMensuelle", 1800.00); // total 3600 > 3200

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CUMUL_PLAFOND_DEPASSE"))
                .andExpect(jsonPath("$.conforme").value(false))
                .andExpect(jsonPath("$.plafondRespecte").value(false))
                .andExpect(jsonPath("$.montantReductionIndemnite").value(400.00));
    }

    // ── Bascule pension légale prioritaire ─────────────────────────────────

    @Test
    void POST_be_ageLegalAtteint_returns200_basculePension() throws Exception {
        Map<String, Object> body = baseBody();
        // Né en 1960, RCC en 2026 → 65 ans
        body.put("dateNaissance", "1960-01-15");
        body.put("dateDebutRcc", "2025-06-01");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("BASCULE_PENSION_LEGALE"))
                .andExpect(jsonPath("$.ageLegalPensionAtteint").value(true));
    }

    // ── Activité non déclarée prioritaire ──────────────────────────────────

    @Test
    void POST_be_activiteNonDeclaree_returns200_incompatible() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("activiteComplementaire", "ACTIVITE_NON_DECLAREE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INCOMPATIBLE_ACTIVITE_NON_AUTORISEE"));
    }

    // ── Régime de disponibilité ─────────────────────────────────────────────

    @Test
    void POST_be_age62_disponibilitePassive() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateNaissance", "1963-01-15");
        body.put("dateDebutRcc", "2025-06-01"); // 62 ans

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeDisponibilite")
                        .value("DISPONIBILITE_PASSIVE"));
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

        // user BE essaie d'accéder à un case file FR auquel il n'appartient pas
        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    // ── Validation 400 ──────────────────────────────────────────────────────

    @Test
    void POST_dateNaissanceManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateNaissance");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_allocationManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("allocationChomageMensuelle");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_allocationNegative_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("allocationChomageMensuelle", -100);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_carriereNegative_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("anneesCarriereTotale", -3);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_activiteManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("activiteComplementaire");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDebutRccAvantNaissance_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("dateNaissance", "2026-01-15");
        body.put("dateDebutRcc", "2024-01-15");

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
        first.put("indemniteComplementaireMensuelle", 700.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteComplementaireMensuelle").value(700.00));

        Map<String, Object> second = baseBody();
        second.put("indemniteComplementaireMensuelle", 900.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indemniteComplementaireMensuelle").value(900.00));
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
                .andExpect(jsonPath("$.verdict").value("CUMUL_CONFORME"))
                .andExpect(jsonPath("$.cumulMensuelTotal").value(2500.00));
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
        body.put("dateNaissance", "1965-01-15");
        body.put("dateDebutRcc", "2026-01-15"); // 61 ans
        body.put("allocationChomageMensuelle", 1800.00);
        body.put("indemniteComplementaireMensuelle", 700.00);
        body.put("remunerationNetteReference", 3200.00);
        body.put("anneesCarriereTotale", 35);
        body.put("activiteComplementaire", "AUCUNE");
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
