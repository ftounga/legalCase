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

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RecompensesControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFrFa;
    private OAuth2AuthenticationToken authBeFa;
    private OAuth2AuthenticationToken authFrDt;
    private CaseFile faFrCf;
    private CaseFile faBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_FAMILLE → cible
        User uFr = save(new User(), u -> { u.setEmail("rec-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rec-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSREC " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFREC " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-rec-fr-" + ts, "rec-fr-" + ts + "@ex.com");

        // BE DROIT_FAMILLE → 400 FR-only
        User uBe = save(new User(), u -> { u.setEmail("rec-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rec-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSRECBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CFRECBE " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-rec-be-" + ts, "rec-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL → 400 domaine
        User uDt = save(new User(), u -> { u.setEmail("rec-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-rec-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSRECDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRECDT " + ts, "DROIT_DU_TRAVAIL");
        authFrDt = buildAuth("g-rec-dt-" + ts, "rec-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String regime, List<Map<String, Object>> ops) {
        Map<String, Object> m = new HashMap<>();
        m.put("regimeMatrimonial", regime);
        m.put("operations", ops);
        return m;
    }

    private Map<String, Object> opMap(String id, String type, String nature,
                                      String depense, String vInit, String vAct, String descr) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("type", type);
        m.put("natureBien", nature);
        m.put("montantDepenseEur", depense);
        m.put("valeurInitialeBienEur", vInit);
        m.put("valeurActuelleBienEur", vAct);
        m.put("description", descr);
        return m;
    }

    @Test
    void POST_fr_amelioration_calculProfit() throws Exception {
        // dépense 50000, vInit 200000, vAct 350000
        // plus-value 150000, ratio 87500 → profit min(150000, 87500) = 87500
        // max(50000, 87500) = 87500 → PROPRE_DOIT_COMMUNAUTE
        var op = opMap("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", "Travaux agrandissement");
        var body = body("COMMUNAUTE_LEGALE", List.of(op));
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.regimeMatrimonial").value("COMMUNAUTE_LEGALE"))
                .andExpect(jsonPath("$.recompenses[0].operationId").value("op-1"))
                .andExpect(jsonPath("$.recompenses[0].montantRecompenseEur").value(87500.00))
                .andExpect(jsonPath("$.recompenses[0].profitSubsistantEur").value(87500.00))
                .andExpect(jsonPath("$.recompenses[0].directionRecompense").value("PROPRE_DOIT_COMMUNAUTE"))
                .andExpect(jsonPath("$.recompenses[0].regleApplicable").value("PROFIT_SUBSISTANT_PLUS_FORTE"))
                .andExpect(jsonPath("$.totalRecompensesDuesParEpouxEur").value(87500.00))
                .andExpect(jsonPath("$.totalRecompensesDuesParCommunauteEur").value(0.00))
                .andExpect(jsonPath("$.soldeNetPourEpouxEur").value(-87500.00))
                .andExpect(jsonPath("$.baseJuridiqueGenerale")
                        .value(org.hamcrest.Matchers.containsString("1469")));
    }

    @Test
    void POST_fr_acquisition_calculProportion() throws Exception {
        // dépense 60000, vInit 300000, vAct 420000 → ratio 84000
        var op = opMap("op-acq", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "ACQUISITION_BIEN_PROPRE",
                "60000", "300000", "420000", null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recompenses[0].profitSubsistantEur").value(84000.00))
                .andExpect(jsonPath("$.recompenses[0].montantRecompenseEur").value(84000.00));
    }

    @Test
    void POST_fr_depenseUsuelle_alinea2() throws Exception {
        // dépense 5000, sans valeurs → profit fallback dépense → min = 5000
        var op = opMap("op-2", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "DEPENSES_USUELLES",
                "5000", null, null, "Dette communauté");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recompenses[0].regleApplicable").value("PLUS_FAIBLE_DEPENSE_OU_PROFIT"))
                .andExpect(jsonPath("$.recompenses[0].montantRecompenseEur").value(5000.00))
                .andExpect(jsonPath("$.recompenses[0].directionRecompense").value("COMMUNAUTE_DOIT_PROPRE"))
                .andExpect(jsonPath("$.totalRecompensesDuesParCommunauteEur").value(5000.00));
    }

    @Test
    void POST_fr_autre_alinea1() throws Exception {
        var op = opMap("op-3", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "8000", null, null, "Vacances");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recompenses[0].regleApplicable").value("DEPENSE_FAITE"))
                .andExpect(jsonPath("$.recompenses[0].montantRecompenseEur").value(8000.00));
    }

    @Test
    void POST_fr_operationsMultiples_totauxAgreges() throws Exception {
        var op1 = opMap("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", null);
        var op2 = opMap("op-2", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "8000", null, null, null);
        var op3 = opMap("op-3", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "ACQUISITION_BIEN_PROPRE",
                "60000", "300000", "420000", null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op1, op2, op3)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recompenses.length()").value(3))
                .andExpect(jsonPath("$.totalRecompensesDuesParEpouxEur").value(171500.00))
                .andExpect(jsonPath("$.totalRecompensesDuesParCommunauteEur").value(8000.00))
                .andExpect(jsonPath("$.soldeNetPourEpouxEur").value(-163500.00));
    }

    @Test
    void POST_fr_separationBiens_400() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("SEPARATION_BIENS", List.of(op)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_regimeInvalide_400() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("FOOBAR", List.of(op)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_aucuneOperation_okZero() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recompenses.length()").value(0))
                .andExpect(jsonPath("$.totalRecompensesDuesParCommunauteEur").value(0))
                .andExpect(jsonPath("$.totalRecompensesDuesParEpouxEur").value(0))
                .andExpect(jsonPath("$.soldeNetPourEpouxEur").value(0));
    }

    @Test
    void POST_fr_natureBienInvalide_400() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "BAD_NATURE",
                "100", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_montantNegatif_400() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "-100", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_400() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/recompenses")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_400() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/recompenses")
                        .with(authentication(authFrDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_404() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "1000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        var op1 = opMap("op-A", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRecompensesDuesParEpouxEur").value(87500.00));

        var op2 = opMap("op-B", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "2000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op2)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recompenses.length()").value(1))
                .andExpect(jsonPath("$.recompenses[0].operationId").value("op-B"))
                .andExpect(jsonPath("$.totalRecompensesDuesParCommunauteEur").value(2000.00))
                .andExpect(jsonPath("$.totalRecompensesDuesParEpouxEur").value(0));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        var op = opMap("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "50000", "200000", "350000", "Travaux");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_LEGALE", List.of(op)))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.regimeMatrimonial").value("COMMUNAUTE_LEGALE"))
                .andExpect(jsonPath("$.recompenses[0].operationId").value("op-1"))
                .andExpect(jsonPath("$.recompenses[0].montantRecompenseEur").value(87500.00))
                .andExpect(jsonPath("$.totalRecompensesDuesParEpouxEur").value(87500.00));
    }

    @Test
    void GET_withoutPost_404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_participationAcquets_ok() throws Exception {
        var op = opMap("op-1", "DEPENSE_COMMUNAUTE_AU_PROFIT_PROPRE", "AMELIORATION_BIEN_PROPRE",
                "10000", "100000", "120000", null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("PARTICIPATION_AUX_ACQUETS", List.of(op)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeMatrimonial").value("PARTICIPATION_AUX_ACQUETS"));
    }

    @Test
    void POST_communauteUniverselle_ok() throws Exception {
        var op = opMap("op-1", "DEPENSE_PROPRE_AU_PROFIT_COMMUNAUTE", "AUTRE",
                "5000", null, null, null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/recompenses")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body("COMMUNAUTE_UNIVERSELLE", List.of(op)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.regimeMatrimonial").value("COMMUNAUTE_UNIVERSELLE"));
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
