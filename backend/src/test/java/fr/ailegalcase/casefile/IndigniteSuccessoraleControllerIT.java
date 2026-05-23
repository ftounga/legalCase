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
 * SF-216-19 : IT minimal POST + GET + gates pour l'outil Indignité
 * successorale FR (art. 726-729-1 Cciv + Loi 2022-1617).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class IndigniteSuccessoraleControllerIT {

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
    private CaseFile faFrCf;
    private CaseFile faBeCf;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();
        User uFr = save(new User(), u -> { u.setEmail("indignite-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-indignite-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRINDIGN " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRINDIGN " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-indignite-fr-" + ts, "indignite-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("indignite-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-indignite-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEINDIGN " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEINDIGN " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-indignite-be-" + ts, "indignite-be-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("motifIndignite", "MEURTRE");
        m.put("condamnationPrononcee", true);
        m.put("indigniteJudiciaireDemandee", false);
        m.put("pardonTestamentaireDetecte", false);
        m.put("dateOuvertureSuccession", LocalDate.now().minusYears(1).toString());
        m.put("nbCoheritiersRestants", 2);
        return m;
    }

    @Test
    void POST_fr_meurtre_condamne_renvoie_indignite_plein_droit() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.typeIndignite").value("PLEIN_DROIT"))
                .andExpect(jsonPath("$.verdictIndignite").value("INDIGNITE_PLEIN_DROIT"))
                .andExpect(jsonPath("$.representationPossible").value(false));
    }

    @Test
    void POST_fr_pardon_testamentaire_neutralise() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("pardonTestamentaireDetecte", true);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeIndignite").value("PARDONNEE"))
                .andExpect(jsonPath("$.verdictIndignite").value("PARDON_NEUTRALISANT"))
                .andExpect(jsonPath("$.pardonNeutralisant").value(true));
    }

    @Test
    void POST_fr_violences_graves_indignite_judiciaire() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("motifIndignite", "VIOLENCES_GRAVES");
        body.put("condamnationPrononcee", false);
        body.put("indigniteJudiciaireDemandee", true);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeIndignite").value("JUDICIAIRE"))
                .andExpect(jsonPath("$.verdictIndignite").value("INDIGNITE_JUDICIAIRE_POSSIBLE"))
                .andExpect(jsonPath("$.representationPossible").value(true));
    }

    @Test
    void POST_be_returns400_gate_pays() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/indignite-successorale")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.typeIndignite").value("PLEIN_DROIT"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_sans_motif_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.remove("motifIndignite");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_sans_date_ouverture_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.remove("dateOuvertureSuccession");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_date_future_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateOuvertureSuccession", LocalDate.now().plusDays(1).toString());
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/indignite-successorale")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
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
