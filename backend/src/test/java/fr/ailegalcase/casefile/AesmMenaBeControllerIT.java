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

/**
 * SF-215-11 — IT AESM + tutelle MENA BE (F-IM-30-aesm-mena-be).
 * Loi 04/05/2007 tutelle MENA + loi 15/12/1980 art. 9bis adapté MENA + circulaire
 * OE 15/09/2005.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AesmMenaBeControllerIT {

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

        User uBe = save(new User(), u -> { u.setEmail("aesm-mena-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-aesm-mena-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEAESMMENA " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEAESMMENA " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-aesm-mena-be-" + ts, "aesm-mena-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("aesm-mena-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-aesm-mena-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRAESMMENA " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRAESMMENA " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-aesm-mena-fr-" + ts, "aesm-mena-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("aesm-mena-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-aesm-mena-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETAESMMENA " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETAESMMENA " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-aesm-mena-dt-" + ts, "aesm-mena-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(int ageActuel, String dateArrivee,
                                     boolean tuteurDesigne,
                                     boolean integrationScolaire, Integer dureeScolaire,
                                     boolean projetVieElabore, boolean perspectiveAutonomie,
                                     boolean menaceOrdrePublic) {
        Map<String, Object> m = new HashMap<>();
        m.put("ageActuel", ageActuel);
        m.put("dateArriveeBelgique", dateArrivee);
        m.put("tuteurDesigne", tuteurDesigne);
        m.put("integrationScolaire", integrationScolaire);
        m.put("dureeScolaire", dureeScolaire);
        m.put("projetVieElabore", projetVieElabore);
        m.put("perspectiveAutonomie", perspectiveAutonomie);
        m.put("menaceOrdrePublic", menaceOrdrePublic);
        return m;
    }

    @Test
    void POST_be_favorable_returns200() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAESM").value("FAVORABLE"))
                .andExpect(jsonPath("$.scoreIntegration").value(100))
                .andExpect(jsonPath("$.bonus").value(5))
                .andExpect(jsonPath("$.prioriteUrgence").value(false))
                .andExpect(jsonPath("$.etapeTutelle").doesNotExist())
                .andExpect(jsonPath("$.delaiDesignationTuteur").value("2022-10-01"))
                .andExpect(jsonPath("$.criteresNonRemplis").isArray())
                .andExpect(jsonPath("$.criteresNonRemplis").isEmpty())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("Loi 04/05/2007")))
                .andExpect(jsonPath("$.prochainActe")
                        .value(org.hamcrest.Matchers.containsString("Désignation tuteur DGDE")));
    }

    @Test
    void POST_be_sousReserve_returns200() throws Exception {
        // 40 + 0 + 0 + 10 + 0 = 50 → SOUS_RESERVE
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(14, "2022-09-01", true, true, 12, false, false, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAESM").value("SOUS_RESERVE"))
                .andExpect(jsonPath("$.scoreIntegration").value(50))
                .andExpect(jsonPath("$.criteresNonRemplis", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("Projet de vie"))));
    }

    @Test
    void POST_be_age17_returnsPrioriteUrgence() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(17, "2024-01-15", true, true, 12, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prioriteUrgence").value(true));
    }

    @Test
    void POST_be_tuteurNonDesigne_returnsEtapeTutelle() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2024-03-15", false, true, 24, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.etapeTutelle")
                        .value(org.hamcrest.Matchers.containsString("Service des Tutelles")))
                .andExpect(jsonPath("$.delaiDesignationTuteur").value("2024-04-14"))
                .andExpect(jsonPath("$.criteresNonRemplis", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("Tuteur DGDE"))));
    }

    @Test
    void POST_age18_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(18, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_integrationScolaireSansDuree_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2022-09-01", true, true, null, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe → dossier FR : isolation workspace
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_upsert_replacesAnalysis() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(14, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAESM").value("FAVORABLE"));

        // Replay avec menace ordre public → DEFAVORABLE
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(14, "2022-09-01", true, false, null, false, false, true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdictAESM").value("DEFAVORABLE"))
                .andExpect(jsonPath("$.scoreIntegration").value(0));
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(15, "2022-09-01", true, true, 30, true, true, false))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageActuel").value(15))
                .andExpect(jsonPath("$.dateArriveeBelgique").value("2022-09-01"))
                .andExpect(jsonPath("$.dureeScolaire").value(30))
                .andExpect(jsonPath("$.verdictAESM").value("FAVORABLE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/aesm-mena-be-analysis")
                        .with(authentication(authBe)))
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
