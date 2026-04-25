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
class OrdonnanceProtectionControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("op-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-op-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFROP " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFROP " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-op-fr-" + ts, "op-fr-" + ts + "@ex.com");

        // BE DROIT_FAMILLE → gate FR → 400
        User uBe = save(new User(), u -> { u.setEmail("op-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-op-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEOP " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEOP " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-op-be-" + ts, "op-be-" + ts + "@ex.com");

        // FR DROIT_DU_TRAVAIL → gate domaine → 400
        User uDt = save(new User(), u -> { u.setEmail("op-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-op-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSFRTOP " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uDt, wsDt);
        dtFrCf = saveCf(uDt, wsDt, "CFRTOP " + ts, "DROIT_DU_TRAVAIL");
        authFrDt = buildAuth("g-op-dt-" + ts, "op-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(List<String> violences,
                                     List<String> preuves,
                                     Boolean dangerImmediat,
                                     Boolean presenceEnfants,
                                     Boolean logementCommun,
                                     Boolean dependance,
                                     Boolean dejaProtege,
                                     List<String> mesures) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateRequete", "2026-04-20");
        m.put("violencesAlleguees", violences);
        m.put("preuvesViolences", preuves);
        m.put("dangerImmediat", dangerImmediat);
        m.put("presenceEnfants", presenceEnfants);
        m.put("ageEnfants", null);
        m.put("logementCommun", logementCommun);
        m.put("victimeFinanciairementDependante", dependance);
        m.put("demandeurDejaProtege", dejaProtege);
        m.put("demandeMesures", mesures);
        return m;
    }

    @Test
    void POST_fr_nominal_eleveeAvecToutes() throws Exception {
        Map<String, Object> body = body(
                List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL", "TEMOIGNAGES"),
                true, true, true, false, false,
                List.of("EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"));
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.scoreVraisemblance").value(95))
                .andExpect(jsonPath("$.verdictProbabiliteOctroi").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiTraitementJoursPrevisionnel").value(6))
                .andExpect(jsonPath("$.mesuresRecommandees", org.hamcrest.Matchers.hasSize(4)))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("515-9")));
    }

    @Test
    void POST_fr_sansDanger_tgdEtBarExclus() throws Exception {
        Map<String, Object> body = body(
                List.of("PHYSIQUES", "PSYCHOLOGIQUES"),
                List.of("MAIN_COURANTE"),
                false, false, true, false, false,
                List.of("EVICTION_CONJOINT", "INTERDICTION_APPROCHER", "TGD", "BAR"));
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mesuresRecommandees",
                        org.hamcrest.Matchers.containsInAnyOrder("EVICTION_CONJOINT", "INTERDICTION_APPROCHER")));
    }

    @Test
    void POST_fr_violencesVides_returns400() throws Exception {
        Map<String, Object> body = body(
                List.of(), List.of(),
                true, true, true, false, false, List.of("TGD"));
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_fr_violenceInconnue_returns400() throws Exception {
        Map<String, Object> body = body(
                List.of("HARCELEMENT"), List.of(),
                true, true, true, false, false, List.of());
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceBe_returns400() throws Exception {
        Map<String, Object> body = body(
                List.of("PHYSIQUES"), List.of(),
                true, false, true, false, false, List.of("TGD"));
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/ordonnance-protection")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        Map<String, Object> body = body(
                List.of("PHYSIQUES"), List.of(),
                true, false, true, false, false, List.of("TGD"));
        mockMvc.perform(post("/api/v1/case-files/" + dtFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        Map<String, Object> body = body(
                List.of("PHYSIQUES"), List.of(),
                true, false, true, false, false, List.of("TGD"));
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        // 1er POST score élevé
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL"),
                                true, true, true, false, false, List.of("TGD")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteOctroi").value("ELEVEE"));

        // 2e POST score faible (1 violence, pas de preuves, rien)
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                List.of("PHYSIQUES"), List.of(),
                                false, false, false, false, true, List.of()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictProbabiliteOctroi").value("FAIBLE"));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                List.of("PHYSIQUES", "PSYCHOLOGIQUES", "MENACES_MORT"),
                                List.of("CONSTAT_HUISSIER", "MAIN_COURANTE", "CERTIFICAT_MEDICAL"),
                                true, true, true, false, false, List.of("TGD", "BAR")))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.verdictProbabiliteOctroi").value("ELEVEE"))
                .andExpect(jsonPath("$.delaiTraitementJoursPrevisionnel").value(6));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/ordonnance-protection")
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
