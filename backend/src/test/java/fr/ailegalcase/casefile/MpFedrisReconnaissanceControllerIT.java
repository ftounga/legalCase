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
 * SF-219-28 : tests d'intégration de l'endpoint <i>Reconnaissance d'une
 * maladie professionnelle Fedris BE</i> (Lois coordonnees du 03/06/1970
 * + AR du 28/03/1969 liste fermee modifie 06/12/2018 + AR du 16/12/1985
 * systeme ouvert + Loi du 11/01/2018 reformant Fedris).
 *
 * <p>Couvre : POST BE 200 (MALADIE_LISTE_FERMEE_PRESOMPTION /
 * MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE / SYSTEME_OUVERT_
 * CAUSALITE_DIRECTE_DETERMINANTE / SYSTEME_OUVERT_CAUSALITE_
 * INSUFFISANTE / DECLARATION_PRESCRITE / A_QUALIFIER), POST FR 404
 * (gate BE strict), POST caseFile autre workspace 404, POST dossier
 * non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persiste,
 * validations Bean Validation 400, calcul prescription triennale,
 * drapeaux presomptionApplicable et causaliteRequise.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class MpFedrisReconnaissanceControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/mp-fedris-reconnaissance";

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
    private CaseFile beFamilleCaseFile;
    private CaseFile frCaseFile;
    private CaseFile beOtherWorkspaceCaseFile;

    @BeforeEach
    void setUp() {
        long ts = System.nanoTime();

        // Workspace BE DROIT_DU_TRAVAIL → cible
        User uBe = save(new User(), u -> { u.setEmail("mpbe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-mpbe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE MP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE MP " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-mpbe-" + ts, "mpbe-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("mpfr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-mpfr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR MP " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR MP " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-mpfr-" + ts, "mpfr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("mpbe2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-mpbe2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 MP " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 MP " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — LISTE_FERMEE + exposition AVEREE → présomption. */
    private Map<String, Object> bodyListeFermeePresomption() {
        Map<String, Object> m = new HashMap<>();
        m.put("typeMaladie", "LISTE_FERMEE");
        m.put("codeMaladieListe", "1.404.01 silicose");
        m.put("libelleMaladie", "Silicose pulmonaire");
        m.put("dateConstatationMedicale", "2024-01-15");
        m.put("dateConnaissanceProfessionnel", "2024-02-01");
        m.put("dateDeclarationEnvisagee", "2024-06-01");
        m.put("exposition", "AVEREE");
        m.put("causalite", "NON_APPLICABLE");
        return m;
    }

    @Test
    void POST_workspaceBe_listeFermeeAveree_returnsPresomption() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyListeFermeePresomption())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("MALADIE_LISTE_FERMEE_PRESOMPTION"))
                .andExpect(jsonPath("$.presomptionApplicable").value(true))
                .andExpect(jsonPath("$.causaliteRequise").value(false))
                .andExpect(jsonPath("$.prescriteAuJour").value(false))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Lois coordonnees du 03/06/1970")));
    }

    @Test
    void POST_workspaceBe_listeFermeeExpositionInsuffisante_returnsRefus() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.put("exposition", "INSUFFISANTE");
        body.put("codeMaladieListe", "2.2.2 surdite bruit");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE"))
                .andExpect(jsonPath("$.presomptionApplicable").value(false));
    }

    @Test
    void POST_workspaceBe_listeFermeeExpositionIndeterminee_returnsAQualifier() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.put("exposition", "INDETERMINEE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_QUALIFIER"));
    }

    @Test
    void POST_workspaceBe_horsListeCausaliteEtablie_returnsSystemeOuvert() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeMaladie", "HORS_LISTE");
        body.put("codeMaladieListe", null);
        body.put("libelleMaladie", "Burn-out syndrome professionnel");
        body.put("dateConstatationMedicale", "2024-01-15");
        body.put("dateConnaissanceProfessionnel", "2024-02-01");
        body.put("dateDeclarationEnvisagee", "2024-06-01");
        body.put("exposition", "AVEREE");
        body.put("causalite", "DIRECTE_DETERMINANTE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE"))
                .andExpect(jsonPath("$.presomptionApplicable").value(false))
                .andExpect(jsonPath("$.causaliteRequise").value(true));
    }

    @Test
    void POST_workspaceBe_horsListeCausaliteInsuffisante_returnsRefus() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeMaladie", "HORS_LISTE");
        body.put("libelleMaladie", "Trouble musculo-squelettique non liste");
        body.put("dateConstatationMedicale", "2024-01-15");
        body.put("dateConnaissanceProfessionnel", "2024-02-01");
        body.put("dateDeclarationEnvisagee", "2024-06-01");
        body.put("exposition", "AVEREE");
        body.put("causalite", "INSUFFISANTE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE"))
                .andExpect(jsonPath("$.causaliteRequise").value(true));
    }

    @Test
    void POST_workspaceBe_horsListeCausaliteIndeterminee_returnsAQualifier() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeMaladie", "HORS_LISTE");
        body.put("libelleMaladie", "Trouble anxieux professionnel");
        body.put("dateConstatationMedicale", "2024-01-15");
        body.put("dateConnaissanceProfessionnel", "2024-02-01");
        body.put("dateDeclarationEnvisagee", "2024-06-01");
        body.put("exposition", "AVEREE");
        body.put("causalite", "INDETERMINEE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_QUALIFIER"));
    }

    @Test
    void POST_workspaceBe_prescriptionDepassee_returnsPrescrit() throws Exception {
        // connaissance 2020-01-01 + 3 ans = 2023-01-01 ; declaration 2024-06-01 → prescrit
        Map<String, Object> body = bodyListeFermeePresomption();
        body.put("dateConstatationMedicale", "2020-01-01");
        body.put("dateConnaissanceProfessionnel", "2020-01-01");
        body.put("dateDeclarationEnvisagee", "2024-06-01");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("DECLARATION_PRESCRITE"))
                .andExpect(jsonPath("$.prescriteAuJour").value(true));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyListeFermeePresomption())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyListeFermeePresomption())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyListeFermeePresomption())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyListeFermeePresomption())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("MALADIE_LISTE_FERMEE_PRESOMPTION"))
                .andExpect(jsonPath("$.libelleMaladie")
                        .value("Silicose pulmonaire"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_workspaceFr_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_typeMaladieManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.remove("typeMaladie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_libelleManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.remove("libelleMaladie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateConstatationManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.remove("dateConstatationMedicale");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_expositionManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.remove("exposition");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_causaliteManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.remove("causalite");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_listeFermeeSansCode_returns400_serviceValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.put("codeMaladieListe", null);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_connaissanceAvantConstatation_returns400_serviceValidation() throws Exception {
        Map<String, Object> body = bodyListeFermeePresomption();
        body.put("dateConstatationMedicale", "2024-06-01");
        body.put("dateConnaissanceProfessionnel", "2024-01-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur TravailNoirBeDimonaControllerIT) ----

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
