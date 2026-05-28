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
 * SF-219-30 : tests d'intégration de l'endpoint <i>Saisine du
 * Conseiller en Prévention Aspects Psychosociaux (CPAP) — procédure
 * RPS interne BE</i> (Loi du 04/08/1996 art. 32sexies + AR du
 * 10/04/2014).
 *
 * <p>Couvre : POST BE 200 sur l'ensemble des verdicts (SAISINE_CONFORME,
 * SAISINE_INFORMELLE_EN_COURS, SAISINE_FORMELLE_EN_COURS,
 * AVIS_RENDU_DELAI_RESPECTE, AVIS_RENDU_DELAI_DEPASSE,
 * NON_CONFORME_FORMALITES_MANQUANTES, NON_CONFORME_PAS_DE_CONSEILLER),
 * POST FR 404 (gate BE strict), POST caseFile autre workspace 404,
 * POST dossier non DROIT_DU_TRAVAIL 400, GET 404 sans POST, GET retour
 * persisté, validations Bean Validation 400, upsert.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class BienEtreRpsConseillerPreventionControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/bien-etre-rps-conseiller-prevention";

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
                u -> { u.setEmail("rps-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-rps-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE RPS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE RPS " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE RPS FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-rps-be-" + ts, "rps-be-" + ts + "@ex.com");

        User uFr = save(new User(),
                u -> { u.setEmail("rps-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-rps-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR RPS " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR RPS " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-rps-fr-" + ts, "rps-fr-" + ts + "@ex.com");

        User uBe2 = save(new User(),
                u -> { u.setEmail("rps-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-rps-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 RPS " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile =
                saveCf(uBe2, wsBe2, "CFBE2 RPS " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — saisine conforme préparation (entretien préalable réalisé). */
    private Map<String, Object> bodyConformePrep() {
        Map<String, Object> m = new HashMap<>();
        m.put("typeRisque", "HARCELEMENT_MORAL");
        m.put("modeDemande", "FORMELLE_INDIVIDUELLE");
        m.put("etapeProcedure", "ENTRETIEN_PREALABLE_REALISE");
        m.put("conseillerIdentifie", true);
        m.put("entretienPrealableRealise", true);
        m.put("demandeEcriteSignee", false);
        m.put("accuseReceptionRecu", false);
        m.put("notificationEmployeurFaite", false);
        return m;
    }

    /** Body demande formelle déposée. */
    private Map<String, Object> bodyFormelleDeposee() {
        Map<String, Object> m = new HashMap<>();
        m.put("typeRisque", "HARCELEMENT_SEXUEL");
        m.put("modeDemande", "FORMELLE_INDIVIDUELLE");
        m.put("etapeProcedure", "DEMANDE_FORMELLE_DEPOSEE");
        m.put("conseillerIdentifie", true);
        m.put("entretienPrealableRealise", true);
        m.put("demandeEcriteSignee", true);
        m.put("accuseReceptionRecu", true);
        m.put("notificationEmployeurFaite", true);
        m.put("dateDepotDemande", "2026-01-15");
        m.put("joursDepuisDepot", 20);
        return m;
    }

    @Test
    void POST_workspaceBe_saisineConforme_returnsVerdict() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConformePrep())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_CONFORME"))
                .andExpect(jsonPath("$.formalitesPrealablesCompletes").value(false))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 04/08/1996")))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("AR du 10/04/2014")));
    }

    @Test
    void POST_workspaceBe_informelleEnCours_returnsVerdict() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.put("modeDemande", "INFORMELLE");
        body.put("etapeProcedure", "DEMANDE_INFORMELLE_EN_COURS");
        body.put("typeRisque", "AUTRES_RPS");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("SAISINE_INFORMELLE_EN_COURS"))
                .andExpect(jsonPath("$.protectionRepresaillesActive").value(false))
                .andExpect(jsonPath("$.formalitesPrealablesCompletes").value(true));
    }

    @Test
    void POST_workspaceBe_formelleEnCours_returnsVerdict() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyFormelleDeposee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("SAISINE_FORMELLE_EN_COURS"))
                .andExpect(jsonPath("$.protectionRepresaillesActive").value(true))
                .andExpect(jsonPath("$.formalitesPrealablesCompletes").value(true))
                .andExpect(jsonPath("$.echeanceEnqueteTroisMois").value("2026-04-15"))
                .andExpect(jsonPath("$.finProtectionRepresailles").value("2027-01-15"));
    }

    @Test
    void POST_workspaceBe_avisRenduDelaiRespecte() throws Exception {
        Map<String, Object> body = bodyFormelleDeposee();
        body.put("etapeProcedure", "AVIS_RENDU");
        body.put("dateAvisRendu", "2026-04-10");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("AVIS_RENDU_DELAI_RESPECTE"))
                .andExpect(jsonPath("$.delaiEnqueteRespecte").value(true));
    }

    @Test
    void POST_workspaceBe_avisRenduDelaiDepasse() throws Exception {
        Map<String, Object> body = bodyFormelleDeposee();
        body.put("etapeProcedure", "AVIS_RENDU");
        body.put("dateAvisRendu", "2026-08-15"); // > 6 mois après dépôt
        body.put("joursDepuisDepot", 212);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("AVIS_RENDU_DELAI_DEPASSE"))
                .andExpect(jsonPath("$.delaiEnqueteRespecte").value(false));
    }

    @Test
    void POST_workspaceBe_formalitesManquantes_returnsVerdict() throws Exception {
        Map<String, Object> body = bodyFormelleDeposee();
        body.put("demandeEcriteSignee", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("NON_CONFORME_FORMALITES_MANQUANTES"))
                .andExpect(jsonPath("$.formalitesPrealablesCompletes").value(false));
    }

    @Test
    void POST_workspaceBe_pasDeConseiller_returnsVerdict() throws Exception {
        Map<String, Object> body = bodyFormelleDeposee();
        body.put("conseillerIdentifie", false);

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("NON_CONFORME_PAS_DE_CONSEILLER"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConformePrep())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConformePrep())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConformePrep())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyFormelleDeposee())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_FORMELLE_EN_COURS"))
                .andExpect(jsonPath("$.typeRisque").value("HARCELEMENT_SEXUEL"));
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
    void POST_typeRisqueManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("typeRisque");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_modeDemandeManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("modeDemande");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_etapeProcedureManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("etapeProcedure");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_conseillerIdentifieManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("conseillerIdentifie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_entretienPrealableManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("entretienPrealableRealise");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_demandeEcriteManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("demandeEcriteSignee");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_accuseReceptionManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("accuseReceptionRecu");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_notificationEmployeurManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.remove("notificationEmployeurFaite");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_joursDepuisDepotNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyConformePrep();
        body.put("joursDepuisDepot", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateDepotManquante_pour_etape_formelle_returns400() throws Exception {
        Map<String, Object> body = bodyFormelleDeposee();
        body.remove("dateDepotDemande");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_meme_dossier_deuxFois_upsert() throws Exception {
        // 1er POST — conforme préparation
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyConformePrep())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("SAISINE_CONFORME"));

        // 2e POST avec verdict différent → mise à jour
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyFormelleDeposee())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("SAISINE_FORMELLE_EN_COURS"));

        // GET retourne le 2nd verdict (upsert)
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("SAISINE_FORMELLE_EN_COURS"));
    }

    // ---- helpers ----

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
