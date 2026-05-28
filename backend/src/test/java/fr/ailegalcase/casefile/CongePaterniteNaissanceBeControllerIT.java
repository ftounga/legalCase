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
 * SF-219-31 : tests d'intégration de l'endpoint <i>Congé paternité /
 * naissance BE</i> (Loi du 03/07/1978 art. 30 § 2 + Loi du 12/08/2000
 * + Loi du 07/04/2023 Deal pour l'emploi).
 *
 * <p>Couvre : POST BE 200 sur l'ensemble des verdicts
 * (ELIGIBLE_CONGE_OUVERT, CONGE_EN_COURS_PROTECTION_ACTIVE,
 * CONGE_PRIS_PROTECTION_RESIDUELLE, INELIGIBLE_STATUT_NON_COUVERT,
 * INELIGIBLE_FILIATION_NON_ETABLIE, DROIT_PERDU_DELAI_DEPASSE,
 * INELIGIBLE_NAISSANCE_FUTURE), POST FR 404 (gate BE strict), POST
 * caseFile autre workspace 404, POST dossier non DROIT_DU_TRAVAIL 400,
 * GET 404 sans POST, GET retour persisté, validations Bean Validation
 * 400, upsert.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
@AutoConfigureMockMvc
class CongePaterniteNaissanceBeControllerIT {

    private static final String URL =
            "/api/v1/case-files/%s/decision-tools/conge-paternite-naissance-be";

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
                u -> { u.setEmail("cpn-be-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe, "g-cpn-be-" + ts);
        Workspace wsBe = saveWs(uBe, "WSBE CPN " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe, wsBe);
        beCaseFile = saveCf(uBe, wsBe, "CFBE CPN " + ts, "DROIT_DU_TRAVAIL");
        beFamilleCaseFile = saveCf(uBe, wsBe, "CFBE CPN FAM " + ts, "DROIT_DE_LA_FAMILLE");
        authBe = buildAuth("g-cpn-be-" + ts, "cpn-be-" + ts + "@ex.com");

        User uFr = save(new User(),
                u -> { u.setEmail("cpn-fr-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uFr, "g-cpn-fr-" + ts);
        Workspace wsFr = saveWs(uFr, "WSFR CPN " + ts, "DROIT_DU_TRAVAIL", "FRANCE");
        saveMember(uFr, wsFr);
        frCaseFile = saveCf(uFr, wsFr, "CFFR CPN " + ts, "DROIT_DU_TRAVAIL");
        authFr = buildAuth("g-cpn-fr-" + ts, "cpn-fr-" + ts + "@ex.com");

        User uBe2 = save(new User(),
                u -> { u.setEmail("cpn-be2-" + ts + "@ex.com"); u.setStatus("ACTIVE"); });
        saveAuth(uBe2, "g-cpn-be2-" + ts);
        Workspace wsBe2 = saveWs(uBe2, "WSBE2 CPN " + ts, "DROIT_DU_TRAVAIL", "BELGIQUE");
        saveMember(uBe2, wsBe2);
        beOtherWorkspaceCaseFile =
                saveCf(uBe2, wsBe2, "CFBE2 CPN " + ts, "DROIT_DU_TRAVAIL");
    }

    /** Body nominal — éligibilité ouverte naissance 2026. */
    private Map<String, Object> bodyEligibleOuvert() {
        Map<String, Object> m = new HashMap<>();
        m.put("statutTravailleur", "SALARIE");
        m.put("lienFiliation", "PERE_BIOLOGIQUE");
        m.put("etapeProcedure", "NAISSANCE_SURVENUE");
        m.put("filiationEtablie", true);
        m.put("contratTravailEnCours", true);
        m.put("notificationEmployeurFaite", false);
        m.put("dateNaissance", "2026-01-15");
        m.put("joursDejaPrisOuvrables", 0);
        return m;
    }

    /** Body congé en cours, 5 jours pris. */
    private Map<String, Object> bodyCongeEnCours() {
        Map<String, Object> m = new HashMap<>();
        m.put("statutTravailleur", "SALARIE");
        m.put("lienFiliation", "COPARENT_COMATERNITE");
        m.put("etapeProcedure", "CONGE_EN_COURS_DE_PRISE");
        m.put("filiationEtablie", true);
        m.put("contratTravailEnCours", true);
        m.put("notificationEmployeurFaite", true);
        m.put("dateNaissance", "2026-01-15");
        m.put("dateNotificationEmployeur", "2026-01-20");
        m.put("joursDejaPrisOuvrables", 5);
        return m;
    }

    @Test
    void POST_workspaceBe_eligibleOuvert_returnsVerdict() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleOuvert())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_CONGE_OUVERT"))
                .andExpect(jsonPath("$.dureeApplicableJoursOuvrables").value(20))
                .andExpect(jsonPath("$.joursRestantsAPrendre").value(20))
                .andExpect(jsonPath("$.echeanceQuatreMoisPostNaissance")
                        .value("2026-05-15"))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 03/07/1978")))
                .andExpect(jsonPath("$.baseJuridique",
                        org.hamcrest.Matchers.containsString("Loi du 07/04/2023")));
    }

    @Test
    void POST_workspaceBe_congeEnCours_returnsVerdict() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCongeEnCours())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("CONGE_EN_COURS_PROTECTION_ACTIVE"))
                .andExpect(jsonPath("$.protectionLicenciementActive").value(true))
                .andExpect(jsonPath("$.indemniteEmployeurJoursCent").value(3))
                .andExpect(jsonPath("$.indemniteMutuelleJoursQuatreVingtDeux").value(2));
    }

    @Test
    void POST_workspaceBe_congePris_returnsVerdict() throws Exception {
        Map<String, Object> body = bodyCongeEnCours();
        body.put("etapeProcedure", "CONGE_PRIS_ENTIEREMENT");
        body.put("joursDejaPrisOuvrables", 20);
        body.put("dateFinPriseEffective", "2026-02-28");

        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("CONGE_PRIS_PROTECTION_RESIDUELLE"))
                .andExpect(jsonPath("$.joursRestantsAPrendre").value(0))
                .andExpect(jsonPath("$.finProtectionLicenciement")
                        .value("2026-07-28"));
    }

    @Test
    void POST_workspaceBe_naissance2022_quinzeJours() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("dateNaissance", "2022-06-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeApplicableJoursOuvrables").value(15));
    }

    @Test
    void POST_workspaceBe_naissance2020_dixJours() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("dateNaissance", "2020-06-01");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dureeApplicableJoursOuvrables").value(10));
    }

    @Test
    void POST_workspaceBe_statutIndependant_ineligible() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("statutTravailleur", "INDEPENDANT");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_STATUT_NON_COUVERT"));
    }

    @Test
    void POST_workspaceBe_filiationNonEtablie_ineligible() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("filiationEtablie", false);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_FILIATION_NON_ETABLIE"));
    }

    @Test
    void POST_workspaceBe_delaiDepasse_droitPerdu() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("etapeProcedure", "DELAI_QUATRE_MOIS_DEPASSE");
        body.put("joursDejaPrisOuvrables", 5);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("DROIT_PERDU_DELAI_DEPASSE"))
                .andExpect(jsonPath("$.joursRestantsAPrendre").value(15));
    }

    @Test
    void POST_workspaceBe_avantNaissance_calculIndicatif() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("etapeProcedure", "AVANT_NAISSANCE");
        body.remove("dateNaissance");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("INELIGIBLE_NAISSANCE_FUTURE"));
    }

    @Test
    void POST_workspaceFr_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, frCaseFile.getId()))
                        .with(authentication(authFr))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleOuvert())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_caseFileAutreWorkspace_returns404() throws Exception {
        mockMvc.perform(post(String.format(URL, beOtherWorkspaceCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleOuvert())))
                .andExpect(status().isNotFound());
    }

    @Test
    void POST_dossierPasDroitDuTravail_returns400() throws Exception {
        mockMvc.perform(post(String.format(URL, beFamilleCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleOuvert())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_apresPost_returnsPersisted() throws Exception {
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCongeEnCours())))
                .andExpect(status().isOk());

        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("CONGE_EN_COURS_PROTECTION_ACTIVE"))
                .andExpect(jsonPath("$.lienFiliation").value("COPARENT_COMATERNITE"));
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
    void POST_statutTravailleurManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("statutTravailleur");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_lienFiliationManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("lienFiliation");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_etapeProcedureManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("etapeProcedure");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_filiationEtablieManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("filiationEtablie");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_contratTravailEnCoursManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("contratTravailEnCours");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_notificationEmployeurManquant_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("notificationEmployeurFaite");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_joursDejaPrisNegatif_returns400_beanValidation() throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.put("joursDejaPrisOuvrables", -1);
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_dateNaissanceManquante_pour_etape_post_naissance_returns400()
            throws Exception {
        Map<String, Object> body = bodyEligibleOuvert();
        body.remove("dateNaissance");
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void POST_meme_dossier_deuxFois_upsert() throws Exception {
        // 1er POST — éligibilité ouverte
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyEligibleOuvert())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value("ELIGIBLE_CONGE_OUVERT"));

        // 2e POST avec verdict différent → mise à jour
        mockMvc.perform(post(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bodyCongeEnCours())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("CONGE_EN_COURS_PROTECTION_ACTIVE"));

        // GET retourne le 2nd verdict (upsert)
        mockMvc.perform(get(String.format(URL, beCaseFile.getId()))
                        .with(authentication(authBe)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict")
                        .value("CONGE_EN_COURS_PROTECTION_ACTIVE"));
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
