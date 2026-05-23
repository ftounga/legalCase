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
 * SF-216-27 : IT minimal POST + GET + gates pour l'outil Partage
 * successoral notarié FR (art. 816 et s. Cciv + art. 870 Cciv + art. 1592
 * CGI + art. 641 CGI + art. 840 Cciv).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class PartageNotarialControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("part-not-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-part-not-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRPARTNOT " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRPARTNOT " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-part-not-fr-" + ts, "part-not-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("part-not-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-part-not-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEPARTNOT " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEPARTNOT " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-part-not-be-" + ts, "part-not-be-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateOuvertureSuccession", LocalDate.now().minusMonths(2).toString());
        m.put("nombreCoheritiers", 3);
        m.put("consentementsTousDetecte", true);
        m.put("presenceImmeuble", true);
        m.put("desaccordPersistant", false);
        m.put("valeurMasseSuccessoraleEur", 500000);
        m.put("notaireDesigne", true);
        return m;
    }

    @Test
    void POST_fr_nominal_notaire_obligatoire_calendrier_5_etapes() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.notaireObligatoire").value(true))
                .andExpect(jsonPath("$.calendrierEtapes.length()").value(5))
                .andExpect(jsonPath("$.orientationJudiciaire").value(false));
    }

    @Test
    void POST_fr_desaccord_orientation_judiciaire() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("desaccordPersistant", true);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orientationJudiciaire").value(true));
    }

    @Test
    void POST_fr_delai_fiscal_depasse_alerte() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateOuvertureSuccession", LocalDate.now().minusMonths(8).toString());
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alerteDelai").value(true));
    }

    @Test
    void POST_be_returns400_gate_pays() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/partage-notarial")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaireObligatoire").value(true));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_sans_dateOuverture_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.remove("dateOuvertureSuccession");
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateOuverture_future_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("dateOuvertureSuccession", LocalDate.now().plusMonths(1).toString());
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nbHeritiers_zero_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("nombreCoheritiers", 0);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_valeur_negative_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("valeurMasseSuccessoraleEur", -1);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/partage-notarial")
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
