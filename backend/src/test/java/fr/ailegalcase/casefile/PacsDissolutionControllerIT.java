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
class PacsDissolutionControllerIT {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthAccountRepository authAccountRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired CaseFileRepository caseFileRepository;
    @Autowired ObjectMapper objectMapper;
    @MockBean AnthropicService anthropicService;

    private OAuth2AuthenticationToken authFr;
    private OAuth2AuthenticationToken authBe;
    private OAuth2AuthenticationToken authDt;
    private CaseFile famFrCf;
    private CaseFile famBeCf;
    private CaseFile dtFrCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uFr = save(new User(), u -> { u.setEmail("pacs-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-pacs-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFAP " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFAP " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-pacs-fr-" + ts, "pacs-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("pacs-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-pacs-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEP " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CBEP " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-pacs-be-" + ts, "pacs-be-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("pacs-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-pacs-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRTP " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRTP " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-pacs-dt-" + ts, "pacs-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String mode,
                                     LocalDate dateConclusion,
                                     LocalDate dateDissolution,
                                     Integer dureeUnion,
                                     String regime,
                                     Boolean patrimoine,
                                     List<String> creances,
                                     Integer enfants,
                                     LocalDate dateNotif) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateConclusionPacs", dateConclusion != null ? dateConclusion.toString() : null);
        m.put("modeDissolution", mode);
        m.put("dateDissolution", dateDissolution != null ? dateDissolution.toString() : null);
        m.put("dureeUnionAnnees", dureeUnion);
        m.put("regimeBiens", regime);
        m.put("patrimoineCommunSignificatif", patrimoine);
        m.put("creancesAlleguees", creances);
        m.put("enfantsCommuns", enfants);
        m.put("dateNotificationPartenaire", dateNotif != null ? dateNotif.toString() : null);
        return m;
    }

    private Map<String, Object> bodyContentieux() {
        return body("DECLARATION_CONJOINTE",
                LocalDate.now().minusYears(5),
                LocalDate.now().minusMonths(3),
                5,
                "INDIVISION_AMENAGEE",
                true,
                List.of("CONTRIBUTION_DESEQUILIBRE", "INVESTISSEMENT_BIEN_PROPRE"),
                1,
                null);
    }

    @Test
    void POST_fr_nominal_contentieux() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyContentieux())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.dissolutionValide").value(true))
                .andExpect(jsonPath("$.dureeUnionEligibleCreances").value(true))
                .andExpect(jsonPath("$.scoreCreancesProbables").value(100))
                .andExpect(jsonPath("$.verdictRecommandation").value("CONTENTIEUX_INEVITABLE"))
                .andExpect(jsonPath("$.delaiPrescriptionAnnees").value(5))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("515-7")));
    }

    @Test
    void POST_unilateraleAvecNotification_dissolutionValide() throws Exception {
        Map<String, Object> b = body("DECLARATION_UNILATERALE",
                LocalDate.now().minusYears(3),
                LocalDate.now().minusMonths(2),
                3,
                "SEPARATION_BIENS",
                false,
                List.of("AUCUNE"),
                0,
                LocalDate.now().minusMonths(1));
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dissolutionValide").value(true))
                .andExpect(jsonPath("$.delaiNotificationOk").value(true));
    }

    @Test
    void POST_unilateraleSansNotification_dissolutionInvalide() throws Exception {
        Map<String, Object> b = body("DECLARATION_UNILATERALE",
                LocalDate.now().minusYears(3),
                LocalDate.now().minusMonths(2),
                3,
                "SEPARATION_BIENS",
                false,
                List.of(),
                0,
                null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dissolutionValide").value(false))
                .andExpect(jsonPath("$.delaiNotificationOk").value(false));
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/pacs-dissolution")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyContentieux())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyContentieux())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyContentieux())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_missingDureeUnion_returns400() throws Exception {
        Map<String, Object> b = body("DECLARATION_CONJOINTE",
                LocalDate.now().minusYears(5),
                LocalDate.now().minusMonths(3),
                null,
                "INDIVISION_AMENAGEE",
                false,
                List.of(),
                0,
                null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_missingMode_returns400() throws Exception {
        Map<String, Object> b = body(null,
                LocalDate.now().minusYears(5),
                LocalDate.now().minusMonths(3),
                5,
                "INDIVISION_AMENAGEE",
                false,
                List.of(),
                0,
                null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFuture_returns400() throws Exception {
        Map<String, Object> b = body("DECLARATION_CONJOINTE",
                LocalDate.now().minusYears(5),
                LocalDate.now().plusDays(2),
                5,
                "INDIVISION_AMENAGEE",
                false,
                List.of(),
                0,
                null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyContentieux())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreCreancesProbables").value(100));

        Map<String, Object> next = body("DECLARATION_CONJOINTE",
                LocalDate.now().minusYears(1),
                LocalDate.now().minusMonths(2),
                0,
                "SEPARATION_BIENS",
                false,
                List.of("AUCUNE"),
                0,
                null);
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreCreancesProbables").value(0))
                .andExpect(jsonPath("$.verdictRecommandation").value("RIEN_A_FAIRE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyContentieux())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreCreancesProbables").value(100))
                .andExpect(jsonPath("$.verdictRecommandation").value("CONTENTIEUX_INEVITABLE"))
                .andExpect(jsonPath("$.delaiPrescriptionAnnees").value(5));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/pacs-dissolution")
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
