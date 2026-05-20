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
 * SF-207-03 : tests d'intégration de l'endpoint contestation C4 ONEM.
 *
 * <p>Couvre les cas mini-spec : POST BE 200 (cas A nominal), POST FR 404
 * (isolation pays — gate BE strict), POST caseFile autre workspace 404
 * (isolation workspace), POST validation 400 (Bean Validation +
 * conditionnelle dateDecisionDirecteur), GET après POST, GET sans POST 404.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class ContestationC4OnemControllerIT {

    private static final String URL = "/api/v1/case-files/%s/decision-tools/contestation-c4-onem";

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
    private CaseFile beCaseFile;
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        User uBe = save(new User(), u -> { u.setEmail("cc4-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cc4-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE CC4 " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE CC4 " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-cc4-be-" + ts, "cc4-be-" + ts + "@ex.com");

        User uFr = save(new User(), u -> { u.setEmail("cc4-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cc4-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR CC4 " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR CC4 " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-cc4-fr-" + ts, "cc4-fr-" + ts + "@ex.com");

        User uBe2 = save(new User(), u -> { u.setEmail("cc4-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-cc4-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 CC4 " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 CC4 " + ts, "DROIT_DU_TRAVAIL");
    }

    /**
     * Body cas A nominal : notif récente (ouverture), action explicite pour
     * rester déterministe vis-à-vis du seuil OUVERT/IMMINENT.
     */
    private Map<String, Object> bodyCasAOuvert() {
        Map<String, Object> m = new HashMap<>();
        LocalDate notif = LocalDate.now().minusDays(2);
        m.put("dateNotificationDecisionOnem", notif.toString());
        m.put("dateActionEnvisagee", notif.plusDays(2).toString());
        m.put("recoursAdminDejaForme", false);
        return m;
    }

    /**
     * Body cas B nominal : recours admin déjà formé, décision Directeur
     * récente → palier TRIBUNAL ouvert.
     */
    private Map<String, Object> bodyCasBOuvert() {
        Map<String, Object> m = new HashMap<>();
        LocalDate notif = LocalDate.now().minusDays(60);
        LocalDate decisionDir = LocalDate.now().minusDays(10);
        m.put("dateNotificationDecisionOnem", notif.toString());
        m.put("dateActionEnvisagee", LocalDate.now().toString());
        m.put("recoursAdminDejaForme", true);
        m.put("dateDecisionDirecteur", decisionDir.toString());
        return m;
    }

    @Test
    void POST_workspaceBe_casA_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCasAOuvert())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("RECOURS_ADMIN_OUVERT"))
                .andExpect(jsonPath("$.etapeSuivante").value("RECOURS_ADMIN_DIRECTEUR"))
                .andExpect(jsonPath("$.paliers").isArray())
                .andExpect(jsonPath("$.paliers.length()").value(2))
                .andExpect(jsonPath("$.paliers[0].type").value("ADMIN"))
                .andExpect(jsonPath("$.paliers[0].dateLimite").exists())
                .andExpect(jsonPath("$.paliers[1].type").value("TRIBUNAL"))
                .andExpect(jsonPath("$.paliers[1].dateLimite").doesNotExist())
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("25 novembre 1991")));
    }

    @Test
    void POST_workspaceBe_casB_returns200_verdictTribunal() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCasBOuvert())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict",
                        org.hamcrest.Matchers.startsWith("RECOURS_TRIBUNAL_")))
                .andExpect(jsonPath("$.etapeSuivante").value("RECOURS_TRIBUNAL_TRAVAIL"))
                .andExpect(jsonPath("$.paliers[1].dateLimite").exists())
                .andExpect(jsonPath("$.paliers[1].joursRestants").exists());
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        // Avocat FR essaie l'endpoint sur son propre dossier FR → 404
        // (gate BE strict, l'outil n'existe pas côté FR — équivalent F-DT-35).
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCasAOuvert())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        // Avocat BE essaie d'accéder à un caseFile d'un autre workspace BE → 404.
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCasAOuvert())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateNotificationManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyCasAOuvert();
        body.remove("dateNotificationDecisionOnem");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_recoursAdminTrue_sansDateDecisionDirecteur_returns400() throws Exception {
        Map<String, Object> body = bodyCasAOuvert();
        body.put("recoursAdminDejaForme", true);
        // pas de dateDecisionDirecteur → validation conditionnelle 400.
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCasAOuvert())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("RECOURS_ADMIN_OUVERT"))
                .andExpect(jsonPath("$.dateNotificationDecisionOnem").exists());
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    // ---- helpers (alignés sur C4OnemChecklistControllerIT) ----

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
