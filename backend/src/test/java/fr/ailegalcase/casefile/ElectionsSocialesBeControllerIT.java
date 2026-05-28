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
 * SF-219-09 : tests d'intégration end-to-end de l'outil
 * elections-sociales-be — gate BE-only strict, isolation workspace,
 * gate DROIT_DU_TRAVAIL, persistance et upsert, validation Bean,
 * verdicts métier (seuils CE / CPPT + zone limite ETP), calendrier,
 * fenêtre de protection candidats, validation jour Y dans la fenêtre
 * AR du cycle.
 *
 * <p>Pattern miroir de {@link LicenciementBeCollectifRenaultControllerIT}
 * (SF-219-07).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ElectionsSocialesBeControllerIT {

    private static final String PATH_SUFFIX =
            "/decision-tools/elections-sociales-be";

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
                u -> { u.setEmail("esbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-esbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSESBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CESBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-esbe-" + ts, "esbe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject 404)
        User uFr = save(new User(),
                u -> { u.setEmail("esfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-esfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSESFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CESFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-esfr-" + ts, "esfr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject 400)
        User uOt = save(new User(),
                u -> { u.setEmail("esot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-esot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSESOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CESOT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-esot-" + ts, "esot-" + ts + "@ex.com");
    }

    // ── POST nominal : obligation CE + CPPT (150 ETP) ──────────────────────

    @Test
    void POST_be_effectif150_returns200_obligationCeCppt() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OBLIGATION_CE_ET_CPPT"))
                .andExpect(jsonPath("$.conseilEntrepriseObligatoire").value(true))
                .andExpect(jsonPath("$.comitePreventionProtectionObligatoire").value(true))
                .andExpect(jsonPath("$.seuilCeApplique").value(100))
                .andExpect(jsonPath("$.seuilCpptApplique").value(50))
                .andExpect(jsonPath("$.jourX").value("2024-02-13"))
                .andExpect(jsonPath("$.dateProclamationResultats").value("2024-05-19"))
                .andExpect(jsonPath("$.dateDebutPeriodeProtegeeCandidats").value("2024-02-23"))
                .andExpect(jsonPath("$.dateFinPeriodeProtegeeNonElus").value("2026-03-24"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("04/12/2007")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("19/03/1991")))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("calculateur")));
    }

    // ── OBLIGATION_CPPT_SEUL (75 ETP) ─────────────────────────────────────

    @Test
    void POST_be_effectif75_returns200_obligationCpptSeul() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("effectifMoyenEtp", 75);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OBLIGATION_CPPT_SEUL"))
                .andExpect(jsonPath("$.conseilEntrepriseObligatoire").value(false))
                .andExpect(jsonPath("$.comitePreventionProtectionObligatoire").value(true));
    }

    // ── NON_APPLICABLE_SEUIL ───────────────────────────────────────────────

    @Test
    void POST_be_effectif30_returns200_nonApplicableSeuil() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("effectifMoyenEtp", 30);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_APPLICABLE_SEUIL"))
                .andExpect(jsonPath("$.conseilEntrepriseObligatoire").value(false))
                .andExpect(jsonPath("$.comitePreventionProtectionObligatoire").value(false))
                .andExpect(jsonPath("$.dateDebutPeriodeProtegeeCandidats").doesNotExist());
    }

    // ── EFFECTIF_A_RECALCULER (zone limite ±1) ─────────────────────────────

    @Test
    void POST_be_effectif50_returns200_effectifARecalculer() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("effectifMoyenEtp", 50);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EFFECTIF_A_RECALCULER"));
    }

    @Test
    void POST_be_effectif100_returns200_effectifARecalculer() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("effectifMoyenEtp", 100);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EFFECTIF_A_RECALCULER"));
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
    void POST_cycleElectoralManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("cycleElectoral");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateJourYManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateJourY");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("effectifMoyenEtp", -3);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_uteConfirmeeNull_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("uniteTechniqueExploitationConfirmee");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_jourYHorsFenetreCycle_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        // 01/05/2024 hors fenêtre cycle 2024 (13-26/05/2024).
        body.put("dateJourY", "2024-05-01");

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
        first.put("effectifMoyenEtp", 75);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OBLIGATION_CPPT_SEUL"));

        Map<String, Object> second = baseBody();
        second.put("effectifMoyenEtp", 300);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("OBLIGATION_CE_ET_CPPT"))
                .andExpect(jsonPath("$.effectifMoyenEtp").value(300));
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
                .andExpect(jsonPath("$.verdict").value("OBLIGATION_CE_ET_CPPT"))
                .andExpect(jsonPath("$.seuilCeApplique").value(100));
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
        body.put("cycleElectoral", "CYCLE_2024");
        body.put("dateJourY", "2024-05-13");
        body.put("effectifMoyenEtp", 150);
        body.put("uniteTechniqueExploitationConfirmee", true);
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
