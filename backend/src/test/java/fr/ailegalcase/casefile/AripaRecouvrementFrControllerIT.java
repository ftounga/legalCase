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
 * SF-216-07 : IT minimal POST + GET + gate BE pour l'outil ARIPA recouvrement.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AripaRecouvrementFrControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("aripa-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-aripa-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRARIPA " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRARIPA " + ts, "DROIT_FAMILLE");
        authFrFa = buildAuth("g-aripa-fr-" + ts, "aripa-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("aripa-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-aripa-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEARIPA " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEARIPA " + ts, "DROIT_FAMILLE");
        authBeFa = buildAuth("g-aripa-be-" + ts, "aripa-be-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("montantPensionMensuelleEur", 400);
        m.put("nombreMoisImpayes", 3);
        m.put("situationCreancier", "SALARIE");
        m.put("situationDebiteur", "SALARIE");
        m.put("titreExecutoire", true);
        m.put("debiteurEnFrance", true);
        m.put("debiteurEmployeurPublic", false);
        m.put("nombreEnfantsACharge", 1);
        return m;
    }

    @Test
    void POST_fr_donnees_valides_renvoie_200_avec_voie_saisie_sur_salaire() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.voieRecommandee").value("SAISIE_SUR_SALAIRE"))
                .andExpect(jsonPath("$.montantArrieres").value(1_200));
    }

    @Test
    void POST_be_returns400_gate_pays() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authBeFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voieRecommandee").value("SAISIE_SUR_SALAIRE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_nombreMoisImpayesZero_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("nombreMoisImpayes", 0);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_sansTitreExecutoire_returns200_voie_titre_requis() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("titreExecutoire", false);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voieRecommandee").value("TITRE_REQUIS"));
    }

    @Test
    void POST_debiteurEtranger_returns200_voie_convention_internationale() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("debiteurEnFrance", false);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/aripa-recouvrement-fr")
                        .with(authentication(authFrFa)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voieRecommandee").value("CONVENTION_INTERNATIONALE"));
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
