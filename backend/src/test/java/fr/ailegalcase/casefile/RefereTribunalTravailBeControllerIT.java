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
 * SF-207-05 : tests d'intégration de l'endpoint référé tribunal du travail BE.
 *
 * <p>Couvre les cas mini-spec : POST BE 200 (nominal REFERE_ELIGIBLE), POST FR
 * 404 (gate BE strict — isolation pays), POST caseFile autre workspace 404
 * (isolation workspace), GET après POST, GET sans POST 404, validation 400
 * (Bean Validation sur motifUrgence / mesureProvisoireDemandee), date future
 * 400, INCERTAIN + squelette présent, NON_ELIGIBLE + squelette absent.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class RefereTribunalTravailBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/refere-tribunal-travail-be";

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

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("rtbe-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rtbe-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE RTBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE RTBE " + ts, "DROIT_DU_TRAVAIL");
        authBe = buildAuth("g-rtbe-be-" + ts, "rtbe-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("rtbe-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rtbe-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR RTBE " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR RTBE " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rtbe-fr-" + ts, "rtbe-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("rtbe-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-rtbe-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 RTBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 RTBE " + ts, "DROIT_DU_TRAVAIL");
    }

    /**
     * Body remplissant les 5 conditions cumulatives (verdict attendu : ELIGIBLE).
     */
    private Map<String, Object> bodyEligible() {
        Map<String, Object> m = new HashMap<>();
        m.put("motifUrgence", "HARCELEMENT");
        m.put("motifUrgenceDescription",
                "Harcèlement persistant du supérieur N+1 depuis février 2026");
        m.put("dateFaitGenerateur", "2026-05-15");
        m.put("dateDemarcheAmiable", "2026-05-16");
        m.put("preuveUrgenceJointe", true);
        m.put("mesureProvisoireDemandee", "Cessation immédiate des actes de harcèlement");
        m.put("perilEnDemeure", true);
        m.put("competenceTerritorialeIdentifiee", true);
        return m;
    }

    @Test
    void POST_workspaceBe_eligible_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("REFERE_ELIGIBLE"))
                .andExpect(jsonPath("$.scoreConditions").value(5))
                .andExpect(jsonPath("$.conditionsNonRemplies").isArray())
                .andExpect(jsonPath("$.conditionsNonRemplies.length()").value(0))
                .andExpect(jsonPath("$.requeteSquelette",
                        org.hamcrest.Matchers.containsString("REQUÊTE EN RÉFÉRÉ")))
                .andExpect(jsonPath("$.requeteSquelette",
                        org.hamcrest.Matchers.containsString("art. 584")))
                .andExpect(jsonPath("$.etapeSuivante").value("DEPOSER_REQUETE"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("art. 584")));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("BELGIQUE"))
                .andExpect(jsonPath("$.verdict").value("REFERE_ELIGIBLE"))
                .andExpect(jsonPath("$.motifUrgence").value("HARCELEMENT"))
                .andExpect(jsonPath("$.scoreConditions").value(5));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_motifUrgenceManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("motifUrgence");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_mesureProvisoireBlanche_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("mesureProvisoireDemandee", "   ");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateFaitGenerateurFuture_returns400() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dateFaitGenerateur", "2099-01-01");
        body.remove("dateDemarcheAmiable");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDemarcheAmiableAvantDateFait_returns400() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dateFaitGenerateur", "2026-05-15");
        body.put("dateDemarcheAmiable", "2026-05-10");                 // antérieure → 400
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_scoreIncertain_returnsSqueletteEtRenforcerDossier() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("preuveUrgenceJointe", false);                        // -1 → score 4
        body.put("perilEnDemeure", false);                             // -1 → score 3
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REFERE_INCERTAIN"))
                .andExpect(jsonPath("$.scoreConditions").value(3))
                .andExpect(jsonPath("$.requeteSquelette").isNotEmpty())
                .andExpect(jsonPath("$.etapeSuivante").value("RENFORCER_DOSSIER"))
                .andExpect(jsonPath("$.conditionsNonRemplies",
                        org.hamcrest.Matchers.hasItem("PEUVE_URGENCE_JOINTE")))
                .andExpect(jsonPath("$.conditionsNonRemplies",
                        org.hamcrest.Matchers.hasItem("PERIL_EN_DEMEURE")));
    }

    @Test
    void POST_scoreNonEligible_returnsSquletteNullEtAlternativeProcedure() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("motifUrgence", "AUTRE");
        body.put("motifUrgenceDescription", "court");                  // URGENCE_QUALIFIABLE KO
        body.put("preuveUrgenceJointe", false);
        body.put("mesureProvisoireDemandee", "stop");                  // MESURE KO
        body.put("perilEnDemeure", false);
        body.put("competenceTerritorialeIdentifiee", false);
        body.remove("dateDemarcheAmiable");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("REFERE_NON_ELIGIBLE"))
                .andExpect(jsonPath("$.scoreConditions").value(0))
                .andExpect(jsonPath("$.requeteSquelette").doesNotExist())
                .andExpect(jsonPath("$.etapeSuivante").value("ALTERNATIVE_PROCEDURE_FOND"));
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
