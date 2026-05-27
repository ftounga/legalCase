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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SF-213-09 : tests d'intégration end-to-end de l'outil
 * licenciement-be-acte-equivalent — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean, matrice de décision
 * (verdict + ICP indicatif + risque acceptation tacite).
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
class LicenciementBeActeEquipollentControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/licenciement-be-acte-equivalent";
    /** Date assez récente pour rester dans les 30 j même quand les tests
     *  tournent dans le futur — calculée dynamiquement. */
    private static final String DATE_MOD_RECENTE = LocalDate.now()
            .minusDays(5).toString();

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
                u -> { u.setEmail("aeq-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-aeq-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSAEQBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "CAEQBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-aeq-be-" + ts, "aeq-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL (gate BE-only doit reject)
        User uFr = save(new User(),
                u -> { u.setEmail("aeq-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-aeq-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSAEQFR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "CAEQFR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-aeq-fr-" + ts, "aeq-fr-" + ts + "@ex.com");

        // BE workspace IMMIGRATION (gate legal_domain doit reject)
        User uOt = save(new User(),
                u -> { u.setEmail("aeq-ot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-aeq-ot-" + ts);
        Workspace wsOt = saveWs(uOt, "WSAEQOT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "CAEQOT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-aeq-ot-" + ts, "aeq-ot-" + ts + "@ex.com");
    }

    // ── POST nominal : substantielle + essentiel → ACTE_EQUIPOLLENT_PROBABLE ──

    @Test
    void POST_be_substantielleElementEssentiel_returns200_verdictProbable() throws Exception {
        Map<String, Object> body = baseBody();

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("ACTE_EQUIPOLLENT_PROBABLE"))
                .andExpect(jsonPath("$.risqueAcceptationTacite").value(false))
                .andExpect(jsonPath("$.delaiRecommandeProtestationJours").value(30))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("03/07/1978")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("Cass. BE")));
    }

    // ── ICP indicatif quand rémunération + durée préavis fournies ─────────

    @Test
    void POST_be_avecRemunerationEtDureePreavis_calculeIcpIndicatif() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationHebdomadaireBrute", 500.00);
        body.put("dureePreavisCalculeeSemaines", 27);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("ACTE_EQUIPOLLENT_PROBABLE"))
                .andExpect(jsonPath("$.icpIndicatif").value(13500.00));
    }

    // ── Modification mineure → PAS_ACTE_EQUIPOLLENT ───────────────────────

    @Test
    void POST_be_mineure_returnsPasActeEquipollent() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("ampleurModification", "MINEURE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("PAS_ACTE_EQUIPOLLENT"))
                .andExpect(jsonPath("$.icpIndicatif").doesNotExist());
    }

    // ── Risque acceptation tacite (silence > 30 j) ────────────────────────

    @Test
    void POST_be_silence35j_returnsRisqueAcceptationTacite() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("salarieAProteste", false);
        body.put("delaiDepuisModificationJours", 35);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("RISQUE_ACCEPTATION_TACITE"))
                .andExpect(jsonPath("$.risqueAcceptationTacite").value(true))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("acceptation tacite")));
    }

    // ── Substantielle mais pas essentiel → A_ANALYSER ─────────────────────

    @Test
    void POST_be_substantielleNonEssentiel_returnsAAnalyser() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("elementEssentielDuContrat", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.icpIndicatif").doesNotExist());
    }

    // ── Gate BE-only ──────────────────────────────────────────────────────

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

    // ── Validation 400 ────────────────────────────────────────────────────

    @Test
    void POST_typeModificationManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("typeModification");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ampleurModificationManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("ampleurModification");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateModificationManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("dateModification");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationNegative_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationHebdomadaireBrute", -100);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_delaiNegatif_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("delaiDepuisModificationJours", -3);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ── Upsert + GET ──────────────────────────────────────────────────────

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        Map<String, Object> first = baseBody();
        first.put("typeModification", "SALAIRE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeModification").value("SALAIRE"));

        Map<String, Object> second = baseBody();
        second.put("typeModification", "LIEU_TRAVAIL");
        second.put("ampleurModification", "MINEURE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeModification").value("LIEU_TRAVAIL"))
                .andExpect(jsonPath("$.verdict").value("PAS_ACTE_EQUIPOLLENT"));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationHebdomadaireBrute", 500.00);
        body.put("dureePreavisCalculeeSemaines", 27);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeModification").value("SALAIRE"))
                .andExpect(jsonPath("$.ampleurModification").value("SUBSTANTIELLE"))
                .andExpect(jsonPath("$.elementEssentielDuContrat").value(true))
                .andExpect(jsonPath("$.verdict").value("ACTE_EQUIPOLLENT_PROBABLE"))
                .andExpect(jsonPath("$.icpIndicatif").value(13500.00));
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

    // ── helpers ───────────────────────────────────────────────────────────

    private Map<String, Object> baseBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("typeModification", "SALAIRE");
        body.put("ampleurModification", "SUBSTANTIELLE");
        body.put("dateModification", DATE_MOD_RECENTE);
        body.put("elementEssentielDuContrat", true);
        body.put("salarieAProteste", true);
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
