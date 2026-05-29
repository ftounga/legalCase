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

/**
 * SF-215-15 — IT recours CCE extrême urgence 5 jours OUVRABLES BE
 * (Conseil du Contentieux des Étrangers, art. 39/82 §4 al. 2-3 Loi 15/12/1980 — droit des étrangers).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CceExtremeUrgenceBeControllerIT {

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

        User uBe = save(new User(), u -> { u.setEmail("ceu-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ceu-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEICEU " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEICEU " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-ceu-be-" + ts, "ceu-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("ceu-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ceu-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRICEU " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRICEU " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-ceu-fr-" + ts, "ceu-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("ceu-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-ceu-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETCEU " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETCEU " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-ceu-dt-" + ts, "ceu-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dateActe, String typeActe,
                                     boolean recoursForme, String dateRecours) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateActeExecutoire", dateActe);
        m.put("typeActe", typeActe);
        m.put("recoursForme", recoursForme);
        if (dateRecours != null) {
            m.put("dateRecours", dateRecours);
        }
        return m;
    }

    @Test
    void POST_be_nominal_returns200() throws Exception {
        // Acte du jour → délai ouvert (DISPONIBLE ou CRITIQUE selon le jour de la semaine).
        String acte = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "OQT_EXECUTE", false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeActe").value("OQT_EXECUTE"))
                .andExpect(jsonPath("$.statut")
                        .value(org.hamcrest.Matchers.in(List.of("DISPONIBLE", "CRITIQUE"))))
                .andExpect(jsonPath("$.dateLimiteRecours").exists())
                .andExpect(jsonPath("$.audienceEstimee").exists())
                .andExpect(jsonPath("$.baseJuridique")
                        .value(org.hamcrest.Matchers.containsString("39/82")));
    }

    @Test
    void POST_be_expire_actionImmediate() throws Exception {
        // Acte ancien (20 jours) → délai de 5 jours ouvrables dépassé → EXPIRE.
        String acte = LocalDate.now().minusDays(20).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "TRANSFERT_DUBLIN", false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("EXPIRE"))
                .andExpect(jsonPath("$.actionImmediate")
                        .value(org.hamcrest.Matchers.containsString("ACTION IMMÉDIATE")));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        String acte = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "OQT_EXECUTE", false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        String acte = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "OQT_EXECUTE", false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_acteFutureAuDelaTolerance_returns400() throws Exception {
        // Acte à +30 jours → non encore imminent → 400.
        String acteFutur = LocalDate.now().plusDays(30).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acteFutur, "EXPULSION_IMMEDIATE", false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_recoursFormeSansDateRecours_returns400() throws Exception {
        String acte = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "OQT_EXECUTE", true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        // authBe tente d'accéder au dossier du workspace FR → 404 (isolation workspace).
        String acte = LocalDate.now().toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "OQT_EXECUTE", false, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        String acte = LocalDate.now().minusDays(3).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                body(acte, "REFUS_ACCES_TERRITOIRE", true, LocalDate.now().minusDays(1).toString()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("RECOURS_FORME"));

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeActe").value("REFUS_ACCES_TERRITOIRE"))
                .andExpect(jsonPath("$.statut").value("RECOURS_FORME"))
                .andExpect(jsonPath("$.recoursForme").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/cce-extreme-urgence-be-analysis")
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
