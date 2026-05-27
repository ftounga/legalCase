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
 * SF-213-10 : tests d'intégration end-to-end de l'outil
 * licenciement-be-cct109-deraisonnable — gate BE-only strict, isolation
 * workspace, persistance et upsert, validation Bean, attribution des
 * échelons CCT n° 109 art. 9, cumul ICP systématique, avertissement
 * non-null systématique.
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
class LicenciementBeCct109DeraisonnableControllerIT {

    private static final String PATH_SUFFIX = "/decision-tools/licenciement-be-cct109-deraisonnable";

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

        User uBe = save(new User(),
                u -> { u.setEmail("c109be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-c109be-" + ts);
        Workspace wsBe = saveWs(uBe, "WS109BE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        travailBeCf = saveCf(uBe, wsBe, "C109BE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-c109be-" + ts, "c109be-" + ts + "@ex.com");

        User uFr = save(new User(),
                u -> { u.setEmail("c109fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-c109fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WS109FR " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        travailFrCf = saveCf(uFr, wsFr, "C109FR " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-c109fr-" + ts, "c109fr-" + ts + "@ex.com");

        User uOt = save(new User(),
                u -> { u.setEmail("c109ot-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uOt, "g-c109ot-" + ts);
        Workspace wsOt = saveWs(uOt, "WS109OT " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uOt, wsOt);
        immBeCf = saveCf(uOt, wsOt, "C109OT " + ts, "DROIT_IMMIGRATION");
        authOther = buildAuth("g-c109ot-" + ts, "c109ot-" + ts + "@ex.com");
    }

    // ── POST nominal : motif disputé + procédure incomplète → 8 sem ───────

    @Test
    void POST_be_motifDispute_proceduresNonRespectees_returns200_echelon8semaines() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("motifLieAPersonne", "MOTIF_PRECIS_DISPUTE");
        body.put("proceduresRespectees", false);
        body.put("remunerationHebdomadaireBrute", 500.00);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("8_SEMAINES"))
                .andExpect(jsonPath("$.nombreSemaines").value(8))
                .andExpect(jsonPath("$.indemniteCct109").value(4000.00))
                .andExpect(jsonPath("$.cumulAvecIcp").value(true))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("ICP")))
                .andExpect(jsonPath("$.baseJuridique").value(
                        org.hamcrest.Matchers.containsString("CCT n° 109")));
    }

    // ── Motif valable + procédures → NON_DERAISONNABLE ────────────────────

    @Test
    void POST_be_motifValable_proceduresRespectees_returnsNonDeraisonnable_indemnite0() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("motifLieAPersonne", "MOTIF_PROUVE_VALIDE");
        body.put("proceduresRespectees", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("NON_DERAISONNABLE"))
                .andExpect(jsonPath("$.nombreSemaines").value(0))
                .andExpect(jsonPath("$.indemniteCct109").value(0))
                .andExpect(jsonPath("$.cumulAvecIcp").value(true))
                .andExpect(jsonPath("$.avertissement").value(
                        org.hamcrest.Matchers.containsString("ICP")));
    }

    // ── Discrimination → 12 semaines ───────────────────────────────────────

    @Test
    void POST_be_discriminationSeule_returnsEchelon12() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("discriminationSuspectee", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("12_SEMAINES"))
                .andExpect(jsonPath("$.nombreSemaines").value(12))
                .andExpect(jsonPath("$.indemniteCct109").value(6000.00));
    }

    // ── Discrimination + représailles → 17 semaines (maximum) ─────────────

    @Test
    void POST_be_discriminationEtRepresailles_returnsEchelon17_maximum() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("discriminationSuspectee", true);
        body.put("represaillesSuspectees", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("17_SEMAINES"))
                .andExpect(jsonPath("$.nombreSemaines").value(17))
                .andExpect(jsonPath("$.indemniteCct109").value(8500.00));
    }

    // ── Sans motif → 3 semaines (minimum) ──────────────────────────────────

    @Test
    void POST_be_sansMotif_returnsEchelon3_minimum() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("motifCommunique", false);
        body.put("motifLieAPersonne", "SANS_MOTIF");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("3_SEMAINES"))
                .andExpect(jsonPath("$.nombreSemaines").value(3))
                .andExpect(jsonPath("$.indemniteCct109").value(1500.00));
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

    // ── Validation 400 ─────────────────────────────────────────────────────

    @Test
    void POST_motifCommuniqueManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("motifCommunique");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_motifLieAPersonneManquant_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("motifLieAPersonne");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationZero_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("remunerationHebdomadaireBrute", 0);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_remunerationManquante_returns400() throws Exception {
        Map<String, Object> body = baseBody();
        body.remove("remunerationHebdomadaireBrute");

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
        first.put("motifLieAPersonne", "MOTIF_VAGUE");

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("3_SEMAINES"));

        Map<String, Object> second = baseBody();
        second.put("discriminationSuspectee", true);
        second.put("represaillesSuspectees", true);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("17_SEMAINES"));
    }

    @Test
    void GET_afterPost_returnsPersistedSnapshot() throws Exception {
        Map<String, Object> body = baseBody();
        body.put("motifLieAPersonne", "MOTIF_PRECIS_DISPUTE");
        body.put("proceduresRespectees", false);

        mockMvc.perform(post("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + travailBeCf.getId() + PATH_SUFFIX)
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.echelonCct109").value("8_SEMAINES"))
                .andExpect(jsonPath("$.nombreSemaines").value(8))
                .andExpect(jsonPath("$.indemniteCct109").value(4000.00))
                .andExpect(jsonPath("$.cumulAvecIcp").value(true));
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
        body.put("motifCommunique", true);
        body.put("motifLieAPersonne", "MOTIF_PRECIS_DISPUTE");
        body.put("discriminationSuspectee", false);
        body.put("represaillesSuspectees", false);
        body.put("proceduresRespectees", true);
        body.put("remunerationHebdomadaireBrute", 500.00);
        body.put("argumentsPatronal", null);
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
