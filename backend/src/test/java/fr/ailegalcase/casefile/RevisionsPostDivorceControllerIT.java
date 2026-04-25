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
class RevisionsPostDivorceControllerIT {

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

    private static final String LONG_MOTIF =
            "Perte d'emploi du débiteur depuis 6 mois, baisse de 60% des revenus mensuels.";

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // FR DROIT_FAMILLE → cible de l'outil
        User uFr = save(new User(), u -> { u.setEmail("rpd-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rpd-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRRPD " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRRPD " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-rpd-fr-" + ts, "rpd-fr-" + ts + "@ex.com");

        // BE DROIT_FAMILLE → gate country FRANCE → rejet
        User uBe = save(new User(), u -> { u.setEmail("rpd-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rpd-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBERPD " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBERPD " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-rpd-be-" + ts, "rpd-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL → gate legal_domain → rejet
        User uDt = save(new User(), u -> { u.setEmail("rpd-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-rpd-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRTRPD " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRTRPD " + ts, "DROIT_DU_TRAVAIL");
        authFrDt = buildAuth("g-rpd-dt-" + ts, "rpd-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyPension(String revInitDeb, String revActDeb, Integer nbEnfants,
                                            String dateInit) {
        Map<String, Object> m = new HashMap<>();
        m.put("typeRevision", "PENSION_ALIMENTAIRE");
        m.put("dateDecisionInitiale", dateInit);
        m.put("changementCirconstance", LONG_MOTIF);
        m.put("revenusInitialsDebiteurEur", revInitDeb);
        m.put("revenusActuelsDebiteurEur", revActDeb);
        m.put("nbEnfantsACharge", nbEnfants);
        m.put("ageEnfants", List.of(10, 14));
        return m;
    }

    @Test
    void POST_fr_pensionAlimentaire_baisse60Pct_elevee() throws Exception {
        Map<String, Object> body = bodyPension("4000", "1600", 2, "2024-06-15");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.typeRevision").value("PENSION_ALIMENTAIRE"))
                .andExpect(jsonPath("$.ecartRevenusPct").value(-60))
                .andExpect(jsonPath("$.modificationSubstantielle").value(true))
                .andExpect(jsonPath("$.motivationSuffisante").value(true))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictRevisionPossible").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("209")));
    }

    @Test
    void POST_fr_residence_changementMode_elevee() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeRevision", "RESIDENCE");
        body.put("dateDecisionInitiale", "2024-06-15");
        body.put("changementCirconstance",
                "Le père a déménagé à 200km, il n'est plus possible d'exercer la résidence alternée.");
        body.put("nbEnfantsACharge", 2);
        body.put("ageEnfants", List.of(8, 12));
        body.put("modeResidenceActuel", "ALTERNEE");
        body.put("modeResidenceDemande", "EXCLUSIVE_MERE");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRevision").value("RESIDENCE"))
                .andExpect(jsonPath("$.modificationSubstantielle").value(true))
                .andExpect(jsonPath("$.verdictRevisionPossible").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("373-2-13")));
    }

    @Test
    void POST_fr_droitVisite_enfantPreado_elevee() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeRevision", "DROIT_VISITE");
        body.put("dateDecisionInitiale", "2024-06-15");
        body.put("changementCirconstance",
                "L'adolescent de 14 ans souhaite être entendu et demande à voir son père moins souvent.");
        body.put("nbEnfantsACharge", 1);
        body.put("ageEnfants", List.of(14));
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRevision").value("DROIT_VISITE"))
                .andExpect(jsonPath("$.modificationSubstantielle").value(true))
                .andExpect(jsonPath("$.verdictRevisionPossible").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("388-1")));
    }

    @Test
    void POST_fr_prestationCompensatoire_baisse40Pct_elevee() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeRevision", "PRESTATION_COMPENSATOIRE");
        body.put("dateDecisionInitiale", "2024-06-15");
        body.put("changementCirconstance",
                "Mise en invalidité du débiteur — perte irréversible de 40% des revenus.");
        body.put("revenusInitialsDebiteurEur", "5000");
        body.put("revenusActuelsDebiteurEur", "3000");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRevision").value("PRESTATION_COMPENSATOIRE"))
                .andExpect(jsonPath("$.ecartRevenusPct").value(-40))
                .andExpect(jsonPath("$.modificationSubstantielle").value(true))
                .andExpect(jsonPath("$.verdictRevisionPossible").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("279")));
    }

    @Test
    void POST_fr_demenagementParent_distance200km_elevee() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeRevision", "DEMENAGEMENT_PARENT");
        body.put("dateDecisionInitiale", "2024-06-15");
        body.put("changementCirconstance",
                "Mutation professionnelle du père dans une autre région — déménagement à 200 km.");
        body.put("nbEnfantsACharge", 2);
        body.put("ageEnfants", List.of(8, 11));
        body.put("modeResidenceActuel", "ALTERNEE");
        body.put("informationPrealable", true);
        body.put("distanceDemenagementKm", 200);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRevision").value("DEMENAGEMENT_PARENT"))
                .andExpect(jsonPath("$.modificationSubstantielle").value(true))
                .andExpect(jsonPath("$.verdictRevisionPossible").value("ELEVEE"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("373-2")));
    }

    @Test
    void POST_fr_typeRevisionManquant_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("changementCirconstance", LONG_MOTIF);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_typeRevisionInconnu_returns400() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeRevision", "AUTRE_INCONNU");
        body.put("dateDecisionInitiale", "2024-06-15");
        body.put("changementCirconstance", LONG_MOTIF);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        Map<String, Object> body = bodyPension("4000", "1600", 2, "2024-06-15");
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        Map<String, Object> body = bodyPension("4000", "1600", 2, "2024-06-15");
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = bodyPension("4000", "1600", 2, "2024-06-15");
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        // 1ère analyse PENSION
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyPension("4000", "1600", 2, "2024-06-15"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.typeRevision").value("PENSION_ALIMENTAIRE"));

        // 2nde analyse RESIDENCE
        Map<String, Object> body2 = new HashMap<>();
        body2.put("typeRevision", "RESIDENCE");
        body2.put("dateDecisionInitiale", "2024-06-15");
        body2.put("changementCirconstance",
                "Le père a déménagé à 200km, il n'est plus possible d'exercer la résidence alternée.");
        body2.put("ageEnfants", List.of(10));
        body2.put("modeResidenceActuel", "ALTERNEE");
        body2.put("modeResidenceDemande", "EXCLUSIVE_MERE");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeRevision").value("RESIDENCE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                bodyPension("4000", "1600", 2, "2024-06-15"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictRevisionPossible").value("ELEVEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/revisions-post-divorce")
                        .with(authentication(authFrFa)))
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
