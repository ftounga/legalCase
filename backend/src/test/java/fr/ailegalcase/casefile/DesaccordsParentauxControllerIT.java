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
class DesaccordsParentauxControllerIT {

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

        // FR workspace DROIT_FAMILLE
        User uFr = save(new User(), u -> { u.setEmail("dp-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-dp-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSDPFR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        famFrCf = saveCf(uFr, wsFr, "CFFR " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-dp-fr-" + ts, "dp-fr-" + ts + "@ex.com");

        // BE workspace DROIT_FAMILLE
        User uBe = save(new User(), u -> { u.setEmail("dp-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-dp-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSDPBE " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        famBeCf = saveCf(uBe, wsBe, "CFBE " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-dp-be-" + ts, "dp-be-" + ts + "@ex.com");

        // FR workspace DROIT_DU_TRAVAIL
        User uDt = save(new User(), u -> { u.setEmail("dp-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-dp-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSDPDT " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFFRT " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-dp-dt-" + ts, "dp-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String domaine,
                                     String intensite,
                                     List<String> tentatives,
                                     List<Integer> ages,
                                     Boolean interet,
                                     Boolean expertiseDeja,
                                     Boolean urgence,
                                     LocalDate dateRequete) {
        Map<String, Object> m = new HashMap<>();
        m.put("domaineDesaccord", domaine);
        m.put("intensiteDesaccord", intensite);
        m.put("tentativesMediation", tentatives);
        m.put("ageEnfantsConcernes", ages);
        m.put("interetSuperieurInvoque", interet);
        m.put("expertiseDejaRealisee", expertiseDeja);
        m.put("urgence", urgence);
        m.put("dateRequete", dateRequete != null ? dateRequete.toString() : null);
        return m;
    }

    private Map<String, Object> bodyNominalElevee() {
        return body("SCOLARITE", "MAJEUR",
                List.of("MEDIATION_FAMILIALE", "DISCUSSIONS_DIRECTES"),
                List.of(10, 14),
                false, false, false,
                LocalDate.of(2026, 5, 10));
    }

    @Test
    void POST_fr_nominal_elevee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreEligibiliteJaf").value(75))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.mediationPrealableRecommandee").value(false))
                .andExpect(jsonPath("$.auditionEnfantsRecommandee").value(true))
                .andExpect(jsonPath("$.expertisePsyRecommandee").value(true))
                .andExpect(jsonPath("$.delaiTraitementJoursPrevisionnel").value(90))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("373-2-10")));
    }

    @Test
    void POST_fr_domaineAutre_aucuneMediation_faible() throws Exception {
        Map<String, Object> b = body("AUTRE", "MINEUR",
                List.of(), List.of(),
                false, false, false,
                LocalDate.of(2026, 5, 10));
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreEligibiliteJaf").value(5))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"))
                .andExpect(jsonPath("$.mediationPrealableRecommandee").value(true));
    }

    @Test
    void POST_fr_urgence_delai30() throws Exception {
        Map<String, Object> b = body("SANTE", "MAJEUR",
                List.of("DISCUSSIONS_DIRECTES"),
                List.of(8),
                true, false, true,
                LocalDate.of(2026, 5, 10));
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delaiTraitementJoursPrevisionnel").value(30));
    }

    @Test
    void POST_fr_domaineInvalide_returns400() throws Exception {
        Map<String, Object> b = body("INVALIDE", "MAJEUR",
                List.of(), List.of(),
                false, false, false,
                LocalDate.of(2026, 5, 10));
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_intensiteInvalide_returns400() throws Exception {
        Map<String, Object> b = body("SCOLARITE", "EXTREME",
                List.of(), List.of(),
                false, false, false,
                LocalDate.of(2026, 5, 10));
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(b)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authFr essaie d'accéder à dossier BE → 404 isolation
        mockMvc.perform(post("/api/v1/case-files/" + famBeCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreEligibiliteJaf").value(75));

        // 2e appel : domaine AUTRE intensité MINEUR → score 5
        Map<String, Object> next = body("AUTRE", "MINEUR",
                List.of(), List.of(),
                false, false, false,
                LocalDate.of(2026, 6, 1));
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(next)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scoreEligibiliteJaf").value(5))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominalElevee())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreEligibiliteJaf").value(75))
                .andExpect(jsonPath("$.verdictProbabiliteAcceptation").value("ELEVEE"))
                .andExpect(jsonPath("$.tentativesMediation.length()").value(2));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + famFrCf.getId() + "/desaccords-parentaux")
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
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
