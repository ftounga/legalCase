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
class Belgian40bisControllerIT {

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
    private OAuth2AuthenticationToken authDt;
    private CaseFile immBeCf;
    private CaseFile immFrCf;
    private CaseFile dtBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uBe = save(new User(), u -> { u.setEmail("40bis-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-40bis-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEI " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEI " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-40bis-be-" + ts, "40bis-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("40bis-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-40bis-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRI " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRI " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-40bis-fr-" + ts, "40bis-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("40bis-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-40bis-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBET " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBET " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-40bis-dt-" + ts, "40bis-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String lien, boolean citoyenUe, String activite,
                                     boolean ressources, boolean assurance,
                                     boolean logement, boolean menace,
                                     LocalDate dateDepot) {
        Map<String, Object> m = new HashMap<>();
        m.put("lienFamilial", lien);
        m.put("regroupantCitoyenUe", citoyenUe);
        m.put("regroupantActiviteCategorie", activite);
        m.put("ressourcesSuffisantes", ressources);
        m.put("assuranceMaladieUe", assurance);
        m.put("logementSuffisant", logement);
        m.put("menaceOrdrePublic", menace);
        m.put("dateDepotDemande", dateDepot != null ? dateDepot.toString() : null);
        return m;
    }

    @Test
    void POST_be_nominal_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", true, "TRAVAILLEUR", true, true, true, false,
                                        LocalDate.of(2026, 4, 1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienFamilial").value("CONJOINT"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.scoreGlobal").value(100))
                .andExpect(jsonPath("$.verdictProbabilite").value("ELEVEE"))
                .andExpect(jsonPath("$.lienValide").value(true))
                .andExpect(jsonPath("$.regroupantValide").value(true))
                .andExpect(jsonPath("$.pasMenace").value(true))
                .andExpect(jsonPath("$.dateExpirationInstruction").value("2026-10-01"))
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("40bis")));
    }

    @Test
    void POST_menaceOrdrePublic_reduitScore() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", true, "TRAVAILLEUR", true, true, true, true, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pasMenace").value(false))
                .andExpect(jsonPath("$.scoreGlobal").value(85));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/belgian-40bis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", true, "TRAVAILLEUR", true, true, true, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", true, "TRAVAILLEUR", true, true, true, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", true, "TRAVAILLEUR", true, true, true, false, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_invalidLien_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONCUBIN_NON_ENREGISTRE", true, "TRAVAILLEUR",
                                        true, true, true, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("CONJOINT", true, "TRAVAILLEUR", true, true, true, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienFamilial").value("CONJOINT"));

        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("ASCENDANT_CHARGE", true, "INACTIF_AVEC_RESSOURCES",
                                        false, true, true, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienFamilial").value("ASCENDANT_CHARGE"))
                .andExpect(jsonPath("$.regroupantActiviteCategorie").value("INACTIF_AVEC_RESSOURCES"))
                .andExpect(jsonPath("$.ressourcesSuffisantes").value(false));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body("PARTENAIRE_ENREGISTRE", true, "ETUDIANT",
                                        true, true, true, false, null))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/belgian-40bis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lienFamilial").value("PARTENAIRE_ENREGISTRE"))
                .andExpect(jsonPath("$.regroupantActiviteCategorie").value("ETUDIANT"))
                .andExpect(jsonPath("$.country").value("BELGIQUE"));
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
