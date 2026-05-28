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
 * SF-219-16 : tests d'intégration de l'endpoint <i>télétravail BE — CCT
 * n° 85 (structurel) / CCT n° 149 (occasionnel)</i>.
 *
 * <p>Couvre : POST BE 200 (verdicts CONFORME_CCT_85_STRUCTUREL /
 * CONFORME_CCT_149_OCCASIONNEL / NON_CONFORME_CONVENTION_ECRITE_MANQUANTE
 * / NON_CONFORME_EQUIPEMENT_NON_FOURNI / NON_CONFORME_DROITS_REDUITS
 * / FRAGILE_DECONNEXION_NON_DEFINIE / A_ANALYSER), POST FR 404 (gate
 * BE strict), POST caseFile autre workspace 404, POST dossier non
 * DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour persisté,
 * validations Bean Validation 400 (champs requis et bornes).</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class TeletravailBeCct85149ControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/teletravail-be-cct-85-149";

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
        User uBe = save(new User(), u -> { u.setEmail("ttv-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-ttv-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE TTV " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE TTV " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-ttv-be-" + ts, "ttv-be-" + ts + "@ex.com");

        // Workspace FR DROIT_DU_TRAVAIL → gate BE → 404
        User uFr = save(new User(), u -> { u.setEmail("ttv-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-ttv-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR TTV " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR TTV " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-ttv-fr-" + ts, "ttv-fr-" + ts + "@ex.com");

        // Workspace BE bis → isolation workspace
        User uBe2 = save(new User(), u -> { u.setEmail("ttv-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-ttv-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 TTV " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile = saveCf(uBe2, wsBe2, "CFBE2 TTV " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — télétravail structurel CCT n° 85 conforme. */
    private Map<String, Object> bodyStructurelConforme() {
        Map<String, Object> m = new HashMap<>();
        m.put("dateDebutTeletravail", "2024-06-01");
        m.put("typeTeletravail", "STRUCTUREL_CCT_85");
        m.put("volontariatReciproque", true);
        m.put("conventionEcriteIndividuelle", true);
        m.put("equipementFourniOuIndemnise", true);
        m.put("indemniteForfaitaireMensuelleEuros", 30.00);
        m.put("plafondIndemniteMensuelleEuros", 154.74);
        m.put("droitsSociauxMaintenus", true);
        m.put("deconnexionDefinie", true);
        m.put("effectifEntreprise", 10);
        return m;
    }

    @Test
    void POST_workspaceBe_structurelConforme_returns200AndPersists() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStructurelConforme())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_CCT_85_STRUCTUREL"))
                .andExpect(jsonPath("$.conventionRespectee").value(true))
                .andExpect(jsonPath("$.equipementRespecte").value(true))
                .andExpect(jsonPath("$.droitsRespectes").value(true))
                .andExpect(jsonPath("$.deconnexionRespectee").value(true))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("CCT n° 85")));
    }

    @Test
    void POST_workspaceBe_occasionnelConforme_returnsCct149() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("typeTeletravail", "OCCASIONNEL_CCT_149");
        body.put("conventionEcriteIndividuelle", false);
        body.put("equipementFourniOuIndemnise", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_CCT_149_OCCASIONNEL"))
                .andExpect(jsonPath("$.raison").value("CONFORME_OCCASIONNEL"));
    }

    @Test
    void POST_workspaceBe_typeIndetermine_returnsAanalyser() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("typeTeletravail", "INDETERMINE");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("A_ANALYSER"))
                .andExpect(jsonPath("$.raison").value("TYPE_TELETRAVAIL_INDETERMINE"));
    }

    @Test
    void POST_workspaceBe_conventionEcriteManquante_returnsConventionManquante() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("conventionEcriteIndividuelle", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_CONVENTION_ECRITE_MANQUANTE"))
                .andExpect(jsonPath("$.conventionRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_equipementNonFourni_returnsEquipementNonFourni() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("equipementFourniOuIndemnise", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_EQUIPEMENT_NON_FOURNI"))
                .andExpect(jsonPath("$.equipementRespecte").value(false));
    }

    @Test
    void POST_workspaceBe_droitsReduits_returnsDroitsReduits() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("droitsSociauxMaintenus", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("NON_CONFORME_DROITS_REDUITS"))
                .andExpect(jsonPath("$.droitsRespectes").value(false));
    }

    @Test
    void POST_workspaceBe_deconnexionNonDefinie30Travailleurs_returnsFragileDeconnexion() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("deconnexionDefinie", false);
        body.put("effectifEntreprise", 30);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("FRAGILE_DECONNEXION_NON_DEFINIE"))
                .andExpect(jsonPath("$.deconnexionRespectee").value(false));
    }

    @Test
    void POST_workspaceBe_indemniteExcessive_conformeAvecExcedent() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("indemniteForfaitaireMensuelleEuros", 200.00);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_CCT_85_STRUCTUREL"))
                .andExpect(jsonPath("$.indemniteExcedentaire").value(45.26));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStructurelConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStructurelConforme())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStructurelConforme())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyStructurelConforme())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("CONFORME_CCT_85_STRUCTUREL"))
                .andExpect(jsonPath("$.typeTeletravail").value("STRUCTUREL_CCT_85"));
    }

    @Test
    void GET_sansPost_returns404() throws Exception {
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dateManquante_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.remove("dateDebutTeletravail");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_typeManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.remove("typeTeletravail");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_indemniteNegative_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("indemniteForfaitaireMensuelleEuros", -1.00);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_plafondZero_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("plafondIndemniteMensuelleEuros", 0.0);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_effectifNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyStructurelConforme();
        body.put("effectifEntreprise", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    // ---- helpers (alignés sur InterimBeCct322ControllerIT) ----

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
