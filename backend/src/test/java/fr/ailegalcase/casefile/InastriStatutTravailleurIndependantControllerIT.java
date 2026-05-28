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
 * SF-219-27 : tests d'intégration de l'endpoint <i>INASTI — statut
 * travailleur indépendant et qualification salarié / indépendant</i>
 * (Loi du 27/06/1969 + AR n° 38 du 27/07/1967 + Loi-programme I du
 * 27/12/2006 art. 328 à 333 + critères sectoriels art. 337/2).
 *
 * <p>Couvre : POST BE 200 (INDEPENDANT_CONFIRME / SALARIE_REQUALIFIE
 * / FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE / PRESUMPTION_GENERALE_SALARIAT
 * / A_QUALIFIER), POST FR 404 (gate BE strict), POST caseFile autre
 * workspace 404, POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans
 * POST, GET retour persisté, validations Bean Validation 400,
 * cohérence DIMONA / statut déclaré, drapeau mono-client.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class InastriStatutTravailleurIndependantControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/inastri-statut-travailleur-independant";

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

        User uBe = save(new User(),
                u -> { u.setEmail("inabe-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-inabe-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE INA " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE INA " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE INA FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-inabe-" + ts, "inabe-" + ts + "@ex.com");

        User uFr = save(new User(),
                u -> { u.setEmail("inafr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-inafr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR INA " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR INA " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-inafr-" + ts, "inafr-" + ts + "@ex.com");

        User uBe2 = save(new User(),
                u -> { u.setEmail("inabe2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-inabe2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 INA " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile =
                saveCf(uBe2, wsBe2, "CFBE2 INA " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — indépendant confirmé. */
    private Map<String, Object> bodyIndependantConfirme() {
        Map<String, Object> m = new HashMap<>();
        m.put("volonteParties", "INDEPENDANT_EXPLICITE");
        m.put("liberteOrganisationTemps", "TOTALE_INDEPENDANT");
        m.put("liberteOrganisationTravail", "TOTALE_INDEPENDANT");
        m.put("controleHierarchique", "AUCUNE_SALARIE");
        m.put("secteur", "AUTRE");
        m.put("nbCriteresSectorielsSalarie", 0);
        m.put("statutDeclare", "INDEPENDANT_INASTI");
        m.put("dimonaPresente", false);
        m.put("autreClientPresent", true);
        return m;
    }

    @Test
    void POST_workspaceBe_independantConfirme_returnsVerdict() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INDEPENDANT_CONFIRME"))
                .andExpect(jsonPath("$.scoreCriteresGenerauxIndependant").value(4))
                .andExpect(jsonPath("$.scoreCriteresGenerauxSalarie").value(0))
                .andExpect(jsonPath("$.requalificationSalariatRecommandee").value(false))
                .andExpect(jsonPath("$.presumptionSectorielleApplicable").value(false))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 27/06/1969")))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("AR n° 38")));
    }

    @Test
    void POST_workspaceBe_salarieRequalifie_returnsVerdict() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("volonteParties", "SALARIE_EXPLICITE");
        body.put("liberteOrganisationTemps", "AUCUNE_SALARIE");
        body.put("liberteOrganisationTravail", "AUCUNE_SALARIE");
        body.put("controleHierarchique", "TOTALE_INDEPENDANT");
        body.put("statutDeclare", "SALARIE_DIMONA");
        body.put("dimonaPresente", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SALARIE_REQUALIFIE"))
                .andExpect(jsonPath("$.scoreCriteresGenerauxSalarie").value(4))
                .andExpect(jsonPath("$.requalificationSalariatRecommandee").value(true));
    }

    @Test
    void POST_workspaceBe_presomptionSectorielleConstruction_returnsVerdict() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("secteur", "CONSTRUCTION");
        body.put("nbCriteresSectorielsSalarie", 5);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE"))
                .andExpect(jsonPath("$.presumptionSectorielleApplicable").value(true))
                .andExpect(jsonPath("$.presumptionSectorielleDeclenchee").value(true))
                .andExpect(jsonPath("$.requalificationSalariatRecommandee").value(true));
    }

    @Test
    void POST_workspaceBe_presomptionGenerale_dimonaAbsente() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("volonteParties", "IMPLICITE_OU_AMBIGUE");
        body.put("liberteOrganisationTemps", "PARTIELLE");
        body.put("liberteOrganisationTravail", "PARTIELLE");
        body.put("controleHierarchique", "PARTIELLE");
        body.put("statutDeclare", "SANS_DECLARATION");
        body.put("dimonaPresente", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("PRESUMPTION_GENERALE_SALARIAT"))
                .andExpect(jsonPath("$.presumptionGeneraleSalariatMobilisable").value(true))
                .andExpect(jsonPath("$.requalificationSalariatRecommandee").value(true));
    }

    @Test
    void POST_workspaceBe_aQualifier_egalite() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("volonteParties", "IMPLICITE_OU_AMBIGUE");
        body.put("liberteOrganisationTemps", "PARTIELLE");
        body.put("liberteOrganisationTravail", "PARTIELLE");
        body.put("controleHierarchique", "PARTIELLE");
        body.put("statutDeclare", "INDEPENDANT_INASTI");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_QUALIFIER"))
                .andExpect(jsonPath("$.scoreCriteresGenerauxIndependant").value(0))
                .andExpect(jsonPath("$.scoreCriteresGenerauxSalarie").value(0));
    }

    @Test
    void POST_workspaceBe_monoClient_drapeauActif() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("autreClientPresent", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monoClientIndiceSalariat").value(true));
    }

    @Test
    void POST_workspaceBe_dimonaCoherenteIndependant() throws Exception {
        // INDEPENDANT_INASTI + dimonaPresente = false → cohérent
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimonaCoherenteAvecStatutDeclare").value(true));
    }

    @Test
    void POST_workspaceBe_dimonaIncoherente_independantAvecDimona() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("dimonaPresente", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dimonaCoherenteAvecStatutDeclare").value(false));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INDEPENDANT_CONFIRME"))
                .andExpect(jsonPath("$.volonteParties").value("INDEPENDANT_EXPLICITE"));
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
    void POST_volontePartiesManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("volonteParties");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_liberteTempsManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("liberteOrganisationTemps");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_liberteTravailManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("liberteOrganisationTravail");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_controleHierarchiqueManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("controleHierarchique");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_secteurManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("secteur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nbCriteresSectorielsNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("nbCriteresSectorielsSalarie", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_nbCriteresSectorielsTropGrand_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.put("nbCriteresSectorielsSalarie", 10);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_statutDeclareManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("statutDeclare");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dimonaPresenteManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("dimonaPresente");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_autreClientPresentManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyIndependantConfirme();
        body.remove("autreClientPresent");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_meme_dossier_deuxFois_upsert() throws Exception {
        // 1er POST
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyIndependantConfirme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("INDEPENDANT_CONFIRME"));

        // 2e POST avec verdict différent → mise à jour
        Map<String, Object> body2 = bodyIndependantConfirme();
        body2.put("volonteParties", "SALARIE_EXPLICITE");
        body2.put("liberteOrganisationTemps", "AUCUNE_SALARIE");
        body2.put("liberteOrganisationTravail", "AUCUNE_SALARIE");
        body2.put("controleHierarchique", "TOTALE_INDEPENDANT");
        body2.put("statutDeclare", "SALARIE_DIMONA");
        body2.put("dimonaPresente", true);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SALARIE_REQUALIFIE"));

        // GET retourne le 2nd verdict (upsert)
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SALARIE_REQUALIFIE"));
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
        Map<String, Object> claims =
                Map.of("sub", sub, "email", email, "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token",
                Instant.now(), Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
