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
 * SF-216-13 : IT minimal POST + GET + gates pour l'outil Audition du mineur
 * par le JAF FR (art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC + CIDE art. 12).
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class AuditionMineurControllerIT {

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
        User uFr = save(new User(), u -> { u.setEmail("audmin-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-audmin-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFRAUDMIN " + ts, "DROIT_FAMILLE", "FRANCE");
        saveMember(uFr, wsFr);
        faFrCf = saveCf(uFr, wsFr, "CFRAUDMIN " + ts, "DROIT_FAMILLE");
        authFr = buildAuth("g-audmin-fr-" + ts, "audmin-fr-" + ts + "@ex.com");

        User uBe = save(new User(), u -> { u.setEmail("audmin-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-audmin-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBEAUDMIN " + ts, "DROIT_FAMILLE", "BELGIQUE");
        saveMember(uBe, wsBe);
        faBeCf = saveCf(uBe, wsBe, "CBEAUDMIN " + ts, "DROIT_FAMILLE");
        authBe = buildAuth("g-audmin-be-" + ts, "audmin-be-" + ts + "@ex.com");
    }

    private Map<String, Object> bodyNominal() {
        Map<String, Object> m = new HashMap<>();
        m.put("ageEnfant", 10);
        m.put("capaciteDiscernement", "PROBABLE");
        m.put("demandeFormalisee", true);
        m.put("demandeParEnfantLuiMeme", false);
        m.put("refusMotive", false);
        m.put("motivationRefus", null);
        m.put("procedureEnCours", "DIVORCE");
        return m;
    }

    @Test
    void POST_fr_donnees_valides_renvoie_200_avec_audition_recommandee() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("FRANCE"))
                .andExpect(jsonPath("$.conditionsRemplies").value(true))
                .andExpect(jsonPath("$.verdict").value("AUDITION_RECOMMANDEE"))
                .andExpect(jsonPath("$.droitAuditionReconnu").value(true));
    }

    @Test
    void POST_fr_demande_par_enfant_lui_meme_audition_de_droit() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("ageEnfant", 13);
        body.put("capaciteDiscernement", "CERTAINE");
        body.put("demandeFormalisee", false);
        body.put("demandeParEnfantLuiMeme", true);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("AUDITION_DE_DROIT"))
                .andExpect(jsonPath("$.droitAuditionReconnu").value(true));
    }

    @Test
    void POST_fr_refus_non_motive_renvoie_refus_contestable() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("ageEnfant", 12);
        body.put("capaciteDiscernement", "CERTAINE");
        body.put("refusMotive", true);
        body.put("motivationRefus", null);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refusContestable").value(true))
                .andExpect(jsonPath("$.verdict").value("REFUS_CONTESTABLE"));
    }

    @Test
    void POST_fr_enfant_3_ans_alerte_discernement() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("ageEnfant", 3);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alertes").isArray());
    }

    @Test
    void POST_be_returns400_gate_pays() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faBeCf.getId() + "/audition-mineur")
                        .with(authentication(authBe)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_age_negatif_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("ageEnfant", -1);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_age_majeur_returns400() throws Exception {
        Map<String, Object> body = bodyNominal();
        body.put("ageEnfant", 18);
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_afterPost_returnsPersisted() throws Exception {
        mockMvc.perform(post("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyNominal())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("AUDITION_RECOMMANDEE"));
    }

    @Test
    void GET_withoutPost_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/case-files/" + faFrCf.getId() + "/audition-mineur")
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
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
