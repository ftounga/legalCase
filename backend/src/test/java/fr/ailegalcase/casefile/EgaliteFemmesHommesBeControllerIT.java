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
 * SF-219-22 : tests d'intégration de l'endpoint <i>égalité salariale
 * femmes/hommes BE</i> (Loi 22/04/2012 art. 2 + AR 17/08/2013 + AR
 * 25/04/2014 + CCT n° 25 + C. pén. social art. 195/1).
 *
 * <p>Couvre : POST BE 200 (verdicts CONFORME_RAPPORT_PLAN_COMPLETS /
 * HORS_CHAMP_SEUIL_NON_ATTEINT / NON_CONFORME_RAPPORT_INCOMPLET /
 * NON_CONFORME_PLAN_ACTION_MANQUANT /
 * MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE / A_ANALYSER), POST FR 404
 * (gate BE strict), POST caseFile autre workspace 404, POST dossier
 * non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400, type de formulaire requis selon
 * effectif.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class EgaliteFemmesHommesBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/egalite-femmes-hommes-be";

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
        User uBe = save(new User(), u -> { u.setEmail("efh-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-efh-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE EFH " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE EFH " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-efh-be-" + ts, "efh-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("efh-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-efh-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR EFH " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR EFH " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-efh-fr-" + ts, "efh-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("efh-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-efh-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 EFH " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 EFH " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — rapport biennal complet déposé, pas d'écart. */
    private Map<String, Object> bodyConforme() {
        Map<String, Object> m = new HashMap<>();
        m.put("effectifEntreprise", 75);
        m.put("statutRapport", "DEPOSE_FORMULAIRE_COMPLET");
        m.put("dateDepotRapport", "2024-06-01");
        m.put("ventilationNiveauFonctionFournie", true);
        m.put("ventilationAncienneteFournie", true);
        m.put("ventilationQualificationFournie", true);
        m.put("ventilationRegimeTravailFournie", true);
        m.put("ventilationComposantsRemunerationFournie", true);
        m.put("ecartSalarialNonJustifieConstate", false);
        m.put("pourcentageEcartConstate", null);
        m.put("planActionEtabli", true);
        m.put("mediateurDesigne", true);
        m.put("plainteIefhOuInspectionEnCours", false);
        return m;
    }

    @Test
    void POST_workspaceBe_conforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_RAPPORT_PLAN_COMPLETS"))
                .andExpect(jsonPath("$.seuilAtteint").value(true))
                .andExpect(jsonPath("$.rapportDepose").value(true))
                .andExpect(jsonPath("$.ventilationComplete").value(true))
                .andExpect(jsonPath("$.planActionRequis").value(false))
                .andExpect(jsonPath("$.typeFormulaireRequis").value("ANNEXE_II_SIMPLIFIE"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 22/04/2012")));
    }

    @Test
    void POST_workspaceBe_effectif150_basculeFormulaireDetaille() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("effectifEntreprise", 150);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_RAPPORT_PLAN_COMPLETS"))
                .andExpect(jsonPath("$.typeFormulaireRequis").value("ANNEXE_I_DETAILLE"));
    }

    @Test
    void POST_workspaceBe_effectif40_returnsHorsChamp() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("effectifEntreprise", 40);
        body.put("statutRapport", "NON_DEPOSE_DELAI_DEPASSE");
        body.put("ventilationNiveauFonctionFournie", false);
        body.put("ventilationAncienneteFournie", false);
        body.put("ventilationQualificationFournie", false);
        body.put("ventilationRegimeTravailFournie", false);
        body.put("ventilationComposantsRemunerationFournie", false);
        body.put("planActionEtabli", false);
        body.put("mediateurDesigne", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("HORS_CHAMP_SEUIL_NON_ATTEINT"))
                .andExpect(jsonPath("$.seuilAtteint").value(false))
                .andExpect(jsonPath("$.typeFormulaireRequis").value("NON_APPLICABLE"));
    }

    @Test
    void POST_workspaceBe_rapportNonDepose_returnsManquementGrave() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutRapport", "NON_DEPOSE_DELAI_DEPASSE");
        body.put("dateDepotRapport", null);
        body.put("ventilationNiveauFonctionFournie", false);
        body.put("ventilationAncienneteFournie", false);
        body.put("ventilationQualificationFournie", false);
        body.put("ventilationRegimeTravailFournie", false);
        body.put("ventilationComposantsRemunerationFournie", false);
        body.put("planActionEtabli", false);
        body.put("mediateurDesigne", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE"));
    }

    @Test
    void POST_workspaceBe_ventilationIncomplete_returnsRapportIncomplet() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("ventilationNiveauFonctionFournie", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_RAPPORT_INCOMPLET"))
                .andExpect(jsonPath("$.ventilationComplete").value(false));
    }

    @Test
    void POST_workspaceBe_ecartSansPlanAction_returnsPlanActionManquant() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("ecartSalarialNonJustifieConstate", true);
        body.put("pourcentageEcartConstate", 9.5);
        body.put("planActionEtabli", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_PLAN_ACTION_MANQUANT"))
                .andExpect(jsonPath("$.planActionRequis").value(true))
                .andExpect(jsonPath("$.planActionConforme").value(false));
    }

    @Test
    void POST_workspaceBe_statutIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutRapport", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("STATUT_RAPPORT_INDETERMINE"));
    }

    @Test
    void POST_workspaceBe_enPreparation_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("statutRapport", "EN_PREPARATION");
        body.put("dateDepotRapport", null);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("RAPPORT_EN_PREPARATION"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConforme())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_RAPPORT_PLAN_COMPLETS"))
                .andExpect(jsonPath("$.statutRapport").value("DEPOSE_FORMULAIRE_COMPLET"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_effectifManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("effectifEntreprise");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutRapportManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("statutRapport");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("effectifEntreprise", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_ventilationNiveauFonctionManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.remove("ventilationNiveauFonctionFournie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_pourcentageEcartSuperieur100_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConforme();
        body.put("pourcentageEcartConstate", 150);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur DroitDeconnexionBeControllerIT) ----

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
