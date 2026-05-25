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
 * SF-213-02 : tests d'intégration end-to-end de l'outil rappel-salaire-be —
 * gate BE-only strict, isolation workspace, persistance et upsert, validation
 * Bean, prescription PENDANT_CONTRAT vs POST_RUPTURE.
 *
 * <p>Pattern mirroré de {@link ClauseNonConcurrenceBeControllerIT}
 * (SF-213-01).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RappelSalaireBeControllerIT {

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
        User uBe = save(new User(), u -> { u.setEmail("rsbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rsbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-rsbe-" + ts, "rsbe-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(), u -> { u.setEmail("rsfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rsfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rsfr-" + ts, "rsfr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(), u -> { u.setEmail("rso-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-rso-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-rso-" + ts, "rso-" + ts + "@ex.com");
    }

    @Test
    void POST_be_nominalPendantContrat_returns200() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("dateActionEnvisagee", "2025-06-30");
        body.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantBrut").value(5000.00))
                .andExpect(jsonPath("$.interetsCourus").value(500.00))
                .andExpect(jsonPath("$.totalReclame").value(5500.00))
                .andExpect(jsonPath("$.tauxMoratoire").value(
                        org.hamcrest.Matchers.containsString("10")))
                .andExpect(jsonPath("$.dateLimitePrescription").value("2029-06-30"))
                .andExpect(jsonPath("$.statutPrescription").value("NON_PRESCRIT"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Loi 12/04/1965")));
    }

    @Test
    void POST_be_postRuptureNominal_returns200() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 3000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("dateRupture", "2024-06-30");
        body.put("dateActionEnvisagee", "2025-01-01");
        body.put("typeArriere", "POST_RUPTURE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dateLimitePrescription").value("2025-06-30"))
                .andExpect(jsonPath("$.statutPrescription").value("NON_PRESCRIT"));
    }

    @Test
    void POST_be_postRupturePrescrit_returnsPrescrit() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 3000.00);
        body.put("dateDebutPeriode", "2022-01-01");
        body.put("dateFinPeriode", "2022-06-30");
        body.put("dateRupture", "2023-01-01");
        body.put("dateActionEnvisagee", "2025-01-01");
        body.put("typeArriere", "POST_RUPTURE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statutPrescription").value("PRESCRIT"));
    }

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation workspace)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_montantNegatif_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", -10);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_typeArriereManquant_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        // typeArriere omis → Bean Validation @NotNull

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_postRuptureSansDateRupture_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("typeArriere", "POST_RUPTURE");
        // dateRupture absente → 400 levé par le calculateur

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("montantBrut", 5000.00);
        first.put("dateDebutPeriode", "2024-01-01");
        first.put("dateFinPeriode", "2024-06-30");
        first.put("dateActionEnvisagee", "2025-06-30");
        first.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantBrut").value(5000.00));

        // Deuxième POST avec autres inputs : upsert → on remplace
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("montantBrut", 8000.00);
        second.put("dateDebutPeriode", "2024-01-01");
        second.put("dateFinPeriode", "2024-06-30");
        second.put("dateActionEnvisagee", "2025-06-30");
        second.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantBrut").value(8000.00))
                .andExpect(jsonPath("$.interetsCourus").value(800.00));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("montantBrut", 5000.00);
        body.put("dateDebutPeriode", "2024-01-01");
        body.put("dateFinPeriode", "2024-06-30");
        body.put("dateActionEnvisagee", "2025-06-30");
        body.put("typeArriere", "PENDANT_CONTRAT");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.montantBrut").value(5000.00))
                .andExpect(jsonPath("$.totalReclame").value(5500.00))
                .andExpect(jsonPath("$.statutPrescription").value("NON_PRESCRIT"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId()
                                + "/decision-tools/rappel-salaire-be")
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404_isolationBE() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + travailFrCf.getId()
                                + "/decision-tools/rappel-salaire-be")
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
