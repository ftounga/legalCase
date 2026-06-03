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
 * SF-221-01 — IT prorogation carte A BE
 * (art. 13 Loi 15/12/1980 + art. 33 AR 08/10/1981 — séjour temporaire / limité).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CarteAProrogationBeControllerIT {

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

        User uBe = save(new User(), u -> { u.setEmail("cap-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cap-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEICAP " + ts, "DROIT_IMMIGRATION", "BELGIQUE");
        saveMember(uBe, wsBe);
        immBeCf = saveCf(uBe, wsBe, "CBEICAP " + ts, "DROIT_IMMIGRATION");
        authBe = buildAuth("g-cap-be-" + ts, "cap-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("cap-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cap-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRICAP " + ts, "DROIT_IMMIGRATION", "FRANCE");
        saveMember(uFr, wsFr);
        immFrCf = saveCf(uFr, wsFr, "CFRICAP " + ts, "DROIT_IMMIGRATION");
        authFr = buildAuth("g-cap-fr-" + ts, "cap-fr-" + ts + "@ex.com");

        User uDt = save(new User(), u -> { u.setEmail("cap-dt-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uDt, "g-cap-dt-" + ts);
        Workspace wsDt = saveWs(uDt, "WSBETCAP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uDt, wsDt);
        dtBeCf = saveCf(uDt, wsDt, "CBETCAP " + ts, "DROIT_DU_TRAVAIL");
        authDt = buildAuth("g-cap-dt-" + ts, "cap-dt-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String dateExp, boolean motifPersiste,
                                     boolean conditionsReunies, boolean demandeDeposee,
                                     String dateDemande) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateExpirationCarteA", dateExp);
        m.put("motifSejourPersiste", motifPersiste);
        m.put("conditionsInitialesToujoursReunies", conditionsReunies);
        m.put("demandeDeposee", demandeDeposee);
        if (dateDemande != null) {
            m.put("dateDemande", dateDemande);
        }
        return m;
    }

    @Test
    void POST_be_nominal_prorogeable_returns200() throws Exception {
        String exp = LocalDate.now().plusDays(40).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("PROROGEABLE"))
                .andExpect(jsonPath("$.joursAvantExpiration").value(40))
                .andExpect(jsonPath("$.basesJuridiques[0]")
                        .value(org.hamcrest.Matchers.containsString("art. 13")));
    }

    @Test
    void POST_be_expiree_returns200() throws Exception {
        String exp = LocalDate.now().minusDays(3).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, false, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("EXPIREE"));
    }

    @Test
    void POST_workspaceFr_returns400() throws Exception {
        String exp = LocalDate.now().plusDays(40).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_droitDuTravail_returns400() throws Exception {
        String exp = LocalDate.now().plusDays(40).toString();
        mockMvc.perform(post("/api/v1/case-files/" + dtBeCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authDt)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, false, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_demandeDeposeeSansDate_returns400() throws Exception {
        String exp = LocalDate.now().plusDays(40).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, true, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_otherWorkspace_returns404() throws Exception {
        String exp = LocalDate.now().plusDays(40).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immFrCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, false, null))))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        String exp = LocalDate.now().plusDays(40).toString();
        String dem = LocalDate.now().minusDays(2).toString();
        mockMvc.perform(post("/api/v1/case-files/" + immBeCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(exp, true, true, true, dem))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DEMANDE_DEPOSEE"));

        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/carte-a-prorogation-be-analysis")
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("DEMANDE_DEPOSEE"))
                .andExpect(jsonPath("$.demandeDeposee").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + immBeCf.getId() + "/carte-a-prorogation-be-analysis")
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
