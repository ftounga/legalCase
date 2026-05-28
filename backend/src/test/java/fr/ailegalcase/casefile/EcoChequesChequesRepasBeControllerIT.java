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
 * SF-219-21 : tests d'intégration de l'endpoint <i>éco-chèques +
 * chèques-repas BE</i> (CCT n°98 + Loi 25/04/2014 + AR 03/02/2010).
 *
 * <p>Couvre : POST BE 200 (verdicts CONFORME_EXONERATION_INTEGRALE,
 * CONFORME_PARTIELLEMENT_EXONERE, NON_CONFORME_DEPASSEMENT_PLAFOND,
 * NON_CONFORME_SUBSTITUTION_REMUNERATION,
 * NON_CONFORME_CONDITION_MANQUANTE, A_ANALYSER), POST FR 404 (gate BE
 * strict), POST caseFile autre workspace 404, POST dossier non
 * DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class EcoChequesChequesRepasBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/eco-cheques-cheques-repas-be";

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
        User uBe = save(new User(), u -> { u.setEmail("ecbe-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ecbe-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE ECBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE ECBE " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-ecbe-be-" + ts, "ecbe-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("ecbe-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ecbe-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR ECBE " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR ECBE " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ecbe-fr-" + ts, "ecbe-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("ecbe-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-ecbe-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 ECBE " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 ECBE " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — éco-chèques conformes 240 EUR < 250 EUR plafond. */
    private Map<String, Object> bodyEcoChequesConforme() {
        Map<String, Object> m = new HashMap<>();
        m.put("typeAvantage", "ECO_CHEQUES");
        m.put("montantAnnuelEur", 240.00);
        m.put("valeurFacialeUnitaireEur", 10.00);
        m.put("contributionTravailleurUnitaireEur", 0.00);
        m.put("joursEffectivementPrestes", 0);
        m.put("cctSectorielleOuEntrepriseExiste", true);
        m.put("conventionIndividuelleEcrite", false);
        m.put("paiementElectronique", false);
        m.put("cumulFraisBouche", false);
        m.put("substitutionRemuneration", false);
        m.put("dateAttribution", "2024-06-01");
        return m;
    }

    @Test
    void POST_workspaceBe_ecoChequesConforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEcoChequesConforme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_EXONERATION_INTEGRALE"))
                .andExpect(jsonPath("$.plafondLegalApplicableEur").value(250.00))
                .andExpect(jsonPath("$.montantExonereEur").value(240.00))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("CCT n°98")));
    }

    @Test
    void POST_workspaceBe_ecoChequesDepassementFaible_returnsConformePartiellement() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("montantAnnuelEur", 265.00); // 15 EUR de dépassement, < 10%

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_PARTIELLEMENT_EXONERE"))
                .andExpect(jsonPath("$.montantRequalifieEnRemunerationEur").value(15.00));
    }

    @Test
    void POST_workspaceBe_ecoChequesDepassementStructurel_returnsNonConforme() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("montantAnnuelEur", 500.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_DEPASSEMENT_PLAFOND"))
                .andExpect(jsonPath("$.montantRequalifieEnRemunerationEur").value(250.00))
                .andExpect(jsonPath("$.cotisationsOnssEstimeesEur").value(62.50));
    }

    @Test
    void POST_workspaceBe_chequesRepasConforme_returnsConformeIntegrale() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeAvantage", "CHEQUES_REPAS");
        body.put("montantAnnuelEur", 1380.00);
        body.put("valeurFacialeUnitaireEur", 8.00);
        body.put("contributionTravailleurUnitaireEur", 1.09);
        body.put("joursEffectivementPrestes", 220);
        body.put("cctSectorielleOuEntrepriseExiste", true);
        body.put("conventionIndividuelleEcrite", false);
        body.put("paiementElectronique", true);
        body.put("cumulFraisBouche", false);
        body.put("substitutionRemuneration", false);
        body.put("dateAttribution", "2024-06-01");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_EXONERATION_INTEGRALE"))
                .andExpect(jsonPath("$.plafondLegalApplicableEur").value(1760.00));
    }

    @Test
    void POST_workspaceBe_chequesRepasPaiementPapierPost2016_returnsNonConforme() throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("typeAvantage", "CHEQUES_REPAS");
        body.put("montantAnnuelEur", 1380.00);
        body.put("valeurFacialeUnitaireEur", 8.00);
        body.put("contributionTravailleurUnitaireEur", 1.09);
        body.put("joursEffectivementPrestes", 220);
        body.put("cctSectorielleOuEntrepriseExiste", true);
        body.put("conventionIndividuelleEcrite", false);
        body.put("paiementElectronique", false);
        body.put("cumulFraisBouche", false);
        body.put("substitutionRemuneration", false);
        body.put("dateAttribution", "2024-06-01");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_CONDITION_MANQUANTE"))
                .andExpect(jsonPath("$.raison").value("PAIEMENT_NON_ELECTRONIQUE_POST_2016"));
    }

    @Test
    void POST_workspaceBe_substitutionRemuneration_returnsNonConformeSubstitution() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("substitutionRemuneration", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_SUBSTITUTION_REMUNERATION"))
                .andExpect(jsonPath("$.raison").value("SUBSTITUTION_REMUNERATION"));
    }

    @Test
    void POST_workspaceBe_absenceCctEtConvention_returnsConditionManquante() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("cctSectorielleOuEntrepriseExiste", false);
        body.put("conventionIndividuelleEcrite", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_CONDITION_MANQUANTE"))
                .andExpect(jsonPath("$.raison").value("ABSENCE_CCT_OU_CONVENTION_ECRITE"));
    }

    @Test
    void POST_workspaceBe_typeIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("typeAvantage", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("TYPE_AVANTAGE_INDETERMINE"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEcoChequesConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEcoChequesConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEcoChequesConforme())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEcoChequesConforme())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_EXONERATION_INTEGRALE"))
                .andExpect(jsonPath("$.typeAvantage").value("ECO_CHEQUES"))
                .andExpect(jsonPath("$.montantAnnuelEur").value(240.00));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_typeAvantageManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.remove("typeAvantage");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montantManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.remove("montantAnnuelEur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_montantNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("montantAnnuelEur", -1.00);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_joursNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.put("joursEffectivementPrestes", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_substitutionManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEcoChequesConforme();
        body.remove("substitutionRemuneration");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur PeculeVacancesBeControllerIT) ----

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
