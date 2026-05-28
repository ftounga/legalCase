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
 * SF-219-10 : tests d'intégration de l'endpoint <i>statut délégué
 * syndical CCT n° 5</i> (CCT n° 5 du 24/05/1971 + CCT sectorielles).
 *
 * <p>Couvre : POST BE 200 (verdicts RECONNU / FRAGILE / HORS_CHAMP /
 * PAS_DESIGNE / A_ANALYSER), POST FR 404 (gate BE strict), POST
 * caseFile autre workspace 404 (isolation workspace), POST dossier
 * non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400 (champs requis).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class DelegueSyndicalCct5ControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/delegue-syndical-cct-5";

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
        User uBe = save(new User(), u -> { u.setEmail("ds-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ds-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE DS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE DS " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-ds-be-" + ts, "ds-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("ds-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ds-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR DS " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR DS " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ds-fr-" + ts, "ds-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("ds-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-ds-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 DS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 DS " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — statut reconnu (effectif 80, seuil 50, OS présente, désignation OK, CE+CPPT). */
    private Map<String, Object> bodyReconnu() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDesignation", "2026-04-01");
        m.put("effectifEntreprise", 80);
        m.put("seuilSectorielRequis", 50);
        m.put("presenceOsRepresentative", true);
        m.put("statutDesignation", "DESIGNE_PAR_OS_REPRESENTATIVE");
        m.put("ceExistant", true);
        m.put("cpptExistant", true);
        return m;
    }

    @Test
    void POST_workspaceBe_statutReconnu_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnu())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("STATUT_RECONNU"))
                .andExpect(jsonPath("$.eligibleStatutDs").value(true))
                .andExpect(jsonPath("$.entrepriseDansChamp").value(true))
                .andExpect(jsonPath("$.designationReguliere").value(true))
                .andExpect(jsonPath("$.dateFinMandatIndicative").value("2030-04-01"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("CCT n° 5 du 24/05/1971")));
    }

    @Test
    void POST_workspaceBe_notificationManquante_returnsStatutFragile() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("statutDesignation", "NOTIFICATION_EMPLOYEUR_MANQUANTE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("STATUT_FRAGILE_NOTIFICATION_MANQUANTE"))
                .andExpect(jsonPath("$.eligibleStatutDs").value(false))
                .andExpect(jsonPath("$.entrepriseDansChamp").value(true))
                .andExpect(jsonPath("$.designationReguliere").value(false))
                .andExpect(jsonPath("$.raison").value("NOTIFICATION_EMPLOYEUR_MANQUANTE"));
    }

    @Test
    void POST_workspaceBe_effectifSousSeuil_returnsHorsChamp() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("effectifEntreprise", 30);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_ENTREPRISE_HORS_CHAMP"))
                .andExpect(jsonPath("$.entrepriseDansChamp").value(false))
                .andExpect(jsonPath("$.raison").value("ENTREPRISE_HORS_CHAMP_CCT5"));
    }

    @Test
    void POST_workspaceBe_pasOsRepresentative_returnsHorsChamp() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("presenceOsRepresentative", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_ENTREPRISE_HORS_CHAMP"));
    }

    @Test
    void POST_workspaceBe_pasDesignePerOs_returnsIneligible() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("statutDesignation", "PAS_DESIGNE_PAR_OS");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_PAS_DESIGNE_PAR_OS"))
                .andExpect(jsonPath("$.raison").value("PAS_DESIGNE_PAR_OS_REPRESENTATIVE"));
    }

    @Test
    void POST_workspaceBe_designationContestee_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("statutDesignation", "DESIGNATION_CONTESTEE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("DESIGNATION_CONTESTEE"));
    }

    @Test
    void POST_workspaceBe_missionsSuppletivesArt24_quandCeEtCpptAbsents() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("ceExistant", false);
        body.put("cpptExistant", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("STATUT_RECONNU"))
                .andExpect(jsonPath("$.missionsExercables",
                        org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsString("Conseil d'entreprise"))))
                .andExpect(jsonPath("$.missionsExercables",
                        org.hamcrest.Matchers.hasItem(
                                org.hamcrest.Matchers.containsString("Comité PPT"))));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnu())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnu())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnu())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyReconnu())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("STATUT_RECONNU"))
                .andExpect(jsonPath("$.dateDesignation").value("2026-04-01"))
                .andExpect(jsonPath("$.effectifEntreprise").value(80));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateDesignationManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.remove("dateDesignation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.remove("effectifEntreprise");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_seuilManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.remove("seuilSectorielRequis");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutDesignationManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.remove("statutDesignation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyReconnu();
        body.put("effectifEntreprise", -5);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur TransfertEntrepriseCct32bisControllerIT) ----

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
