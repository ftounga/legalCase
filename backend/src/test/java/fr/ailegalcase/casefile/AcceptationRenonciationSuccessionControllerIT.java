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

/** SF-210-03 : IT minimal POST + GET. */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AcceptationRenonciationSuccessionControllerIT {

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
    private CaseFile faFrCf;
    private CaseFile faBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uFr = save(new User(), u -> { u.setEmail("ars-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ars-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRAR " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRAR " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-ars-fr-" + ts, "ars-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("ars-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ars-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEAR " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEAR " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-ars-be-" + ts, "ars-be-" + ts + "@ex.com");
    }

    private Map<String, Object> body(String date, String qualite, double actif, double passif,
                                     boolean actes, boolean inv, boolean dettesIncert,
                                     String intention) {
        Map<String, Object> m = new HashMap<>();
        m.put("dateOuvertureSuccession", date);
        m.put("qualiteHeritier", qualite);
        m.put("actifBrutEur", actif);
        m.put("passifEur", passif);
        m.put("actesEquivalentAcceptationDejaPosesDetected", actes);
        m.put("inventaireRealise", inv);
        m.put("dettesIncertainesDetected", dettesIncert);
        m.put("intentionExprimee", intention);
        return m;
    }

    private String recentDate() {
        return LocalDate.now().minusDays(15).toString();
    }

    @Test
    void POST_fr_actif_positif_recommande_pure_simple() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                recentDate(), "PREMIER_RANG", 250000d, 0d,
                                false, false, false, "INCERTAIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.optionRecommandee").value("ACCEPTATION_PURE_SIMPLE"))
                .andExpect(jsonPath("$.delaiTotalJours").value(120))
                .andExpect(jsonPath("$.optionsOuvertes", org.hamcrest.Matchers.hasSize(3)));
    }

    @Test
    void POST_fr_actes_equivalents_force_pure_simple_et_ferme_renonciation() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                recentDate(), "PREMIER_RANG", 100000d, 30000d,
                                true, false, false, "INCERTAIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionsOuvertes", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.optionsOuvertes[0]").value("ACCEPTATION_PURE_SIMPLE"));
    }

    @Test
    void POST_fr_passif_excede_actif_renonciation() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                recentDate(), "PREMIER_RANG", 5000d, 50000d,
                                false, false, false, "INCERTAIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionRecommandee").value("RENONCIATION"));
    }

    @Test
    void POST_be_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                recentDate(), "PREMIER_RANG", 100000d, 0d,
                                false, false, false, "INCERTAIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNull_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                null, "PREMIER_RANG", 100000d, 0d,
                                false, false, false, "INCERTAIN"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body(
                                recentDate(), "PREMIER_RANG", 100000d, 0d,
                                false, false, false, "INCERTAIN"))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/acceptation-renonciation-succession")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optionRecommandee").value("ACCEPTATION_PURE_SIMPLE"));
    }

    // helpers
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
