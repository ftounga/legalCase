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
 * SF-219-12 : tests d'intégration de l'endpoint <i>flexi-job BE</i>
 * (Loi-programme 26/12/2013 + extensions 2015/2018/2023).
 *
 * <p>Couvre : POST BE 200 (verdicts ELIGIBLE / FRAGILE_CONTRAT /
 * FRAGILE_PLAFOND / INELIGIBLE_TRAVAILLEUR / INELIGIBLE_SECTEUR /
 * CUMUL_INTERDIT / A_ANALYSER), POST FR 404 (gate BE strict), POST
 * caseFile autre workspace 404, POST dossier non DROIT_DU_TRAVAIL 400,
 * GET 404 sans POST, GET retour persisté, validations Bean Validation
 * 400 (champs requis et bornes décimales).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class FlexiJobBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/flexi-job-be";

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
        User uBe = save(new User(), u -> { u.setEmail("flx-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-flx-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE FLX " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE FLX " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-flx-be-" + ts, "flx-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("flx-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-flx-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR FLX " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR FLX " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-flx-fr-" + ts, "flx-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("flx-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-flx-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 FLX " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 FLX " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — flexi-job éligible (salarié 4/5 ETP T-3, HoReCa, contrat-cadre + Dimona OK). */
    private Map<String, Object> bodyEligible() {
        Map<String, Object> m = new HashMap<>();
        m.put("datePremiereOccupationFlexi", "2024-06-01");
        m.put("statutTravailleur", "SALARIE_4_5_TEMPS_T_MOINS_3");
        m.put("secteurEmployeur", "HORECA_302");
        m.put("dejaOccupeChezMemeEmployeur", false);
        m.put("contratCadreSigne", true);
        m.put("dimonaFlxDeclaree", true);
        m.put("flexiSalaireHoraireBrut", 13.31);
        m.put("flexiSalaireMinimumApplicable", 12.33);
        m.put("revenuAnnuelFlexiCumule", 5000.00);
        m.put("plafondAnnuelExonere", 12000.00);
        return m;
    }

    @Test
    void POST_workspaceBe_eligible_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.travailleurEligible").value(true))
                .andExpect(jsonPath("$.secteurEligible").value(true))
                .andExpect(jsonPath("$.cumulRespecte").value(true))
                .andExpect(jsonPath("$.formalismeRespecte").value(true))
                .andExpect(jsonPath("$.remunerationConforme").value(true))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("26 décembre 2013")));
    }

    @Test
    void POST_workspaceBe_pensionne_pasDePlafondCheck() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("statutTravailleur", "PENSIONNE");
        body.put("revenuAnnuelFlexiCumule", 50000.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"));
    }

    @Test
    void POST_workspaceBe_travailleurAutre_returnsTravailleurHorsCondition() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("statutTravailleur", "AUTRE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_TRAVAILLEUR_HORS_CONDITION"))
                .andExpect(jsonPath("$.travailleurEligible").value(false))
                .andExpect(jsonPath("$.raison").value("TRAVAILLEUR_HORS_CONDITION"));
    }

    @Test
    void POST_workspaceBe_secteurNonEligible_returnsSecteurNonEligible() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("secteurEmployeur", "AUTRE_NON_ELIGIBLE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_SECTEUR_NON_ELIGIBLE"))
                .andExpect(jsonPath("$.secteurEligible").value(false));
    }

    @Test
    void POST_workspaceBe_zoneGriseExt_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("secteurEmployeur", "ZONE_GRISE_EXTENSION_RECENTE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("SECTEUR_ZONE_GRISE"));
    }

    @Test
    void POST_workspaceBe_cumulInterdit_returnsCumulInterdit() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dejaOccupeChezMemeEmployeur", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR"))
                .andExpect(jsonPath("$.cumulRespecte").value(false));
    }

    @Test
    void POST_workspaceBe_dimonaManquante_returnsFragileContrat() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("dimonaFlxDeclaree", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_CONTRAT_OU_DIMONA_MANQUANT"))
                .andExpect(jsonPath("$.formalismeRespecte").value(false));
    }

    @Test
    void POST_workspaceBe_plafondDepasse_returnsFragilePlafond() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("revenuAnnuelFlexiCumule", 15000.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_PLAFOND_DEPASSE"))
                .andExpect(jsonPath("$.remunerationConforme").value(false))
                .andExpect(jsonPath("$.montantExcedentaireAuPlafond").value(3000.00));
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
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligible())))
                .andExpect(status().isBadRequest());
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
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE"))
                .andExpect(jsonPath("$.statutTravailleur").value("SALARIE_4_5_TEMPS_T_MOINS_3"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("datePremiereOccupationFlexi");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("statutTravailleur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_secteurManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.remove("secteurEmployeur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_salaireHoraireNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("flexiSalaireHoraireBrut", -1.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_plafondZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligible();
        body.put("plafondAnnuelExonere", 0.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur DelegueSyndicalCct5ControllerIT) ----

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
