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
 * SF-213-03 : tests d'intégration end-to-end de l'outil
 * licenciement-be-statut-unique-preavis — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean.
 *
 * <p>Pattern mirroré de {@link ClauseNonConcurrenceBeControllerIT} (SF-213-01).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class LicenciementBeStatutUniquePreavisControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/licenciement-be-statut-unique-preavis";

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
        User uBe = save(new User(), u -> { u.setEmail("preavis-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-preavis-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-preavis-be-" + ts, "preavis-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(), u -> { u.setEmail("preavis-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-preavis-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-preavis-fr-" + ts, "preavis-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(), u -> { u.setEmail("preavis-o-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-preavis-o-" + ts);
        Workspace wsOt = saveWs(uOt, "WSOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "COT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-preavis-o-" + ts, "preavis-o-" + ts + "@ex.com");
    }

    @Test
    void POST_be_nominal_9ans_27semaines_returns200() throws Exception {
        // Cas mini-spec : 9 ans → 27 semaines, salaire 500 €/sem → ICP = 13 500 €
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("ancienneteMoisSupplementaires", 0);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");
        body.put("partieStatutUniqueSeulement", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisEnSemaines").value(27))
                .andExpect(jsonPath("$.indemniteCompensatoire").value(13500.00))
                .andExpect(jsonPath("$.dateFinPreavis").value("2025-11-24"))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Loi 26/12/2013")))
                .andExpect(jsonPath("$.formuleCalcul").value(
                        org.hamcrest.Matchers.containsString("27 semaines")));
    }

    @Test
    void POST_be_anciennete20Ans_returns62SemainesPlafond() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 20);
        body.put("salaireHebdomadaireBrut", 600.00);
        body.put("dateNotificationLicenciement", "2025-06-01");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisEnSemaines").value(62))
                .andExpect(jsonPath("$.indemniteCompensatoire").value(37200.00));
    }

    @Test
    void POST_be_contratPre2014_avertissementClaeys() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");
        body.put("partieStatutUniqueSeulement", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.partieStatutUniqueSeulement").value(false))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("Formule Claeys")));
    }

    @Test
    void POST_workspaceFr_returns404_isolationBE() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_immigrationCaseFile_returns400_gateDomain() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authOther))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation workspace)
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + travailFrCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_ancienneteNegative_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", -1);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireNegatif_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", -1);
        body.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNotificationManquante_returns400() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", 500.00);
        // dateNotificationLicenciement omise → Bean Validation @NotNull

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = new LinkedHashMap<>();
        first.put("ancienneteAnnees", 9);
        first.put("salaireHebdomadaireBrut", 500.00);
        first.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteAnnees").value(9));

        // Deuxième POST avec autres inputs : upsert → on remplace
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("ancienneteAnnees", 20);
        second.put("salaireHebdomadaireBrut", 600.00);
        second.put("dateNotificationLicenciement", "2025-06-01");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ancienneteAnnees").value(20))
                .andExpect(jsonPath("$.dureePreavisEnSemaines").value(62));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ancienneteAnnees", 9);
        body.put("salaireHebdomadaireBrut", 500.00);
        body.put("dateNotificationLicenciement", "2025-05-19");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureePreavisEnSemaines").value(27))
                .andExpect(jsonPath("$.indemniteCompensatoire").value(13500.00));
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
